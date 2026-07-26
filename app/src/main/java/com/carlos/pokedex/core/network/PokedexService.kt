package com.carlos.pokedex.core.network

import com.carlos.pokedex.dashboard.data.remote.pokemon.PokemonRootResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface PokedexService {

    @GET("pokemon?limit=100000&offset=0")
    suspend fun getAll(): Response<PokemonRootResponse>

    @GET("/{id}")
    suspend fun getById(@Path("id") id: String): Response<PokemonRootResponse>


}

