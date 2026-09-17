package com.carlos.pokedex.dashboard.data.local.pokemon

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Catálogo completo de nombres, separado de [PokemonEntity] a propósito: esa tabla es propiedad
 * del RemoteMediator y se vacía en cada refresh, mientras que este índice se baja de una sola vez
 * y solo existe para que la búsqueda pueda hacer coincidencias parciales sin ir a la red.
 */
@Entity(tableName = "pokemon_index")
data class PokemonIndexEntity(
    @PrimaryKey val id: String,
    val name: String,
    val orderIndex: Int
)
