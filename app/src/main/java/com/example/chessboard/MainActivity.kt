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
import com.example.chessboard.runtimecontext.RuntimeContext
import com.example.chessboard.ui.screen.analysis.GameAnalysisInitialPosition
import com.example.chessboard.ui.screen.analysis.GameAnalysisScreenContainer
import com.example.chessboard.ui.screen.createOpening.CreateOpeningScreenContainer
import com.example.chessboard.ui.screen.BackupScreenContainer
import com.example.chessboard.ui.screen.GameEditorScreenContainer
import com.example.chessboard.ui.screen.gamesExplorer.GamesExplorerScreenContainer
import com.example.chessboard.ui.screen.HomeScreenContainer
import com.example.chessboard.ui.screen.openingDeviation.OpeningDeviationDisplayScreen
import com.example.chessboard.ui.screen.openingDeviation.OpeningDeviationSelectionScreenContainer
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
import com.example.chessboard.ui.screen.training.train.TrainingSettingsScreenContainer
import com.example.chessboard.ui.screen.training.TrainingListScreenContainer
import com.example.chessboard.ui.theme.ChessBoardTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                var gamesExplorerOnBackClick by remember { mutableStateOf<() -> Unit>({ currentScreen = ScreenType.Home }) }
                var gameEditorOnBackClick by remember { mutableStateOf<() -> Unit>({ currentScreen = ScreenType.GamesExplorer }) }
                var createOpeningDraft by remember { mutableStateOf(GameDraft()) }
                var createOpeningOnBackClick by remember { mutableStateOf<() -> Unit>({ currentScreen = ScreenType.Home }) }
                var simpleViewEnabled by remember { mutableStateOf(false) }
                var removeLineIfRepIsZero by remember { mutableStateOf(true) }
                var hideLinesWithWeightZero by remember { mutableStateOf(false) }
                var profileLoaded by remember { mutableStateOf(false) }
                val runtimeContext = remember { RuntimeContext() }
                val scope = rememberCoroutineScope()
                val userProfileService = remember { dbProvider.createUserProfileService() }

                LaunchedEffect(Unit) {
                    val profile = userProfileService.getProfile()
                    simpleViewEnabled = profile.simpleViewEnabled
                    removeLineIfRepIsZero = profile.removeLineIfRepIsZero
                    hideLinesWithWeightZero = profile.hideLinesWithWeightZero
                    profileLoaded = true
                }

                fun openGamesExplorer() {
                    scope.launch {
                        runtimeContext.gamesExplorer.loadAllGameIds(dbProvider)
                        gamesExplorerSelectedGameId = null
                        gamesExplorerOnBackClick = { currentScreen = ScreenType.Home }
                        currentScreen = ScreenType.GamesExplorer
                    }
                }

                fun openGamesExplorerForOpeningDeviationBranch(branchFen: String) {
                    scope.launch {
                        val gameIds = withContext(Dispatchers.IO) {
                            dbProvider.findGameIdsByFenWithoutMoveNumber(branchFen)
                        }

                        runtimeContext.gamesExplorer.setGameIds(gameIds)
                        gamesExplorerSelectedGameId = null
                        gamesExplorerOnBackClick = {
                            currentScreen = ScreenType.ShowOpeningDeviation
                        }
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

                fun navigateToOpeningDeviationSelection() {
                    currentScreen = ScreenType.SelectOpeningDeviationPosition
                }

                fun navigateToOpeningDeviationDisplay() {
                    val selectedDeviationItem = runtimeContext.openingDeviation.selectedDeviationItem()
                    if (selectedDeviationItem == null) {
                        navigateToOpeningDeviationSelection()
                        return
                    }

                    currentScreen = ScreenType.ShowOpeningDeviation
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
                            onBackClick = { gamesExplorerOnBackClick() },
                        ),
                        onCloneGameClick = { draft ->
                            createOpeningDraft = draft
                            createOpeningOnBackClick = { currentScreen = ScreenType.GamesExplorer }
                            currentScreen = ScreenType.CreateOpening
                        },
                        onAnalyzeGameClick = { uciMoves, initialPly ->
                            openGameAnalysis(
                                uciMoves = uciMoves,
                                initialPly = initialPly,
                                backTarget = ScreenType.GamesExplorer,
                            )
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
                        onShowOpeningDeviationSelection = { sourcePositionFen, deviationItems ->
                            runtimeContext.openingDeviation.setDeviationItems(
                                sourcePositionFen = sourcePositionFen,
                                deviationItems = deviationItems,
                            )
                            currentScreen = ScreenType.SelectOpeningDeviationPosition
                        },
                    )

                    ScreenType.SelectOpeningDeviationPosition -> OpeningDeviationSelectionScreenContainer(
                        deviationItems = runtimeContext.openingDeviation.deviationItems,
                        selectedDeviationIndex = runtimeContext.openingDeviation.selectedDeviationIndex,
                        onDeviationSelected = { index ->
                            runtimeContext.openingDeviation.selectDeviation(index)
                        },
                        onStartClick = {
                            navigateToOpeningDeviationDisplay()
                        },
                        onBackClick = { currentScreen = ScreenType.SavedPositions },
                    )

                    ScreenType.ShowOpeningDeviation -> runtimeContext.openingDeviation.selectedDeviationItem()?.let { deviationItem ->
                        OpeningDeviationDisplayScreen(
                            deviationItem = deviationItem,
                            selectedBranchIndex = runtimeContext.openingDeviation.selectedBranchIndex,
                            onBranchSelected = { index ->
                                runtimeContext.openingDeviation.selectBranch(index)
                            },
                            onOpenGamesClick = { branch ->
                                openGamesExplorerForOpeningDeviationBranch(branch.resultFen)
                            },
                            onBackClick = { currentScreen = ScreenType.SelectOpeningDeviationPosition },
                        )
                    } ?: run {
                        navigateToOpeningDeviationSelection()
                    }

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
                        simpleViewEnabled = simpleViewEnabled,
                        onStartGameTrainingClick = { gameId, orderedGameIds ->
                            runtimeContext.trainingOrderedGameIds = orderedGameIds
                            currentScreen = ScreenType.TrainSingleGame(screen.trainingId, gameId)
                        },
                        onAnalyzeGameClick = { uciMoves, initialPly ->
                            openGameAnalysis(
                                uciMoves = uciMoves,
                                initialPly = initialPly,
                                backTarget = ScreenType.EditTraining(screen.trainingId),
                            )
                        },
                        onOpenGameEditorClick = { game ->
                            selectedGame = game
                            gameEditorOnBackClick = { currentScreen = ScreenType.EditTraining(screen.trainingId) }
                            currentScreen = ScreenType.GameEditor
                        },
                        onOpenSettingsClick = {
                            currentScreen = ScreenType.TrainingSettings(screen.trainingId)
                        },
                    )

                    is ScreenType.TrainingSettings -> TrainingSettingsScreenContainer(
                        trainingId = screen.trainingId,
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.EditTraining(screen.trainingId) },
                        ),
                        initialMoveFrom = runtimeContext.trainingMoveFrom,
                        initialMoveTo = runtimeContext.trainingMoveTo,
                        onMoveRangeChange = { from, to ->
                            runtimeContext.trainingMoveFrom = from
                            runtimeContext.trainingMoveTo = to
                        },
                        onBackClick = { currentScreen = ScreenType.EditTraining(screen.trainingId) },
                    )

                    is ScreenType.TrainSingleGame -> TrainSingleGameLauncherScreenContainer(
                        trainingId = screen.trainingId,
                        gameId = screen.gameId,
                        moveFrom = runtimeContext.trainingMoveFrom,
                        moveTo = runtimeContext.trainingMoveTo,
                        keepLineIfZero = !removeLineIfRepIsZero,
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
                                    removeLineIfRepIsZero = removeLineIfRepIsZero,
                                    hideLinesWithWeightZero = hideLinesWithWeightZero,
                                )
                            }
                        },
                        removeLineIfRepIsZero = removeLineIfRepIsZero,
                        onRemoveLineIfRepIsZeroToggle = { newValue ->
                            removeLineIfRepIsZero = newValue
                            scope.launch {
                                userProfileService.updateSettings(
                                    simpleViewEnabled = simpleViewEnabled,
                                    removeLineIfRepIsZero = newValue,
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
                                    removeLineIfRepIsZero = removeLineIfRepIsZero,
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
                        keepLineIfZero = !removeLineIfRepIsZero,
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
