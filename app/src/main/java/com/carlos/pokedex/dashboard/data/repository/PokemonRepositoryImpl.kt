package com.carlos.pokedex.dashboard.data.repository

import android.util.Log
import com.carlos.pokedex.core.network.PokedexService
import com.carlos.pokedex.core.network.Resource
import com.carlos.pokedex.core.network.Resource.Success
import com.carlos.pokedex.core.network.toDomain
import com.carlos.pokedex.dashboard.data.local.pokemon.PokemonDao
import com.carlos.pokedex.dashboard.data.local.pokemon.PokemonEntity
import com.carlos.pokedex.dashboard.data.local.pokemon.toDomain
import com.carlos.pokedex.dashboard.data.local.pokemon.toEntity
import com.carlos.pokedex.dashboard.domain.model.Pokemon
import com.carlos.pokedex.dashboard.domain.model.PokemonDetails
import com.carlos.pokedex.dashboard.domain.repository.IPokemonRepository

private const val TAG = "PokemonRepository"

/**
 * La lista de nombres es barata (un solo request para toda la pokédex) y es lo que permite
 * buscar por coincidencia parcial sin pegarle a la red en cada tecla.
 */
private const val NAME_INDEX_LIMIT = 100_000

class PokemonRepositoryImpl(
    private val apiService: PokedexService,
    private val pokemonDao: PokemonDao
) : IPokemonRepository {

    private var nameIndexSynced = false

    override suspend fun getAll(limit: Int, offset: Int): Resource<List<Pokemon>> {
        val cachedPage = pokemonDao.getPage(limit, offset)
        if (cachedPage.size == limit) {
            Log.d(TAG, "getAll() offset=$offset served from cache")
            return Success(cachedPage.toDomainWithDetails())
        }

        return try {
            apiService.getAll(limit, offset).let { response ->
                Log.d(TAG, "getAll() -> code=${response.code()}, successful=${response.isSuccessful}")
                if (response.isSuccessful) {
                    val results = response.body()?.results
                    Log.d(TAG, "getAll() body=${response.body()}, resultsCount=${results?.size}")
                    if (results.isNullOrEmpty()) {
                        getCachedPage(limit, offset, "Empty response body")
                    } else {
                        val pokemons = results.map { it.toDomain() }
                        pokemonDao.insertAll(pokemons.mapIndexed { index, pokemon -> pokemon.toEntity(offset + index) })
                        Success(pokemons)
                    }
                } else {
                    Log.e(TAG, "getAll() error=${response.errorBody()?.string()}")
                    getCachedPage(limit, offset, response.message())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getAll() network failure", e)
            getCachedPage(limit, offset, e.message ?: "Network error")
        }
    }

    private suspend fun getCachedPage(limit: Int, offset: Int, errorMessage: String): Resource<List<Pokemon>> {
        val cached = pokemonDao.getPage(limit, offset)
        Log.d(TAG, "getCachedPage() offset=$offset cachedCount=${cached.size}")
        return if (cached.isNotEmpty()) {
            Success(cached.toDomainWithDetails())
        } else {
            Resource.Error(errorMessage)
        }
    }

    private suspend fun List<PokemonEntity>.toDomainWithDetails(): List<Pokemon> =
        map { entity -> entity.toDomain(pokemonDao.getDetailsByName(entity.name)?.toDomain()) }

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

    override suspend fun search(query: String, limit: Int): Resource<List<Pokemon>> {
        ensureNameIndex()

        // Los nombres de la pokéapi nunca traen comodines de SQL, así que descartarlos evita
        // que un "%" escrito por el usuario se interprete como patrón.
        val sanitized = query.trim().replace("%", "").replace("_", "")
        if (sanitized.isEmpty()) return Success(emptyList())

        return try {
            val matches = pokemonDao.searchByName(sanitized, limit)
            Log.d(TAG, "search($sanitized) matches=${matches.size}")
            Success(matches.map { it.toDomain() })
        } catch (e: Exception) {
            Log.e(TAG, "search($sanitized) failed", e)
            Resource.Error(e.message ?: "Search error")
        }
    }

    /**
     * Descarga el índice completo de nombres una vez por proceso. Si falla (sin conexión), la
     * búsqueda sigue funcionando sobre lo que ya haya en caché.
     */
    private suspend fun ensureNameIndex() {
        if (nameIndexSynced) return

        runCatching {
            val response = apiService.getAll(NAME_INDEX_LIMIT, 0)
            if (!response.isSuccessful) {
                Log.e(TAG, "ensureNameIndex() error code=${response.code()}")
                return@runCatching
            }
            val results = response.body()?.results.orEmpty()
            if (results.isEmpty()) return@runCatching

            val pokemons = results.map { it.toDomain() }
            pokemonDao.insertAll(pokemons.mapIndexed { index, pokemon -> pokemon.toEntity(index) })
            nameIndexSynced = true
            Log.d(TAG, "ensureNameIndex() cached ${pokemons.size} names")
        }.onFailure { e ->
            Log.e(TAG, "ensureNameIndex() network failure", e)
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
