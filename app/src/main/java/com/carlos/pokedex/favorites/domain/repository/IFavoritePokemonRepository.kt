package com.carlos.pokedex.favorites.domain.repository

import com.carlos.pokedex.dashboard.domain.model.Pokemon
import kotlinx.coroutines.flow.Flow

interface IFavoritePokemonRepository {
    fun observeFavorites(): Flow<List<Pokemon>>
    fun isFavorite(id: String): Flow<Boolean>
    suspend fun addFavorite(pokemon: Pokemon)
    suspend fun removeFavorite(id: String)
}
