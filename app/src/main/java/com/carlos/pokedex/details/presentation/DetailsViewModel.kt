package com.carlos.pokedex.details.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carlos.pokedex.core.network.Resource
import com.carlos.pokedex.dashboard.domain.model.Pokemon
import com.carlos.pokedex.dashboard.domain.model.PokemonDetails
import com.carlos.pokedex.dashboard.domain.usecase.GetPokemonByNameUseCase
import com.carlos.pokedex.favorites.domain.usecase.AddFavoriteUseCase
import com.carlos.pokedex.favorites.domain.usecase.IsFavoriteUseCase
import com.carlos.pokedex.favorites.domain.usecase.RemoveFavoriteUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "DetailsViewModel"

class DetailsViewModel(
    private val pokemonName: String,
    private val getPokemonByNameUseCase: GetPokemonByNameUseCase,
    private val isFavoriteUseCase: IsFavoriteUseCase,
    private val addFavoriteUseCase: AddFavoriteUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DetailsState(isLoading = true))
    val state: StateFlow<DetailsState> = _state
    private var favoriteObservationJob: Job? = null

    init {
        loadDetails()
    }

    fun onAction(action: DetailsAction) {
        when (action) {
            DetailsAction.ToggleFavorite -> toggleFavorite()
            is DetailsAction.SpriteSelected -> _state.value = _state.value.copy(selectedSprite = action.sprite)
        }
    }

    private fun loadDetails() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = getPokemonByNameUseCase(pokemonName)) {
                is Resource.Success -> {
                    val details = result.data
                    Log.d(TAG, "loadDetails($pokemonName) success")
                    _state.value = _state.value.copy(isLoading = false, details = details, error = null)
                    observeFavoriteStatus(details?.id)
                }

                is Resource.Error -> {
                    Log.e(TAG, "loadDetails($pokemonName) failed: ${result.message}")
                    _state.value = _state.value.copy(isLoading = false, error = result.message)
                }

                is Resource.Loading -> Unit
            }
        }
    }

    private fun observeFavoriteStatus(pokemonId: String?) {
        favoriteObservationJob?.cancel()
        if (pokemonId == null) {
            _state.value = _state.value.copy(isFavorite = false)
            return
        }
        favoriteObservationJob = viewModelScope.launch {
            isFavoriteUseCase(pokemonId).collect { isFavorite ->
                _state.value = _state.value.copy(isFavorite = isFavorite)
            }
        }
    }

    private fun toggleFavorite() {
        val details = _state.value.details ?: return
        viewModelScope.launch {
            if (_state.value.isFavorite) {
                removeFavoriteUseCase(details.id)
            } else {
                addFavoriteUseCase(Pokemon(id = details.id, name = details.name, details = details))
            }
        }
    }
}

enum class SpriteVariant(val label: String) {
    FRONT("Frente"),
    BACK("Espalda"),
    SHINY("Shiny"),
    SHINY_BACK("Shiny atrás")
}

fun PokemonDetails.spriteUrl(variant: SpriteVariant): String? = when (variant) {
    SpriteVariant.FRONT -> imageUrl
    SpriteVariant.BACK -> backImageUrl
    SpriteVariant.SHINY -> shinyImageUrl
    SpriteVariant.SHINY_BACK -> backShinyImageUrl
}

data class DetailsState(
    val isLoading: Boolean = false,
    val details: PokemonDetails? = null,
    val error: String? = null,
    val isFavorite: Boolean = false,
    val selectedSprite: SpriteVariant = SpriteVariant.FRONT
) {
    val availableSprites: List<SpriteVariant>
        get() = details?.let { pokemon ->
            SpriteVariant.entries.filter { !pokemon.spriteUrl(it).isNullOrEmpty() }
        }.orEmpty()

    val currentImageUrl: String?
        get() = details?.let { it.spriteUrl(selectedSprite) ?: it.imageUrl }
}

sealed class DetailsAction {
    object ToggleFavorite : DetailsAction()
    data class SpriteSelected(val sprite: SpriteVariant) : DetailsAction()
}
