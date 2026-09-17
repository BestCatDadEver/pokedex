package com.carlos.pokedex.dashboard.domain.usecase

import com.carlos.pokedex.dashboard.domain.repository.IPokemonRepository

class GetPagedPokemonUseCase(private val repository: IPokemonRepository) {
    operator fun invoke(pageSize: Int) = repository.pagedPokemon(pageSize)
}
