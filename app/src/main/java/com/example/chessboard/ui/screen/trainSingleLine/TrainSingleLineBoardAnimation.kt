package com.example.chessboard.ui.screen.trainSingleLine

/**
 * Screen-local board-playback helpers for single-line training.
 * Keep only active-training playback planning and hard-reset sync for the TrainSingleLine screen here.
 * Do not add generic board-animation abstractions, replay-screen helpers, or unrelated screen orchestration.
 * Validation date: 2026-07-11
 */

import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.boardmodel.buildUciFromChesslibMove
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.boardanimation.BoardAnimationQueueController
import com.example.chessboard.ui.boardanimation.BoardPlaybackAction
import com.example.chessboard.ui.boardanimation.DefaultBoardMoveAnimationDurationMs
import com.example.chessboard.ui.boardanimation.replay.buildReplayForwardPlaybackActionOrNull
import com.example.chessboard.ui.boardanimation.replay.resetAnimatedReplayBoard
import com.example.chessboard.ui.boardrender.BoardRenderScene

internal fun resetTrainSingleLineAnimatedBoard(
    boardAnimationController: BoardAnimationQueueController,
    lineController: LineController,
) {
    resetAnimatedReplayBoard(
        boardAnimationController = boardAnimationController,
        lineController = lineController,
    )
}

internal fun buildTrainSingleLineProgressPlaybackActions(
    sceneBeforeUserMove: BoardRenderScene?,
    sceneAfterUserMove: BoardRenderScene,
    sceneAfterProgress: BoardRenderScene,
    uiState: TrainSingleLineUiState,
    uciMoves: List<String>,
    currentOrientation: BoardOrientation,
    hasMoveCap: Boolean,
): List<BoardPlaybackAction>? {
    val currentScene = sceneBeforeUserMove ?: return null
    val expectedPly = uiState.expectedPly
    return buildTrainSingleLinePlaybackActions(
        sceneBeforeUserMove = currentScene,
        sceneAfterUserMove = sceneAfterUserMove,
        sceneAfterProgress = sceneAfterProgress,
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
    val moves = lineController.getMovesCopy()
    val lastMoveIndex = lineController.currentMoveIndex - 1
    if (lastMoveIndex !in moves.indices) {
        return false
    }

    val lastMove = moves[lastMoveIndex]
    val lastMoveUci = buildUciFromChesslibMove(lastMove)

    return lastMoveUci == uciMoves[expectedPly]
}

private fun buildTrainSingleLinePlaybackActions(
    sceneBeforeUserMove: BoardRenderScene,
    sceneAfterUserMove: BoardRenderScene,
    sceneAfterProgress: BoardRenderScene,
    expectedPly: Int,
    uciMoves: List<String>,
    currentOrientation: BoardOrientation,
    hasMoveCap: Boolean,
): List<BoardPlaybackAction>? {
    val userMoveUci = uciMoves.getOrNull(expectedPly) ?: return null
    val userMoveAction = buildReplayForwardPlaybackActionOrNull(
        sourceScene = sceneBeforeUserMove,
        targetScene = sceneAfterUserMove,
        moveUci = userMoveUci,
        logicalPlyAfter = expectedPly + 1,
        durationMs = DefaultBoardMoveAnimationDurationMs,
    ) ?: return null

    if (!shouldQueueForcedReply(
            expectedPly = expectedPly,
            uciMoves = uciMoves,
            currentOrientation = currentOrientation,
            hasMoveCap = hasMoveCap,
        )
    ) {
        return listOf(userMoveAction)
    }

    val forcedReplyPly = expectedPly + 1
    val forcedReplyUci = uciMoves.getOrNull(forcedReplyPly) ?: return null
    val forcedReplyAction = buildReplayForwardPlaybackActionOrNull(
        sourceScene = sceneAfterUserMove,
        targetScene = sceneAfterProgress,
        moveUci = forcedReplyUci,
        logicalPlyAfter = forcedReplyPly + 1,
        durationMs = DefaultBoardMoveAnimationDurationMs,
    ) ?: return null

    return listOf(userMoveAction, forcedReplyAction)
}

private fun shouldQueueForcedReply(
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
