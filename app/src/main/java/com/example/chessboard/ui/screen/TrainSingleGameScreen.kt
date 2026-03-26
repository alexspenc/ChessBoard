package com.example.chessboard.ui.screen

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.entity.SideMask
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.ChessBoardWithCoordinates
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TrainingBackgroundDark
import com.example.chessboard.ui.theme.TrainingTextPrimary
import com.example.chessboard.ui.theme.TrainingTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class TrainSingleGameData(
    val game: GameEntity,
    val uciMoves: List<String>
)

data class TrainSingleGameResult(
    val gameId: Long,
    val trainingId: Long,
    val mistakesCount: Int
)

private const val ShowLineMoveDelayMs = 500L

private enum class TrainSingleGamePhase {
    Idle,
    ShowingLine,
    Training,
    Mistake
}

private data class TrainSingleGameUiState(
    // Index of the currently trained side in the resolved orientation list.
    val currentSideIndex: Int = 0,
    // Current phase of the training session.
    val phase: TrainSingleGamePhase = TrainSingleGamePhase.Idle,
    // Index of the next expected move in the training line.
    val expectedPly: Int = 0,
    // Total number of mistakes made during the session.
    val mistakesCount: Int = 0,
    // Completion dialog state shown after finishing a variation.
    val completionDialog: TrainSingleGameCompletionState? = null
)

/**
 * Loads the selected game, runs the single-game training flow and updates the training entry
 * before returning the session result.
 *
 * @param activity Host activity used by the screen container.
 * @param gameId Identifier of the game being trained.
 * @param trainingId Identifier of the training that owns the game.
 * @param onTrainingFinished Called after the training is updated in the database.
 * @param onBackClick Called when the user presses the top bar back button.
 * @param onNavigate Called when the user selects an item in the bottom navigation.
 * @param modifier Modifier for the root container.
 * @param inDbProvider Database access used to load the game and update the training.
 */
@Composable
fun TrainSingleGameScreenContainer(
    activity: Activity,
    gameId: Long,
    trainingId: Long,
    onTrainingFinished: (TrainSingleGameResult) -> Unit = {},
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    modifier: Modifier = Modifier,
    inDbProvider: DatabaseProvider,
) {
    var trainingGameData by remember { mutableStateOf<TrainSingleGameData?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(gameId) {
        trainingGameData = withContext(Dispatchers.IO) {
            val game = inDbProvider.getGameById(gameId) ?: return@withContext null
            TrainSingleGameData(
                game = game,
                uciMoves = parsePgnMoves(game.pgn)
            )
        }
    }

    TrainSingleGameScreen(
        gameId = gameId,
        trainingId = trainingId,
        trainingGameData = trainingGameData,
        onTrainingFinished = { result ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    inDbProvider.decreaseLineWeight(
                        trainingId = result.trainingId,
                        gameId = result.gameId
                    )
                }
                onTrainingFinished(result)
            }
        },
        onBackClick = onBackClick,
        onNavigate = onNavigate,
        modifier = modifier
    )
}

