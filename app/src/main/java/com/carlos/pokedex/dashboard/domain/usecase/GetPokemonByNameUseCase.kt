package com.carlos.pokedex.dashboard.domain.usecase

import com.carlos.pokedex.core.network.Resource
import com.carlos.pokedex.dashboard.domain.model.Pokemon
import com.carlos.pokedex.dashboard.domain.repository.IPokemonRepository

class GetPokemonByNameUseCase(
    private val repository: IPokemonRepository
) {

    suspend operator fun invoke(name: String ) = repository.getByName(name)

}

