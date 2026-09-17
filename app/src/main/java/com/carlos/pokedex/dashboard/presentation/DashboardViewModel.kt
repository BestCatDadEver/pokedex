package com.carlos.pokedex.dashboard.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carlos.pokedex.core.network.Resource
import com.carlos.pokedex.dashboard.domain.model.Pokemon
import com.carlos.pokedex.dashboard.domain.usecase.GetAllPokemonUseCase
import com.carlos.pokedex.dashboard.domain.usecase.GetPokemonByNameUseCase
import com.carlos.pokedex.dashboard.domain.usecase.SearchPokemonUseCase
import com.carlos.pokedex.favorites.domain.usecase.AddFavoriteUseCase
import com.carlos.pokedex.favorites.domain.usecase.IsFavoriteUseCase
import com.carlos.pokedex.favorites.domain.usecase.RemoveFavoriteUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private const val TAG = "DashboardViewModel"
private const val PAGE_SIZE = 20
private const val MAX_CONCURRENT_DETAIL_REQUESTS = 10
private const val SEARCH_DEBOUNCE_MILLIS = 300L
private const val SEARCH_RESULT_LIMIT = 20

class DashboardViewModel(
    private val getAllPokemonUseCase: GetAllPokemonUseCase,
    private val getPokemonByNameUseCase: GetPokemonByNameUseCase,
    private val searchPokemonUseCase: SearchPokemonUseCase,
    private val isFavoriteUseCase: IsFavoriteUseCase,
    private val addFavoriteUseCase: AddFavoriteUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state
    private val searchQuery = MutableStateFlow("")
    private var job: Job? = null
    private var favoriteObservationJob: Job? = null
    private var offset = 0

    init {
        loadData(reset = true)
        observeSearchQuery()
    }

    fun onAction(action: DashboardAction) {
        when (action) {
            DashboardAction.Reload -> loadData(reset = true)
            DashboardAction.LoadMore -> if (!_state.value.isSearchActive) loadData(reset = false)
            is DashboardAction.ItemClicked -> selectItem(action.item)
            DashboardAction.NextPokemon -> moveSelection(1)
            DashboardAction.PreviousPokemon -> moveSelection(-1)
            DashboardAction.ToggleFavorite -> toggleFavorite()
            is DashboardAction.SearchQueryChanged -> updateSearchQuery(action.query)
            DashboardAction.ClearSearch -> updateSearchQuery("")
        }
    }

    /**
     * El debounce evita disparar una búsqueda por tecla y flatMapLatest descarta la búsqueda en
     * curso apenas la query cambia, de modo que solo llega el resultado de lo último que se tipeó.
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
                    _state.value = _state.value.copy(
                        searchResults = results,
                        isSearching = false,
                        selectedIndex = 0
                    )
                    observeFavoriteStatus(_state.value.selectedPokemon?.id)
                }
        }
    }

    private fun updateSearchQuery(query: String) {
        searchQuery.value = query
        _state.value = _state.value.copy(
            searchQuery = query,
            isSearching = query.isNotBlank(),
            searchResults = if (query.isBlank()) emptyList() else _state.value.searchResults,
            selectedIndex = 0
        )
        if (query.isBlank()) {
            observeFavoriteStatus(_state.value.selectedPokemon?.id)
        }
    }

    private suspend fun runSearch(query: String): List<Pokemon> =
        when (val result = searchPokemonUseCase(query, SEARCH_RESULT_LIMIT)) {
            is Resource.Success -> withDetails(result.data.orEmpty())
            is Resource.Error -> {
                Log.e(TAG, "runSearch($query) failed: ${result.message}")
                emptyList()
            }

            is Resource.Loading -> emptyList()
        }

    private fun selectItem(item: Pokemon) {
        val index = _state.value.displayedList.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            updateSelectedIndex(index)
        }
    }

    private fun moveSelection(delta: Int) {
        val list = _state.value.displayedList
        if (list.isEmpty()) return
        val newIndex = (_state.value.selectedIndex + delta).coerceIn(0, list.lastIndex)
        updateSelectedIndex(newIndex)
    }

    private fun updateSelectedIndex(index: Int) {
        _state.value = _state.value.copy(selectedIndex = index)
        observeFavoriteStatus(_state.value.selectedPokemon?.id)
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

    /** El listado solo trae id y nombre, así que el sprite y las medidas se piden aparte. */
    private suspend fun withDetails(pokemons: List<Pokemon>): List<Pokemon> {
        val semaphore = Semaphore(MAX_CONCURRENT_DETAIL_REQUESTS)
        return coroutineScope {
            pokemons.map { pokemon ->
                async {
                    semaphore.withPermit {
                        when (val detail = getPokemonByNameUseCase.invoke(pokemon.name)) {
                            is Resource.Success -> pokemon.copy(details = detail.data)
                            is Resource.Error -> {
                                Log.e(TAG, "withDetails() failed for ${pokemon.name}: ${detail.message}")
                                pokemon
                            }

                            is Resource.Loading -> pokemon
                        }
                    }
                }
            }.awaitAll()
        }
    }

    private fun loadData(reset: Boolean) {

        if (reset) {
            job?.cancel()
            offset = 0
            _state.value = _state.value.copy(isLoading = true, endReached = false)
        } else {
            if (_state.value.isLoadingMore || _state.value.endReached) return
            _state.value = _state.value.copy(isLoadingMore = true)
        }

        job = viewModelScope.launch {
            runCatching {
                val allResource = getAllPokemonUseCase.invoke(PAGE_SIZE, offset)
                Log.d(TAG, "loadData() offset=$offset allResource=$allResource")

                val newItems = when (allResource) {
                    is Resource.Success -> allResource.data.orEmpty()
                    is Resource.Error -> throw IllegalStateException(allResource.message)
                    is Resource.Loading -> emptyList()
                }

                withDetails(newItems)
            }.onSuccess { detailedItems ->
                offset += PAGE_SIZE
                _state.value = _state.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    itemList = if (reset) detailedItems else _state.value.itemList + detailedItems,
                    selectedIndex = if (reset) 0 else _state.value.selectedIndex,
                    endReached = detailedItems.size < PAGE_SIZE,
                    error = null
                )
                if (reset) {
                    observeFavoriteStatus(_state.value.selectedPokemon?.id)
                }
            }.onFailure { e ->
                Log.e(TAG, "loadData() failed", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = e.message
                )
            }
        }
    }
}


data class DashboardState(
    val isLoading: Boolean = false,
    val itemList: List<Pokemon> = emptyList(),
    val error: String? = null,
    val endReached: Boolean = false,
    val isLoadingMore: Boolean = false,
    val selectedIndex: Int = 0,
    val isSelectedFavorite: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<Pokemon> = emptyList(),
    val isSearching: Boolean = false
) {
    val isSearchActive: Boolean get() = searchQuery.isNotBlank()

    /** La carrusel muestra los resultados mientras haya búsqueda y el listado paginado si no. */
    val displayedList: List<Pokemon> get() = if (isSearchActive) searchResults else itemList

    val selectedPokemon: Pokemon? get() = displayedList.getOrNull(selectedIndex)

    val showEmptySearchMessage: Boolean get() = isSearchActive && !isSearching && searchResults.isEmpty()
}

sealed class DashboardAction {
    object Reload : DashboardAction()
    object LoadMore : DashboardAction()
    object NextPokemon : DashboardAction()
    object PreviousPokemon : DashboardAction()
    object ToggleFavorite : DashboardAction()
    object ClearSearch : DashboardAction()
    data class ItemClicked(val item: Pokemon) : DashboardAction()
    data class SearchQueryChanged(val query: String) : DashboardAction()
}
