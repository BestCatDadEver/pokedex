package com.carlos.pokedex.core.network

import com.carlos.pokedex.dashboard.data.remote.pokemon.PokemonDto
import com.carlos.pokedex.dashboard.data.remote.pokemon.PokemonRootResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PokedexService {

    @GET("pokemon")
    suspend fun getAll(@Query("limit") limit: Int, @Query("offset") offset: Int) : Response<PokemonRootResponse>

    @GET("pokemon/{name}")
    suspend fun getByName(@Path("name") name: String): Response<PokemonDto>


}

