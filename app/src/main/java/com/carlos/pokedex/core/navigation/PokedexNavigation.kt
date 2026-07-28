package com.carlos.pokedex.core.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.carlos.pokedex.R
import com.carlos.pokedex.dashboard.presentation.DashboardScreen
import com.carlos.pokedex.favorites.presentation.FavoritesScreen

sealed class PokedexDestination(val route: String, val label: String) {
    object Dashboard : PokedexDestination("dashboard", "Dashboard")
    object Favorites : PokedexDestination("favorites", "Favoritos")
}

private val bottomBarDestinations = listOf(PokedexDestination.Dashboard, PokedexDestination.Favorites)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokedexApp(navController: NavHostController = rememberNavController()) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.pokemon_center_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(containerColor = Color.Transparent) {
                    bottomBarDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (destination == PokedexDestination.Dashboard) {
                                        Icons.Default.Home
                                    } else {
                                        Icons.Default.Favorite
                                    },
                                    contentDescription = destination.label
                                )
                            },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = PokedexDestination.Dashboard.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(PokedexDestination.Dashboard.route) { DashboardScreen() }
                composable(PokedexDestination.Favorites.route) { FavoritesScreen() }
            }
        }
    }
}
