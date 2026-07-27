package com.carlos.pokedex.dashboard.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carlos.pokedex.core.network.Resource
import com.carlos.pokedex.dashboard.domain.model.Pokemon
import com.carlos.pokedex.dashboard.domain.usecase.GetAllPokemonUseCase
import com.carlos.pokedex.dashboard.domain.usecase.GetPokemonByNameUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private const val TAG = "DashboardViewModel"
private const val PAGE_SIZE = 20
private const val MAX_CONCURRENT_DETAIL_REQUESTS = 10

class DashboardViewModel(
    private val getAllPokemonUseCase: GetAllPokemonUseCase,
    private val getPokemonByNameUseCase: GetPokemonByNameUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state
    private var job: Job? = null
    private var offset = 0

    init {
        loadData(reset = true)
    }

    fun onAction(action: DashboardAction) {
        when (action) {
            DashboardAction.Reload -> loadData(reset = true)
            DashboardAction.LoadMore -> loadData(reset = false)
            is DashboardAction.ItemClicked -> selectItem(action.item)
            DashboardAction.NextPokemon -> moveSelection(1)
            DashboardAction.PreviousPokemon -> moveSelection(-1)
        }
    }

    private fun selectItem(item: Pokemon) {
        val index = _state.value.itemList.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            _state.value = _state.value.copy(selectedIndex = index)
        }
    }

    private fun moveSelection(delta: Int) {
        val list = _state.value.itemList
        if (list.isEmpty()) return
        val newIndex = (_state.value.selectedIndex + delta).coerceIn(0, list.lastIndex)
        _state.value = _state.value.copy(selectedIndex = newIndex)
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

                val semaphore = Semaphore(MAX_CONCURRENT_DETAIL_REQUESTS)
                coroutineScope {
                    newItems.map { pokemon ->
                        async {
                            semaphore.withPermit {
                                when (val detail = getPokemonByNameUseCase.invoke(pokemon.name)) {
                                    is Resource.Success -> pokemon.copy(details = detail.data)
                                    is Resource.Error -> {
                                        Log.e(TAG, "loadData() detail failed for ${pokemon.name}: ${detail.message}")
                                        pokemon
                                    }
                                    is Resource.Loading -> pokemon
                                }
                            }
                        }
                    }.awaitAll()
                }
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
    val selectedIndex: Int = 0
) {
    val selectedPokemon: Pokemon? get() = itemList.getOrNull(selectedIndex)
}

sealed class DashboardAction {
    object Reload : DashboardAction()
    object LoadMore : DashboardAction()
    object NextPokemon : DashboardAction()
    object PreviousPokemon : DashboardAction()
    data class ItemClicked(val item: Pokemon) : DashboardAction()
}

