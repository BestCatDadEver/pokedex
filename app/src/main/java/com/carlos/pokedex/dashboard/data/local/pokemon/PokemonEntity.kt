package com.carlos.pokedex.dashboard.data.local.pokemon

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pokemon")
data class PokemonEntity(
    @PrimaryKey val id: String,
    val name: String,
    val orderIndex: Int
)
