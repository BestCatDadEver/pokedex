package com.carlos.pokedex.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carlos.pokedex.dashboard.domain.model.Pokemon
import com.carlos.pokedex.dashboard.domain.usecase.GetAllPokemonUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(private val getAllPokemonUseCase: GetAllPokemonUseCase) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state
    private var job: Job? = null

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
                val result: List<Pokemon> = listOf()
                result
            }.onSuccess { result ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    itemList = result,
                    error = null
                )
            }.onFailure { e ->
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

