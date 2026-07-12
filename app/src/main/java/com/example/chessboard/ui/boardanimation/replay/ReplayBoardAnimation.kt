package com.example.chessboard.ui.boardanimation.replay

/**
 * Shared replay-board playback helpers for non-interactive board flows.
 * Keep reset, forward navigation, and action planning that convert LineController state into queued playback actions here.
 * Do not add screen-specific selection logic, interactive board input handling, or unrelated UI orchestration here.
 * Validation date: 2026-07-10
 */

import com.example.chessboard.boardmodel.LastMoveHighlight
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.ui.boardanimation.AnimateCaptureMoveAction
import com.example.chessboard.ui.boardanimation.AnimateCastlingMoveAction
import com.example.chessboard.ui.boardanimation.AnimateSimpleMoveAction
import com.example.chessboard.ui.boardanimation.AnimatedBoardMoveAction
import com.example.chessboard.ui.boardanimation.ApplyBoardSceneAction
import com.example.chessboard.ui.boardanimation.BoardAnimationQueueController
import com.example.chessboard.ui.boardanimation.BoardPlaybackAction
import com.example.chessboard.ui.boardanimation.DefaultBoardMoveAnimationDurationMs
import com.example.chessboard.ui.boardanimation.ResetBoardSceneAction
import com.example.chessboard.ui.boardrender.BoardRenderPiece
import com.example.chessboard.ui.boardrender.BoardRenderScene
import com.example.chessboard.ui.boardrender.buildBoardRenderScene

internal fun resetAnimatedReplayBoard(
    boardAnimationController: BoardAnimationQueueController,
    lineController: LineController,
) {
    boardAnimationController.submit(
        ResetBoardSceneAction(
            scene = buildReplayBoardRenderScene(lineController),
            renderPly = lineController.currentMoveIndex,
        )
    )
}

internal fun moveReplayBoardForward(
    uciMoves: List<String>,
    lineController: LineController,
    boardAnimationController: BoardAnimationQueueController,
): Boolean {
    val currentPly = lineController.currentMoveIndex
    val nextMoveUci = uciMoves.getOrNull(currentPly) ?: return false
    val sourceScene = buildReplayBoardRenderScene(lineController)
    if (!lineController.redoMove()) {
        return false
    }

    val targetScene = buildReplayBoardRenderScene(lineController)
    val playbackAction = buildReplayForwardPlaybackActionOrNull(
        sourceScene = sourceScene,
        targetScene = targetScene,
        moveUci = nextMoveUci,
        logicalPlyAfter = currentPly + 1,
        durationMs = DefaultBoardMoveAnimationDurationMs,
    )
    if (playbackAction == null) {
        resetAnimatedReplayBoard(
            boardAnimationController = boardAnimationController,
            lineController = lineController,
        )
        return true
    }

    boardAnimationController.submit(playbackAction)
    return true
}

internal fun buildReplayBoardRenderScene(
    lineController: LineController,
): BoardRenderScene {
    return buildBoardRenderScene(
        position = lineController.getBoardPosition(),
        orientation = lineController.getSide(),
        lastMoveHighlight = lineController.getLastMoveHighlight(),
    )
}

internal fun buildReplayForwardPlaybackActionOrNull(
    sourceScene: BoardRenderScene,
    targetScene: BoardRenderScene,
    moveUci: String,
    logicalPlyAfter: Int,
    durationMs: Int,
): BoardPlaybackAction? {
    val animatedAction = buildReplayAnimatedMoveActionOrNull(
        scene = sourceScene,
        moveUci = moveUci,
        logicalPlyAfter = logicalPlyAfter,
        durationMs = durationMs,
    )
    if (animatedAction != null) {
        return animatedAction
    }

    val movingPiece = findReplayMovingPiece(sourceScene, moveUci) ?: return null
    if (!isReplayInstantTransition(movingPiece, sourceScene, moveUci)) {
        return null
    }

    return ApplyBoardSceneAction(
        scene = targetScene,
        logicalPlyAfter = logicalPlyAfter,
        durationMs = durationMs,
    )
}

