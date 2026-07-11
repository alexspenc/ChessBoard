package com.example.chessboard.ui.screen.trainSingleLine

/**
 * Screen-local board-animation helpers for single-line training.
 * Keep only active-training animation planning and hard-reset sync for the TrainSingleLine screen here.
 * Do not add generic board-animation abstractions, replay-screen helpers, or unrelated screen orchestration.
 * Validation date: 2026-07-11
 */

import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.boardanimation.AnimateSimpleMoveAction
import com.example.chessboard.ui.boardanimation.BoardAnimationQueueController
import com.example.chessboard.ui.boardanimation.applyAnimatedSimpleMove
import com.example.chessboard.ui.boardanimation.replay.buildReplaySimpleMoveActionOrNull
import com.example.chessboard.ui.boardanimation.replay.resetAnimatedReplayBoard
import com.example.chessboard.ui.boardrender.BoardRenderScene

internal const val TrainSingleLineSimpleMoveDurationMs = 80

internal fun resetTrainSingleLineAnimatedBoard(
    boardAnimationController: BoardAnimationQueueController,
    lineController: LineController,
) {
    resetAnimatedReplayBoard(
        boardAnimationController = boardAnimationController,
        lineController = lineController,
    )
}

internal fun buildTrainSingleLineProgressAnimationActions(
    scene: BoardRenderScene?,
    uiState: TrainSingleLineUiState,
    lineController: LineController,
    uciMoves: List<String>,
    currentOrientation: BoardOrientation,
    hasMoveCap: Boolean,
): List<AnimateSimpleMoveAction>? {
    val currentScene = scene ?: return null
    if (!isTrainSingleLineCorrectUserMove(uiState, lineController, uciMoves, currentOrientation)) {
        return null
    }

    val expectedPly = uiState.expectedPly
    return buildTrainSingleLineAnimationActions(
        scene = currentScene,
        expectedPly = expectedPly,
        uciMoves = uciMoves,
        currentOrientation = currentOrientation,
        hasMoveCap = hasMoveCap,
    )
}

internal fun isTrainSingleLineCorrectUserMove(
    uiState: TrainSingleLineUiState,
    lineController: LineController,
    uciMoves: List<String>,
    currentOrientation: BoardOrientation,
): Boolean {
    if (uiState.phase != TrainSingleLinePhase.Training) {
        return false
    }

    val expectedPly = uiState.expectedPly
    if (expectedPly >= uciMoves.size) {
        return false
    }
    if (!isUserTurn(expectedPly, currentOrientation)) {
        return false
    }
    if (lineController.currentMoveIndex <= expectedPly) {
        return false
    }

    // TODO: Replace getMovesCopy() with a narrow LineController accessor for the
    // last applied move so this helper does not need the full move-history copy.
    val lastMoveUci = lineController.getMovesCopy()
        .getOrNull(lineController.currentMoveIndex - 1)
        ?.let(::moveToUci)
        ?: return false

    return lastMoveUci == uciMoves[expectedPly]
}

private fun buildTrainSingleLineAnimationActions(
    scene: BoardRenderScene,
    expectedPly: Int,
    uciMoves: List<String>,
    currentOrientation: BoardOrientation,
    hasMoveCap: Boolean,
): List<AnimateSimpleMoveAction>? {
    val userMoveUci = uciMoves.getOrNull(expectedPly) ?: return null
    val userMoveAction = buildReplaySimpleMoveActionOrNull(
        scene = scene,
        moveUci = userMoveUci,
        logicalPlyAfter = expectedPly + 1,
        durationMs = TrainSingleLineSimpleMoveDurationMs,
    ) ?: return null

    if (!shouldAnimateForcedReply(
            expectedPly = expectedPly,
            uciMoves = uciMoves,
            currentOrientation = currentOrientation,
            hasMoveCap = hasMoveCap,
        )
    ) {
        return listOf(userMoveAction)
    }

    val sceneAfterUserMove = applyAnimatedSimpleMove(scene, userMoveAction)
    val forcedReplyPly = expectedPly + 1
    val forcedReplyUci = uciMoves.getOrNull(forcedReplyPly) ?: return null
    val forcedReplyAction = buildReplaySimpleMoveActionOrNull(
        scene = sceneAfterUserMove,
        moveUci = forcedReplyUci,
        logicalPlyAfter = forcedReplyPly + 1,
        durationMs = TrainSingleLineSimpleMoveDurationMs,
    ) ?: return null

    return listOf(userMoveAction, forcedReplyAction)
}

private fun shouldAnimateForcedReply(
    expectedPly: Int,
    uciMoves: List<String>,
    currentOrientation: BoardOrientation,
    hasMoveCap: Boolean,
): Boolean {
    val forcedReplyPly = expectedPly + 1
    if (forcedReplyPly >= uciMoves.size) {
        return false
    }
    if (isUserTurn(forcedReplyPly, currentOrientation)) {
        return false
    }
    if (hasMoveCap && forcedReplyPly + 1 >= uciMoves.size) {
        return false
    }

    return true
}
