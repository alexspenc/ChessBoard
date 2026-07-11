package com.example.chessboard.ui.screen.trainSingleLine

// Pure session-transition logic for single-line training.
// This file is separate so move progression, completion handling, and mistake handling
// can evolve independently from Compose rendering.

import android.util.Log
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.ui.BoardOrientation
import kotlinx.coroutines.delay

internal fun resetSessionState(uiState: TrainSingleLineUiState): TrainSingleLineUiState =
    uiState.copy(
        phase = TrainSingleLinePhase.Idle,
        expectedPly = 0,
        completionDialog = null,
        wrongMoveSquare = null,
        hintSquare = null,
        showLineCompleted = false,
    )

internal fun buildShowLineState(uiState: TrainSingleLineUiState): TrainSingleLineUiState =
    uiState.copy(
        completionDialog = null,
        expectedPly = 0,
        phase = TrainSingleLinePhase.ShowingLine,
        wrongMoveSquare = null,
        hintSquare = null,
        showLineCompleted = false,
    )

internal fun buildStartTrainingState(uiState: TrainSingleLineUiState): TrainSingleLineUiState =
    uiState.copy(
        completionDialog = null,
        expectedPly = 0,
        phase = TrainSingleLinePhase.Training,
        wrongMoveSquare = null,
        hintSquare = null,
        showLineCompleted = false,
    )

internal fun buildRepeatVariationState(uiState: TrainSingleLineUiState): TrainSingleLineUiState =
    uiState.copy(
        completionDialog = null,
        expectedPly = 0,
        phase = TrainSingleLinePhase.Training,
        wrongMoveSquare = null,
        hintSquare = null,
        showLineCompleted = false,
    )

// Replays the full variation from the start with a fixed delay between moves.
internal suspend fun runShowLine(
    uiState: TrainSingleLineUiState,
    lineController: LineController,
    uciMoves: List<String>,
    moveDelayMs: Long,
    onBoardStateChanged: () -> Unit,
    startFen: String? = null,
): TrainSingleLineUiState {
    if (uiState.phase != TrainSingleLinePhase.ShowingLine) {
        Log.d(
            TrainSingleLineLogTag,
            "runShowLine skipped because phase=${uiState.phase} boardState=${lineController.boardState}"
        )
        return uiState
    }

    Log.d(
        TrainSingleLineLogTag,
        "runShowLine started. boardState=${lineController.boardState} movesCount=${uciMoves.size}"
    )
    lineController.loadFromUciMoves(uciMoves, 0, startFen)
    onBoardStateChanged()
    Log.d(
        TrainSingleLineLogTag,
        "runShowLine loaded start position. boardState=${lineController.boardState}"
    )

    for (ply in 1..uciMoves.size) {
        Log.d(
            TrainSingleLineLogTag,
            "runShowLine step. ply=$ply move=${uciMoves[ply - 1]} boardStateBefore=${lineController.boardState}"
        )
        delay(moveDelayMs)
        lineController.redoMove()
        onBoardStateChanged()
        Log.d(
            TrainSingleLineLogTag,
            "runShowLine step applied. ply=$ply boardStateAfter=${lineController.boardState}"
        )
    }

    Log.d(
        TrainSingleLineLogTag,
        "runShowLine finished. boardState=${lineController.boardState}"
    )
    return uiState.copy(phase = TrainSingleLinePhase.Idle, showLineCompleted = true)
}

// Advances the session by applying forced program moves or validating the latest user move.
internal fun handleTrainingProgress(
    uiState: TrainSingleLineUiState,
    lineController: LineController,
    uciMoves: List<String>,
    currentOrientation: BoardOrientation,
    sidesCount: Int,
    startFen: String? = null,
    hasMoveCap: Boolean = false,
): TrainSingleLineUiState {
    if (uiState.phase != TrainSingleLinePhase.Training) {
        return uiState
    }

    if (uciMoves.isEmpty()) {
        return uiState
    }

    if (uiState.expectedPly >= uciMoves.size) {
        return uiState.copy(
            phase = TrainSingleLinePhase.Idle,
            completionDialog = buildCompletionDialog(
                currentSideIndex = uiState.currentSideIndex,
                sidesCount = sidesCount,
                currentOrientation = currentOrientation
            )
        )
    }

    if (!isUserTurn(uiState.expectedPly, currentOrientation)) {
        // When a move cap is set, skip the last computer reply so training ends
        // after the player's final move, not after the opponent's response.
        if (hasMoveCap && uiState.expectedPly + 1 >= uciMoves.size) {
            return uiState.copy(
                phase = TrainSingleLinePhase.Idle,
                completionDialog = buildCompletionDialog(
                    currentSideIndex = uiState.currentSideIndex,
                    sidesCount = sidesCount,
                    currentOrientation = currentOrientation
                )
            )
        }
        lineController.loadFromUciMoves(uciMoves, uiState.expectedPly + 1, startFen)
        return uiState.copy(expectedPly = uiState.expectedPly + 1)
    }

    if (lineController.currentMoveIndex <= uiState.expectedPly) {
        return uiState
    }

    val lastMoveUci = lineController.getMovesCopy()
        .getOrNull(lineController.currentMoveIndex - 1)
        ?.let(::moveToUci)
        ?: return uiState

    if (lastMoveUci == uciMoves[uiState.expectedPly]) {
        return advanceProgramMoves(
            uiState = uiState.copy(expectedPly = uiState.expectedPly + 1, wrongMoveSquare = null, hintSquare = null),
            lineController = lineController,
            uciMoves = uciMoves,
            currentOrientation = currentOrientation,
            sidesCount = sidesCount,
            startFen = startFen,
            hasMoveCap = hasMoveCap,
        )
    }

    val wrongSquare = lastMoveUci.substring(2, 4)
    lineController.loadFromUciMoves(uciMoves, uiState.expectedPly, startFen)
    return uiState.copy(
        mistakesCount = uiState.mistakesCount + 1,
        wrongMoveSquare = wrongSquare,
        phase = TrainSingleLinePhase.Training,
    )
}

