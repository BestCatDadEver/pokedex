package com.carlos.pokedex.dashboard.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.carlos.pokedex.R
import com.carlos.pokedex.core.ui.formatHeightOrDash
import com.carlos.pokedex.core.ui.formatWeightOrDash
import com.carlos.pokedex.dashboard.domain.model.Pokemon
import org.koin.androidx.compose.koinViewModel

@Composable
fun DashboardScreen(
    onPokemonClick: (String) -> Unit,
    viewModel: DashboardViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagingItems = viewModel.pokemonPages.collectAsLazyPagingItems()

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            val refreshState = pagingItems.loadState.refresh
            when {
                refreshState is LoadState.Loading && pagingItems.itemCount == 0 -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                refreshState is LoadState.Error && pagingItems.itemCount == 0 -> {
                    LoadErrorMessage(
                        message = refreshState.error.message ?: "No se pudo cargar la pokédex",
                        onRetry = pagingItems::retry,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    DashboardScreenContent(
                        state = state,
                        pagingItems = pagingItems,
                        onAction = viewModel::onAction,
                        onPokemonClick = onPokemonClick
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardScreenContent(
    state: DashboardState,
    pagingItems: LazyPagingItems<Pokemon>,
    onAction: (DashboardAction) -> Unit,
    onPokemonClick: (String) -> Unit
) {
    val listState = rememberLazyListState()

    // La búsqueda y el listado paginado se leen distinto, así que el resto de la pantalla habla
    // con ellos a través de estos dos accesos.
    val itemCount = if (state.isSearchActive) state.searchResults.size else pagingItems.itemCount
    val itemAt: (Int) -> Pokemon? = { index ->
        if (state.isSearchActive) {
            state.searchResults.getOrNull(index)
        } else {
            pagingItems.itemSnapshotList.getOrNull(index)
        }
    }

    // Al abrir la pantalla (o al salir de una búsqueda) se preselecciona el primero disponible.
    LaunchedEffect(state.isSearchActive, state.selectedPokemon, itemCount) {
        if (!state.isSearchActive && state.selectedPokemon == null && itemCount > 0) {
            itemAt(0)?.let { onAction(DashboardAction.ItemSelected(0, it)) }
        }
    }

    LaunchedEffect(state.selectedIndex, itemCount) {
        val targetIndex = state.selectedIndex
        if (targetIndex !in 0 until itemCount) return@LaunchedEffect

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

        PokemonSearchField(
            query = state.searchQuery,
            isSearching = state.isSearching,
            onQueryChange = { onAction(DashboardAction.SearchQueryChanged(it)) },
            onClear = { onAction(DashboardAction.ClearSearch) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        if (state.showEmptySearchMessage) {
            Text(
                text = "Ningún pokémon coincide con \"${state.searchQuery}\"",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(166.dp)
                    .padding(32.dp)
            )
        } else {
            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(166.dp)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.isSearchActive) {
                    items(
                        count = state.searchResults.size,
                        key = { state.searchResults[it].id }
                    ) { index ->
                        val pokemon = state.searchResults[index]
                        DashboardItem(
                            item = pokemon,
                            isSelected = index == state.selectedIndex
                        ) {
                            onAction(DashboardAction.ItemSelected(index, pokemon))
                            onPokemonClick(pokemon.name)
                        }
                    }
                } else {
                    items(
                        count = pagingItems.itemCount,
                        key = pagingItems.itemKey { it.id }
                    ) { index ->
                        pagingItems[index]?.let { pokemon ->
                            DashboardItem(
                                item = pokemon,
                                isSelected = index == state.selectedIndex
                            ) {
                                onAction(DashboardAction.ItemSelected(index, pokemon))
                                onPokemonClick(pokemon.name)
                            }
                        }
                    }

                    // Paging expone el estado de la carga incremental, así que ya no hace falta
                    // rastrear el scroll a mano para saber cuándo pedir la página siguiente.
                    when (val appendState = pagingItems.loadState.append) {
                        is LoadState.Loading -> item {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is LoadState.Error -> item {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                LoadErrorMessage(
                                    message = appendState.error.message ?: "Error al cargar",
                                    onRetry = pagingItems::retry
                                )
                            }
                        }

                        else -> Unit
                    }
                }
            }
        }

        SelectedPokemonSection(
            pokemon = state.selectedPokemon,
            selectedIndex = state.selectedIndex,
            isFavorite = state.isSelectedFavorite,
            hasPrevious = state.selectedIndex > 0,
            hasNext = state.selectedIndex < itemCount - 1,
            onPrevious = {
                val index = state.selectedIndex - 1
                itemAt(index)?.let { onAction(DashboardAction.ItemSelected(index, it)) }
            },
            onNext = {
                val index = state.selectedIndex + 1
                itemAt(index)?.let { onAction(DashboardAction.ItemSelected(index, it)) }
            },
            onToggleFavorite = { onAction(DashboardAction.ToggleFavorite) },
            onDetails = onPokemonClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun LoadErrorMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = message, color = Color.Red, textAlign = TextAlign.Center)
        Button(onClick = onRetry) {
            Text(text = "Reintentar")
        }
    }
}

@Composable
fun PokemonSearchField(
    query: String,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        singleLine = true,
        placeholder = { Text(text = "Buscar pokémon") },
        leadingIcon = {
            Icon(imageVector = Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            when {
                isSearching -> CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )

                query.isNotEmpty() -> IconButton(onClick = onClear) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                }
            }
        },
        shape = RoundedCornerShape(50),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Black
        )
    )
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
                model = item.spriteUrl,
                contentDescription = item.name,
                modifier = Modifier.size(80.dp)
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
    onDetails: (String) -> Unit,
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
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = pokemon != null) { pokemon?.let { onDetails(it.name) } },
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
                                model = targetPokemon.spriteUrl,
                                contentDescription = targetPokemon.name,
                                modifier = Modifier.size(220.dp)
                            )
                            Text(
                                text = targetPokemon.name,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                Text(text = "Altura: ${formatHeightOrDash(targetPokemon.details?.height)}")
                                Text(text = "Peso: ${formatWeightOrDash(targetPokemon.details?.weight)}")
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
