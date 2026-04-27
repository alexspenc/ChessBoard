package com.example.chessboard.ui.screen.trainSingleGame

// Shared state models and small pure helpers for single-game training.
// This file keeps lightweight data structures in one place so the screen, logic,
// and UI component files can depend on the same vocabulary.

import com.example.chessboard.entity.GameEntity
import com.example.chessboard.entity.SideMask
import com.example.chessboard.ui.BoardOrientation
import com.github.bhlangonijr.chesslib.move.Move

data class TrainSingleGameData(
    val game: GameEntity,
    val uciMoves: List<String>,
    val startFen: String? = null,
    val hasMoveCap: Boolean = false,
    val analysisUciMoves: List<String> = uciMoves,
    val analysisStartPly: Int = 0,
)

data class TrainSingleGameResult(
    val gameId: Long,
    val trainingId: Long,
    val mistakesCount: Int
)

internal const val ShowLineMoveDelayMs = 500L
internal const val MinShowLineMoveDelayMs = 100L
internal const val MaxShowLineMoveDelayMs = 1500L
internal const val ShowLineMoveDelayStepMs = 100L
internal const val TrainSingleGameLogTag = "TrainSingleGame"

internal enum class TrainSingleGamePhase {
    Idle,
    ShowingLine,
    Training,
    Mistake
}

internal data class TrainSingleGameUiState(
    val currentSideIndex: Int = 0,
    val phase: TrainSingleGamePhase = TrainSingleGamePhase.Idle,
    val expectedPly: Int = 0,
    val mistakesCount: Int = 0,
    val completionDialog: TrainSingleGameCompletionState? = null,
    val wrongMoveSquare: String? = null,
    val hintSquare: String? = null,
    val showLineMoveDelayInput: String = ShowLineMoveDelayMs.toString(),
    val showLineCompleted: Boolean = false
)

internal data class TrainSingleGameCompletionState(
    val title: String,
    val message: String,
    val finishLabel: String,
    val hasNextSide: Boolean
)

internal data class TrainSingleGameContentState(
    val gameId: Long,
    val trainingId: Long,
    val trainingGameData: TrainSingleGameData,
    val currentOrientation: BoardOrientation,
    val sidesCount: Int,
    val sessionCurrent: Int,
    val sessionTotal: Int,
    val currentPly: Int,
    val moveLabels: List<String>,
    val phase: TrainSingleGamePhase,
    val mistakesCount: Int,
    val showLineMoveDelayInput: String,
    val showLineCompleted: Boolean,
    val wrongMoveSquare: String? = null,
    val hintSquare: String? = null,
)

internal data class TrainSingleGameContentActions(
    val onShowLineClick: () -> Unit,
    val onConfirmShowLineClick: () -> Unit,
    val onDismissShowLineDialogClick: () -> Unit,
    val onStopShowLineClick: () -> Unit,
    val onAnalyzeGameClick: () -> Unit,
    val onStartTrainingClick: () -> Unit,
    val onStopTrainingClick: () -> Unit,
    val onMakeCorrectMoveClick: () -> Unit,
    val onHintClick: () -> Unit,
    val onShowLineMoveDelayInputChange: (String) -> Unit,
    val onDecreaseShowLineMoveDelayClick: () -> Unit,
    val onIncreaseShowLineMoveDelayClick: () -> Unit,
    val onMovePlyClick: (Int) -> Unit,
    val onPrevMoveClick: () -> Unit,
    val onNextMoveClick: () -> Unit,
    val onResetMovesClick: () -> Unit
)

internal sealed interface TrainingSingleGameActionsState {
    data object ShowingLine : TrainingSingleGameActionsState
    data object Idle : TrainingSingleGameActionsState
    data object Training : TrainingSingleGameActionsState
    data object Mistake : TrainingSingleGameActionsState
}

internal fun resolveTrainingSingleGameActionsState(
    phase: TrainSingleGamePhase
): TrainingSingleGameActionsState {
    if (phase == TrainSingleGamePhase.ShowingLine) {
        return TrainingSingleGameActionsState.ShowingLine
    }

    if (phase == TrainSingleGamePhase.Training) {
        return TrainingSingleGameActionsState.Training
    }

    if (phase == TrainSingleGamePhase.Mistake) {
        return TrainingSingleGameActionsState.Mistake
    }

    return TrainingSingleGameActionsState.Idle
}

internal fun resolveTrainingAnalysisInitialPly(
    trainingGameData: TrainSingleGameData,
    currentPly: Int,
): Int {
    val requestedPly = trainingGameData.analysisStartPly + currentPly
    return requestedPly.coerceIn(0, trainingGameData.analysisUciMoves.size)
}

internal fun resolveTrainingOrientations(sideMask: Int): List<BoardOrientation> {
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

internal fun orientationLabel(orientation: BoardOrientation): String =
    when (orientation) {
        BoardOrientation.WHITE -> "White"
        BoardOrientation.BLACK -> "Black"
    }

internal fun isUserTurn(expectedPly: Int, orientation: BoardOrientation): Boolean =
    when (orientation) {
        BoardOrientation.WHITE -> expectedPly % 2 == 0
        BoardOrientation.BLACK -> expectedPly % 2 == 1
    }

internal fun moveToUci(move: Move): String =
    "${move.from.value().lowercase()}${move.to.value().lowercase()}"

internal fun buildCompletionDialog(
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

internal fun resolveShowLineMoveDelayMs(input: String): Long {
    return input.toLongOrNull()
        ?.coerceIn(MinShowLineMoveDelayMs, MaxShowLineMoveDelayMs)
        ?: ShowLineMoveDelayMs
}

internal fun changeShowLineMoveDelay(input: String, delta: Long): String {
    val nextValue = (resolveShowLineMoveDelayMs(input) + delta)
        .coerceIn(MinShowLineMoveDelayMs, MaxShowLineMoveDelayMs)
    return nextValue.toString()
}
