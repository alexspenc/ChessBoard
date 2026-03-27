package com.example.chessboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.ui.screen.CreateOpeningScreenContainer
import com.example.chessboard.ui.screen.CreateTrainingScreenContainer
import com.example.chessboard.ui.screen.GameEditorScreenContainer
import com.example.chessboard.ui.screen.GamesExplorerScreenContainer
import com.example.chessboard.ui.screen.HomeScreenContainer
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.screen.TrainingListScreenContainer
import com.example.chessboard.ui.screen.trainSingleGame.TrainSingleGameLauncherScreenContainer
import com.example.chessboard.ui.theme.ChessBoardTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dbProvider = DatabaseProvider.createInstance(this)

        enableEdgeToEdge()
        setContent {
            ChessBoardTheme {
                var currentScreen by remember { mutableStateOf<ScreenType>(ScreenType.Home) }
                var selectedGame by remember { mutableStateOf<GameEntity?>(null) }

                when (val screen = currentScreen) {
                    ScreenType.Training -> TrainingListScreenContainer(
                        activity = this@MainActivity,
                        inDbProvider = dbProvider,
                        onBackClick = { currentScreen = ScreenType.Home },
                        onNavigate = { currentScreen = it },
                        onOpenTraining = { trainingId ->
                            currentScreen = ScreenType.CreateTraining(trainingId)
                        },
                    )

                    ScreenType.GamesExplorer -> GamesExplorerScreenContainer(
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

                    is ScreenType.CreateTraining -> CreateTrainingScreenContainer(
                        trainingId = screen.trainingId,
                        activity = this@MainActivity,
                        onBackClick = { currentScreen = ScreenType.Home },
                        onNavigate = { currentScreen = it },
                        onStartGameTrainingClick = { gameId ->
                            val trainingId = screen.trainingId ?: return@CreateTrainingScreenContainer
                            currentScreen = ScreenType.TrainSingleGame(trainingId, gameId)
                        },
                        inDbProvider = dbProvider,
                    )

                    is ScreenType.TrainSingleGame -> TrainSingleGameLauncherScreenContainer(
                        trainingId = screen.trainingId,
                        gameId = screen.gameId,
                        onTrainingFinished = {
                            currentScreen = ScreenType.Home
                        },
                        onBackClick = {
                            currentScreen = ScreenType.Home
                        },
                        onNavigate = { currentScreen = it },
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
                        onCreateTrainingClick = {
                            currentScreen = ScreenType.CreateTraining(null)
                        },
                        onStartFirstTrainingClick = {
                            currentScreen = ScreenType.Training
                        },
                    )

                    else -> currentScreen = ScreenType.Home
                }
            }
        }
    }
}
