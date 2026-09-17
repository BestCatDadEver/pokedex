package com.carlos.pokedex.dashboard.domain.usecase

import com.carlos.pokedex.dashboard.domain.repository.IPokemonRepository

class SearchPokemonUseCase(private val repository: IPokemonRepository) {
    suspend operator fun invoke(query: String, limit: Int) = repository.search(query, limit)
}
