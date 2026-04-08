package com.example.chessboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.ui.screen.createOpening.CreateOpeningScreenContainer
import com.example.chessboard.ui.screen.BackupScreenContainer
import com.example.chessboard.ui.screen.GameEditorScreenContainer
import com.example.chessboard.ui.screen.gamesExplorer.GamesExplorerScreenContainer
import com.example.chessboard.ui.screen.HomeScreenContainer
import com.example.chessboard.ui.screen.PositionEditorScreenContainer
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.screen.ProfileScreenContainer
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.SettingsScreenContainer
import com.example.chessboard.ui.screen.trainSingleGame.TrainSingleGameLauncherScreenContainer
import com.example.chessboard.ui.screen.training.CreateTrainingByStatisticsScreenContainer
import com.example.chessboard.ui.screen.training.CreateTrainingChoiceScreenContainer
import com.example.chessboard.ui.screen.training.TrainingTemplateSelectionScreenContainer
import com.example.chessboard.ui.screen.training.TrainingTemplateBrowserScreenContainer
import com.example.chessboard.ui.screen.training.EditTrainingTemplatePlaceholderScreenContainer
import com.example.chessboard.ui.screen.training.CreateTrainingFromAllGamesScreenContainer
import com.example.chessboard.ui.screen.training.CreateTrainingFromGameIdsScreenContainer
import com.example.chessboard.ui.screen.training.CreateTrainingFromTemplateScreenContainer
import com.example.chessboard.ui.screen.training.EditTrainingScreenContainer
import com.example.chessboard.ui.screen.training.TrainingListScreenContainer
import com.example.chessboard.ui.theme.ChessBoardTheme

class MainActivity : ComponentActivity() {

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (!hasFocus) {
            return
        }

        hideSystemBars()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dbProvider = DatabaseProvider.createInstance(this)

