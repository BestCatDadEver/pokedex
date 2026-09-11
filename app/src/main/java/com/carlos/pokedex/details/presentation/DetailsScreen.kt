package com.carlos.pokedex.details.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.carlos.pokedex.core.ui.formatHeight
import com.carlos.pokedex.core.ui.formatWeight
import com.carlos.pokedex.core.ui.toTitleCase
import com.carlos.pokedex.dashboard.domain.model.PokemonDetails
import com.carlos.pokedex.dashboard.domain.model.PokemonStat
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/** Highest base stat reachable in the games, used to scale the stat bars. */
private const val MAX_BASE_STAT = 255f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    pokemonName: String,
    onBack: () -> Unit,
    viewModel: DetailsViewModel = koinViewModel(parameters = { parametersOf(pokemonName) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(text = state.details?.name ?: pokemonName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onAction(DetailsAction.ToggleFavorite) },
                        enabled = state.details != null
                    ) {
                        Icon(
                            imageVector = if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (state.isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                            tint = if (state.isFavorite) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            val details = state.details
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                details == null -> {
                    Text(
                        text = state.error ?: "No se encontró el pokémon",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    DetailsContent(
                        details = details,
                        imageUrl = state.currentImageUrl,
                        availableSprites = state.availableSprites,
                        selectedSprite = state.selectedSprite,
                        onSpriteSelected = { viewModel.onAction(DetailsAction.SpriteSelected(it)) }
                    )
                }
            }
        }
    }
}

@Composable
fun DetailsContent(
    details: PokemonDetails,
    imageUrl: String?,
    availableSprites: List<SpriteVariant>,
    selectedSprite: SpriteVariant,
    onSpriteSelected: (SpriteVariant) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DetailsCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = details.name,
                    modifier = Modifier.size(220.dp)
                )
                Text(
                    text = "#${details.id}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = details.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                if (details.types.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        details.types.forEach { type -> TypeChip(type = type) }
                    }
                }

                if (availableSprites.size > 1) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableSprites.forEach { sprite ->
                            FilterChip(
                                selected = sprite == selectedSprite,
                                onClick = { onSpriteSelected(sprite) },
                                label = { Text(text = sprite.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }

        DetailsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MeasurementColumn(label = "Altura", value = formatHeight(details.height))
                MeasurementColumn(label = "Peso", value = formatWeight(details.weight))
            }
        }

        if (details.stats.isNotEmpty()) {
            DetailsCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Estadísticas base",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    details.stats.forEach { stat -> StatRow(stat = stat) }
                }
            }
        }

        if (details.abilities.isNotEmpty()) {
            DetailsCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Habilidades",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    details.abilities.forEach { ability ->
                        Text(
                            text = ability.toTitleCase(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color.Black)
    ) {
        content()
    }
}

@Composable
private fun TypeChip(type: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(typeColor(type))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = typeLabel(type),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MeasurementColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatRow(stat: PokemonStat) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = statLabel(stat.name),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = stat.value.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(40.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (stat.value / MAX_BASE_STAT).coerceIn(0f, 1f))
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(statBarColor(stat.value))
            )
        }
    }
}

private fun statBarColor(value: Int): Color = when {
    value >= 120 -> Color(0xFF4CAF50)
    value >= 80 -> Color(0xFF8BC34A)
    value >= 50 -> Color(0xFFFFC107)
    else -> Color(0xFFF44336)
}
