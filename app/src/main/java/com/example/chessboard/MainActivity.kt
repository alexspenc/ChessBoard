package com.example.chessboard

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*

import com.example.chessboard.ui.theme.ChessBoardTheme
import com.example.chessboard.ui.screen.HomeScreen
import com.example.chessboard.ui.screen.TrainingScreenContainer

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            ChessBoardTheme {
                var currentScreen by remember { mutableStateOf("Home") }
                when (currentScreen) {
                    "Training" -> TrainingScreenContainer(
                        activity = this@MainActivity,
                        onBackClick = { currentScreen = "Home" },
                        onNavigate = { currentScreen = it }
                    )
                    else -> HomeScreen(onNavigate = { currentScreen = it })
                }
            }
        }
    }
}
