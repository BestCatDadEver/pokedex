package com.carlos.pokedex.details.presentation

import androidx.compose.ui.graphics.Color
import com.carlos.pokedex.core.ui.toTitleCase

private val typeColors = mapOf(
    "normal" to Color(0xFFA8A77A),
    "fire" to Color(0xFFEE8130),
    "water" to Color(0xFF6390F0),
    "electric" to Color(0xFFF7D02C),
    "grass" to Color(0xFF7AC74C),
    "ice" to Color(0xFF96D9D6),
    "fighting" to Color(0xFFC22E28),
    "poison" to Color(0xFFA33EA1),
    "ground" to Color(0xFFE2BF65),
    "flying" to Color(0xFFA98FF3),
    "psychic" to Color(0xFFF95587),
    "bug" to Color(0xFFA6B91A),
    "rock" to Color(0xFFB6A136),
    "ghost" to Color(0xFF735797),
    "dragon" to Color(0xFF6F35FC),
    "dark" to Color(0xFF705746),
    "steel" to Color(0xFFB7B7CE),
    "fairy" to Color(0xFFD685AD)
)

private val typeLabels = mapOf(
    "normal" to "Normal",
    "fire" to "Fuego",
    "water" to "Agua",
    "electric" to "Eléctrico",
    "grass" to "Planta",
    "ice" to "Hielo",
    "fighting" to "Lucha",
    "poison" to "Veneno",
    "ground" to "Tierra",
    "flying" to "Volador",
    "psychic" to "Psíquico",
    "bug" to "Bicho",
    "rock" to "Roca",
    "ghost" to "Fantasma",
    "dragon" to "Dragón",
    "dark" to "Siniestro",
    "steel" to "Acero",
    "fairy" to "Hada"
)

private val statLabels = mapOf(
    "hp" to "PS",
    "attack" to "Ataque",
    "defense" to "Defensa",
    "special-attack" to "At. Especial",
    "special-defense" to "Def. Especial",
    "speed" to "Velocidad"
)

fun typeColor(type: String): Color = typeColors[type.lowercase()] ?: Color(0xFF9E9E9E)

fun typeLabel(type: String): String = typeLabels[type.lowercase()] ?: type.toTitleCase()

fun statLabel(stat: String): String = statLabels[stat.lowercase()] ?: stat.toTitleCase()
