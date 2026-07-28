package com.carlos.pokedex.favorites.data.repository

import com.carlos.pokedex.dashboard.domain.model.Pokemon
import com.carlos.pokedex.favorites.data.local.FavoritePokemonDao
import com.carlos.pokedex.favorites.data.local.toDomain
import com.carlos.pokedex.favorites.data.local.toFavoriteEntity
import com.carlos.pokedex.favorites.domain.repository.IFavoritePokemonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoritePokemonRepositoryImpl(
    private val favoritePokemonDao: FavoritePokemonDao
) : IFavoritePokemonRepository {

    override fun observeFavorites(): Flow<List<Pokemon>> =
        favoritePokemonDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun isFavorite(id: String): Flow<Boolean> =
        favoritePokemonDao.observeIsFavorite(id)

    override suspend fun addFavorite(pokemon: Pokemon) {
        favoritePokemonDao.insert(pokemon.toFavoriteEntity())
    }

    override suspend fun removeFavorite(id: String) {
        favoritePokemonDao.deleteById(id)
    }
}
