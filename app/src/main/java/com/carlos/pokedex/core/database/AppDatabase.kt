package com.carlos.pokedex.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.carlos.pokedex.dashboard.data.local.pokemon.PokemonDao
import com.carlos.pokedex.dashboard.data.local.pokemon.PokemonDetailsEntity
import com.carlos.pokedex.dashboard.data.local.pokemon.PokemonEntity
import com.carlos.pokedex.dashboard.data.local.pokemon.PokemonIndexEntity
import com.carlos.pokedex.favorites.data.local.FavoritePokemonDao
import com.carlos.pokedex.favorites.data.local.FavoritePokemonEntity

@Database(
    entities = [
        PokemonEntity::class,
        PokemonIndexEntity::class,
        PokemonDetailsEntity::class,
        FavoritePokemonEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pokemonDao(): PokemonDao
    abstract fun favoritePokemonDao(): FavoritePokemonDao
}
