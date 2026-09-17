package com.carlos.pokedex.dashboard.domain.repository

import androidx.paging.PagingData
import com.carlos.pokedex.core.network.Resource
import com.carlos.pokedex.dashboard.domain.model.Pokemon
import com.carlos.pokedex.dashboard.domain.model.PokemonDetails
import kotlinx.coroutines.flow.Flow

interface IPokemonRepository {

    fun pagedPokemon(pageSize: Int): Flow<PagingData<Pokemon>>
    suspend fun getByName(name: String): Resource<PokemonDetails>
    suspend fun search(query: String, limit: Int): Resource<List<Pokemon>>
}
