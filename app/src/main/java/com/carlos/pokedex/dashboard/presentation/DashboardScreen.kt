package com.carlos.pokedex.dashboard.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.carlos.pokedex.dashboard.domain.model.Pokemon
import com.carlos.pokedex.dashboard.domain.model.PokemonDetails
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "DashboardScreen") })
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.error != null -> {
                    Text(
                        text = state.error ?: "",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    DashboardScreenContent(state = state, onAction = viewModel::onAction)
                }
            }
        }
    }
}

@Composable
fun DashboardScreenContent(
    state: DashboardState,
    onAction: (DashboardAction) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, state.itemList.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null && lastVisibleIndex >= state.itemList.size - 5) {
                    onAction(DashboardAction.LoadMore)
                }

            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(166.dp)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.itemList.size) {
                DashboardItem(
                    item = state.itemList[it],
                    isSelected = it == state.selectedIndex
                ) {
                    onAction(DashboardAction.ItemClicked(state.itemList[it]))
                }
            }

            if (state.isLoadingMore) {
                item {
                    Box(modifier = Modifier.fillMaxHeight().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        SelectedPokemonSection(
            pokemon = state.selectedPokemon,
            hasPrevious = state.selectedIndex > 0,
            hasNext = state.selectedIndex < state.itemList.lastIndex,
            onPrevious = { onAction(DashboardAction.PreviousPokemon) },
            onNext = { onAction(DashboardAction.NextPokemon) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun DashboardItem(
    item: Pokemon,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .height(150.dp)
            .padding(8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = item.details?.imageUrl,
                contentDescription = item.name,
                modifier = Modifier.size(80.dp)
            )
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun SelectedPokemonSection(
    pokemon: Pokemon?,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (pokemon == null) {
            Text(text = "Selecciona un pokémon de la lista")
        } else {
            AsyncImage(
                model = pokemon.details?.imageUrl,
                contentDescription = pokemon.name,
                modifier = Modifier.size(220.dp)
            )
            Text(
                text = pokemon.name,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Text(text = "Altura: ${pokemon.details?.height ?: "-"}")
                Text(text = "Peso: ${pokemon.details?.weight ?: "-"}")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = onPrevious, enabled = hasPrevious) {
                    Text("Atrás")
                }
                Button(onClick = onNext, enabled = hasNext) {
                    Text("Siguiente")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    DashboardScreenContent(
        state = DashboardState(
            itemList = listOf(
                Pokemon("1", "Pikachu", PokemonDetails("Pikachu", "", 4, 60)),
                Pokemon("2", "Bulbasaur", PokemonDetails("Bulbasaur", "", 7, 69)),
                Pokemon("3", "Hitmonchan", PokemonDetails("Hitmonchan", "", 14, 502))
            )
        ),
        onAction = {}
    )
}
