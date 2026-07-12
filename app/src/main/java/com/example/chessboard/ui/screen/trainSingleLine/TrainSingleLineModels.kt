package com.example.chessboard.ui.screen.trainSingleLine

// Shared state models and small pure helpers for single-line training.
// This file keeps lightweight data structures in one place so the screen, logic,
// and UI component files can depend on the same vocabulary.

import com.example.chessboard.boardmodel.LineDraft
import com.example.chessboard.entity.LineEntity
import com.example.chessboard.entity.SideMask
import com.example.chessboard.ui.BoardOrientation

data class TrainSingleLineData(
    val line: LineEntity,
    val uciMoves: List<String>,
    val startFen: String? = null,
    val hasMoveCap: Boolean = false,
    val analysisUciMoves: List<String> = uciMoves,
    val analysisStartPly: Int = 0,
)

data class TrainSingleLineTarget(
    val trainingId: Long,
    val lineId: Long,
)

data class TrainSingleLineResult(
    val lineId: Long,
    val trainingId: Long,
    val mistakesCount: Int
)

data class TrainSingleLineLaunchRequest(
    val target: TrainSingleLineTarget,
    val moveFrom: Int = 1,
    val moveTo: Int = 0,
)

data class TrainSingleLineLaunchActions(
    val onTrainingFinished: (TrainSingleLineResult) -> Unit,
    val onNextTrainingClick: (TrainSingleLineResult) -> Unit,
    val onInterruptTrainingClick: () -> Unit,
    val onOpenLineEditorClick: (LineEntity) -> Unit,
    val onCloneLineClick: (LineDraft) -> Unit,
    val onSearchByPositionClick: (String) -> Unit,
    val onAnalyzeLineClick: (List<String>, Int) -> Unit,
    val onSimpleViewUpgradePromptRequested: (Int) -> Unit,
)

data class TrainSingleLineSessionProgress(
    val hasNextTrainingLine: Boolean = false,
    val sessionCurrent: Int = 0,
    val sessionTotal: Int = 0,
)

internal const val ShowLineMoveDelayMs = 500L
internal const val MinShowLineMoveDelayMs = 100L
internal const val MaxShowLineMoveDelayMs = 4000L
internal const val TrainSingleLineLogTag = "TrainSingleLine"

internal enum class TrainSingleLinePhase {
    Idle,
    ShowingLine,
    Training,
    Mistake
}

internal data class TrainSingleLineUiState(
    val currentSideIndex: Int = 0,
    val phase: TrainSingleLinePhase = TrainSingleLinePhase.Idle,
    val expectedPly: Int = 0,
    val mistakesCount: Int = 0,
    val completionDialog: TrainSingleLineCompletionState? = null,
    val wrongMoveSquare: String? = null,
    val hintSquare: String? = null,
    val showLineMoveDelayInput: String = ShowLineMoveDelayMs.toString(),
    val showLineCompleted: Boolean = false
)

internal data class TrainSingleLineCompletionState(
    val completedOrientation: BoardOrientation,
    val hasNextSide: Boolean
)

internal data class TrainSingleLineContentState(
    val target: TrainSingleLineTarget,
    val trainingLineData: TrainSingleLineData,
    val uiState: TrainSingleLineUiState,
    val currentOrientation: BoardOrientation,
    val sidesCount: Int,
    val sessionProgress: TrainSingleLineSessionProgress,
    val currentPly: Int,
    val moveLabels: List<String>,
    val phase: TrainSingleLinePhase,
    val mistakesCount: Int,
    val showLineMoveDelayInput: String,
    val showLineCompleted: Boolean,
    val wrongMoveSquare: String? = null,
    val hintSquare: String? = null,
)

internal data class TrainSingleLineContentActions(
    val onShowLineClick: () -> Unit,
    val onConfirmShowLineClick: () -> Unit,
    val onDismissShowLineDialogClick: () -> Unit,
    val onStopShowLineClick: () -> Unit,
    val onAnalyzeLineClick: () -> Unit,
    val onStartTrainingClick: () -> Unit,
    val onStopTrainingClick: () -> Unit,
    val onMakeCorrectMoveClick: () -> Unit,
    val onHintClick: () -> Unit,
    val onShowLineMoveDelayInputChange: (String) -> Unit,
    val onMovePlyClick: (Int) -> Unit,
    val onPrevMoveClick: () -> Unit,
    val onNextMoveClick: () -> Unit,
    val onResetMovesClick: () -> Unit
)

internal sealed interface TrainingSingleLineActionsState {
    data object ShowingLine : TrainingSingleLineActionsState
    data object Idle : TrainingSingleLineActionsState
    data object Training : TrainingSingleLineActionsState
    data object Mistake : TrainingSingleLineActionsState
}

internal fun resolveTrainingSingleLineActionsState(
    phase: TrainSingleLinePhase
): TrainingSingleLineActionsState {
    if (phase == TrainSingleLinePhase.ShowingLine) {
        return TrainingSingleLineActionsState.ShowingLine
    }

    if (phase == TrainSingleLinePhase.Training) {
        return TrainingSingleLineActionsState.Training
    }

    if (phase == TrainSingleLinePhase.Mistake) {
        return TrainingSingleLineActionsState.Mistake
    }

    return TrainingSingleLineActionsState.Idle
}

internal fun resolveTrainingAnalysisInitialPly(
    trainingLineData: TrainSingleLineData,
    currentPly: Int,
): Int {
    val requestedPly = trainingLineData.analysisStartPly + currentPly
    return requestedPly.coerceIn(0, trainingLineData.analysisUciMoves.size)
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

internal fun isUserTurn(expectedPly: Int, orientation: BoardOrientation): Boolean =
    when (orientation) {
        BoardOrientation.WHITE -> expectedPly % 2 == 0
        BoardOrientation.BLACK -> expectedPly % 2 == 1
    }

internal fun buildCompletionDialog(
    currentSideIndex: Int,
    sidesCount: Int,
    currentOrientation: BoardOrientation
): TrainSingleLineCompletionState {
    if (currentSideIndex + 1 < sidesCount) {
        return TrainSingleLineCompletionState(
            completedOrientation = currentOrientation,
            hasNextSide = true
        )
    }

    return TrainSingleLineCompletionState(
        completedOrientation = currentOrientation,
        hasNextSide = false
    )
}

internal fun resolveShowLineMoveDelayMs(input: String): Long {
    return input.toLongOrNull()
        ?.coerceIn(MinShowLineMoveDelayMs, MaxShowLineMoveDelayMs)
        ?: ShowLineMoveDelayMs
}

internal fun formatShowLineMoveDelayInput(delayMs: Long): String {
    return delayMs.coerceIn(MinShowLineMoveDelayMs, MaxShowLineMoveDelayMs).toString()
}
