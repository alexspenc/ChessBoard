package com.example.chessboard

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import com.example.chessboard.ui.theme.ChessBoardTheme
import com.example.chessboard.ui.screen.TrainingScreenContainer

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            ChessBoardTheme {
                TrainingScreenContainer(activity = this@MainActivity)
            }
        }
    }
}