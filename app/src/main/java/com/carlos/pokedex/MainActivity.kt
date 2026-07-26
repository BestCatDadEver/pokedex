package com.carlos.pokedex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.carlos.pokedex.dashboard.presentation.DashboardScreen
import com.carlos.pokedex.dashboard.presentation.DashboardViewModel
import com.carlos.pokedex.ui.theme.PokedexTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PokedexTheme {
                DashboardScreen()
            }
        }
    }
}

