package com.carlos.pokedex.favorites.domain.usecase

import com.carlos.pokedex.dashboard.domain.model.Pokemon
import com.carlos.pokedex.favorites.domain.repository.IFavoritePokemonRepository

class AddFavoriteUseCase(private val repository: IFavoritePokemonRepository) {
    suspend operator fun invoke(pokemon: Pokemon) = repository.addFavorite(pokemon)
}
