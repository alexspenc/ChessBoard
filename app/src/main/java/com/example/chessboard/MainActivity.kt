package com.example.chessboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.ui.theme.ChessBoardTheme
import com.example.chessboard.ui.screen.CreateOpeningScreenContainer
import com.example.chessboard.ui.screen.CreateTrainingScreenContainer
import com.example.chessboard.ui.screen.GameEditorScreenContainer
import com.example.chessboard.ui.screen.HomeScreenContainer
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.screen.TrainSingleGameScreenContainer
import com.example.chessboard.ui.screen.TrainingScreenContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        val dbProvider =  DatabaseProvider.createInstance(this)

        enableEdgeToEdge()
        setContent {
            ChessBoardTheme {
                val scope = rememberCoroutineScope()

                var currentScreen by remember { mutableStateOf<ScreenType>(ScreenType.Home) }
                var selectedGame by remember { mutableStateOf<GameEntity?>(null) }

                // Data for start one game training
                // TODO look like this is wrong place for this data
                var selectedTrainingGameId by remember { mutableStateOf<Long?>(null) }
                var selectedTrainingId by remember { mutableStateOf<Long?>(null) }

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

                    ScreenType.CreateTraining -> CreateTrainingScreenContainer(
                        activity = this@MainActivity,
                        onBackClick = { currentScreen = ScreenType.Home },
                        onNavigate = { currentScreen = it },
                        inDbProvider = dbProvider,
                    )

                    ScreenType.TrainSingleGame -> {
                        val gameId = selectedTrainingGameId
                        val trainingId = selectedTrainingId

                        if (gameId == null || trainingId == null) {
                            currentScreen = ScreenType.Home
                        } else {
                            TrainSingleGameScreenContainer(
                                activity = this@MainActivity,
                                gameId = gameId,
                                trainingId = trainingId,
                                onTrainingFinished = {
                                    currentScreen = ScreenType.Home
                                },
                                onBackClick = { currentScreen = ScreenType.Home },
                                onNavigate = { currentScreen = it },
                                inDbProvider = dbProvider,
                            )
                        }
                    }

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
                        onCreateTrainingClick = {
                            currentScreen = ScreenType.CreateTraining
                        },
                        onStartFirstTrainingClick = {
                            scope.launch {
                                val launchData = withContext(Dispatchers.IO) {
                                    dbProvider.getFirstTrainingGameLaunchData()
                                } ?: return@launch
                                selectedTrainingGameId = launchData.gameId
                                selectedTrainingId = launchData.trainingId
                                currentScreen = ScreenType.TrainSingleGame
                            }
                        },
                        inDbProvider = dbProvider,
                    )

                    else -> currentScreen = ScreenType.Home
                }
            }
        }
    }
}
