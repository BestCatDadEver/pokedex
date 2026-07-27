package com.carlos.pokedex.dashboard.data.local.pokemon

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pokemon_details")
data class PokemonDetailsEntity(
    @PrimaryKey val name: String,
    val imageUrl: String,
    val height: Int,
    val weight: Int
)
