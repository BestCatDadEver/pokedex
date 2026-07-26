package com.carlos.pokedex.core.di

import com.carlos.pokedex.core.network.ApiClient
import com.carlos.pokedex.core.network.PokedexService
import com.carlos.pokedex.dashboard.data.repository.PokemonRepositoryImpl
import com.carlos.pokedex.dashboard.domain.repository.IPokemonRepository
import com.carlos.pokedex.dashboard.domain.usecase.GetAllPokemonUseCase
import com.carlos.pokedex.dashboard.presentation.DashboardViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { ApiClient.create<PokedexService>() }
    single<IPokemonRepository> { PokemonRepositoryImpl(get()) }
    single { GetAllPokemonUseCase(get()) }
    viewModel { DashboardViewModel(get()) }
}
