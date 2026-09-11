package com.carlos.pokedex.dashboard.data.local.pokemon

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.carlos.pokedex.dashboard.domain.model.PokemonStat

@Entity(tableName = "pokemon_details")
data class PokemonDetailsEntity(
    @PrimaryKey val name: String,
    val id: String,
    val imageUrl: String,
    val height: Int,
    val weight: Int,
    val types: List<String>,
    val abilities: List<String>,
    val stats: List<PokemonStat>,
    val backImageUrl: String?,
    val shinyImageUrl: String?,
    val backShinyImageUrl: String?
)
