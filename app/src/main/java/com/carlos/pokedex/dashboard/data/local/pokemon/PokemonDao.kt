package com.carlos.pokedex.dashboard.data.local.pokemon

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PokemonDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pokemons: List<PokemonEntity>)

    @Query("SELECT * FROM pokemon ORDER BY orderIndex ASC")
    fun pagingSource(): PagingSource<Int, PokemonEntity>

    @Query("SELECT COUNT(*) FROM pokemon")
    suspend fun count(): Int

    @Query("SELECT MAX(orderIndex) FROM pokemon")
    suspend fun maxOrderIndex(): Int?

    @Query("DELETE FROM pokemon")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIndex(entries: List<PokemonIndexEntity>)

    @Query("SELECT COUNT(*) FROM pokemon_index")
    suspend fun indexCount(): Int

    @Query(
        "SELECT * FROM pokemon_index WHERE name LIKE '%' || :query || '%' " +
            "ORDER BY CASE WHEN name LIKE :query || '%' THEN 0 ELSE 1 END, orderIndex ASC " +
            "LIMIT :limit"
    )
    suspend fun searchIndex(query: String, limit: Int): List<PokemonIndexEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetails(details: PokemonDetailsEntity)

    @Query("SELECT * FROM pokemon_details WHERE name = :name LIMIT 1")
    suspend fun getDetailsByName(name: String): PokemonDetailsEntity?
}
