package com.carlos.pokedex.favorites.data.local

import com.carlos.pokedex.dashboard.domain.model.Pokemon
import com.carlos.pokedex.dashboard.domain.model.PokemonDetails

fun Pokemon.toFavoriteEntity(): FavoritePokemonEntity = FavoritePokemonEntity(
    id = id,
    name = name,
    imageUrl = details?.imageUrl,
    height = details?.height,
    weight = details?.weight,
    addedAt = System.currentTimeMillis()
)

fun FavoritePokemonEntity.toDomain(): Pokemon = Pokemon(
    id = id,
    name = name,
    details = if (imageUrl != null && height != null && weight != null) {
        PokemonDetails(name = name, imageUrl = imageUrl, height = height, weight = weight)
    } else {
        null
    }
)
