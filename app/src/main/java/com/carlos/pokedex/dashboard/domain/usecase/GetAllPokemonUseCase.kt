package com.carlos.pokedex.dashboard.domain.usecase

import com.carlos.pokedex.dashboard.domain.repository.IPokemonRepository

class GetAllPokemonUseCase(private val repository: IPokemonRepository) {
    suspend operator fun invoke() = repository.getAll()

}