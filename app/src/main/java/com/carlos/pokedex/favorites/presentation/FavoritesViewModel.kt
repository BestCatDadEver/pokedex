package com.carlos.pokedex.favorites.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carlos.pokedex.dashboard.domain.model.Pokemon
import com.carlos.pokedex.favorites.domain.usecase.ObserveFavoritesUseCase
import com.carlos.pokedex.favorites.domain.usecase.RemoveFavoriteUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(
    observeFavoritesUseCase: ObserveFavoritesUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase
) : ViewModel() {

    val state: StateFlow<FavoritesState> = observeFavoritesUseCase()
        .map { favorites -> FavoritesState(isLoading = false, items = favorites) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FavoritesState(isLoading = true)
        )

    fun onAction(action: FavoritesAction) {
        when (action) {
            is FavoritesAction.RemoveFavorite -> viewModelScope.launch {
                removeFavoriteUseCase(action.id)
            }
        }
    }
}

data class FavoritesState(
    val isLoading: Boolean = false,
    val items: List<Pokemon> = emptyList()
)

sealed class FavoritesAction {
    data class RemoveFavorite(val id: String) : FavoritesAction()
}