internal fun handleCorrectMove(
    uiState: TrainSingleLineUiState,
    lineController: LineController,
    uciMoves: List<String>,
    currentOrientation: BoardOrientation,
    sidesCount: Int,
    startFen: String? = null,
    hasMoveCap: Boolean = false,
): TrainSingleLineUiState {
    if (uiState.expectedPly >= uciMoves.size) {
        return uiState.copy(phase = TrainSingleLinePhase.Idle, showLineCompleted = true)
    }

    lineController.loadFromUciMoves(uciMoves, uiState.expectedPly + 1, startFen)
    return advanceProgramMoves(
        uiState = uiState.copy(
            expectedPly = uiState.expectedPly + 1,
            phase = TrainSingleLinePhase.Training
        ),
        lineController = lineController,
        uciMoves = uciMoves,
        currentOrientation = currentOrientation,
        sidesCount = sidesCount,
        startFen = startFen,
        hasMoveCap = hasMoveCap,
    )
}

// Continues applying forced replies until control returns to the user or the line ends.
internal fun advanceProgramMoves(
    uiState: TrainSingleLineUiState,
    lineController: LineController,
    uciMoves: List<String>,
    currentOrientation: BoardOrientation,
    sidesCount: Int,
    startFen: String? = null,
    hasMoveCap: Boolean = false,
): TrainSingleLineUiState {
    if (uiState.phase != TrainSingleLinePhase.Training) {
        return uiState
    }

    if (uciMoves.isEmpty()) {
        return uiState
    }

    var nextState = uiState

    while (nextState.expectedPly < uciMoves.size && !isUserTurn(
            nextState.expectedPly,
            currentOrientation
        )
    ) {
        // When a move cap is set, skip the final computer reply so training ends
        // after the player's last move rather than after the opponent's response.
        if (hasMoveCap && nextState.expectedPly + 1 >= uciMoves.size) {
            return nextState.copy(
                phase = TrainSingleLinePhase.Idle,
                completionDialog = buildCompletionDialog(
                    currentSideIndex = nextState.currentSideIndex,
                    sidesCount = sidesCount,
                    currentOrientation = currentOrientation
                )
            )
        }
        lineController.loadFromUciMoves(uciMoves, nextState.expectedPly + 1, startFen)
        nextState = nextState.copy(expectedPly = nextState.expectedPly + 1)
    }

    if (nextState.expectedPly < uciMoves.size) {
        return nextState
    }

    return nextState.copy(
        phase = TrainSingleLinePhase.Idle,
        completionDialog = buildCompletionDialog(
            currentSideIndex = nextState.currentSideIndex,
            sidesCount = sidesCount,
            currentOrientation = currentOrientation
        )
    )
}

// Moves to the next side when needed, otherwise completes the training session.
internal fun handleCompletionFinish(
    uiState: TrainSingleLineUiState,
    lineId: Long,
    trainingId: Long,
    onTrainingFinished: (TrainSingleLineResult) -> Unit
): TrainSingleLineUiState {
    val dialogState = uiState.completionDialog ?: return uiState
    val clearedDialogState = uiState.copy(completionDialog = null)

    if (dialogState.hasNextSide) {
        return clearedDialogState.copy(
            currentSideIndex = clearedDialogState.currentSideIndex + 1
        )
    }

    onTrainingFinished(
        TrainSingleLineResult(
            lineId = lineId,
            trainingId = trainingId,
            mistakesCount = uiState.mistakesCount
        )
    )
    return clearedDialogState
}

internal fun resolveAllowedUserMoveUci(
    uiState: TrainSingleLineUiState,
    currentOrientation: BoardOrientation,
    uciMoves: List<String>
): String? {
    if (uiState.phase != TrainSingleLinePhase.Training) {
        return null
    }

    if (uiState.expectedPly >= uciMoves.size) {
        return null
    }

    if (!isUserTurn(uiState.expectedPly, currentOrientation)) {
        return null
    }

    return uciMoves[uiState.expectedPly]
}

internal fun resolveBoardInteractionEnabled(uiState: TrainSingleLineUiState): Boolean {
    return resolveBoardInteractionEnabled(
        uiState = uiState,
        isBoardAnimating = false,
    )
}

internal fun resolveBoardInteractionEnabled(
    uiState: TrainSingleLineUiState,
    isBoardAnimating: Boolean,
): Boolean {
    if (isBoardAnimating) {
        return false
    }

    return uiState.phase == TrainSingleLinePhase.Training
}
