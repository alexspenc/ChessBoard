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
import com.example.chessboard.boardmodel.LineDraft
import com.example.chessboard.entity.LineEntity
import com.example.chessboard.localization.AppLanguage
import com.example.chessboard.localization.ProvideAppLanguage
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.runtimecontext.RuntimeContext
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.error.AppErrorReporter
import com.example.chessboard.ui.error.AppErrorUiState
import com.example.chessboard.ui.error.launchAppCatching
import com.example.chessboard.ui.screen.analysis.LineAnalysisInitialPosition
import com.example.chessboard.ui.screen.analysis.LineAnalysisScreenContainer
import com.example.chessboard.ui.screen.createOpening.CreateOpeningScreenContainer
import com.example.chessboard.ui.screen.BackupScreenContainer
import com.example.chessboard.ui.screen.LineEditorScreenContainer
import com.example.chessboard.ui.screen.linesExplorer.LinesExplorerScreenContainer
import com.example.chessboard.ui.screen.openingDeviation.OpeningDeviationDisplayScreen
import com.example.chessboard.ui.screen.openingDeviation.OpeningDeviationSelectionScreenContainer
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.screen.ProfileScreenContainer
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.SettingsScreenContainer
import com.example.chessboard.ui.screen.SmartSettingsScreenContainer
import com.example.chessboard.ui.screen.SmartTrainingScreenContainer
import com.example.chessboard.ui.screen.home.HomeScreenContainer
import com.example.chessboard.ui.screen.positions.positionSearch.PositionSearchScreenContainer
import com.example.chessboard.ui.screen.positions.positionSearch.PositionSearchSettingsScreenContainer
import com.example.chessboard.ui.screen.positions.savedPositions.SavedPositionsScreenContainer
import com.example.chessboard.ui.screen.trainSingleLine.TrainSingleLineLauncherScreenContainer
import com.example.chessboard.ui.screen.trainSingleLine.TrainSingleLineLaunchActions
import com.example.chessboard.ui.screen.trainSingleLine.TrainSingleLineLaunchRequest
import com.example.chessboard.ui.screen.trainSingleLine.TrainSingleLineSessionProgress
import com.example.chessboard.ui.screen.trainSingleLine.TrainSingleLineTarget
import com.example.chessboard.ui.screen.training.create.CreateTrainingByStatisticsScreenContainer
import com.example.chessboard.ui.screen.training.create.CreateTrainingChoiceScreenContainer
import com.example.chessboard.ui.screen.training.create.StatisticsTrainingFormulaSettingsScreenContainer
import com.example.chessboard.ui.screen.training.template.TrainingTemplateSelectionScreenContainer
import com.example.chessboard.ui.screen.training.template.TrainingTemplateBrowserScreenContainer
import com.example.chessboard.ui.screen.training.template.EditTrainingTemplateScreenContainer
import com.example.chessboard.ui.screen.training.create.CreateTrainingFromAllLinesScreenContainer
import com.example.chessboard.ui.screen.training.create.CreateTrainingFromLineIdsScreenContainer
import com.example.chessboard.ui.screen.training.create.CreateTrainingFromTemplateScreenContainer
import com.example.chessboard.ui.screen.training.flow.RegularTrainingFlowCoordinator
import com.example.chessboard.ui.screen.training.flow.SmartTrainingFlowCoordinator
import com.example.chessboard.ui.screen.training.flow.TrainingFlowResult
import com.example.chessboard.ui.screen.training.train.EditTrainingScreenContainer
import com.example.chessboard.ui.screen.training.train.TrainingSettingsScreenContainer
import com.example.chessboard.ui.screen.training.train.TrainingListScreenContainer
import com.example.chessboard.ui.theme.ChessBoardTheme
import kotlinx.coroutines.CancellationException
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
                var selectedLine by remember { mutableStateOf<LineEntity?>(null) }
                var linesExplorerSelectedLineId by remember { mutableStateOf<Long?>(null) }
                var linesExplorerOnBackClick by remember { mutableStateOf<() -> Unit>({ currentScreen = ScreenType.Home }) }
                var lineEditorOnBackClick by remember { mutableStateOf<() -> Unit>({ currentScreen = ScreenType.LinesExplorer }) }
                var createOpeningDraft by remember { mutableStateOf(LineDraft()) }
                var createOpeningOnBackClick by remember { mutableStateOf<() -> Unit>({ currentScreen = ScreenType.Home }) }
                var simpleViewEnabled by remember { mutableStateOf(false) }
                var removeLineIfRepIsZero by remember { mutableStateOf(true) }
                var hideLinesWithWeightZero by remember { mutableStateOf(false) }
                var appLanguage by remember { mutableStateOf(AppLanguage.Default) }
                var profileLoaded by remember { mutableStateOf(false) }
                var appError by remember { mutableStateOf<AppErrorUiState?>(null) }
                val runtimeContext = remember { RuntimeContext() }

                fun openHome() {
                    runtimeContext.linesExplorer.clearFilter()
                    currentScreen = ScreenType.Home
                }
                val regularTrainingFlow = remember(runtimeContext) {
                    RegularTrainingFlowCoordinator(runtimeContext)
                }
                val smartTrainingFlow = remember(runtimeContext) {
                    SmartTrainingFlowCoordinator(runtimeContext)
                }
                val scope = rememberCoroutineScope()
                val userProfileService = remember { dbProvider.createUserProfileService() }
                val errorReporter = remember(scope) {
                    AppErrorReporter { errorState ->
                        scope.launch {
                            appError = errorState
                        }
                    }
                }

                appError?.let { errorState ->
                    AppMessageDialog(
                        title = errorState.title,
                        message = errorState.message,
                        onDismiss = { appError = null },
                    )
                }

                LaunchedEffect(Unit) {
                    try {
                        val profile = userProfileService.getProfile()
                        simpleViewEnabled = profile.simpleViewEnabled
                        removeLineIfRepIsZero = profile.removeLineIfRepIsZero
                        hideLinesWithWeightZero = profile.hideLinesWithWeightZero
                        appLanguage = AppLanguage.fromTag(profile.languageTag)
                        profileLoaded = true
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        errorReporter.report(error, "Failed to load app settings.")
                        profileLoaded = true
                    }
                }

                fun openLinesExplorer() {
                    scope.launchAppCatching(
                        errorReporter = errorReporter,
                        message = "Failed to open lines.",
                    ) {
                        runtimeContext.linesExplorer.loadAllLineIds(dbProvider)
                        linesExplorerSelectedLineId = null
                        linesExplorerOnBackClick = { openHome() }
                        currentScreen = ScreenType.LinesExplorer
                    }
                }

                fun openLinesExplorerForOpeningDeviationBranch(branchFen: String) {
                    scope.launchAppCatching(
                        errorReporter = errorReporter,
                        message = "Failed to open matching lines.",
                    ) {
                        val lineIds = withContext(Dispatchers.IO) {
                            dbProvider.findLineIdsByFenWithoutMoveNumber(branchFen)
                        }

                        runtimeContext.linesExplorer.setLineIds(lineIds)
                        linesExplorerSelectedLineId = null
                        linesExplorerOnBackClick = {
                            currentScreen = ScreenType.ShowOpeningDeviation
                        }
                        currentScreen = ScreenType.LinesExplorer
                    }
                }

                fun createScreenContext(
                    onBackClick: () -> Unit = {},
                        onNavigate: (ScreenType) -> Unit = navigation@ { screenType ->
                        if (screenType == ScreenType.Home) {
                            openHome()
                            return@navigation
                        }

                        if (screenType == ScreenType.LinesExplorer) {
                            openLinesExplorer()
                            return@navigation
                        }

                        if (screenType == ScreenType.PositionSearch) {
                            runtimeContext.positionSearch.resetToInitialPosition()
                            val previousScreen = currentScreen
                            runtimeContext.positionSearch.onBackClick = { currentScreen = previousScreen }
                            currentScreen = ScreenType.PositionSearch
                            return@navigation
                        }

                        currentScreen = screenType
                    },
                ): ScreenContainerContext {
                    return ScreenContainerContext(
                        onBackClick = onBackClick,
                        onNavigate = onNavigate,
                        inDbProvider = dbProvider,
                        runtimeContext = runtimeContext,
                        errorReporter = errorReporter,
                    )
                }

                fun openLineAnalysis(
                    uciMoves: List<String>,
                    initialPly: Int,
                    backTarget: ScreenType,
                ) {
                    currentScreen = ScreenType.AnalyzeLine(
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

                        is TrainingFlowResult.OpenLineEditor -> {
                            selectedLine = result.line
                            lineEditorOnBackClick = { currentScreen = result.backTarget }
                            currentScreen = ScreenType.LineEditor
                        }

                        is TrainingFlowResult.OpenCreateOpening -> {
                            createOpeningDraft = result.draft
                            createOpeningOnBackClick = { currentScreen = result.backTarget }
                            currentScreen = ScreenType.CreateOpening
                        }

                        is TrainingFlowResult.OpenPositionSearch -> {
                            runtimeContext.positionSearch.initialFen = result.initialFen
                            runtimeContext.positionSearch.onBackClick = {
                                currentScreen = result.backTarget
                            }
                            currentScreen = ScreenType.PositionSearch
                        }

                        is TrainingFlowResult.OpenAnalysis -> {
                            openLineAnalysis(
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

                ProvideAppLanguage(appLanguage) {
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

                    ScreenType.LinesExplorer -> LinesExplorerScreenContainer(
                        observableLinesPage = runtimeContext.linesExplorer,
                        initialSelectedLineId = linesExplorerSelectedLineId,
                        simpleViewEnabled = simpleViewEnabled,
                        screenContext = createScreenContext(
                            onBackClick = { linesExplorerOnBackClick() },
                        ),
                        onCloneLineClick = { draft ->
                            createOpeningDraft = draft
                            createOpeningOnBackClick = { currentScreen = ScreenType.LinesExplorer }
                            currentScreen = ScreenType.CreateOpening
                        },
                        onAnalyzeLineClick = { uciMoves, initialPly ->
                            openLineAnalysis(
                                uciMoves = uciMoves,
                                initialPly = initialPly,
                                backTarget = ScreenType.LinesExplorer,
                            )
                        },
                        onOpenLineEditor = { line ->
                            selectedLine = line
                            linesExplorerSelectedLineId = line.id
                            lineEditorOnBackClick = { currentScreen = ScreenType.LinesExplorer }
                            currentScreen = ScreenType.LineEditor
                        },
                    )

                    ScreenType.CreateOpening -> CreateOpeningScreenContainer(
                        activity = this@MainActivity,
                        screenContext = createScreenContext(
                            onBackClick = { createOpeningOnBackClick() },
                        ),
                        initialDraft = createOpeningDraft,
                    )

                    ScreenType.PositionSearch -> PositionSearchScreenContainer(
                        initialFen = runtimeContext.positionSearch.initialFen,
                        screenContext = createScreenContext(
                            onBackClick = { runtimeContext.positionSearch.onBackClick() },
                        ),
                        simpleViewEnabled = simpleViewEnabled,
                        onNavigateToSettings = { currentFen ->
                            runtimeContext.positionSearch.initialFen = currentFen
                            currentScreen = ScreenType.PositionSearchSettings
                        },
                        onShowFoundLinesClick = { foundLineIds, currentFen ->
                            runtimeContext.positionSearch.initialFen = currentFen
                            runtimeContext.linesExplorer.setLineIds(foundLineIds)
                            linesExplorerSelectedLineId = null
                            linesExplorerOnBackClick = { currentScreen = ScreenType.PositionSearch }
                            currentScreen = ScreenType.LinesExplorer
                        }
                    )

                    ScreenType.PositionSearchSettings -> PositionSearchSettingsScreenContainer(
                        currentFen = runtimeContext.positionSearch.initialFen,
                        onFenChange = { newFen -> runtimeContext.positionSearch.initialFen = newFen },
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.PositionSearch },
                        ),
                    )

                    ScreenType.SavedPositions -> SavedPositionsScreenContainer(
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.Home },
                        ),
                        onOpenPositionSearch = { fen ->
                            runtimeContext.positionSearch.initialFen = fen
                            runtimeContext.positionSearch.onBackClick = {
                                currentScreen = ScreenType.SavedPositions
                            }
                            currentScreen = ScreenType.PositionSearch
                        },
                        onShowFoundLinesClick = { foundLineIds ->
                            runtimeContext.linesExplorer.setLineIds(foundLineIds)
                            linesExplorerSelectedLineId = null
                            linesExplorerOnBackClick = { currentScreen = ScreenType.SavedPositions }
                            currentScreen = ScreenType.LinesExplorer
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
                        onStartClick = { index ->
                            runtimeContext.openingDeviation.selectDeviation(index)
                            navigateToOpeningDeviationDisplay()
                        },
                        onHomeClick = { currentScreen = ScreenType.Home },
                        onBackClick = { currentScreen = ScreenType.SavedPositions },
                    )

                    ScreenType.ShowOpeningDeviation -> runtimeContext.openingDeviation.selectedDeviationItem()?.let { deviationItem ->
                        OpeningDeviationDisplayScreen(
                            deviationItem = deviationItem,
                            selectedBranchIndex = runtimeContext.openingDeviation.selectedBranchIndex,
                            onBranchSelected = { index ->
                                runtimeContext.openingDeviation.selectBranch(index)
                            },
                            onOpenLinesClick = { branch ->
                                openLinesExplorerForOpeningDeviationBranch(branch.resultFen)
                            },
                            onHomeClick = { currentScreen = ScreenType.Home },
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
                        statisticsTrainingRuntimeContext = runtimeContext.statisticsTraining,
                        onOpenFormulaSettings = {
                            currentScreen = ScreenType.TrainingFormulaSettings
                        },
                    )

                    ScreenType.TrainingFormulaSettings -> StatisticsTrainingFormulaSettingsScreenContainer(
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.CreateTrainingByStatistics },
                        ),
                        statisticsTrainingRuntimeContext = runtimeContext.statisticsTraining,
                    )

                    ScreenType.CreateTraining -> CreateTrainingFromAllLinesScreenContainer(
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.CreateTrainingChoice },
                        ),
                    )

                    is ScreenType.CreateTrainingFromTemplate -> CreateTrainingFromTemplateScreenContainer(
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.TrainingTemplateSelection },
                        ),
                        templateId = screen.templateId,
                    )

                    is ScreenType.CreateTrainingFromLineIds -> CreateTrainingFromLineIdsScreenContainer(
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = screen.backTarget },
                        ),
                        lineIds = screen.lineIds,
                        initialTrainingName = screen.initialTrainingName,
                        screenTitle = screen.screenTitle,
                        linesCountLabel = screen.linesCountLabel,
                    )

                    is ScreenType.EditTrainingTemplate -> EditTrainingTemplateScreenContainer(
                        templateId = screen.templateId,
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.TrainingTemplates },
                        ),
                        onOpenLineEditorClick = { line ->
                            selectedLine = line
                            lineEditorOnBackClick = {
                                currentScreen = ScreenType.EditTrainingTemplate(screen.templateId)
                            }
                            currentScreen = ScreenType.LineEditor
                        },
                    )

                    is ScreenType.EditTraining -> EditTrainingScreenContainer(
                        trainingId = screen.trainingId,
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.Training },
                        ),
                        orderLinesInTraining = runtimeContext.orderLinesInTraining,
                        trainingRuntimeContext = runtimeContext.trainingSession,
                        hideLinesWithWeightZero = hideLinesWithWeightZero,
                        simpleViewEnabled = simpleViewEnabled,
                        onStartLineTrainingClick = { lineId, orderedLineIds ->
                            applyTrainingFlowResult(
                                regularTrainingFlow.startLine(
                                    trainingId = screen.trainingId,
                                    lineId = lineId,
                                    orderedLineIds = orderedLineIds,
                                )
                            )
                        },
                        onOpenLineEditorClick = { line ->
                            applyTrainingFlowResult(
                                regularTrainingFlow.openLineEditorFromEditor(
                                    line = line,
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

                    is ScreenType.TrainSingleLine -> TrainSingleLineLauncherScreenContainer(
                        launchRequest = TrainSingleLineLaunchRequest(
                            target = TrainSingleLineTarget(
                                trainingId = screen.trainingId,
                                lineId = screen.lineId,
                            ),
                            moveFrom = runtimeContext.trainingMoveFrom,
                            moveTo = runtimeContext.trainingMoveTo,
                        ),
                        keepLineIfZero = !removeLineIfRepIsZero,
                        simpleViewEnabled = simpleViewEnabled,
                        trainingRuntimeContext = runtimeContext.trainingSession,
                        sessionProgress = TrainSingleLineSessionProgress(
                            hasNextTrainingLine = regularTrainingFlow.hasNextLine(
                                trainingId = screen.trainingId,
                                lineId = screen.lineId,
                            ),
                            sessionCurrent = regularTrainingFlow.sessionCurrent(
                                trainingId = screen.trainingId,
                                lineId = screen.lineId,
                            ),
                            sessionTotal = regularTrainingFlow.sessionTotal(screen.trainingId),
                        ),
                        launchActions = TrainSingleLineLaunchActions(
                            onTrainingFinished = { result ->
                                applyTrainingFlowResult(
                                    regularTrainingFlow.finishLine(result)
                                )
                            },
                            onNextTrainingClick = { result ->
                                applyTrainingFlowResult(
                                    regularTrainingFlow.openNextLine(result)
                                )
                            },
                            onInterruptTrainingClick = {
                                applyTrainingFlowResult(
                                    regularTrainingFlow.interruptTraining(screen.trainingId)
                                )
                            },
                            onOpenLineEditorClick = { line ->
                                applyTrainingFlowResult(
                                    regularTrainingFlow.openLineEditorFromTraining(
                                        line = line,
                                        trainingId = screen.trainingId,
                                        lineId = screen.lineId,
                                    )
                                )
                            },
                            onCloneLineClick = { draft ->
                                applyTrainingFlowResult(
                                    regularTrainingFlow.openCreateOpeningFromTraining(
                                        draft = draft,
                                        trainingId = screen.trainingId,
                                        lineId = screen.lineId,
                                    )
                                )
                            },
                            onSearchByPositionClick = { fen ->
                                applyTrainingFlowResult(
                                    regularTrainingFlow.openPositionSearchFromTraining(
                                        fen = fen,
                                        trainingId = screen.trainingId,
                                        lineId = screen.lineId,
                                    )
                                )
                            },
                            onAnalyzeLineClick = { uciMoves, initialPly ->
                                applyTrainingFlowResult(
                                    regularTrainingFlow.openAnalysisFromTraining(
                                        trainingId = screen.trainingId,
                                        lineId = screen.lineId,
                                        uciMoves = uciMoves,
                                        initialPly = initialPly,
                                    )
                                )
                            },
                        ),
                        screenContext = createScreenContext(
                            onBackClick = {
                                currentScreen = ScreenType.Home
                            },
                        ),
                    )

                    is ScreenType.AnalyzeLine -> LineAnalysisScreenContainer(
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = screen.backTarget },
                        ),
                        initialPosition = LineAnalysisInitialPosition.FromLineLine(
                            uciMoves = screen.uciMoves,
                            initialPly = screen.initialPly,
                        ),
                        onSearchByPositionClick = { fen ->
                            runtimeContext.positionSearch.initialFen = fen
                            runtimeContext.positionSearch.onBackClick = {
                                currentScreen = screen
                            }
                            currentScreen = ScreenType.PositionSearch
                        },
                    )

                    ScreenType.LineEditor -> selectedLine?.let { line ->
                        LineEditorScreenContainer(
                            activity = this@MainActivity,
                            line = line,
                            screenContext = createScreenContext(
                                onBackClick = { lineEditorOnBackClick() },
                            ),
                        )
                    } ?: run {
                        openLinesExplorer()
                    }

                    ScreenType.Home -> HomeScreenContainer(
                        activity = this@MainActivity,
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.Home },
                        ),
                        simpleViewEnabled = simpleViewEnabled,
                        onCreateOpeningClick = {
                            createOpeningDraft = LineDraft()
                            createOpeningOnBackClick = { currentScreen = ScreenType.Home }
                            currentScreen = ScreenType.CreateOpening
                        },
                        onCreateTrainingClick = {
                            currentScreen = ScreenType.CreateTrainingChoice
                        },
                        onOpenPositionSearchClick = {
                            runtimeContext.positionSearch.resetToInitialPosition()
                            runtimeContext.positionSearch.onBackClick = { currentScreen = ScreenType.Home }
                            currentScreen = ScreenType.PositionSearch
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
                            scope.launchAppCatching(
                                errorReporter = errorReporter,
                                message = "Failed to save settings.",
                            ) {
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
                            scope.launchAppCatching(
                                errorReporter = errorReporter,
                                message = "Failed to save settings.",
                            ) {
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
                            scope.launchAppCatching(
                                errorReporter = errorReporter,
                                message = "Failed to save settings.",
                            ) {
                                userProfileService.updateSettings(
                                    simpleViewEnabled = simpleViewEnabled,
                                    removeLineIfRepIsZero = removeLineIfRepIsZero,
                                    hideLinesWithWeightZero = newValue,
                                )
                            }
                        },
                        appLanguage = appLanguage,
                        onAppLanguageChange = { newLanguage ->
                            appLanguage = newLanguage
                            scope.launchAppCatching(
                                errorReporter = errorReporter,
                                message = "Failed to save language.",
                            ) {
                                userProfileService.updateLanguageTag(newLanguage.tag)
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

                    is ScreenType.SmartTrainLine -> TrainSingleLineLauncherScreenContainer(
                        launchRequest = TrainSingleLineLaunchRequest(
                            target = TrainSingleLineTarget(
                                trainingId = screen.trainingId,
                                lineId = screen.lineId,
                            ),
                        ),
                        keepLineIfZero = !removeLineIfRepIsZero,
                        simpleViewEnabled = simpleViewEnabled,
                        trainingRuntimeContext = runtimeContext.trainingSession,
                        sessionProgress = TrainSingleLineSessionProgress(
                            hasNextTrainingLine = smartTrainingFlow.hasNextLine(screen.lineId),
                            sessionCurrent = smartTrainingFlow.sessionCurrent(screen.lineId),
                            sessionTotal = smartTrainingFlow.sessionTotal(),
                        ),
                        launchActions = TrainSingleLineLaunchActions(
                            onTrainingFinished = {
                                applyTrainingFlowResult(smartTrainingFlow.finishLine())
                            },
                            onNextTrainingClick = { result ->
                                applyTrainingFlowResult(smartTrainingFlow.openNextLine(result))
                            },
                            onInterruptTrainingClick = {
                                applyTrainingFlowResult(
                                    smartTrainingFlow.interruptTraining(screen.trainingId)
                                )
                            },
                            onOpenLineEditorClick = { line ->
                                applyTrainingFlowResult(
                                    smartTrainingFlow.openLineEditor(
                                        line = line,
                                        trainingId = screen.trainingId,
                                        lineId = screen.lineId,
                                    )
                                )
                            },
                            onCloneLineClick = { draft ->
                                applyTrainingFlowResult(
                                    smartTrainingFlow.openCreateOpening(
                                        draft = draft,
                                        trainingId = screen.trainingId,
                                        lineId = screen.lineId,
                                    )
                                )
                            },
                            onSearchByPositionClick = { fen ->
                                applyTrainingFlowResult(
                                    smartTrainingFlow.openPositionSearch(
                                        fen = fen,
                                        trainingId = screen.trainingId,
                                        lineId = screen.lineId,
                                    )
                                )
                            },
                            onAnalyzeLineClick = { uciMoves, initialPly ->
                                applyTrainingFlowResult(
                                    smartTrainingFlow.openAnalysis(
                                        trainingId = screen.trainingId,
                                        lineId = screen.lineId,
                                        uciMoves = uciMoves,
                                        initialPly = initialPly,
                                    )
                                )
                            },
                        ),
                        screenContext = createScreenContext(
                            onBackClick = { currentScreen = ScreenType.SmartTraining },
                        ),
                    )

                    else -> currentScreen = ScreenType.Home
                }
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