internal fun buildReplayAnimatedMoveActionOrNull(
    scene: BoardRenderScene,
    moveUci: String,
    logicalPlyAfter: Int,
    durationMs: Int,
): AnimatedBoardMoveAction? {
    if (moveUci.length != 4) {
        return null
    }

    val from = moveUci.substring(0, 2)
    val to = moveUci.substring(2, 4)
    val movingPiece = scene.pieces.find { piece -> piece.square == from } ?: return null
    val capturedPiece = scene.pieces.find { piece -> piece.square == to }
    val castlingSquares = resolveReplayCastlingSquares(from = from, to = to)
    if (movingPiece.letter.lowercaseChar() == 'k' && castlingSquares != null) {
        return buildReplayCastlingMoveActionOrNull(
            scene = scene,
            movingPiece = movingPiece,
            from = from,
            to = to,
            castlingSquares = castlingSquares,
            logicalPlyAfter = logicalPlyAfter,
            durationMs = durationMs,
        )
    }
    if (isReplayEnPassantLikeMove(movingPiece, from, to, targetOccupied = capturedPiece != null)) {
        return null
    }
    if (capturedPiece != null) {
        return AnimateCaptureMoveAction(
            from = from,
            to = to,
            capturedSquare = to,
            lastMoveHighlight = LastMoveHighlight(from = from, to = to),
            logicalPlyAfter = logicalPlyAfter,
            durationMs = durationMs,
        )
    }

    return AnimateSimpleMoveAction(
        from = from,
        to = to,
        lastMoveHighlight = LastMoveHighlight(from = from, to = to),
        logicalPlyAfter = logicalPlyAfter,
        durationMs = durationMs,
    )
}

private fun findReplayMovingPiece(
    scene: BoardRenderScene,
    moveUci: String,
): BoardRenderPiece? {
    if (moveUci.length < 4) {
        return null
    }

    val from = moveUci.substring(0, 2)
    return scene.pieces.find { piece -> piece.square == from }
}

private fun isReplayInstantTransition(
    movingPiece: BoardRenderPiece,
    scene: BoardRenderScene,
    moveUci: String,
): Boolean {
    if (isReplayPromotionMove(movingPiece, moveUci)) {
        return true
    }
    if (moveUci.length != 4) {
        return false
    }

    val from = moveUci.substring(0, 2)
    val to = moveUci.substring(2, 4)
    val targetOccupied = scene.pieces.any { piece -> piece.square == to }
    return isReplayEnPassantLikeMove(
        movingPiece = movingPiece,
        from = from,
        to = to,
        targetOccupied = targetOccupied,
    )
}

private fun isReplayPromotionMove(
    movingPiece: BoardRenderPiece,
    moveUci: String,
): Boolean {
    if (movingPiece.letter.lowercaseChar() != 'p') {
        return false
    }
    if (moveUci.length != 5) {
        return false
    }

    return moveUci[4].lowercaseChar() in setOf('q', 'r', 'b', 'n')
}

private data class ReplayCastlingSquares(
    val rookFrom: String,
    val rookTo: String,
)

private fun buildReplayCastlingMoveActionOrNull(
    scene: BoardRenderScene,
    movingPiece: BoardRenderPiece,
    from: String,
    to: String,
    castlingSquares: ReplayCastlingSquares,
    logicalPlyAfter: Int,
    durationMs: Int,
): AnimateCastlingMoveAction? {
    if (movingPiece.letter.lowercaseChar() != 'k') {
        return null
    }

    val rook = scene.pieces.find { piece -> piece.square == castlingSquares.rookFrom }
        ?: return null
    if (rook.letter.lowercaseChar() != 'r') {
        return null
    }
    if (rook.letter.isUpperCase() != movingPiece.letter.isUpperCase()) {
        return null
    }

    return AnimateCastlingMoveAction(
        from = from,
        to = to,
        rookFrom = castlingSquares.rookFrom,
        rookTo = castlingSquares.rookTo,
        lastMoveHighlight = LastMoveHighlight(from = from, to = to),
        logicalPlyAfter = logicalPlyAfter,
        durationMs = durationMs,
    )
}

private fun resolveReplayCastlingSquares(
    from: String,
    to: String,
): ReplayCastlingSquares? {
    return when (from + to) {
        "e1g1" -> ReplayCastlingSquares(rookFrom = "h1", rookTo = "f1")
        "e1c1" -> ReplayCastlingSquares(rookFrom = "a1", rookTo = "d1")
        "e8g8" -> ReplayCastlingSquares(rookFrom = "h8", rookTo = "f8")
        "e8c8" -> ReplayCastlingSquares(rookFrom = "a8", rookTo = "d8")
        else -> null
    }
}

private fun isReplayEnPassantLikeMove(
    movingPiece: BoardRenderPiece,
    from: String,
    to: String,
    targetOccupied: Boolean,
): Boolean {
    val lowerLetter = movingPiece.letter.lowercaseChar()
    if (lowerLetter != 'p') {
        return false
    }
    if (targetOccupied) {
        return false
    }

    return resolveReplaySquareFileIndex(from) != resolveReplaySquareFileIndex(to)
}

private fun resolveReplaySquareFileIndex(square: String): Int {
    return square[0] - 'a'
}
