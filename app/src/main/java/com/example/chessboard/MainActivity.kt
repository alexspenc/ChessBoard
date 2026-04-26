package com.example.chessboard

/**
 * Legacy mixed-responsibility file.
 * Current role: groups the app entry activity, app-level screen switching, and cross-flow navigation wiring.
 * This file is not cleanly scoped and should not be treated as a good target for new unrelated logic.
 * Allowed here for now:
 * - top-level activity setup and app-shell screen dispatch
 * - thin adapters that map flow results onto app-owned state such as current screen or shared drafts
 * Prefer not to add here:
 * - new training-flow transition rules or runtime bookkeeping
 * - screen-specific business logic that belongs in dedicated screen or flow files
 * Validation date: 2026-04-26
 */

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
import com.example.chessboard.ui.screen.SmartSettingsScreenContainer
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
import com.example.chessboard.ui.screen.training.flow.RegularTrainingFlowCoordinator
import com.example.chessboard.ui.screen.training.flow.SmartTrainingFlowCoordinator
import com.example.chessboard.ui.screen.training.flow.TrainingFlowResult
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
                val regularTrainingFlow = remember(runtimeContext) {
                    RegularTrainingFlowCoordinator(runtimeContext)
                }
                val smartTrainingFlow = remember(runtimeContext) {
                    SmartTrainingFlowCoordinator(runtimeContext)
                }
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

                fun applyTrainingFlowResult(result: TrainingFlowResult) {
                    when (result) {
                        is TrainingFlowResult.Navigate -> {
                            currentScreen = result.screen
                        }

                        is TrainingFlowResult.OpenGameEditor -> {
                            selectedGame = result.game
                            gameEditorOnBackClick = { currentScreen = result.backTarget }
                            currentScreen = ScreenType.GameEditor
                        }

                        is TrainingFlowResult.OpenCreateOpening -> {
                            createOpeningDraft = result.draft
                            createOpeningOnBackClick = { currentScreen = result.backTarget }
                            currentScreen = ScreenType.CreateOpening
                        }

                        is TrainingFlowResult.OpenPositionEditor -> {
                            runtimeContext.positionEditor.initialFen = result.initialFen
                            runtimeContext.positionEditor.onBackClick = {
                                currentScreen = result.backTarget
                            }
                            currentScreen = ScreenType.PositionEditor
                        }

                        is TrainingFlowResult.OpenAnalysis -> {
                            openGameAnalysis(
                                uciMoves = result.uciMoves,
                                initialPly = result.initialPly,
                                backTarget = result.backTarget,
                            )
                        }
                    }
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
                            applyTrainingFlowResult(
                                regularTrainingFlow.openTraining(trainingId)
                            )
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
                        trainingRuntimeContext = runtimeContext.trainingSession,
                        hideLinesWithWeightZero = hideLinesWithWeightZero,
                        simpleViewEnabled = simpleViewEnabled,
                        onStartGameTrainingClick = { gameId, orderedGameIds ->
                            applyTrainingFlowResult(
                                regularTrainingFlow.startGame(
                                    trainingId = screen.trainingId,
                                    gameId = gameId,
                                    orderedGameIds = orderedGameIds,
                                )
                            )
                        },
                        onAnalyzeGameClick = { uciMoves, initialPly ->
                            applyTrainingFlowResult(
                                regularTrainingFlow.openAnalysisFromEditor(
                                    trainingId = screen.trainingId,
                                    uciMoves = uciMoves,
                                    initialPly = initialPly,
                                )
                            )
                        },
                        onOpenGameEditorClick = { game ->
                            applyTrainingFlowResult(
                                regularTrainingFlow.openGameEditorFromEditor(
                                    game = game,
                                    trainingId = screen.trainingId,
                                )
                            )
                        },
                        onOpenSettingsClick = {
                            applyTrainingFlowResult(
                                regularTrainingFlow.openSettings(screen.trainingId)
                            )
                        },
                    )

                    is ScreenType.TrainingSettings -> TrainingSettingsScreenContainer(
                        trainingId = screen.trainingId,
                        screenContext = createScreenContext(
                            onBackClick = {
                                applyTrainingFlowResult(
                                    regularTrainingFlow.closeSettings(screen.trainingId)
                                )
                            },
                        ),
                        initialMoveFrom = runtimeContext.trainingMoveFrom,
                        initialMoveTo = runtimeContext.trainingMoveTo,
                        onMoveRangeChange = { from, to ->
                            runtimeContext.trainingMoveFrom = from
                            runtimeContext.trainingMoveTo = to
                        },
                        onBackClick = {
                            applyTrainingFlowResult(
                                regularTrainingFlow.closeSettings(screen.trainingId)
                            )
                        },
                    )

                    is ScreenType.TrainSingleGame -> TrainSingleGameLauncherScreenContainer(
                        trainingId = screen.trainingId,
                        gameId = screen.gameId,
                        moveFrom = runtimeContext.trainingMoveFrom,
                        moveTo = runtimeContext.trainingMoveTo,
                        keepLineIfZero = !removeLineIfRepIsZero,
                        trainingRuntimeContext = runtimeContext.trainingSession,
                        hasNextTrainingGame = regularTrainingFlow.hasNextGame(
                            trainingId = screen.trainingId,
                            gameId = screen.gameId,
                        ),
                        sessionCurrent = regularTrainingFlow.sessionCurrent(
                            trainingId = screen.trainingId,
                            gameId = screen.gameId,
                        ),
                        sessionTotal = regularTrainingFlow.sessionTotal(screen.trainingId),
                        onTrainingFinished = { result ->
                            applyTrainingFlowResult(
                                regularTrainingFlow.finishGame(result)
                            )
                        },
                        onNextTrainingClick = { result ->
                            applyTrainingFlowResult(
                                regularTrainingFlow.openNextGame(result)
                            )
                        },
                        onOpenGameEditorClick = { game ->
                            applyTrainingFlowResult(
                                regularTrainingFlow.openGameEditorFromTraining(
                                    game = game,
                                    trainingId = screen.trainingId,
                                    gameId = screen.gameId,
                                )
                            )
                        },
                        onCloneGameClick = { draft ->
                            applyTrainingFlowResult(
                                regularTrainingFlow.openCreateOpeningFromTraining(
                                    draft = draft,
                                    trainingId = screen.trainingId,
                                    gameId = screen.gameId,
                                )
                            )
                        },
                        onSearchByPositionClick = { fen ->
                            applyTrainingFlowResult(
                                regularTrainingFlow.openPositionEditorFromTraining(
                                    fen = fen,
                                    trainingId = screen.trainingId,
                                    gameId = screen.gameId,
                                )
                            )
                        },
                        onAnalyzeGameClick = { uciMoves, initialPly ->
                            applyTrainingFlowResult(
                                regularTrainingFlow.openAnalysisFromTraining(
                                    trainingId = screen.trainingId,
                                    gameId = screen.gameId,
                                    uciMoves = uciMoves,
                                    initialPly = initialPly,
                                )
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

                    ScreenType.SmartSettings -> SmartSettingsScreenContainer(
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.SmartTraining },
                        ),
                    )

                    ScreenType.SmartTraining -> SmartTrainingScreenContainer(
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.Home },
                        ),
                        onStartTraining = { queue ->
                            val result = smartTrainingFlow.startTraining(queue)
                            if (result != null) {
                                applyTrainingFlowResult(result)
                            }
                        },
                    )

                    is ScreenType.SmartTrainGame -> TrainSingleGameLauncherScreenContainer(
                        trainingId = screen.trainingId,
                        gameId = screen.gameId,
                        keepLineIfZero = !removeLineIfRepIsZero,
                        trainingRuntimeContext = runtimeContext.trainingSession,
                        hasNextTrainingGame = smartTrainingFlow.hasNextGame(screen.gameId),
                        sessionCurrent = smartTrainingFlow.sessionCurrent(screen.gameId),
                        sessionTotal = smartTrainingFlow.sessionTotal(),
                        onTrainingFinished = {
                            applyTrainingFlowResult(smartTrainingFlow.finishGame())
                        },
                        onNextTrainingClick = { result ->
                            applyTrainingFlowResult(smartTrainingFlow.openNextGame(result))
                        },
                        onOpenGameEditorClick = { game ->
                            applyTrainingFlowResult(
                                smartTrainingFlow.openGameEditor(
                                    game = game,
                                    trainingId = screen.trainingId,
                                    gameId = screen.gameId,
                                )
                            )
                        },
                        onCloneGameClick = { draft ->
                            applyTrainingFlowResult(
                                smartTrainingFlow.openCreateOpening(
                                    draft = draft,
                                    trainingId = screen.trainingId,
                                    gameId = screen.gameId,
                                )
                            )
                        },
                        onSearchByPositionClick = { fen ->
                            applyTrainingFlowResult(
                                smartTrainingFlow.openPositionEditor(
                                    fen = fen,
                                    trainingId = screen.trainingId,
                                    gameId = screen.gameId,
                                )
                            )
                        },
                        onAnalyzeGameClick = { uciMoves, initialPly ->
                            applyTrainingFlowResult(
                                smartTrainingFlow.openAnalysis(
                                    trainingId = screen.trainingId,
                                    gameId = screen.gameId,
                                    uciMoves = uciMoves,
                                    initialPly = initialPly,
                                )
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
