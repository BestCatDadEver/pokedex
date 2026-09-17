package com.carlos.pokedex.dashboard.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.carlos.pokedex.core.network.Resource
import com.carlos.pokedex.dashboard.domain.model.Pokemon
import com.carlos.pokedex.dashboard.domain.usecase.GetPagedPokemonUseCase
import com.carlos.pokedex.dashboard.domain.usecase.GetPokemonByNameUseCase
import com.carlos.pokedex.dashboard.domain.usecase.SearchPokemonUseCase
import com.carlos.pokedex.favorites.domain.usecase.AddFavoriteUseCase
import com.carlos.pokedex.favorites.domain.usecase.IsFavoriteUseCase
import com.carlos.pokedex.favorites.domain.usecase.RemoveFavoriteUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private const val TAG = "DashboardViewModel"
private const val PAGE_SIZE = 20
private const val SEARCH_DEBOUNCE_MILLIS = 300L
private const val SEARCH_RESULT_LIMIT = 20

class DashboardViewModel(
    getPagedPokemonUseCase: GetPagedPokemonUseCase,
    private val getPokemonByNameUseCase: GetPokemonByNameUseCase,
    private val searchPokemonUseCase: SearchPokemonUseCase,
    private val isFavoriteUseCase: IsFavoriteUseCase,
    private val addFavoriteUseCase: AddFavoriteUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase
) : ViewModel() {

    /** cachedIn evita que el stream se reinicie en cada recomposición o cambio de configuración. */
    val pokemonPages: Flow<PagingData<Pokemon>> =
        getPagedPokemonUseCase(PAGE_SIZE).cachedIn(viewModelScope)

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state
    private val searchQuery = MutableStateFlow("")
    private var favoriteObservationJob: Job? = null
    private var detailsJob: Job? = null

    init {
        observeSearchQuery()
    }

    fun onAction(action: DashboardAction) {
        when (action) {
            is DashboardAction.ItemSelected -> selectPokemon(action.index, action.pokemon)
            DashboardAction.ToggleFavorite -> toggleFavorite()
            is DashboardAction.SearchQueryChanged -> updateSearchQuery(action.query)
            DashboardAction.ClearSearch -> updateSearchQuery("")
        }
    }

    /**
     * El debounce evita disparar una búsqueda por tecla y flatMapLatest descarta la búsqueda en
     * curso apenas cambia la query, de modo que solo llega el resultado de lo último que se tipeó.
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observeSearchQuery() {
        viewModelScope.launch {
            searchQuery
                .debounce(SEARCH_DEBOUNCE_MILLIS)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    if (query.isBlank()) flowOf(emptyList()) else flow { emit(runSearch(query)) }
                }
                .collect { results ->
                    _state.value = _state.value.copy(searchResults = results, isSearching = false)
                    results.firstOrNull()?.let { selectPokemon(index = 0, pokemon = it) }
                }
        }
    }

    private fun updateSearchQuery(query: String) {
        searchQuery.value = query
        _state.value = _state.value.copy(
            searchQuery = query,
            isSearching = query.isNotBlank(),
            searchResults = if (query.isBlank()) emptyList() else _state.value.searchResults,
            // Al salir de la búsqueda se limpia la selección y la pantalla vuelve a elegir el
            // primero del listado paginado.
            selectedPokemon = if (query.isBlank()) null else _state.value.selectedPokemon,
            selectedIndex = 0
        )
    }

    private suspend fun runSearch(query: String): List<Pokemon> =
        when (val result = searchPokemonUseCase(query, SEARCH_RESULT_LIMIT)) {
            is Resource.Success -> result.data.orEmpty()
            is Resource.Error -> {
                Log.e(TAG, "runSearch($query) failed: ${result.message}")
                emptyList()
            }

            is Resource.Loading -> emptyList()
        }

    private fun selectPokemon(index: Int, pokemon: Pokemon) {
        if (_state.value.selectedPokemon?.id == pokemon.id && _state.value.selectedIndex == index) {
            return
        }
        _state.value = _state.value.copy(selectedIndex = index, selectedPokemon = pokemon)
        observeFavoriteStatus(pokemon.id)
        loadSelectedDetails(pokemon)
    }

    /**
     * El listado solo trae id y nombre. Altura y peso se piden únicamente para el pokémon que el
     * usuario está mirando, en vez de para toda la página.
     */
    private fun loadSelectedDetails(pokemon: Pokemon) {
        if (pokemon.details != null) return

        detailsJob?.cancel()
        detailsJob = viewModelScope.launch {
            when (val result = getPokemonByNameUseCase(pokemon.name)) {
                is Resource.Success -> {
                    // Descarta la respuesta si el usuario ya cambió de selección mientras llegaba.
                    if (_state.value.selectedPokemon?.id == pokemon.id) {
                        _state.value = _state.value.copy(
                            selectedPokemon = pokemon.copy(details = result.data)
                        )
                    }
                }

                is Resource.Error -> Log.e(TAG, "loadSelectedDetails(${pokemon.name}): ${result.message}")
                is Resource.Loading -> Unit
            }
        }
    }

    private fun observeFavoriteStatus(pokemonId: String?) {
        favoriteObservationJob?.cancel()
        if (pokemonId == null) {
            _state.value = _state.value.copy(isSelectedFavorite = false)
            return
        }
        favoriteObservationJob = viewModelScope.launch {
            isFavoriteUseCase(pokemonId).collect { isFavorite ->
                _state.value = _state.value.copy(isSelectedFavorite = isFavorite)
            }
        }
    }

    private fun toggleFavorite() {
        val pokemon = _state.value.selectedPokemon ?: return
        viewModelScope.launch {
            if (_state.value.isSelectedFavorite) {
                removeFavoriteUseCase(pokemon.id)
            } else {
                addFavoriteUseCase(pokemon)
            }
        }
    }
}

/**
 * El listado ya no vive acá: lo maneja Paging y la pantalla lo consume como LazyPagingItems. Este
 * estado solo guarda lo que Paging no cubre — la selección y la búsqueda.
 */
data class DashboardState(
    val selectedIndex: Int = 0,
    val selectedPokemon: Pokemon? = null,
    val isSelectedFavorite: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<Pokemon> = emptyList(),
    val isSearching: Boolean = false
) {
    val isSearchActive: Boolean get() = searchQuery.isNotBlank()

    val showEmptySearchMessage: Boolean
        get() = isSearchActive && !isSearching && searchResults.isEmpty()
}

sealed class DashboardAction {
    object ToggleFavorite : DashboardAction()
    object ClearSearch : DashboardAction()
    data class ItemSelected(val index: Int, val pokemon: Pokemon) : DashboardAction()
    data class SearchQueryChanged(val query: String) : DashboardAction()
}
