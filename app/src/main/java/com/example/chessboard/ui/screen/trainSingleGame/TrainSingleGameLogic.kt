package com.example.chessboard.ui.screen.trainSingleGame

// Pure session-transition logic for single-game training.
// This file is separate so move progression, completion handling, and mistake handling
// can evolve independently from Compose rendering.

import android.util.Log
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.ui.BoardOrientation
import kotlinx.coroutines.delay

internal fun resetSessionState(uiState: TrainSingleGameUiState): TrainSingleGameUiState =
    uiState.copy(
        phase = TrainSingleGamePhase.Idle,
        expectedPly = 0,
        completionDialog = null,
        wrongMoveSquare = null,
        showLineCompleted = false,
    )

internal fun buildShowLineState(uiState: TrainSingleGameUiState): TrainSingleGameUiState =
    uiState.copy(
        completionDialog = null,
        expectedPly = 0,
        phase = TrainSingleGamePhase.ShowingLine,
        wrongMoveSquare = null,
        showLineCompleted = false,
    )

internal fun buildStartTrainingState(uiState: TrainSingleGameUiState): TrainSingleGameUiState =
    uiState.copy(
        completionDialog = null,
        expectedPly = 0,
        phase = TrainSingleGamePhase.Training,
        wrongMoveSquare = null,
        showLineCompleted = false,
    )

internal fun buildRepeatVariationState(uiState: TrainSingleGameUiState): TrainSingleGameUiState =
    uiState.copy(
        completionDialog = null,
        expectedPly = 0,
        phase = TrainSingleGamePhase.Training,
        wrongMoveSquare = null,
        showLineCompleted = false,
    )

// Replays the full variation from the start with a fixed delay between moves.
internal suspend fun runShowLine(
    uiState: TrainSingleGameUiState,
    gameController: GameController,
    uciMoves: List<String>,
    moveDelayMs: Long,
    startFen: String? = null,
): TrainSingleGameUiState {
    if (uiState.phase != TrainSingleGamePhase.ShowingLine) {
        Log.d(
            TrainSingleGameLogTag,
            "runShowLine skipped because phase=${uiState.phase} boardState=${gameController.boardState}"
        )
        return uiState
    }

    Log.d(
        TrainSingleGameLogTag,
        "runShowLine started. boardState=${gameController.boardState} movesCount=${uciMoves.size}"
    )
    gameController.loadFromUciMoves(uciMoves, 0, startFen)
    Log.d(
        TrainSingleGameLogTag,
        "runShowLine loaded start position. boardState=${gameController.boardState}"
    )

    for (ply in 1..uciMoves.size) {
        Log.d(
            TrainSingleGameLogTag,
            "runShowLine step. ply=$ply move=${uciMoves[ply - 1]} boardStateBefore=${gameController.boardState}"
        )
        delay(moveDelayMs)
        gameController.redoMove()
        Log.d(
            TrainSingleGameLogTag,
            "runShowLine step applied. ply=$ply boardStateAfter=${gameController.boardState}"
        )
    }

    Log.d(
        TrainSingleGameLogTag,
        "runShowLine finished. boardState=${gameController.boardState}"
    )
    return uiState.copy(phase = TrainSingleGamePhase.Idle, showLineCompleted = true)
}

// Advances the session by applying forced program moves or validating the latest user move.
internal fun handleTrainingProgress(
    uiState: TrainSingleGameUiState,
    gameController: GameController,
    uciMoves: List<String>,
    currentOrientation: BoardOrientation,
    sidesCount: Int,
    startFen: String? = null,
    hasMoveCap: Boolean = false,
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
        // When a move cap is set, skip the last computer reply so training ends
        // after the player's final move, not after the opponent's response.
        if (hasMoveCap && uiState.expectedPly + 1 >= uciMoves.size) {
            return uiState.copy(
                phase = TrainSingleGamePhase.Idle,
                completionDialog = buildCompletionDialog(
                    currentSideIndex = uiState.currentSideIndex,
                    sidesCount = sidesCount,
                    currentOrientation = currentOrientation
                )
            )
        }
        gameController.loadFromUciMoves(uciMoves, uiState.expectedPly + 1, startFen)
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
        return advanceProgramMoves(
            uiState = uiState.copy(expectedPly = uiState.expectedPly + 1, wrongMoveSquare = null),
            gameController = gameController,
            uciMoves = uciMoves,
            currentOrientation = currentOrientation,
            sidesCount = sidesCount,
            startFen = startFen,
            hasMoveCap = hasMoveCap,
        )
    }

    val wrongSquare = lastMoveUci.substring(2, 4)
    gameController.loadFromUciMoves(uciMoves, uiState.expectedPly, startFen)
    return uiState.copy(
        mistakesCount = uiState.mistakesCount + 1,
        wrongMoveSquare = wrongSquare,
        phase = TrainSingleGamePhase.Training,
    )
}

internal fun handleCorrectMove(
    uiState: TrainSingleGameUiState,
    gameController: GameController,
    uciMoves: List<String>,
    currentOrientation: BoardOrientation,
    sidesCount: Int,
    startFen: String? = null,
    hasMoveCap: Boolean = false,
): TrainSingleGameUiState {
    if (uiState.expectedPly >= uciMoves.size) {
        return uiState.copy(phase = TrainSingleGamePhase.Idle, showLineCompleted = true)
    }

    gameController.loadFromUciMoves(uciMoves, uiState.expectedPly + 1, startFen)
    return advanceProgramMoves(
        uiState = uiState.copy(
            expectedPly = uiState.expectedPly + 1,
            phase = TrainSingleGamePhase.Training
        ),
        gameController = gameController,
        uciMoves = uciMoves,
        currentOrientation = currentOrientation,
        sidesCount = sidesCount,
        startFen = startFen,
        hasMoveCap = hasMoveCap,
    )
}

// Continues applying forced replies until control returns to the user or the line ends.
internal fun advanceProgramMoves(
    uiState: TrainSingleGameUiState,
    gameController: GameController,
    uciMoves: List<String>,
    currentOrientation: BoardOrientation,
    sidesCount: Int,
    startFen: String? = null,
    hasMoveCap: Boolean = false,
): TrainSingleGameUiState {
    if (uiState.phase != TrainSingleGamePhase.Training) {
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
                phase = TrainSingleGamePhase.Idle,
                completionDialog = buildCompletionDialog(
                    currentSideIndex = nextState.currentSideIndex,
                    sidesCount = sidesCount,
                    currentOrientation = currentOrientation
                )
            )
        }
        gameController.loadFromUciMoves(uciMoves, nextState.expectedPly + 1, startFen)
        nextState = nextState.copy(expectedPly = nextState.expectedPly + 1)
    }

    if (nextState.expectedPly < uciMoves.size) {
        return nextState
    }

    return nextState.copy(
        phase = TrainSingleGamePhase.Idle,
        completionDialog = buildCompletionDialog(
            currentSideIndex = nextState.currentSideIndex,
            sidesCount = sidesCount,
            currentOrientation = currentOrientation
        )
    )
}

// Moves to the next side when needed, otherwise completes the training session.
internal fun handleCompletionFinish(
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

internal fun resolveAllowedUserMoveUci(
    uiState: TrainSingleGameUiState,
    currentOrientation: BoardOrientation,
    uciMoves: List<String>
): String? {
    if (uiState.phase != TrainSingleGamePhase.Training) {
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

internal fun resolveBoardInteractionEnabled(uiState: TrainSingleGameUiState): Boolean {
    return uiState.phase == TrainSingleGamePhase.Training
}
