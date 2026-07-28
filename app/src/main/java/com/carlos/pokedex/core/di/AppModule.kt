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
import com.carlos.pokedex.favorites.data.repository.FavoritePokemonRepositoryImpl
import com.carlos.pokedex.favorites.domain.repository.IFavoritePokemonRepository
import com.carlos.pokedex.favorites.domain.usecase.AddFavoriteUseCase
import com.carlos.pokedex.favorites.domain.usecase.IsFavoriteUseCase
import com.carlos.pokedex.favorites.domain.usecase.ObserveFavoritesUseCase
import com.carlos.pokedex.favorites.domain.usecase.RemoveFavoriteUseCase
import com.carlos.pokedex.favorites.presentation.FavoritesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { ApiClient.create<PokedexService>() }
    single {
        Room.databaseBuilder(get<Context>(), AppDatabase::class.java, "pokedex.db")
            .fallbackToDestructiveMigration(true)
            .build()
    }
    single { get<AppDatabase>().pokemonDao() }
    single { get<AppDatabase>().favoritePokemonDao() }
    single<IPokemonRepository> { PokemonRepositoryImpl(get(), get()) }
    single<IFavoritePokemonRepository> { FavoritePokemonRepositoryImpl(get()) }
    single { GetAllPokemonUseCase(get()) }
    single { GetPokemonByNameUseCase(get()) }
    single { ObserveFavoritesUseCase(get()) }
    single { IsFavoriteUseCase(get()) }
    single { AddFavoriteUseCase(get()) }
    single { RemoveFavoriteUseCase(get()) }
    viewModel { DashboardViewModel(get(), get(), get(), get(), get()) }
    viewModel { FavoritesViewModel(get(), get()) }
}