@Composable
// Hosts the single-game training session state and coordinates training flow.
private fun TrainSingleGameScreen(
    gameId: Long,
    trainingId: Long,
    trainingGameData: TrainSingleGameData? = null,
    onTrainingFinished: (TrainSingleGameResult) -> Unit = {},
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Keeps the parent navigation section highlighted while the training screen is open.
    var selectedNavItem by remember { mutableStateOf<ScreenType>(ScreenType.Home) }
    // Stores the full mutable training session state for the current screen.
    var uiState by remember(trainingGameData?.game?.id) { mutableStateOf(TrainSingleGameUiState()) }
    // Resolves the ordered list of sides that must be trained for the current game.
    val trainingSides = remember(trainingGameData?.game?.sideMask) {
        trainingGameData?.game?.sideMask?.let(::resolveTrainingOrientations).orEmpty()
    }
    // Selects the active board orientation for the current training side.
    val currentOrientation = trainingSides.getOrNull(uiState.currentSideIndex) ?: BoardOrientation.WHITE
    // Owns the interactive board state for the currently trained side.
    val gameController = remember(currentOrientation) { GameController(currentOrientation) }

    LaunchedEffect(gameController, trainingGameData?.game?.id) {
        gameController.resetToStartPosition()
        uiState = resetSessionState(uiState)
    }

    LaunchedEffect(uiState.phase, trainingGameData?.uciMoves, gameController) {
        uiState = runShowLine(
            uiState = uiState,
            gameController = gameController,
            uciMoves = trainingGameData?.uciMoves.orEmpty()
        )
    }

    LaunchedEffect(
        uiState.phase,
        gameController.boardState,
        uiState.expectedPly,
        currentOrientation,
        trainingGameData?.uciMoves
    ) {
        uiState = handleTrainingProgress(
            uiState = uiState,
            gameController = gameController,
            uciMoves = trainingGameData?.uciMoves.orEmpty(),
            currentOrientation = currentOrientation,
            sidesCount = trainingSides.size
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = TrainingBackgroundDark,
        topBar = {
            AppTopBar(
                title = "Train Game",
                onBackClick = onBackClick
            )
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
                gameController.resetToStartPosition()
                uiState = buildRepeatVariationState(uiState)
            },
            onFinishClick = {
                uiState = handleCompletionFinish(
                    uiState = uiState,
                    gameId = gameId,
                    trainingId = trainingId,
                    onTrainingFinished = onTrainingFinished
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            TrainSingleGameContent(
                gameId = gameId,
                trainingId = trainingId,
                trainingGameData = trainingGameData,
                gameController = gameController,
                currentOrientation = currentOrientation,
                currentSideIndex = uiState.currentSideIndex,
                sidesCount = trainingSides.size,
                phase = uiState.phase,
                mistakesCount = uiState.mistakesCount,
                onShowLineClick = {
                    gameController.resetToStartPosition()
                    uiState = buildShowLineState(uiState)
                },
                onStartTrainingClick = {
                    gameController.resetToStartPosition()
                    uiState = buildStartTrainingState(uiState)
                },
                onMakeCorrectMoveClick = {
                    uiState = handleCorrectMove(
                        uiState = uiState,
                        gameController = gameController,
                        uciMoves = trainingGameData?.uciMoves.orEmpty()
                    )
                }
            )
        }
    }
}

@Composable
private fun RenderCompletionDialog(
    dialogState: TrainSingleGameCompletionState?,
    onRepeatClick: () -> Unit,
    onFinishClick: () -> Unit
) {
    if (dialogState == null) {
        return
    }

    TrainSingleGameCompletionDialog(
        dialogState = dialogState,
        onRepeatClick = onRepeatClick,
        onFinishClick = onFinishClick
    )
}

@Composable
// Renders the central training content for the selected game and side.
private fun TrainSingleGameContent(
    gameId: Long,
    trainingId: Long,
    trainingGameData: TrainSingleGameData?,
    gameController: GameController,
    currentOrientation: BoardOrientation,
    currentSideIndex: Int,
    sidesCount: Int,
    phase: TrainSingleGamePhase,
    mistakesCount: Int,
    onShowLineClick: () -> Unit,
    onStartTrainingClick: () -> Unit,
    onMakeCorrectMoveClick: () -> Unit,
) {
    ScreenSection {
        if (trainingGameData == null) {
            BodySecondaryText(
                text = "Loading training session for gameId=$gameId, trainingId=$trainingId",
                color = TrainingTextSecondary
            )
            return@ScreenSection
        }

        Column {
            TrainingGameHeader(title = trainingGameData.game.event)
            Spacer(modifier = Modifier.height(AppDimens.spaceSm))
            TrainingBoardSection(gameController = gameController)
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            TrainingSingleGameActions(
                onShowLineClick = onShowLineClick,
                onStartTrainingClick = onStartTrainingClick,
                onMakeCorrectMoveClick = onMakeCorrectMoveClick,
                isShowingLine = phase == TrainSingleGamePhase.ShowingLine,
                isTraining = phase == TrainSingleGamePhase.Training,
                showCorrectMove = phase == TrainSingleGamePhase.Mistake,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            TrainingSessionInfo(
                trainingId = trainingId,
                gameId = gameId,
                movesCount = trainingGameData.uciMoves.size,
                currentOrientation = currentOrientation,
                currentSideIndex = currentSideIndex,
                sidesCount = sidesCount,
                phase = phase,
                mistakesCount = mistakesCount
            )
        }
    }
}

@Composable
private fun TrainingGameHeader(
    title: String?,
    modifier: Modifier = Modifier
) {
    SectionTitleText(
        text = title ?: "Unnamed Opening",
        modifier = modifier,
        color = TrainingTextPrimary
    )
}

@Composable
private fun TrainingSessionInfo(
    trainingId: Long,
    gameId: Long,
    movesCount: Int,
    currentOrientation: BoardOrientation,
    currentSideIndex: Int,
    sidesCount: Int,
    phase: TrainSingleGamePhase,
    mistakesCount: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        BodySecondaryText(
            text = "Training ID: $trainingId",
            color = TrainingTextSecondary
        )
        BodySecondaryText(
            text = "Game ID: $gameId",
            color = TrainingTextSecondary
        )
        BodySecondaryText(
            text = "Moves loaded: $movesCount",
            color = TrainingTextSecondary
        )
        BodySecondaryText(
            text = "Training side: ${orientationLabel(currentOrientation)}",
            color = TrainingTextSecondary
        )
        if (sidesCount > 1) {
            CardMetaText(
                text = "Side ${currentSideIndex + 1} of $sidesCount",
                color = TrainingTextSecondary
            )
        }
        BodySecondaryText(
            text = "Session state: ${phase.name}",
            color = TrainingTextSecondary
        )
        BodySecondaryText(
            text = "Mistakes: $mistakesCount",
            color = TrainingTextSecondary
        )
    }
}

@Composable
// Displays the session action buttons and the corrective move action after mistakes.
private fun TrainingSingleGameActions(
    onShowLineClick: () -> Unit,
    onStartTrainingClick: () -> Unit,
    onMakeCorrectMoveClick: () -> Unit,
    isShowingLine: Boolean,
    isTraining: Boolean,
    showCorrectMove: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
        ) {
            PrimaryButton(
                text = "Show line",
                onClick = onShowLineClick,
                enabled = !isShowingLine && !isTraining
            )
            PrimaryButton(
                text = "Start training",
                onClick = onStartTrainingClick,
                enabled = !isShowingLine && !isTraining
            )
        }

        if (showCorrectMove) {
            Spacer(modifier = Modifier.height(AppDimens.spaceMd))
            PrimaryButton(
                text = "Make correct move",
                onClick = onMakeCorrectMoveClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
// Renders the interactive chess board used by the training session.
private fun TrainingBoardSection(
    gameController: GameController,
    modifier: Modifier = Modifier
) {
    CardSurface(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(AppDimens.spaceMd)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppDimens.radiusLg))
        ) {
            ChessBoardWithCoordinates(
                gameController = gameController,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
// Shows the completion dialog for repeating the variation or finishing the side/session.
private fun TrainSingleGameCompletionDialog(
    dialogState: TrainSingleGameCompletionState,
    onRepeatClick: () -> Unit,
    onFinishClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onFinishClick,
        title = {
            SectionTitleText(text = dialogState.title, color = TrainingTextPrimary)
        },
        text = {
            BodySecondaryText(text = dialogState.message, color = TrainingTextSecondary)
        },
        confirmButton = {
            TextButton(onClick = onRepeatClick) {
                BodySecondaryText(text = "Repeat variation", color = TrainingTextPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onFinishClick) {
                BodySecondaryText(text = dialogState.finishLabel, color = TrainingTextPrimary)
            }
        }
    )
}

// Resolves the ordered list of training orientations from the stored side mask.
private fun resolveTrainingOrientations(sideMask: Int): List<BoardOrientation> {
    if (sideMask == SideMask.WHITE) {
        return listOf(BoardOrientation.WHITE)
    }

    if (sideMask == SideMask.BLACK) {
        return listOf(BoardOrientation.BLACK)
    }

    if (sideMask == SideMask.BOTH) {
        return listOf(BoardOrientation.WHITE, BoardOrientation.BLACK)
    }

    return listOf(BoardOrientation.WHITE)
}

// Returns the human-readable label for the active training orientation.
private fun orientationLabel(orientation: BoardOrientation): String =
    when (orientation) {
        BoardOrientation.WHITE -> "White"
        BoardOrientation.BLACK -> "Black"
    }

// Determines whether the current ply should be played by the user for the active side.
private fun isUserTurn(expectedPly: Int, orientation: BoardOrientation): Boolean =
    when (orientation) {
        BoardOrientation.WHITE -> expectedPly % 2 == 0
        BoardOrientation.BLACK -> expectedPly % 2 == 1
    }

// Converts a chesslib move into the stored UCI move format.
private fun moveToUci(move: com.github.bhlangonijr.chesslib.move.Move): String =
    "${move.from.value().lowercase()}${move.to.value().lowercase()}"

private fun resetSessionState(uiState: TrainSingleGameUiState): TrainSingleGameUiState =
    uiState.copy(
        phase = TrainSingleGamePhase.Idle,
        expectedPly = 0,
        completionDialog = null
    )

private fun buildShowLineState(uiState: TrainSingleGameUiState): TrainSingleGameUiState =
    uiState.copy(
        completionDialog = null,
        expectedPly = 0,
        phase = TrainSingleGamePhase.ShowingLine
    )

private fun buildStartTrainingState(uiState: TrainSingleGameUiState): TrainSingleGameUiState =
    uiState.copy(
        completionDialog = null,
        expectedPly = 0,
        phase = TrainSingleGamePhase.Training
    )

private fun buildRepeatVariationState(uiState: TrainSingleGameUiState): TrainSingleGameUiState =
    uiState.copy(
        completionDialog = null,
        expectedPly = 0,
        phase = TrainSingleGamePhase.Training
    )

private suspend fun runShowLine(
    uiState: TrainSingleGameUiState,
    gameController: GameController,
    uciMoves: List<String>
): TrainSingleGameUiState {
    if (uiState.phase != TrainSingleGamePhase.ShowingLine) {
        return uiState
    }

    gameController.resetToStartPosition()

    for (ply in 1..uciMoves.size) {
        delay(ShowLineMoveDelayMs)
        gameController.loadFromUciMoves(uciMoves, ply)
    }

    return uiState.copy(phase = TrainSingleGamePhase.Idle)
}

private fun handleTrainingProgress(
    uiState: TrainSingleGameUiState,
    gameController: GameController,
    uciMoves: List<String>,
    currentOrientation: BoardOrientation,
    sidesCount: Int
): TrainSingleGameUiState {
    if (uiState.phase != TrainSingleGamePhase.Training) {
        return uiState
    }

    if (uciMoves.isEmpty()) {
        return uiState
    }

    if (uiState.expectedPly >= uciMoves.size) {
        return uiState.copy(
            phase = TrainSingleGamePhase.Idle,
            completionDialog = buildCompletionDialog(
                currentSideIndex = uiState.currentSideIndex,
                sidesCount = sidesCount,
                currentOrientation = currentOrientation
            )
        )
    }

    if (!isUserTurn(uiState.expectedPly, currentOrientation)) {
        gameController.loadFromUciMoves(uciMoves, uiState.expectedPly + 1)
        return uiState.copy(expectedPly = uiState.expectedPly + 1)
    }

    if (gameController.currentMoveIndex <= uiState.expectedPly) {
        return uiState
    }

    val lastMoveUci = gameController.getMovesCopy()
        .getOrNull(gameController.currentMoveIndex - 1)
        ?.let(::moveToUci)
        ?: return uiState

    if (lastMoveUci == uciMoves[uiState.expectedPly]) {
        return uiState.copy(expectedPly = uiState.expectedPly + 1)
    }

    gameController.loadFromUciMoves(uciMoves, uiState.expectedPly)
    return uiState.copy(
        mistakesCount = uiState.mistakesCount + 1,
        phase = TrainSingleGamePhase.Mistake
    )
}

private fun handleCorrectMove(
    uiState: TrainSingleGameUiState,
    gameController: GameController,
    uciMoves: List<String>
): TrainSingleGameUiState {
    if (uiState.expectedPly >= uciMoves.size) {
        return uiState.copy(phase = TrainSingleGamePhase.Idle)
    }

    gameController.loadFromUciMoves(uciMoves, uiState.expectedPly + 1)
    return uiState.copy(
        expectedPly = uiState.expectedPly + 1,
        phase = TrainSingleGamePhase.Training
    )
}

private fun handleCompletionFinish(
    uiState: TrainSingleGameUiState,
    gameId: Long,
    trainingId: Long,
    onTrainingFinished: (TrainSingleGameResult) -> Unit
): TrainSingleGameUiState {
    val dialogState = uiState.completionDialog ?: return uiState
    val clearedDialogState = uiState.copy(completionDialog = null)

    if (dialogState.hasNextSide) {
        return clearedDialogState.copy(
            currentSideIndex = clearedDialogState.currentSideIndex + 1
        )
    }

    onTrainingFinished(
        TrainSingleGameResult(
            gameId = gameId,
            trainingId = trainingId,
            mistakesCount = uiState.mistakesCount
        )
    )
    return clearedDialogState
}

private data class TrainSingleGameCompletionState(
    val title: String,
    val message: String,
    val finishLabel: String,
    val hasNextSide: Boolean
)

// Builds the completion dialog state for either the next side or final finish.
private fun buildCompletionDialog(
    currentSideIndex: Int,
    sidesCount: Int,
    currentOrientation: BoardOrientation
): TrainSingleGameCompletionState {
    if (currentSideIndex + 1 < sidesCount) {
        return TrainSingleGameCompletionState(
            title = "Variation completed",
            message = "The ${orientationLabel(currentOrientation).lowercase()} side is completed. Continue with the next side or repeat this variation.",
            finishLabel = "Next side",
            hasNextSide = true
        )
    }

    return TrainSingleGameCompletionState(
        title = "Variation completed",
        message = "You reached the end of the game. Repeat the variation or finish this training.",
        finishLabel = "Finish variation",
        hasNextSide = false
    )
}
