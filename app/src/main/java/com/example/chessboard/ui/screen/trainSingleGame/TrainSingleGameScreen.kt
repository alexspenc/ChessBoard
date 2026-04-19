package com.example.chessboard.ui.screen.trainSingleGame

// Orchestrates the single-game training session.
// This file owns screen-level state, lifecycle reactions, navigation callbacks,
// and bridges pure training logic to render components.

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.chessboard.boardmodel.GameDraft
import com.example.chessboard.boardmodel.buildGameDraftFromSourceGame
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.service.buildMoveLabels
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TrainingTextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TrainSingleGameScreenContainer(
    gameId: Long,
    trainingId: Long,
    trainingGameData: TrainSingleGameData,
    keepLineIfZero: Boolean = false,
    hasNextTrainingGame: Boolean = false,
    sessionCurrent: Int = 0,
    sessionTotal: Int = 0,
    onTrainingFinished: (TrainSingleGameResult) -> Unit = {},
    onNextTrainingClick: (TrainSingleGameResult) -> Unit = {},
    onOpenGameEditorClick: () -> Unit = {},
    onCloneGameClick: (GameDraft) -> Unit = {},
    onSearchByPositionClick: (String) -> Unit = {},
    screenContext: ScreenContainerContext,
    modifier: Modifier = Modifier,
) {
    val inDbProvider = screenContext.inDbProvider
    val scope = rememberCoroutineScope()

    TrainSingleGameScreen(
        gameId = gameId,
        trainingId = trainingId,
        trainingGameData = trainingGameData,
        hasNextTrainingGame = hasNextTrainingGame,
        sessionCurrent = sessionCurrent,
        sessionTotal = sessionTotal,
        onLineCompleted = { result ->
            scope.launch(Dispatchers.IO) {
                inDbProvider.recordTrainingGameStats(
                    gameId = result.gameId,
                    mistakesCount = result.mistakesCount,
                )
            }
        },
        onTrainingFinished = { result ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    inDbProvider.finishTrainingGame(
                        trainingId = result.trainingId,
                        gameId = result.gameId,
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
                    inDbProvider.finishTrainingGame(
                        trainingId = result.trainingId,
                        gameId = result.gameId,
                        mistakesCount = result.mistakesCount,
                        keepLineIfZero = keepLineIfZero
                    )
                }
                onNextTrainingClick(result)
            }
        },
        onBackClick = screenContext.onBackClick,
        onNavigate = screenContext.onNavigate,
        onOpenGameEditorClick = onOpenGameEditorClick,
        onCloneGameClick = onCloneGameClick,
        onSearchByPositionClick = onSearchByPositionClick,
        modifier = modifier
    )
}

