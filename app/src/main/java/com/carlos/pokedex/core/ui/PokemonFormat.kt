package com.carlos.pokedex.core.ui

/** Turns API slugs such as "solar-power" into "Solar Power". */
fun String.toTitleCase(): String = split("-")
    .filter { it.isNotEmpty() }
    .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

/** The API reports height in decimetres. */
fun formatHeight(decimetres: Int): String = "${decimetres / 10}.${decimetres % 10} m"

/** The API reports weight in hectograms. */
fun formatWeight(hectograms: Int): String = "${hectograms / 10}.${hectograms % 10} kg"

/** Formats a nullable measurement, falling back to a dash when the details are missing. */
fun formatHeightOrDash(decimetres: Int?): String = decimetres?.let { formatHeight(it) } ?: "-"

fun formatWeightOrDash(hectograms: Int?): String = hectograms?.let { formatWeight(it) } ?: "-"
