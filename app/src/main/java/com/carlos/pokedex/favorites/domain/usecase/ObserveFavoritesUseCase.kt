package com.carlos.pokedex.favorites.domain.usecase

import com.carlos.pokedex.favorites.domain.repository.IFavoritePokemonRepository

class ObserveFavoritesUseCase(private val repository: IFavoritePokemonRepository) {
    operator fun invoke() = repository.observeFavorites()
}
