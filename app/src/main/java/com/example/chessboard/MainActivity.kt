package com.example.chessboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.chessboard.boardmodel.GameDraft
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.ui.screen.analysis.GameAnalysisInitialPosition
import com.example.chessboard.ui.screen.analysis.GameAnalysisScreenContainer
import com.example.chessboard.ui.screen.createOpening.CreateOpeningScreenContainer
import com.example.chessboard.ui.screen.BackupScreenContainer
import com.example.chessboard.ui.screen.GameEditorScreenContainer
import com.example.chessboard.ui.screen.gamesExplorer.GamesExplorerScreenContainer
import com.example.chessboard.ui.screen.HomeScreenContainer
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.screen.ProfileScreenContainer
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.SettingsScreenContainer
import com.example.chessboard.ui.screen.SmartTrainingScreenContainer
import com.example.chessboard.ui.screen.positions.PositionEditorScreenContainer
import com.example.chessboard.ui.screen.positions.SavedPositionsScreenContainer
import com.example.chessboard.ui.screen.trainSingleGame.TrainSingleGameLauncherScreenContainer
import com.example.chessboard.ui.screen.training.CreateTrainingByStatisticsScreenContainer
import com.example.chessboard.ui.screen.training.CreateTrainingChoiceScreenContainer
import com.example.chessboard.ui.screen.training.TrainingTemplateSelectionScreenContainer
import com.example.chessboard.ui.screen.training.TrainingTemplateBrowserScreenContainer
import com.example.chessboard.ui.screen.training.template.EditTrainingTemplateScreenContainer
import com.example.chessboard.ui.screen.training.CreateTrainingFromAllGamesScreenContainer
import com.example.chessboard.ui.screen.training.CreateTrainingFromGameIdsScreenContainer
import com.example.chessboard.ui.screen.training.CreateTrainingFromTemplateScreenContainer
import com.example.chessboard.ui.screen.training.train.EditTrainingScreenContainer
import com.example.chessboard.ui.screen.training.TrainingListScreenContainer
import com.example.chessboard.ui.theme.ChessBoardTheme
import kotlinx.coroutines.launch

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
                var createOpeningDraft by remember { mutableStateOf(GameDraft()) }
                var createOpeningOnBackClick by remember { mutableStateOf<() -> Unit>({ currentScreen = ScreenType.Home }) }
                var simpleViewEnabled by remember { mutableStateOf(false) }
                var dontRemoveLineIfRepIsZero by remember { mutableStateOf(false) }
                var hideLinesWithWeightZero by remember { mutableStateOf(false) }
                var profileLoaded by remember { mutableStateOf(false) }
                val runtimeContext = remember { RuntimeContext() }
                val scope = rememberCoroutineScope()
                val userProfileService = remember { dbProvider.createUserProfileService() }

                LaunchedEffect(Unit) {
                    val profile = userProfileService.getProfile()
                    simpleViewEnabled = profile.simpleViewEnabled
                    dontRemoveLineIfRepIsZero = profile.dontRemoveLineIfRepIsZero
                    hideLinesWithWeightZero = profile.hideLinesWithWeightZero
                    profileLoaded = true
                }

                fun openGamesExplorer() {
                    scope.launch {
                        runtimeContext.gamesExplorer.loadAllGameIds(dbProvider)
                        gamesExplorerSelectedGameId = null
                        currentScreen = ScreenType.GamesExplorer
                    }
                }

                fun createScreenContext(
                    onBackClick: () -> Unit = {},
                    onNavigate: (ScreenType) -> Unit = navigation@ { screenType ->
                        if (screenType == ScreenType.GamesExplorer) {
                            openGamesExplorer()
                            return@navigation
                        }

                        currentScreen = screenType
                    },
                ): ScreenContainerContext {
                    return ScreenContainerContext(
                        onBackClick = onBackClick,
                        onNavigate = onNavigate,
                        inDbProvider = dbProvider,
                    )
                }

                fun openGameAnalysis(
                    uciMoves: List<String>,
                    initialPly: Int,
                    backTarget: ScreenType,
                ) {
                    currentScreen = ScreenType.AnalyzeGame(
                        uciMoves = uciMoves,
                        initialPly = initialPly,
                        backTarget = backTarget,
                    )
                }

                if (!profileLoaded) return@ChessBoardTheme

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
                        observableGamesPage = runtimeContext.gamesExplorer,
                        initialSelectedGameId = gamesExplorerSelectedGameId,
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.Home },
                        ),
                        onCloneGameClick = { draft ->
                            createOpeningDraft = draft
                            createOpeningOnBackClick = { currentScreen = ScreenType.GamesExplorer }
                            currentScreen = ScreenType.CreateOpening
                        },
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
                            onBackClick = { createOpeningOnBackClick() },
                        ),
                        initialDraft = createOpeningDraft,
                    )

                    ScreenType.PositionEditor -> PositionEditorScreenContainer(
                        initialFen = runtimeContext.positionEditor.initialFen,
                        screenContext = createScreenContext(
                            onBackClick = { runtimeContext.positionEditor.onBackClick() },
                        ),
                    )

                    ScreenType.SavedPositions -> SavedPositionsScreenContainer(
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.Home },
                        ),
                        onOpenPositionEditor = { fen ->
                            runtimeContext.positionEditor.initialFen = fen
                            runtimeContext.positionEditor.onBackClick = {
                                currentScreen = ScreenType.SavedPositions
                            }
                            currentScreen = ScreenType.PositionEditor
                        },
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
                            onBackClick = { currentScreen = screen.backTarget },
                        ),
                        gameIds = screen.gameIds,
                        screenTitle = "Create Training From Position",
                        gamesCountLabel = "Games found for position",
                    )

                    is ScreenType.EditTrainingTemplate -> EditTrainingTemplateScreenContainer(
                        templateId = screen.templateId,
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.TrainingTemplates },
                        ),
                        onOpenGameEditorClick = { game ->
                            selectedGame = game
                            gameEditorOnBackClick = {
                                currentScreen = ScreenType.EditTrainingTemplate(screen.templateId)
                            }
                            currentScreen = ScreenType.GameEditor
                        },
                    )

                    is ScreenType.EditTraining -> EditTrainingScreenContainer(
                        trainingId = screen.trainingId,
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.Training },
                        ),
                        orderGamesInTraining = runtimeContext.orderGamesInTraining,
                        hideLinesWithWeightZero = hideLinesWithWeightZero,
                        onStartGameTrainingClick = { gameId, moveFrom, moveTo, orderedGameIds ->
                            runtimeContext.trainingMoveFrom = moveFrom
                            runtimeContext.trainingMoveTo = moveTo
                            runtimeContext.trainingOrderedGameIds = orderedGameIds
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
                        moveFrom = runtimeContext.trainingMoveFrom,
                        moveTo = runtimeContext.trainingMoveTo,
                        keepLineIfZero = dontRemoveLineIfRepIsZero,
                        hasNextTrainingGame = runtimeContext.resolveNextTrainingGameId(screen.gameId) != null,
                        sessionCurrent = runtimeContext.trainingOrderedGameIds.indexOf(screen.gameId).coerceAtLeast(0) + 1,
                        sessionTotal = runtimeContext.trainingOrderedGameIds.size,
                        onTrainingFinished = { result ->
                            runtimeContext.orderGamesInTraining.markGameCompleted(
                                gameId = result.gameId
                            )
                            currentScreen = ScreenType.EditTraining(screen.trainingId)
                        },
                        onNextTrainingClick = { result ->
                            runtimeContext.orderGamesInTraining.markGameCompleted(
                                gameId = result.gameId
                            )
                            val nextGameId = runtimeContext.resolveNextTrainingGameId(result.gameId)
                            currentScreen = ScreenType.EditTraining(screen.trainingId)
                            if (nextGameId != null) {
                                currentScreen = ScreenType.TrainSingleGame(screen.trainingId, nextGameId)
                            }
                        },
                        onOpenGameEditorClick = { game ->
                            selectedGame = game
                            gameEditorOnBackClick = {
                                currentScreen = ScreenType.TrainSingleGame(screen.trainingId, screen.gameId)
                            }
                            currentScreen = ScreenType.GameEditor
                        },
                        onCloneGameClick = { draft ->
                            createOpeningDraft = draft
                            createOpeningOnBackClick = {
                                currentScreen = ScreenType.TrainSingleGame(
                                    screen.trainingId,
                                    screen.gameId
                                )
                            }
                            currentScreen = ScreenType.CreateOpening
                        },
                        onSearchByPositionClick = { fen ->
                            runtimeContext.positionEditor.initialFen = fen
                            runtimeContext.positionEditor.onBackClick = {
                                currentScreen = ScreenType.TrainSingleGame(
                                    screen.trainingId,
                                    screen.gameId
                                )
                            }
                            currentScreen = ScreenType.PositionEditor
                        },
                        onAnalyzeGameClick = { uciMoves, initialPly ->
                            openGameAnalysis(
                                uciMoves = uciMoves,
                                initialPly = initialPly,
                                backTarget = ScreenType.TrainSingleGame(
                                    screen.trainingId,
                                    screen.gameId,
                                ),
                            )
                        },
                        screenContext = createScreenContext(
                            onBackClick = {
                                currentScreen = ScreenType.Home
                            },
                        ),
                    )

                    is ScreenType.AnalyzeGame -> GameAnalysisScreenContainer(
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = screen.backTarget },
                        ),
                        initialPosition = GameAnalysisInitialPosition.FromGameLine(
                            uciMoves = screen.uciMoves,
                            initialPly = screen.initialPly,
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
                        openGamesExplorer()
                    }

                    ScreenType.Home -> HomeScreenContainer(
                        activity = this@MainActivity,
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.Home },
                        ),
                        simpleViewEnabled = simpleViewEnabled,
                        onCreateOpeningClick = {
                            createOpeningDraft = GameDraft()
                            createOpeningOnBackClick = { currentScreen = ScreenType.Home }
                            currentScreen = ScreenType.CreateOpening
                        },
                        onCreateTrainingClick = {
                            currentScreen = ScreenType.CreateTrainingChoice
                        },
                        onOpenPositionEditorClick = {
                            runtimeContext.positionEditor.resetToInitialPosition()
                            runtimeContext.positionEditor.onBackClick = { currentScreen = ScreenType.Home }
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
                        onSimpleViewToggle = { newValue ->
                            simpleViewEnabled = newValue
                            scope.launch {
                                userProfileService.updateSettings(
                                    simpleViewEnabled = newValue,
                                    dontRemoveLineIfRepIsZero = dontRemoveLineIfRepIsZero,
                                    hideLinesWithWeightZero = hideLinesWithWeightZero,
                                )
                            }
                        },
                        dontRemoveLineIfRepIsZero = dontRemoveLineIfRepIsZero,
                        onDontRemoveLineIfRepIsZeroToggle = { newValue ->
                            dontRemoveLineIfRepIsZero = newValue
                            scope.launch {
                                userProfileService.updateSettings(
                                    simpleViewEnabled = simpleViewEnabled,
                                    dontRemoveLineIfRepIsZero = newValue,
                                    hideLinesWithWeightZero = hideLinesWithWeightZero,
                                )
                            }
                        },
                        hideLinesWithWeightZero = hideLinesWithWeightZero,
                        onHideLinesWithWeightZeroToggle = { newValue ->
                            hideLinesWithWeightZero = newValue
                            scope.launch {
                                userProfileService.updateSettings(
                                    simpleViewEnabled = simpleViewEnabled,
                                    dontRemoveLineIfRepIsZero = dontRemoveLineIfRepIsZero,
                                    hideLinesWithWeightZero = newValue,
                                )
                            }
                        },
                        onBackClick = { currentScreen = ScreenType.Home },
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.Home },
                        ),
                    )

                    ScreenType.SmartTraining -> SmartTrainingScreenContainer(
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.Home },
                        ),
                        onStartTraining = { queue ->
                            val first = queue.firstOrNull()
                            if (first != null) {
                                runtimeContext.smartTrainingQueue = queue
                                currentScreen = ScreenType.SmartTrainGame(first.trainingId, first.gameId)
                            }
                        },
                    )

                    is ScreenType.SmartTrainGame -> TrainSingleGameLauncherScreenContainer(
                        trainingId = screen.trainingId,
                        gameId = screen.gameId,
                        keepLineIfZero = dontRemoveLineIfRepIsZero,
                        hasNextTrainingGame = runtimeContext.resolveNextSmartGamePair(screen.gameId) != null,
                        sessionCurrent = runtimeContext.smartTrainingQueue.indexOfFirst { it.gameId == screen.gameId }.coerceAtLeast(0) + 1,
                        sessionTotal = runtimeContext.smartTrainingQueue.size,
                        onTrainingFinished = {
                            currentScreen = ScreenType.SmartTraining
                        },
                        onNextTrainingClick = { result ->
                            val next = runtimeContext.resolveNextSmartGamePair(result.gameId)
                            if (next != null) {
                                currentScreen = ScreenType.SmartTrainGame(next.trainingId, next.gameId)
                            } else {
                                currentScreen = ScreenType.SmartTraining
                            }
                        },
                        onOpenGameEditorClick = { game ->
                            selectedGame = game
                            gameEditorOnBackClick = {
                                currentScreen = ScreenType.SmartTrainGame(screen.trainingId, screen.gameId)
                            }
                            currentScreen = ScreenType.GameEditor
                        },
                        onCloneGameClick = { draft ->
                            createOpeningDraft = draft
                            createOpeningOnBackClick = {
                                currentScreen = ScreenType.SmartTrainGame(screen.trainingId, screen.gameId)
                            }
                            currentScreen = ScreenType.CreateOpening
                        },
                        onSearchByPositionClick = { fen ->
                            runtimeContext.positionEditor.initialFen = fen
                            runtimeContext.positionEditor.onBackClick = {
                                currentScreen = ScreenType.SmartTrainGame(screen.trainingId, screen.gameId)
                            }
                            currentScreen = ScreenType.PositionEditor
                        },
                        onAnalyzeGameClick = { uciMoves, initialPly ->
                            openGameAnalysis(
                                uciMoves = uciMoves,
                                initialPly = initialPly,
                                backTarget = ScreenType.SmartTrainGame(
                                    screen.trainingId,
                                    screen.gameId,
                                ),
                            )
                        },
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.SmartTraining },
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