        enableEdgeToEdge()
        hideSystemBars()
        setContent {
            ChessBoardTheme {
                var currentScreen by remember { mutableStateOf<ScreenType>(ScreenType.Home) }
                var selectedGame by remember { mutableStateOf<GameEntity?>(null) }
                var gamesExplorerSelectedGameId by remember { mutableStateOf<Long?>(null) }
                var gameEditorOnBackClick by remember { mutableStateOf<() -> Unit>({ currentScreen = ScreenType.GamesExplorer }) }
                var simpleViewEnabled by remember { mutableStateOf(false) }
                val runtimeContext = remember { RuntimeContext() }

                fun createScreenContext(
                    onBackClick: () -> Unit = {},
                    onNavigate: (ScreenType) -> Unit = { currentScreen = it },
                ): ScreenContainerContext {
                    return ScreenContainerContext(
                        onBackClick = onBackClick,
                        onNavigate = onNavigate,
                        inDbProvider = dbProvider,
                    )
                }

                when (val screen = currentScreen) {
                    ScreenType.Training -> TrainingListScreenContainer(
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.Home },
                        ),
                        onOpenTraining = { trainingId ->
                            runtimeContext.orderGamesInTraining.reset()
                            currentScreen = ScreenType.EditTraining(trainingId)
                        },
                    )

                    ScreenType.GamesExplorer -> GamesExplorerScreenContainer(
                        initialSelectedGameId = gamesExplorerSelectedGameId,
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.Home },
                        ),
                        onOpenGameEditor = { game ->
                            selectedGame = game
                            gamesExplorerSelectedGameId = game.id
                            gameEditorOnBackClick = { currentScreen = ScreenType.GamesExplorer }
                            currentScreen = ScreenType.GameEditor
                        },
                    )

                    ScreenType.CreateOpening -> CreateOpeningScreenContainer(
                        activity = this@MainActivity,
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.Home },
                        ),
                    )

                    ScreenType.PositionEditor -> PositionEditorScreenContainer(
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.Home },
                        ),
                    )

                    ScreenType.Backup -> BackupScreenContainer(
                        activity = this@MainActivity,
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.Home },
                        ),
                    )

                    ScreenType.CreateTrainingChoice -> CreateTrainingChoiceScreenContainer(
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.Home },
                        ),
                    )

                    ScreenType.TrainingTemplateSelection -> TrainingTemplateSelectionScreenContainer(
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.CreateTrainingChoice },
                        ),
                        onSelectTemplate = { templateId ->
                            currentScreen = ScreenType.CreateTrainingFromTemplate(templateId)
                        },
                    )

                    ScreenType.TrainingTemplates -> TrainingTemplateBrowserScreenContainer(
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.Home },
                        ),
                        onOpenTemplate = { templateId ->
                            currentScreen = ScreenType.EditTrainingTemplate(templateId)
                        },
                    )

                    ScreenType.CreateTrainingByStatistics -> CreateTrainingByStatisticsScreenContainer(
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.CreateTrainingChoice },
                        ),
                    )

                    ScreenType.CreateTraining -> CreateTrainingFromAllGamesScreenContainer(
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.CreateTrainingChoice },
                        ),
                    )

                    is ScreenType.CreateTrainingFromTemplate -> CreateTrainingFromTemplateScreenContainer(
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.TrainingTemplateSelection },
                        ),
                        templateId = screen.templateId,
                        screenTitle = "Create Training From Template",
                        gamesCountLabel = "Games loaded from template",
                    )

                    is ScreenType.CreateTrainingFromGameIds -> CreateTrainingFromGameIdsScreenContainer(
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.PositionEditor },
                        ),
                        gameIds = screen.gameIds,
                        screenTitle = "Create Training From Position",
                        gamesCountLabel = "Games found for position",
                    )

                    is ScreenType.EditTrainingTemplate -> EditTrainingTemplatePlaceholderScreenContainer(
                        templateId = screen.templateId,
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.TrainingTemplates },
                        ),
                    )

                    is ScreenType.EditTraining -> EditTrainingScreenContainer(
                        trainingId = screen.trainingId,
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.Training },
                        ),
                        orderGamesInTraining = runtimeContext.orderGamesInTraining,
                        onStartGameTrainingClick = { gameId ->
                            currentScreen = ScreenType.TrainSingleGame(screen.trainingId, gameId)
                        },
                        onOpenGameEditorClick = { game ->
                            selectedGame = game
                            gameEditorOnBackClick = { currentScreen = ScreenType.EditTraining(screen.trainingId) }
                            currentScreen = ScreenType.GameEditor
                        },
                    )

                    is ScreenType.TrainSingleGame -> TrainSingleGameLauncherScreenContainer(
                        trainingId = screen.trainingId,
                        gameId = screen.gameId,
                        onTrainingFinished = { result ->
                            runtimeContext.orderGamesInTraining.markGameCompleted(
                                gameId = result.gameId
                            )
                            currentScreen = ScreenType.EditTraining(screen.trainingId)
                        },
                        onOpenGameEditorClick = { game ->
                            selectedGame = game
                            gameEditorOnBackClick = {
                                currentScreen = ScreenType.TrainSingleGame(screen.trainingId, screen.gameId)
                            }
                            currentScreen = ScreenType.GameEditor
                        },
                        screenContext = createScreenContext(
                            onBackClick = {
                                currentScreen = ScreenType.Home
                            },
                        ),
                    )

                    ScreenType.GameEditor -> selectedGame?.let { game ->
                        GameEditorScreenContainer(
                            activity = this@MainActivity,
                            game = game,
                            screenContext = createScreenContext(
                                onBackClick = { gameEditorOnBackClick() },
                            ),
                        )
                    } ?: run {
                        currentScreen = ScreenType.GamesExplorer
                    }

                    ScreenType.Home -> HomeScreenContainer(
                        activity = this@MainActivity,
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.Home },
                        ),
                        simpleViewEnabled = simpleViewEnabled,
                        onCreateTrainingClick = {
                            currentScreen = ScreenType.CreateTrainingChoice
                        },
                        onOpenPositionEditorClick = {
                            currentScreen = ScreenType.PositionEditor
                        },
                    )

                    ScreenType.Profile -> ProfileScreenContainer(
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.Home },
                        ),
                    )

                    ScreenType.Settings -> SettingsScreenContainer(
                        simpleViewEnabled = simpleViewEnabled,
                        onSimpleViewToggle = { simpleViewEnabled = it },
                        onBackClick = { currentScreen = ScreenType.Profile },
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.Profile },
                        ),
                    )

                    else -> currentScreen = ScreenType.Home
                }
            }
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}
