package com.carlos.pokedex.dashboard.domain.model

data class Pokemon(val id: String, val name: String, val details: PokemonDetails? = null)