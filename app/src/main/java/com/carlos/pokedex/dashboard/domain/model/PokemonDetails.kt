package com.carlos.pokedex.dashboard.domain.model

data class PokemonDetails(
    val id: String,
    val name: String,
    val imageUrl: String,
    val height: Int,
    val weight: Int,
    val types: List<String> = emptyList(),
    val abilities: List<String> = emptyList(),
    val stats: List<PokemonStat> = emptyList(),
    val backImageUrl: String? = null,
    val shinyImageUrl: String? = null,
    val backShinyImageUrl: String? = null
)
