package com.carlos.pokedex.dashboard.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carlos.pokedex.core.network.Resource
import com.carlos.pokedex.dashboard.domain.model.Pokemon
import com.carlos.pokedex.dashboard.domain.usecase.GetAllPokemonUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "DashboardViewModel"

class DashboardViewModel(private val getAllPokemonUseCase: GetAllPokemonUseCase) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state
    private var job: Job? = null

    init {
        loadData()
    }

    fun onAction(action: DashboardAction) {
        when (action) {
            DashboardAction.Reload -> loadData()
            is DashboardAction.ItemClicked -> {}
        }
    }

    private fun loadData() {
        job?.cancel()

        job = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            runCatching {
                getAllPokemonUseCase.invoke()
            }.onSuccess { resource ->
                Log.d(TAG, "loadData() resource=$resource, itemCount=${resource.data?.size}")
                when (resource) {
                    is Resource.Success -> _state.value = _state.value.copy(
                        isLoading = false,
                        itemList = resource.data.orEmpty(),
                        error = null
                    )

                    is Resource.Error -> _state.value = _state.value.copy(
                        isLoading = false,
                        error = resource.message
                    )

                    is Resource.Loading -> Unit
                }
            }.onFailure { e ->
                Log.e(TAG, "loadData() failed", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}

data class DashboardState(
    val isLoading: Boolean = false,
    val itemList: List<Pokemon> = emptyList(),
    val error: String? = null
)

sealed class DashboardAction {
    object Reload : DashboardAction()
    data class ItemClicked(val item: Pokemon) : DashboardAction()
}

