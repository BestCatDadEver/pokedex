package com.carlos.pokedex.dashboard.data.paging

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.carlos.pokedex.core.database.AppDatabase
import com.carlos.pokedex.core.network.PokedexService
import com.carlos.pokedex.core.network.toDomain
import com.carlos.pokedex.dashboard.data.local.pokemon.PokemonDao
import com.carlos.pokedex.dashboard.data.local.pokemon.PokemonEntity
import com.carlos.pokedex.dashboard.data.local.pokemon.toEntity
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "PokemonRemoteMediator"

/**
 * Room es la única fuente de verdad de la lista: el mediator solo se encarga de rellenarla desde
 * la red cuando Paging detecta que se está por acabar lo que hay en caché.
 */
@OptIn(ExperimentalPagingApi::class)
class PokemonRemoteMediator(
    private val apiService: PokedexService,
    private val database: AppDatabase,
    private val pokemonDao: PokemonDao
) : RemoteMediator<Int, PokemonEntity>() {

    /** El catálogo de la pokéapi es inmutable, así que no vale la pena rearmarlo en cada arranque. */
    override suspend fun initialize(): InitializeAction =
        if (pokemonDao.count() > 0) {
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PokemonEntity>
    ): MediatorResult {
        val offset = when (loadType) {
            LoadType.REFRESH -> 0

            // La pokéapi solo pagina hacia adelante.
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)

            // orderIndex es la posición absoluta del pokémon dentro del catálogo, así que ya hace
            // de remote key y no hace falta una tabla aparte para guardarlas.
            LoadType.APPEND -> {
                val lastIndex = pokemonDao.maxOrderIndex()
                    ?: return MediatorResult.Success(endOfPaginationReached = true)
                lastIndex + 1
            }
        }

        return try {
            val response = apiService.getAll(state.config.pageSize, offset)
            if (!response.isSuccessful) {
                Log.e(TAG, "load($loadType) offset=$offset code=${response.code()}")
                return MediatorResult.Error(HttpException(response))
            }

            val body = response.body()
                ?: return MediatorResult.Success(endOfPaginationReached = true)
            val pokemons = body.results.map { it.toDomain() }
            Log.d(TAG, "load($loadType) offset=$offset received=${pokemons.size}")

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    pokemonDao.clearAll()
                }
                pokemonDao.insertAll(
                    pokemons.mapIndexed { index, pokemon -> pokemon.toEntity(offset + index) }
                )
            }

            MediatorResult.Success(endOfPaginationReached = body.next == null)
        } catch (e: IOException) {
            Log.e(TAG, "load($loadType) network failure", e)
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            Log.e(TAG, "load($loadType) http failure", e)
            MediatorResult.Error(e)
        }
    }
}
