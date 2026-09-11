package com.carlos.pokedex.dashboard.data.local.pokemon

import com.carlos.pokedex.dashboard.domain.model.Pokemon
import com.carlos.pokedex.dashboard.domain.model.PokemonDetails

fun Pokemon.toEntity(orderIndex: Int): PokemonEntity = PokemonEntity(
    id = id,
    name = name,
    orderIndex = orderIndex
)

fun PokemonEntity.toDomain(details: PokemonDetails? = null): Pokemon = Pokemon(
    id = id,
    name = name,
    details = details
)

fun PokemonDetails.toEntity(): PokemonDetailsEntity = PokemonDetailsEntity(
    name = name,
    id = id,
    imageUrl = imageUrl,
    height = height,
    weight = weight,
    types = types,
    abilities = abilities,
    stats = stats,
    backImageUrl = backImageUrl,
    shinyImageUrl = shinyImageUrl,
    backShinyImageUrl = backShinyImageUrl
)

fun PokemonDetailsEntity.toDomain(): PokemonDetails = PokemonDetails(
    id = id,
    name = name,
    imageUrl = imageUrl,
    height = height,
    weight = weight,
    types = types,
    abilities = abilities,
    stats = stats,
    backImageUrl = backImageUrl,
    shinyImageUrl = shinyImageUrl,
    backShinyImageUrl = backShinyImageUrl
)
