package com.carlos.pokedex.core.network

import com.carlos.pokedex.dashboard.data.remote.pokemon.PokemonDto
import com.carlos.pokedex.dashboard.data.remote.pokemon.PokemonResponse
import com.carlos.pokedex.dashboard.data.remote.pokemon.PokemonRootResponse
import com.carlos.pokedex.dashboard.domain.model.Pokemon
import com.carlos.pokedex.dashboard.domain.model.PokemonDetails

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

fun PokemonDto.toDomain(): PokemonDetails {
    return PokemonDetails(
        name = name.replaceFirstChar { it.uppercase() },
        imageUrl = sprites.frontDefault,
        height = height.toInt(),
        weight = weight.toInt()
    )
}
