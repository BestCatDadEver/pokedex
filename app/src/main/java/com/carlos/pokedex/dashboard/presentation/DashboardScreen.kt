package com.carlos.pokedex.dashboard.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.carlos.pokedex.R
import com.carlos.pokedex.dashboard.domain.model.Pokemon
import com.carlos.pokedex.dashboard.domain.model.PokemonDetails
import org.koin.androidx.compose.koinViewModel

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = Color.Transparent
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

    LaunchedEffect(state.selectedIndex) {
        val targetIndex = state.selectedIndex
        if (targetIndex !in state.itemList.indices) return@LaunchedEffect

        val layoutInfo = listState.layoutInfo
        val visibleItem = layoutInfo.visibleItemsInfo.find { it.index == targetIndex }
        val isFullyVisible = visibleItem != null &&
            visibleItem.offset >= layoutInfo.viewportStartOffset &&
            visibleItem.offset + visibleItem.size <= layoutInfo.viewportEndOffset

        if (isFullyVisible) return@LaunchedEffect

        if (targetIndex <= listState.firstVisibleItemIndex) {
            listState.animateScrollToItem(targetIndex)
        } else {
            val itemSize = layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
            val viewportSize = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
            val itemsPerViewport = if (itemSize > 0) viewportSize / itemSize else 1
            val alignIndex = (targetIndex - itemsPerViewport + 1).coerceAtLeast(0)
            listState.animateScrollToItem(alignIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Image(
            painter = painterResource(id = R.drawable.pokemon_logo),
            contentDescription = "Pokémon",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 8.dp),
            contentScale = ContentScale.FillWidth
        )

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
            selectedIndex = state.selectedIndex,
            isFavorite = state.isSelectedFavorite,
            hasPrevious = state.selectedIndex > 0,
            hasNext = state.selectedIndex < state.itemList.lastIndex,
            onPrevious = { onAction(DashboardAction.PreviousPokemon) },
            onNext = { onAction(DashboardAction.NextPokemon) },
            onToggleFavorite = { onAction(DashboardAction.ToggleFavorite) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun DashboardItem(
    item: Pokemon,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
        },
        label = "DashboardItemContainerColor"
    )

    Card(
        modifier = Modifier
            .width(130.dp)
            .height(200.dp)
            .padding(8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, Color.Black)
        }
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
    selectedIndex: Int,
    isFavorite: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    var previousIndex by remember { mutableIntStateOf(selectedIndex) }
    val isForward = selectedIndex >= previousIndex
    SideEffect { previousIndex = selectedIndex }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledIconButton(
            onClick = onPrevious,
            enabled = hasPrevious,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.Black,
                contentColor = Color.White,
                disabledContainerColor = Color.Black.copy(alpha = 0.4f),
                disabledContentColor = Color.White.copy(alpha = 0.6f)
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Pokémon anterior"
            )
        }

        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, Color.Black)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedContent(
                    targetState = pokemon,
                    transitionSpec = {
                        val direction = if (isForward) 1 else -1
                        (slideInHorizontally(animationSpec = tween(300)) { width -> direction * width } + fadeIn(tween(300)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { width -> -direction * width } + fadeOut(tween(300)))
                    },
                    label = "SelectedPokemon"
                ) { targetPokemon ->
                    if (targetPokemon == null) {
                        Text(text = "Selecciona un pokémon de la lista")
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AsyncImage(
                                model = targetPokemon.details?.imageUrl,
                                contentDescription = targetPokemon.name,
                                modifier = Modifier.size(220.dp)
                            )
                            Text(
                                text = targetPokemon.name,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                Text(text = "Altura: ${targetPokemon.details?.height ?: "-"}")
                                Text(text = "Peso: ${targetPokemon.details?.weight ?: "-"}")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                IconButton(onClick = onToggleFavorite, enabled = pokemon != null) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        FilledIconButton(
            onClick = onNext,
            enabled = hasNext,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.Black,
                contentColor = Color.White,
                disabledContainerColor = Color.Black.copy(alpha = 0.4f),
                disabledContentColor = Color.White.copy(alpha = 0.6f)
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Siguiente pokémon"
            )
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
