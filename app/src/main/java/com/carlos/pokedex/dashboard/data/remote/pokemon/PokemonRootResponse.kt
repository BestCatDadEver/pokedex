package com.carlos.pokedex.dashboard.data.remote.pokemon

data class PokemonRootResponse(
    val count: Long,
    val next: Any?,
    val previous: Any?,
    val results: List<PokemonResponse>,
)

data class PokemonResponse(
    val name: String,
    val url: String,
)