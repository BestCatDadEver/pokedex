package com.carlos.pokedex.favorites.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_pokemon")
data class FavoritePokemonEntity(
    @PrimaryKey val id: String,
    val name: String,
    val imageUrl: String?,
    val height: Int?,
    val weight: Int?,
    val addedAt: Long
)
