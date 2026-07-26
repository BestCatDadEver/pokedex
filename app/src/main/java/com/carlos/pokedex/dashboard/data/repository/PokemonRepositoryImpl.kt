package com.carlos.pokedex.dashboard.data.repository

import android.util.Log
import com.carlos.pokedex.core.network.PokedexService
import com.carlos.pokedex.core.network.Resource
import com.carlos.pokedex.core.network.Resource.Success
import com.carlos.pokedex.core.network.toDomain
import com.carlos.pokedex.dashboard.domain.model.Pokemon
import com.carlos.pokedex.dashboard.domain.repository.IPokemonRepository

private const val TAG = "PokemonRepository"

class PokemonRepositoryImpl(private val apiService: PokedexService) : IPokemonRepository {
    override suspend fun getAll(): Resource<List<Pokemon>> {
        return apiService.getAll().let { response ->
            Log.d(TAG, "getAll() -> code=${response.code()}, successful=${response.isSuccessful}")
            if (response.isSuccessful) {
                val results = response.body()?.results
                Log.d(TAG, "getAll() body=${response.body()}, resultsCount=${results?.size}")
                if (results.isNullOrEmpty()) {
                    Resource.Error("Empty response body")
                } else {
                    Success(results.map { it.toDomain() })
                }
            } else {
                Log.e(TAG, "getAll() error=${response.errorBody()?.string()}")
                Resource.Error(response.message())
            }

        }
    }

}
