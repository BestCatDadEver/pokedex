package com.carlos.pokedex.favorites.domain.usecase

import com.carlos.pokedex.favorites.domain.repository.IFavoritePokemonRepository

class IsFavoriteUseCase(private val repository: IFavoritePokemonRepository) {
    operator fun invoke(id: String) = repository.isFavorite(id)
}