@Composable
private fun TrainSingleGameScreen(
    gameId: Long,
    trainingId: Long,
    trainingGameData: TrainSingleGameData,
    hasNextTrainingGame: Boolean = false,
    sessionCurrent: Int = 0,
    sessionTotal: Int = 0,
    onLineCompleted: (TrainSingleGameResult) -> Unit = {},
    onTrainingFinished: (TrainSingleGameResult) -> Unit = {},
    onNextTrainingClick: (TrainSingleGameResult) -> Unit = {},
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onOpenGameEditorClick: () -> Unit = {},
    onCloneGameClick: (GameDraft) -> Unit = {},
    onSearchByPositionClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedNavItem by remember { mutableStateOf<ScreenType>(ScreenType.Home) }
    val loadedGame = trainingGameData.game
    val uciMoves = trainingGameData.uciMoves
    val startFen = trainingGameData.startFen
    val hasMoveCap = trainingGameData.hasMoveCap
    val moveLabels = remember(uciMoves) { buildMoveLabels(uciMoves) }
    var uiState by remember(loadedGame.id) { mutableStateOf(TrainSingleGameUiState()) }
    var showLineJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val trainingSides = remember(loadedGame.sideMask) {
        resolveTrainingOrientations(loadedGame.sideMask)
    }
    val currentOrientation = trainingSides.getOrNull(uiState.currentSideIndex) ?: BoardOrientation.WHITE
    val gameController = remember(currentOrientation) { GameController(currentOrientation) }

    fun resetToTrainingStart() {
        if (startFen != null) gameController.loadFromFen(startFen)
        else gameController.resetToStartPosition()
    }

    fun startTrainingSession(baseState: TrainSingleGameUiState): TrainSingleGameUiState {
        return advanceProgramMoves(
            uiState = buildStartTrainingState(baseState),
            gameController = gameController,
            uciMoves = uciMoves,
            currentOrientation = currentOrientation,
            sidesCount = trainingSides.size,
            startFen = startFen,
            hasMoveCap = hasMoveCap,
        )
    }

    fun createNextTrainingClickAction(): (() -> Unit)? {
        if (!hasNextTrainingGame) {
            return null
        }

        val completionDialog = uiState.completionDialog ?: return null
        if (completionDialog.hasNextSide) {
            return null
        }

        return {
            val result = TrainSingleGameResult(
                gameId = gameId,
                trainingId = trainingId,
                mistakesCount = uiState.mistakesCount,
            )
            uiState = uiState.copy(completionDialog = null)
            onNextTrainingClick(result)
        }
    }

    LaunchedEffect(gameController, loadedGame.id) {
        showLineJob?.cancel()
        showLineJob = null
        resetToTrainingStart()
        val resetState = resetSessionState(uiState)
        // Start training immediately after opening the screen.
        // This preserves the requested flow where choosing a game enters an active
        // training session without an extra tap on the play button.
        uiState = startTrainingSession(resetState)
    }

    SideEffect {
        gameController.setUserMovesEnabled(resolveBoardInteractionEnabled(uiState))
        gameController.setAllowedMoveUci(null)
    }

    LaunchedEffect(uiState.wrongMoveSquare) {
        if (uiState.wrongMoveSquare != null) {
            delay(1000L)
            uiState = uiState.copy(wrongMoveSquare = null)
        }
    }

    LaunchedEffect(
        uiState.phase,
        gameController.boardState,
        uiState.expectedPly,
        currentOrientation,
        uciMoves
    ) {
        uiState = handleTrainingProgress(
            uiState = uiState,
            gameController = gameController,
            uciMoves = uciMoves,
            currentOrientation = currentOrientation,
            sidesCount = trainingSides.size,
            startFen = startFen,
            hasMoveCap = hasMoveCap,
        )
    }

    // Fire stats as soon as the final completion dialog appears, before Finish is pressed.
    val completionDialog = uiState.completionDialog
    LaunchedEffect(completionDialog) {
        if (completionDialog != null && !completionDialog.hasNextSide) {
            onLineCompleted(
                TrainSingleGameResult(
                    gameId = gameId,
                    trainingId = trainingId,
                    mistakesCount = uiState.mistakesCount,
                )
            )
        }
    }

    // Keeps the event handlers together so the main render block stays compact.
    fun createContentActions(): TrainSingleGameContentActions {
        return TrainSingleGameContentActions(
            onShowLineClick = {
                Log.d(
                    TrainSingleGameLogTag,
                    "Show line clicked. gameId= trainingId= boardState= moves="
                )

                showLineJob?.cancel()
                showLineJob = null
                resetToTrainingStart()
                uiState = buildShowLineState(uiState)
                showLineJob = scope.launch {
                    try {
                        Log.d(
                            TrainSingleGameLogTag,
                            "Starting runShowLine coroutine. boardState= movesCount="
                        )
                        uiState = runShowLine(
                            uiState = uiState,
                            gameController = gameController,
                            uciMoves = uciMoves,
                            moveDelayMs = resolveShowLineMoveDelayMs(uiState.showLineMoveDelayInput),
                            startFen = startFen,
                        )
                    } finally {
                        showLineJob = null
                    }
                }
            },
            onStopShowLineClick = {
                showLineJob?.cancel()
                showLineJob = null
                uiState = uiState.copy(phase = TrainSingleGamePhase.Idle)
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
            onMakeCorrectMoveClick = {
                uiState = handleCorrectMove(
                    uiState = uiState,
                    gameController = gameController,
                    uciMoves = uciMoves,
                    currentOrientation = currentOrientation,
                    sidesCount = trainingSides.size,
                    startFen = startFen,
                    hasMoveCap = hasMoveCap,
                )
            },
            onShowLineMoveDelayInputChange = { input ->
                uiState = uiState.copy(
                    showLineMoveDelayInput = input.filter(Char::isDigit)
                )
            },
            onDecreaseShowLineMoveDelayClick = {
                uiState = uiState.copy(
                    showLineMoveDelayInput = changeShowLineMoveDelay(
                        input = uiState.showLineMoveDelayInput,
                        delta = -ShowLineMoveDelayStepMs
                    )
                )
            },
            onIncreaseShowLineMoveDelayClick = {
                uiState = uiState.copy(
                    showLineMoveDelayInput = changeShowLineMoveDelay(
                        input = uiState.showLineMoveDelayInput,
                        delta = ShowLineMoveDelayStepMs
                    )
                )
            },
            onMovePlyClick = { ply ->
                if (uiState.showLineCompleted) {
                    gameController.loadFromUciMoves(uciMoves, ply)
                }
            },
            onPrevMoveClick = {
                if (uiState.showLineCompleted && gameController.canUndo) {
                    gameController.undoMove()
                }
            },
            onNextMoveClick = {
                if (uiState.showLineCompleted && gameController.canRedo) {
                    gameController.redoMove()
                }
            },
            onResetMovesClick = {
                if (uiState.showLineCompleted) {
                    gameController.loadFromUciMoves(uciMoves, 0)
                }
            }
        )
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                AppTopBar(
                    title = "Train Game",
                    onBackClick = onBackClick,
                    actions = {
                        IconButton(
                            onClick = {
                                onSearchByPositionClick(gameController.getFen())
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search by position",
                                tint = TrainingTextPrimary
                            )
                        }
                        IconButton(
                            onClick = {
                                onCloneGameClick(
                                    buildGameDraftFromSourceGame(loadedGame)
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Clone game",
                                tint = TrainingTextPrimary
                            )
                        }
                        IconButton(onClick = onOpenGameEditorClick) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit game",
                                tint = TrainingTextPrimary
                            )
                        }
                    }
                )
                if (sessionTotal > 1) {
                    TrainingSessionProgressBar(
                        current = sessionCurrent,
                        total = sessionTotal,
                    )
                }
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
        RenderCompletionDialog(
            dialogState = uiState.completionDialog,
            onRepeatClick = {
                resetToTrainingStart()
                uiState = buildRepeatVariationState(uiState)
            },
            onFinishClick = {
                uiState = handleCompletionFinish(
                    uiState = uiState,
                    gameId = gameId,
                    trainingId = trainingId,
                    onTrainingFinished = onTrainingFinished
                )
            },
            onNextTrainingClick = createNextTrainingClickAction()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            TrainSingleGameContent(
                state = TrainSingleGameContentState(
                    gameId = gameId,
                    trainingId = trainingId,
                    trainingGameData = trainingGameData,
                    currentOrientation = currentOrientation,
                    sidesCount = trainingSides.size,
                    currentPly = gameController.currentMoveIndex,
                    moveLabels = moveLabels,
                    phase = uiState.phase,
                    mistakesCount = uiState.mistakesCount,
                    showLineMoveDelayInput = uiState.showLineMoveDelayInput,
                    showLineCompleted = uiState.showLineCompleted,
                    wrongMoveSquare = uiState.wrongMoveSquare
                ),
                gameController = gameController,
                actions = createContentActions()
            )
        }
    }
}

@Composable
private fun TrainingSessionProgressBar(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val fraction = if (total > 0) (current.toFloat() / total).coerceIn(0f, 1f) else 0f
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Line $current of $total",
                style = MaterialTheme.typography.bodySmall,
                color = TextColor.Secondary,
            )
            Text(
                text = "${current - 1} completed",
                style = MaterialTheme.typography.bodySmall,
                color = TextColor.Secondary,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF2A2A2A)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(TrainingAccentTeal),
            )
        }
    }
}
