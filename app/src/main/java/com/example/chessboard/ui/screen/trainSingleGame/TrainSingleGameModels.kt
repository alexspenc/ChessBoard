package com.example.chessboard.ui.screen.trainSingleGame

import com.example.chessboard.entity.GameEntity
import com.example.chessboard.entity.SideMask
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.ui.BoardOrientation
import com.github.bhlangonijr.chesslib.move.Move

//__________________________________________________________________________________________
// Shared models and small pure helpers used by the single-game training screen:
// loaded game data, session state, completion dialog data, and side/turn mapping.
//__________________________________________________________________________________________

// Holds the loaded game and parsed move list that the container passes into the screen
// for rendering the board, autoplaying the line, and validating user moves.
data class TrainSingleGameData(
    val game: GameEntity,
    val uciMoves: List<String>
)

// Carries the final result of a single-game training session back to the caller.
data class TrainSingleGameResult(
    val gameId: Long,
    val trainingId: Long,
    val mistakesCount: Int
)

internal const val ShowLineMoveDelayMs = 500L
internal const val TrainSingleGameLogTag = "TrainSingleGame"

// Describes the current stage of the single-game training session.
internal enum class TrainSingleGamePhase {
    Idle,
    ShowingLine,
    Training,
    Mistake
}

internal data class TrainSingleGameUiState(
    // Index of the currently trained side in the resolved orientation list.
    val currentSideIndex: Int = 0,
    // Current phase of the training session.
    val phase: TrainSingleGamePhase = TrainSingleGamePhase.Idle,
    // Index of the next expected move in the training line.
    val expectedPly: Int = 0,
    // Total number of mistakes made during the session.
    val mistakesCount: Int = 0,
    // Completion dialog state shown after finishing a variation.
    val completionDialog: TrainSingleGameCompletionState? = null,
    val wrongMoveDialogMessage: String? = null
)

// Stores the data needed to render the variation completion dialog.
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
    val gameController: GameController,
    val currentOrientation: BoardOrientation,
    val currentSideIndex: Int,
    val sidesCount: Int,
    val currentPly: Int,
    val moveLabels: List<String>,
    val phase: TrainSingleGamePhase,
    val mistakesCount: Int
)

internal data class TrainSingleGameContentActions(
    val onShowLineClick: () -> Unit,
    val onStopShowLineClick: () -> Unit,
    val onStartTrainingClick: () -> Unit,
    val onStopTrainingClick: () -> Unit,
    val onMakeCorrectMoveClick: () -> Unit
)

// Resolves the ordered list of training orientations from the stored side mask.
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

// Returns the human-readable label for the active training orientation.
internal fun orientationLabel(orientation: BoardOrientation): String =
    when (orientation) {
        BoardOrientation.WHITE -> "White"
        BoardOrientation.BLACK -> "Black"
    }

// Determines whether the current ply should be played by the user for the active side.
internal fun isUserTurn(expectedPly: Int, orientation: BoardOrientation): Boolean =
    when (orientation) {
        BoardOrientation.WHITE -> expectedPly % 2 == 0
        BoardOrientation.BLACK -> expectedPly % 2 == 1
    }

// Converts a chesslib move into the stored UCI move format.
internal fun moveToUci(move: Move): String =
    "${move.from.value().lowercase()}${move.to.value().lowercase()}"

// Builds the completion dialog state for either the next side or final finish.
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
