package com.carlos.pokedex.dashboard.data.repository

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.carlos.pokedex.core.database.AppDatabase
import com.carlos.pokedex.core.network.PokedexService
import com.carlos.pokedex.core.network.Resource
import com.carlos.pokedex.core.network.Resource.Success
import com.carlos.pokedex.core.network.toDomain
import com.carlos.pokedex.dashboard.data.local.pokemon.PokemonDao
import com.carlos.pokedex.dashboard.data.local.pokemon.toDomain
import com.carlos.pokedex.dashboard.data.local.pokemon.toEntity
import com.carlos.pokedex.dashboard.data.local.pokemon.toIndexEntity
import com.carlos.pokedex.dashboard.data.paging.PokemonRemoteMediator
import com.carlos.pokedex.dashboard.domain.model.Pokemon
import com.carlos.pokedex.dashboard.domain.model.PokemonDetails
import com.carlos.pokedex.dashboard.domain.repository.IPokemonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val TAG = "PokemonRepository"

/**
 * El catálogo entero cabe en un solo request, así que el índice de búsqueda se baja de una vez en
 * lugar de ir a la red en cada tecla.
 */
private const val NAME_INDEX_LIMIT = 100_000

class PokemonRepositoryImpl(
    private val apiService: PokedexService,
    private val database: AppDatabase,
    private val pokemonDao: PokemonDao
) : IPokemonRepository {

    private var nameIndexSynced = false

    @OptIn(ExperimentalPagingApi::class)
    override fun pagedPokemon(pageSize: Int): Flow<PagingData<Pokemon>> = Pager(
        config = PagingConfig(
            pageSize = pageSize,
            // Sin esto Paging pide 3 páginas de arranque y el mediator terminaría pidiendo a la
            // red un tamaño distinto al que Paging espera de vuelta.
            initialLoadSize = pageSize,
            prefetchDistance = pageSize / 2
        ),
        remoteMediator = PokemonRemoteMediator(apiService, database, pokemonDao),
        pagingSourceFactory = { pokemonDao.pagingSource() }
    ).flow.map { pagingData -> pagingData.map { entity -> entity.toDomain() } }

    override suspend fun search(query: String, limit: Int): Resource<List<Pokemon>> {
        ensureNameIndex()

        // Los nombres de la pokéapi nunca traen comodines de SQL, así que descartarlos evita que
        // un "%" escrito por el usuario se interprete como patrón.
        val sanitized = query.trim().replace("%", "").replace("_", "")
        if (sanitized.isEmpty()) return Success(emptyList())

        return try {
            val matches = pokemonDao.searchIndex(sanitized, limit)
            Log.d(TAG, "search($sanitized) matches=${matches.size}")
            Success(matches.map { it.toDomain() })
        } catch (e: Exception) {
            Log.e(TAG, "search($sanitized) failed", e)
            Resource.Error(e.message ?: "Search error")
        }
    }

    /**
     * Descarga el índice completo de nombres una vez por proceso. Si falla por falta de conexión,
     * la búsqueda sigue respondiendo con lo que ya haya en caché.
     */
    private suspend fun ensureNameIndex() {
        if (nameIndexSynced) return
        if (pokemonDao.indexCount() > 0) {
            nameIndexSynced = true
            return
        }

        runCatching {
            val response = apiService.getAll(NAME_INDEX_LIMIT, 0)
            if (!response.isSuccessful) {
                Log.e(TAG, "ensureNameIndex() error code=${response.code()}")
                return@runCatching
            }
            val results = response.body()?.results.orEmpty()
            if (results.isEmpty()) return@runCatching

            val pokemons = results.map { it.toDomain() }
            pokemonDao.insertIndex(
                pokemons.mapIndexed { index, pokemon -> pokemon.toIndexEntity(index) }
            )
            nameIndexSynced = true
            Log.d(TAG, "ensureNameIndex() cached ${pokemons.size} names")
        }.onFailure { e ->
            Log.e(TAG, "ensureNameIndex() network failure", e)
        }
    }

    override suspend fun getByName(name: String): Resource<PokemonDetails> {
        pokemonDao.getDetailsByName(name)?.let { cached ->
            Log.d(TAG, "getByName($name) served from cache")
            return Success(cached.toDomain())
        }

        return try {
            apiService.getByName(name).let { response ->
                Log.d(TAG, "getByName($name) -> code=${response.code()}, successful=${response.isSuccessful}")
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result == null) {
                        getCachedDetails(name, "Empty response body")
                    } else {
                        val details = result.toDomain()
                        pokemonDao.insertDetails(details.toEntity())
                        Success(details)
                    }
                } else {
                    Log.e(TAG, "getByName($name) error=${response.errorBody()?.string()}")
                    getCachedDetails(name, response.message())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getByName($name) network failure", e)
            getCachedDetails(name, e.message ?: "Network error")
        }
    }

    private suspend fun getCachedDetails(name: String, errorMessage: String): Resource<PokemonDetails> {
        val cached = pokemonDao.getDetailsByName(name)
        return if (cached != null) {
            Success(cached.toDomain())
        } else {
            Resource.Error(errorMessage)
        }
    }
}
