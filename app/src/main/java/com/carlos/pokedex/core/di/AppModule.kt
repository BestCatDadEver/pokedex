package com.carlos.pokedex.core.di

import android.content.Context
import androidx.room.Room
import com.carlos.pokedex.core.database.AppDatabase
import com.carlos.pokedex.core.network.ApiClient
import com.carlos.pokedex.core.network.PokedexService
import com.carlos.pokedex.dashboard.data.repository.PokemonRepositoryImpl
import com.carlos.pokedex.dashboard.domain.repository.IPokemonRepository
import com.carlos.pokedex.dashboard.domain.usecase.GetAllPokemonUseCase
import com.carlos.pokedex.dashboard.domain.usecase.GetPokemonByNameUseCase
import com.carlos.pokedex.dashboard.presentation.DashboardViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { ApiClient.create<PokedexService>() }
    single {
        Room.databaseBuilder(get<Context>(), AppDatabase::class.java, "pokedex.db").build()
    }
    single { get<AppDatabase>().pokemonDao() }
    single<IPokemonRepository> { PokemonRepositoryImpl(get(), get()) }
    single { GetAllPokemonUseCase(get()) }
    single { GetPokemonByNameUseCase(get()) }
    viewModel { DashboardViewModel(get(), get()) }
}
