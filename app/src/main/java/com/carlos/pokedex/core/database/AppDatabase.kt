package com.carlos.pokedex.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.carlos.pokedex.dashboard.data.local.pokemon.PokemonDao
import com.carlos.pokedex.dashboard.data.local.pokemon.PokemonDetailsEntity
import com.carlos.pokedex.dashboard.data.local.pokemon.PokemonEntity

@Database(
    entities = [PokemonEntity::class, PokemonDetailsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pokemonDao(): PokemonDao
}
