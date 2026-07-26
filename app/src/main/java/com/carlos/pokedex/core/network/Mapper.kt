package com.carlos.pokedex.core.network

import com.carlos.pokedex.dashboard.data.remote.pokemon.PokemonResponse
import com.carlos.pokedex.dashboard.data.remote.pokemon.PokemonRootResponse
import com.carlos.pokedex.dashboard.domain.model.Pokemon

fun PokemonResponse.toDomain(): Pokemon {
    val id = url.split("/").filter { it.isNotEmpty() }.last()
    return Pokemon(
        id = id,
        name = name.replaceFirstChar { it.uppercase() }
    )
}

fun PokemonRootResponse.toDomain(): List<Pokemon> {
    return results.map { it.toDomain() }
}
