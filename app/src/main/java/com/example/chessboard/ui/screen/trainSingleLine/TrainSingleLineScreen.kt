package com.example.chessboard.ui.screen.trainSingleLine

/**
 * File role: groups the screen-level orchestration for a single-line training session.
 * Allowed here:
 * - Compose state, lifecycle effects, runtime-session restoration, and training callbacks
 * - wiring between pure training logic and rendered training UI
 * Not allowed here:
 * - reusable generic UI components that belong outside this screen package
 * - persistence helpers or database-facing logic unrelated to this screen flow
 * Validation date: 2026-04-25
 */

import android.content.ClipData
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.boardmodel.LineDraft
import com.example.chessboard.boardmodel.buildLineDraftFromSourceLine
import com.example.chessboard.runtimecontext.TrainingRuntimeContext
import com.example.chessboard.service.buildAnalysisPgnFromLines
import com.example.chessboard.service.buildMoveLabels
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppLoadingDialog
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.HomeIconButton
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.screen.resolvePlayerTier
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TextColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class TrainingLinePgnMessage(
    val title: String,
    val message: String,
)

private data class TrainSingleLineLevelUpState(
    val tierSymbol: String,
    val level: Int,
    val title: String,
)

@Composable
fun TrainSingleLineScreenContainer(
    target: TrainSingleLineTarget,
    trainingLineData: TrainSingleLineData,
    trainingRuntimeContext: TrainingRuntimeContext,
    keepLineIfZero: Boolean = false,
    sessionProgress: TrainSingleLineSessionProgress = TrainSingleLineSessionProgress(),
    onTrainingFinished: (TrainSingleLineResult) -> Unit = {},
    onNextTrainingClick: (TrainSingleLineResult) -> Unit = {},
    autoNextLine: Boolean = false,
    onInterruptTrainingClick: () -> Unit,
    onOpenLineEditorClick: () -> Unit,
    onCloneLineClick: (LineDraft) -> Unit,
    onSearchByPositionClick: (String) -> Unit,
    onAnalyzeLineClick: (List<String>, Int) -> Unit,
    simpleViewEnabled: Boolean = false,
    screenContext: ScreenContainerContext,
    modifier: Modifier = Modifier,
) {
    val inDbProvider = screenContext.inDbProvider
    val scope = rememberCoroutineScope()
    var levelUpState by remember { mutableStateOf<TrainSingleLineLevelUpState?>(null) }
    var autoNextLine by remember { mutableStateOf(autoNextLine) }

    suspend fun checkAndRecordStats(result: TrainSingleLineResult): Boolean {
        val newLevel = withContext(Dispatchers.IO) {
            inDbProvider.recordTrainingLineStatsCheckingLevelUp(
                lineId = result.lineId,
                mistakesCount = result.mistakesCount,
            )
        }
        if (newLevel != null) {
            val tier = resolvePlayerTier(newLevel)
            val title = tier.titles.random()
            withContext(Dispatchers.IO) {
                inDbProvider.createUserProfileService().updateRankTitle(tier.name, title)
            }
            levelUpState = TrainSingleLineLevelUpState(
                tierSymbol = tier.symbol,
                level = newLevel,
                title = title,
            )
            return true
        }
        return false
    }

    TrainSingleLineScreen(
        target = target,
        trainingLineData = trainingLineData,
        trainingRuntimeContext = trainingRuntimeContext,
        sessionProgress = sessionProgress,
        levelUpState = levelUpState,
        onLevelUpDismiss = { levelUpState = null },
        onLineCompleted = { result ->
            scope.launch { checkAndRecordStats(result) }
        },
        checkAndRecordStats = { result -> checkAndRecordStats(result) },
        onTrainingFinished = { result ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    inDbProvider.finishTrainingLine(
                        trainingId = result.trainingId,
                        lineId = result.lineId,
                        mistakesCount = result.mistakesCount,
                        keepLineIfZero = keepLineIfZero
                    )
                }
                onTrainingFinished(result)
            }
        },
        onNextTrainingClick = { result ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    inDbProvider.finishTrainingLine(
                        trainingId = result.trainingId,
                        lineId = result.lineId,
                        mistakesCount = result.mistakesCount,
                        keepLineIfZero = keepLineIfZero
                    )
                }
                onNextTrainingClick(result)
            }
        },
        onMarkDubiousAndNextTrainingClick = { result ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    inDbProvider.createDubiousLineService().markDubious(result.lineId)
                    inDbProvider.finishTrainingLine(
                        trainingId = result.trainingId,
                        lineId = result.lineId,
                        mistakesCount = result.mistakesCount,
                        keepLineIfZero = keepLineIfZero
                    )
                }
                onNextTrainingClick(result)
            }
        },
        onMarkDubiousClick = { lineId ->
            scope.launch(Dispatchers.IO) {
                inDbProvider.createDubiousLineService().markDubious(lineId)
            }
        },
        autoNextLine = autoNextLine,
        onAutoNextLineChange = { enabled ->
            autoNextLine = enabled
            scope.launch(Dispatchers.IO) {
                inDbProvider.createUserProfileService().updateAutoNextLine(enabled)
            }
        },
        onInterruptTrainingClick = onInterruptTrainingClick,
        onBackClick = screenContext.onBackClick,
        onNavigate = screenContext.onNavigate,
        onOpenLineEditorClick = onOpenLineEditorClick,
        onCloneLineClick = onCloneLineClick,
        onSearchByPositionClick = onSearchByPositionClick,
        onAnalyzeLineClick = onAnalyzeLineClick,
        simpleViewEnabled = simpleViewEnabled,
        modifier = modifier
    )
}

