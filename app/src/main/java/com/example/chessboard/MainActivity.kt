package com.example.chessboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*

import com.example.chessboard.entity.GameEntity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.ui.theme.ChessBoardTheme
import com.example.chessboard.ui.screen.CreateOpeningScreenContainer
import com.example.chessboard.ui.screen.GameEditorScreenContainer
import com.example.chessboard.ui.screen.HomeScreenContainer
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.screen.TrainingScreenContainer
import com.example.chessboard.ui.screen.trainingTemplateScreen.CreateTrainingTemplateScreen


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        val dbProvider =  DatabaseProvider.createInstance(this)

        enableEdgeToEdge()
        setContent {
            ChessBoardTheme {
                println("Setup first screen")
                // var currentScreen by remember { mutableStateOf<ScreenType>(ScreenType.Home) }
                var currentScreen by remember { mutableStateOf<ScreenType>(ScreenType.TrainingTemplate) }
                var selectedGame by remember { mutableStateOf<GameEntity?>(null) }

                when (currentScreen) {

                    ScreenType.Training -> TrainingScreenContainer(
                        activity = this@MainActivity,
                        inDbProvider = dbProvider,
                        onBackClick = { currentScreen = ScreenType.Home },
                        onNavigate = { currentScreen = it },
                    )

                    ScreenType.CreateOpening -> CreateOpeningScreenContainer(
                        activity = this@MainActivity,
                        onBackClick = { currentScreen = ScreenType.Home },
                        inDbProvider = dbProvider,
                    )

                    ScreenType.GameEditor -> selectedGame?.let { game ->
                        GameEditorScreenContainer(
                            activity = this@MainActivity,
                            game = game,
                            onBackClick = { currentScreen = ScreenType.Home },
                            inDbProvider = dbProvider,
                        )
                    } ?: run {
                        currentScreen = ScreenType.Home
                    }

                    ScreenType.Home -> HomeScreenContainer(
                        activity = this@MainActivity,
                        onNavigate = { currentScreen = it },
                        onOpenGame = { game ->
                            selectedGame = game
                            currentScreen = ScreenType.GameEditor
                        },
                        inDbProvider = dbProvider,
                    )

                    ScreenType.TrainingTemplate -> {
                        println("Tru run TrainingTemplateScreen")
                        CreateTrainingTemplateScreen()
                    }

                    else -> currentScreen = ScreenType.Home
                }
            }
        }
    }
}