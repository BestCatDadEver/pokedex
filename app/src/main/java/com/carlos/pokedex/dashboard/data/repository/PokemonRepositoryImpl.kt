package com.carlos.pokedex.dashboard.data.repository

import com.carlos.pokedex.core.network.PokedexService
import com.carlos.pokedex.core.network.Resource
import com.carlos.pokedex.core.network.Resource.Success
import com.carlos.pokedex.core.network.toDomain
import com.carlos.pokedex.dashboard.domain.model.Pokemon
import com.carlos.pokedex.dashboard.domain.repository.IPokemonRepository

class PokemonRepositoryImpl(private val apiService: PokedexService) : IPokemonRepository {
    override suspend fun getAll(): Resource<List<Pokemon>> {
        return apiService.getAll().let { response ->
            if (response.isSuccessful) {
                Success(response.body()?.results!!.map { it.toDomain() })
            } else {
                Resource.Error(response.message())
            }

        }
    }

}
