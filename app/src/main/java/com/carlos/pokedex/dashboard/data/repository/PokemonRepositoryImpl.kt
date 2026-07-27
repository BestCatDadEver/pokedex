package com.carlos.pokedex.dashboard.data.repository

import android.util.Log
import com.carlos.pokedex.core.network.PokedexService
import com.carlos.pokedex.core.network.Resource
import com.carlos.pokedex.core.network.Resource.Success
import com.carlos.pokedex.core.network.toDomain
import com.carlos.pokedex.dashboard.data.local.pokemon.PokemonDao
import com.carlos.pokedex.dashboard.data.local.pokemon.toDomain
import com.carlos.pokedex.dashboard.data.local.pokemon.toEntity
import com.carlos.pokedex.dashboard.domain.model.Pokemon
import com.carlos.pokedex.dashboard.domain.model.PokemonDetails
import com.carlos.pokedex.dashboard.domain.repository.IPokemonRepository

private const val TAG = "PokemonRepository"

class PokemonRepositoryImpl(
    private val apiService: PokedexService,
    private val pokemonDao: PokemonDao
) : IPokemonRepository {

    override suspend fun getAll(limit: Int, offset: Int): Resource<List<Pokemon>> {
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
            Success(cached.map { entity -> entity.toDomain(pokemonDao.getDetailsByName(entity.name)?.toDomain()) })
        } else {
            Resource.Error(errorMessage)
        }
    }

    override suspend fun getByName(name: String): Resource<PokemonDetails> {
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
