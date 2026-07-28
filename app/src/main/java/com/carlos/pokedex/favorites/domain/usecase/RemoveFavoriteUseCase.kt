package com.carlos.pokedex.favorites.domain.usecase

import com.carlos.pokedex.favorites.domain.repository.IFavoritePokemonRepository

class RemoveFavoriteUseCase(private val repository: IFavoritePokemonRepository) {
    suspend operator fun invoke(id: String) = repository.removeFavorite(id)
}
