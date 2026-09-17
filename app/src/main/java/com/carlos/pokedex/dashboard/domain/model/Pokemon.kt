package com.carlos.pokedex.dashboard.domain.model

/**
 * La pokéapi sirve los sprites desde una ruta predecible por id: es exactamente la misma URL que
 * devuelve `sprites.front_default`. Derivarla evita pedir el detalle de cada pokémon solo para
 * poder pintar la lista.
 */
private const val SPRITE_BASE_URL =
    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/"

data class Pokemon(val id: String, val name: String, val details: PokemonDetails? = null) {
    val spriteUrl: String get() = "$SPRITE_BASE_URL$id.png"
}
