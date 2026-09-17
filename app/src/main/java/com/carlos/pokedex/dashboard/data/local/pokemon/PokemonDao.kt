package com.carlos.pokedex.dashboard.data.local.pokemon

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PokemonDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pokemons: List<PokemonEntity>)

    @Query("SELECT * FROM pokemon ORDER BY orderIndex ASC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<PokemonEntity>

    @Query("SELECT COUNT(*) FROM pokemon")
    suspend fun count(): Int

    @Query(
        "SELECT * FROM pokemon WHERE name LIKE '%' || :query || '%' " +
            "ORDER BY CASE WHEN name LIKE :query || '%' THEN 0 ELSE 1 END, orderIndex ASC " +
            "LIMIT :limit"
    )
    suspend fun searchByName(query: String, limit: Int): List<PokemonEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetails(details: PokemonDetailsEntity)

    @Query("SELECT * FROM pokemon_details WHERE name = :name LIMIT 1")
    suspend fun getDetailsByName(name: String): PokemonDetailsEntity?
}
