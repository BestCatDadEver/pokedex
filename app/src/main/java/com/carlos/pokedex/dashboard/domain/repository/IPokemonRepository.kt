package com.carlos.pokedex.dashboard.domain.repository

import com.carlos.pokedex.core.network.Resource
import com.carlos.pokedex.dashboard.domain.model.Pokemon
import com.carlos.pokedex.dashboard.domain.model.PokemonDetails

interface IPokemonRepository {

    suspend fun getAll(limit: Int, offset: Int): Resource<List<Pokemon>>
    suspend fun getByName(name: String): Resource<PokemonDetails>
}