@Composable
private fun TrainSingleLineScreen(
    target: TrainSingleLineTarget,
    trainingLineData: TrainSingleLineData,
    trainingRuntimeContext: TrainingRuntimeContext,
    sessionProgress: TrainSingleLineSessionProgress = TrainSingleLineSessionProgress(),
    levelUpState: TrainSingleLineLevelUpState? = null,
    onLevelUpDismiss: () -> Unit = {},
    onLineCompleted: (TrainSingleLineResult) -> Unit = {},
    checkAndRecordStats: suspend (TrainSingleLineResult) -> Boolean = { false },
    onTrainingFinished: (TrainSingleLineResult) -> Unit = {},
    onNextTrainingClick: (TrainSingleLineResult) -> Unit = {},
    onMarkDubiousAndNextTrainingClick: (TrainSingleLineResult) -> Unit = {},
    onMarkDubiousClick: (Long) -> Unit = {},
    autoNextLine: Boolean = false,
    onAutoNextLineChange: (Boolean) -> Unit = {},
    onInterruptTrainingClick: () -> Unit,
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onOpenLineEditorClick: () -> Unit,
    onCloneLineClick: (LineDraft) -> Unit,
    onSearchByPositionClick: (String) -> Unit,
    onAnalyzeLineClick: (List<String>, Int) -> Unit,
    simpleViewEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val loadedLine = trainingLineData.line
    val trainingId = target.trainingId
    val lineId = target.lineId
    var selectedNavItem by remember { mutableStateOf<ScreenType>(ScreenType.Training) }
    var showShowLineDialog by remember(loadedLine.id) { mutableStateOf(false) }
    var showAdditionalMenu by remember(loadedLine.id) { mutableStateOf(false) }
    var isBuildingLinePgn by remember(loadedLine.id) { mutableStateOf(false) }
    var linePgnMessage by remember(loadedLine.id) {
        mutableStateOf<TrainingLinePgnMessage?>(null)
    }
    val uciMoves = trainingLineData.uciMoves
    val startFen = trainingLineData.startFen
    val hasMoveCap = trainingLineData.hasMoveCap
    val lineFingerprint = remember(startFen, uciMoves) {
        resolveTrainingLineFingerprint(
            startFen = startFen,
            uciMoves = uciMoves,
        )
    }
    val moveLabels = remember(uciMoves) { buildMoveLabels(uciMoves) }
    var uiState by remember(loadedLine.id) { mutableStateOf(TrainSingleLineUiState()) }
    var hasInitializedSession by remember(loadedLine.id) { mutableStateOf(false) }
    var showLineJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val trainingSides = remember(loadedLine.sideMask) {
        resolveTrainingOrientations(loadedLine.sideMask)
    }
    val currentOrientation = trainingSides.getOrNull(uiState.currentSideIndex) ?: BoardOrientation.WHITE
    val lineController = remember(currentOrientation) { LineController(currentOrientation) }
    val clipboard = LocalClipboard.current

    fun resetToTrainingStart() {
        if (startFen != null) lineController.loadFromFen(startFen)
        else lineController.resetToStartPosition()
    }

    fun copyTrainingLinePgn() {
        if (isBuildingLinePgn) {
            return
        }

        scope.launch {
            isBuildingLinePgn = true
            try {
                val linePgn = withContext(Dispatchers.Default) {
                    buildAnalysisPgnFromLines(listOf(loadedLine))
                }
                if (linePgn.isBlank()) {
                    linePgnMessage = TrainingLinePgnMessage(
                        title = "PGN unavailable",
                        message = "Training line PGN could not be built.",
                    )
                    return@launch
                }

                clipboard.setClipEntry(
                    ClipEntry(
                        ClipData.newPlainText(
                            "Training Line PGN",
                            linePgn,
                        )
                    )
                )
                linePgnMessage = TrainingLinePgnMessage(
                    title = "PGN copied",
                    message = "Training line PGN was copied to the clipboard.",
                )
            } finally {
                isBuildingLinePgn = false
            }
        }
    }

    fun startTrainingSession(baseState: TrainSingleLineUiState): TrainSingleLineUiState {
        return advanceProgramMoves(
            uiState = buildStartTrainingState(baseState),
            lineController = lineController,
            uciMoves = uciMoves,
            currentOrientation = currentOrientation,
            sidesCount = trainingSides.size,
            startFen = startFen,
            hasMoveCap = hasMoveCap,
        )
    }

    fun startShowLine() {
        Log.d(
            TrainSingleLineLogTag,
            "Show line clicked. lineId= trainingId= boardState= moves="
        )

        showLineJob?.cancel()
        showLineJob = null
        resetToTrainingStart()
        uiState = buildShowLineState(uiState)
        showLineJob = scope.launch {
            try {
                Log.d(
                    TrainSingleLineLogTag,
                    "Starting runShowLine coroutine. boardState= movesCount="
                )
                uiState = runShowLine(
                    uiState = uiState,
                    lineController = lineController,
                    uciMoves = uciMoves,
                    moveDelayMs = resolveShowLineMoveDelayMs(uiState.showLineMoveDelayInput),
                    startFen = startFen,
                )
            } finally {
                showLineJob = null
            }
        }
    }

    fun completeCurrentLineAndRun(action: (TrainSingleLineResult) -> Unit) {
        val result = TrainSingleLineResult(
            lineId = lineId,
            trainingId = trainingId,
            mistakesCount = uiState.mistakesCount,
        )
        hasInitializedSession = false
        trainingRuntimeContext.clearLineProgress(trainingId, lineId)
        uiState = uiState.copy(completionDialog = null)
        action(result)
    }

    fun createNextTrainingClickAction(
        action: (TrainSingleLineResult) -> Unit,
    ): (() -> Unit)? {
        if (!sessionProgress.hasNextTrainingLine) {
            return null
        }

        val completionDialog = uiState.completionDialog ?: return null
        if (completionDialog.hasNextSide) {
            return null
        }

        return { completeCurrentLineAndRun(action) }
    }

    LaunchedEffect(lineController, loadedLine.id) {
        showLineJob?.cancel()
        showLineJob = null
        val savedProgress = trainingRuntimeContext.restoreLineProgress(trainingId, loadedLine.id)
        if (
            savedProgress != null &&
            shouldRestoreTrainingSnapshot(
                snapshotFingerprint = savedProgress.lineFingerprint,
                currentFingerprint = lineFingerprint,
            )
        ) {
            lineController.loadFromUciMoves(
                uciMoves = uciMoves,
                targetPly = savedProgress.currentPly,
                startFen = startFen,
            )
            uiState = savedProgress.uiState
            hasInitializedSession = true
            return@LaunchedEffect
        }

        if (savedProgress != null) {
            trainingRuntimeContext.clearLineProgress(trainingId, loadedLine.id)
        }

        resetToTrainingStart()
        val resetState = resetSessionState(uiState)
        uiState = startTrainingSession(resetState)
        hasInitializedSession = true
    }

    SideEffect {
        lineController.setUserMovesEnabled(resolveBoardInteractionEnabled(uiState))
        lineController.setAllowedMoveUci(null)
    }

    LaunchedEffect(uiState.wrongMoveSquare) {
        if (uiState.wrongMoveSquare != null) {
            delay(1000L)
            uiState = uiState.copy(wrongMoveSquare = null)
        }
    }

    LaunchedEffect(uiState.hintSquare) {
        if (uiState.hintSquare != null) {
            delay(1500L)
            uiState = uiState.copy(hintSquare = null)
        }
    }

    LaunchedEffect(
        uiState.phase,
        lineController.boardState,
        uiState.expectedPly,
        currentOrientation,
        uciMoves
    ) {
        uiState = handleTrainingProgress(
            uiState = uiState,
            lineController = lineController,
            uciMoves = uciMoves,
            currentOrientation = currentOrientation,
            sidesCount = trainingSides.size,
            startFen = startFen,
            hasMoveCap = hasMoveCap,
        )
    }

    LaunchedEffect(uiState, lineController.currentMoveIndex, trainingId, loadedLine.id) {
        if (!hasInitializedSession) {
            return@LaunchedEffect
        }

        trainingRuntimeContext.saveLineProgress(
            trainingId = trainingId,
            lineId = loadedLine.id,
            currentPly = lineController.currentMoveIndex,
            lineFingerprint = lineFingerprint,
            uiState = uiState,
        )
    }

    // Fire stats as soon as the final completion dialog appears, before Finish is pressed.
    // Skipped when auto-next is on — auto-next calls checkAndRecordStats inline so it can
    // await the result and react to level-up before deciding whether to navigate.
    val completionDialog = uiState.completionDialog
    LaunchedEffect(completionDialog) {
        if (autoNextLine) return@LaunchedEffect
        if (completionDialog != null && !completionDialog.hasNextSide) {
            onLineCompleted(
                TrainSingleLineResult(
                    lineId = lineId,
                    trainingId = trainingId,
                    mistakesCount = uiState.mistakesCount,
                )
            )
        }
    }

    // Tracks whether stats were recorded for the current completion so the effect doesn't
    // double-record when it restarts after the level-up dialog is dismissed.
    // remember(completionDialog) resets this flag on each new completion.
    var statsRecordedForCompletion by remember(completionDialog) { mutableStateOf(false) }

    // Auto-advance to the next side or next line when auto-next is enabled.
    // levelUpState is a key so the effect restarts whenever the level-up dialog appears or
    // is dismissed — no snapshotFlow or arbitrary delay needed.
    LaunchedEffect(completionDialog, autoNextLine, levelUpState) {
        val dialog = completionDialog ?: return@LaunchedEffect
        if (!autoNextLine) return@LaunchedEffect
        if (levelUpState != null) return@LaunchedEffect  // wait until user dismisses the level-up dialog
        if (!dialog.hasNextSide && !sessionProgress.hasNextTrainingLine) return@LaunchedEffect

        if (dialog.hasNextSide) {
            uiState = handleCompletionFinish(
                uiState = uiState,
                lineId = lineId,
                trainingId = trainingId,
                onTrainingFinished = onTrainingFinished
            )
            return@LaunchedEffect
        }

        val result = TrainSingleLineResult(
            lineId = lineId,
            trainingId = trainingId,
            mistakesCount = uiState.mistakesCount,
        )
        if (!statsRecordedForCompletion) {
            statsRecordedForCompletion = true
            val leveledUp = checkAndRecordStats(result)
            if (leveledUp) return@LaunchedEffect  // levelUpState is set and triggers key restart
        }

        hasInitializedSession = false
        trainingRuntimeContext.clearLineProgress(trainingId, lineId)
        uiState = uiState.copy(completionDialog = null)
        onNextTrainingClick(result)
    }

    // Keeps the event handlers together so the main render block stays compact.
    fun createContentActions(): TrainSingleLineContentActions {
        return TrainSingleLineContentActions(
            onShowLineClick = {
                showShowLineDialog = true
            },
            onConfirmShowLineClick = {
                showShowLineDialog = false
                startShowLine()
            },
            onDismissShowLineDialogClick = {
                showShowLineDialog = false
            },
            onStopShowLineClick = {
                showLineJob?.cancel()
                showLineJob = null
                uiState = uiState.copy(phase = TrainSingleLinePhase.Idle)
            },
            onAnalyzeLineClick = {
                onAnalyzeLineClick(
                    trainingLineData.analysisUciMoves,
                    resolveTrainingAnalysisInitialPly(
                        trainingLineData = trainingLineData,
                        currentPly = lineController.currentMoveIndex,
                    ),
                )
            },
            onStartTrainingClick = {
                resetToTrainingStart()
                uiState = startTrainingSession(uiState)
            },
            onStopTrainingClick = {
                showLineJob?.cancel()
                showLineJob = null
                resetToTrainingStart()
                uiState = resetSessionState(uiState)
            },
            onHintClick = {
                val fromSquare = uciMoves.getOrNull(uiState.expectedPly)?.take(2)
                if (fromSquare != null && uiState.phase == TrainSingleLinePhase.Training) {
                    uiState = uiState.copy(
                        hintSquare = fromSquare,
                        mistakesCount = uiState.mistakesCount + 1,
                    )
                }
            },
            onMakeCorrectMoveClick = {
                uiState = handleCorrectMove(
                    uiState = uiState,
                    lineController = lineController,
                    uciMoves = uciMoves,
                    currentOrientation = currentOrientation,
                    sidesCount = trainingSides.size,
                    startFen = startFen,
                    hasMoveCap = hasMoveCap,
                )
            },
            onShowLineMoveDelayInputChange = { input ->
                uiState = uiState.copy(
                    showLineMoveDelayInput = formatShowLineMoveDelayInput(
                        input.toLongOrNull() ?: ShowLineMoveDelayMs
                    )
                )
            },
            onMovePlyClick = { ply ->
                if (uiState.showLineCompleted) {
                    lineController.loadFromUciMoves(uciMoves, ply, startFen)
                }
            },
            onPrevMoveClick = {
                if (uiState.showLineCompleted && lineController.canUndo) {
                    lineController.undoMove()
                }
            },
            onNextMoveClick = {
                if (uiState.showLineCompleted && lineController.canRedo) {
                    lineController.redoMove()
                }
            },
            onResetMovesClick = {
                if (uiState.showLineCompleted) {
                    lineController.loadFromUciMoves(uciMoves, 0, startFen)
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
    AppScreenScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                AppTopBar(
                    title = "Train Line",
                    onBackClick = onBackClick,
                    actions = {
                        HomeIconButton(onClick = { onNavigate(ScreenType.Home) })
                        if (!simpleViewEnabled) {
                            IconButton(
                                onClick = { showAdditionalMenu = true },
                            ) {
                                IconMd(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Line actions",
                                )
                            }
                        }
                        IconButton(onClick = onInterruptTrainingClick) {
                            IconMd(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Interrupt training",
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            AppBottomNavigation(
                items = defaultAppBottomNavigationItems(),
                selectedItem = selectedNavItem,
                onItemSelected = {
                    selectedNavItem = it
                    onNavigate(it)
                }
            )
        }
    ) { paddingValues ->
        val autoNextWillAdvance = autoNextLine && completionDialog != null &&
            (completionDialog.hasNextSide || sessionProgress.hasNextTrainingLine)

        if (levelUpState == null) {
            RenderCompletionDialog(
                dialogState = if (autoNextWillAdvance) null else uiState.completionDialog,
                onRepeatClick = {
                    resetToTrainingStart()
                    uiState = buildRepeatVariationState(uiState)
                },
                onFinishClick = {
                    val isFinalCompletion = uiState.completionDialog?.hasNextSide != true
                    if (isFinalCompletion) {
                        hasInitializedSession = false
                        trainingRuntimeContext.clearLineProgress(trainingId, lineId)
                    }
                    uiState = handleCompletionFinish(
                        uiState = uiState,
                        lineId = lineId,
                        trainingId = trainingId,
                        onTrainingFinished = onTrainingFinished
                    )
                },
                onNextTrainingClick = createNextTrainingClickAction(onNextTrainingClick),
                onMarkDubiousAndNextTrainingClick = createNextTrainingClickAction(
                    onMarkDubiousAndNextTrainingClick,
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            TrainSingleLineContent(
                simpleViewEnabled = simpleViewEnabled,
                state = TrainSingleLineContentState(
                    target = target,
                    trainingLineData = trainingLineData,
                    currentOrientation = currentOrientation,
                    sidesCount = trainingSides.size,
                    sessionProgress = sessionProgress,
                    currentPly = lineController.currentMoveIndex,
                    moveLabels = moveLabels,
                    phase = uiState.phase,
                    mistakesCount = uiState.mistakesCount,
                    showLineMoveDelayInput = uiState.showLineMoveDelayInput,
                    showLineCompleted = uiState.showLineCompleted,
                    wrongMoveSquare = uiState.wrongMoveSquare,
                    hintSquare = uiState.hintSquare
                ),
                lineController = lineController,
                actions = createContentActions(),
                showShowLineDialog = showShowLineDialog,
            )
        }
    }

    RenderAdditionalMenu(
        visible = showAdditionalMenu,
        canCopyLinePgn = !isBuildingLinePgn,
        onDismiss = { showAdditionalMenu = false },
        onSearchClick = {
            showAdditionalMenu = false
            onSearchByPositionClick(lineController.getFen())
        },
        onCloneClick = {
            showAdditionalMenu = false
            onCloneLineClick(buildLineDraftFromSourceLine(loadedLine))
        },
        onEditClick = {
            showAdditionalMenu = false
            onOpenLineEditorClick()
        },
        onCopyLinePgnClick = {
            showAdditionalMenu = false
            copyTrainingLinePgn()
        },
        onMarkDubiousClick = {
            showAdditionalMenu = false
            onMarkDubiousClick(lineId)
        },
    )

    linePgnMessage?.let { message ->
        AppMessageDialog(
            title = message.title,
            message = message.message,
            onDismiss = { linePgnMessage = null },
        )
    }

    if (isBuildingLinePgn) {
        AppLoadingDialog(
            title = "Building PGN",
            message = "Preparing training line PGN...",
        )
    }

    levelUpState?.let { currentLevelUpState ->
        LevelUpDialog(
            tierSymbol = currentLevelUpState.tierSymbol,
            levelNumber = currentLevelUpState.level,
            rankTitle = currentLevelUpState.title,
            onDismiss = onLevelUpDismiss,
        )
    }
    } // Box
}

@Composable
private fun RenderAdditionalMenu(
    visible: Boolean,
    canCopyLinePgn: Boolean,
    onDismiss: () -> Unit,
    onSearchClick: () -> Unit,
    onCloneClick: () -> Unit,
    onEditClick: () -> Unit,
    onCopyLinePgnClick: () -> Unit,
    onMarkDubiousClick: () -> Unit,
) {
    if (!visible) {
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Background.ScreenDark,
        title = {
            SectionTitleText(text = "Line Actions")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
            ) {
                TrainingLineDialogAction(
                    label = "Search",
                    onClick = onSearchClick,
                ) { isEnabled ->
                    IconMd(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search by position",
                        tint = resolveTrainingLineDialogActionTint(isEnabled),
                    )
                }
                TrainingLineDialogAction(
                    label = "Clone",
                    onClick = onCloneClick,
                ) { isEnabled ->
                    IconMd(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Clone line",
                        tint = resolveTrainingLineDialogActionTint(isEnabled),
                    )
                }
                TrainingLineDialogAction(
                    label = "Edit",
                    onClick = onEditClick,
                ) { isEnabled ->
                    IconMd(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit line",
                        tint = resolveTrainingLineDialogActionTint(isEnabled),
                    )
                }
                TrainingLineDialogAction(
                    label = "Doubt",
                    onClick = onMarkDubiousClick,
                ) { isEnabled ->
                    IconMd(
                        imageVector = Icons.Default.ReportProblem,
                        contentDescription = "Mark line as dubious",
                        tint = resolveTrainingLineDialogActionTint(isEnabled),
                    )
                }
                TrainingLineDialogAction(
                    label = "Export PGN",
                    enabled = canCopyLinePgn,
                    onClick = onCopyLinePgnClick,
                ) { isEnabled ->
                    IconMd(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export training line PGN",
                        tint = resolveTrainingLineDialogActionTint(isEnabled),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                CardMetaText(text = "Cancel")
            }
        },
    )
}

@Composable
private fun TrainingLineDialogAction(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: @Composable (Boolean) -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon(enabled)
            Text(
                text = label,
                color = resolveTrainingLineDialogActionTint(enabled),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun resolveTrainingLineDialogActionTint(isEnabled: Boolean): Color {
    if (isEnabled) {
        return TextColor.Primary
    }

    return TextColor.Primary.copy(alpha = 0.5f)
}
