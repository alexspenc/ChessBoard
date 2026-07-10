package com.example.chessboard.ui.screen.training.common

/**
 * Shared board-animation wiring for training editor preview screens.
 * Keep only the replay-board integration rules here for editor/template flows.
 * Do not add generic app-wide animation abstractions, Room access, or route orchestration.
 * Validation date: 2026-07-10
 */

import com.example.chessboard.boardmodel.LastMoveHighlight
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.ui.boardanimation.AnimateSimpleMoveAction
import com.example.chessboard.ui.boardanimation.BoardAnimationQueueController
import com.example.chessboard.ui.boardanimation.ResetBoardSceneAction
import com.example.chessboard.ui.boardrender.BoardRenderPiece
import com.example.chessboard.ui.boardrender.BoardRenderScene
import com.example.chessboard.ui.boardrender.buildBoardRenderScene

private const val TrainingEditorSimpleMoveDurationMs = 80

internal fun resetTrainingEditorAnimatedBoard(
    boardAnimationController: BoardAnimationQueueController,
    lineController: LineController,
) {
    boardAnimationController.submit(
        ResetBoardSceneAction(
            scene = buildBoardRenderScene(
                position = lineController.getBoardPosition(),
                orientation = lineController.getSide(),
                lastMoveHighlight = lineController.getLastMoveHighlight(),
            ),
            renderPly = lineController.currentMoveIndex,
        )
    )
}

internal fun buildTrainingEditorNextMoveAnimationAction(
    parsedLine: ParsedTrainingEditorLine,
    lineController: LineController,
): AnimateSimpleMoveAction? {
    val currentPly = lineController.currentMoveIndex
    val nextMoveUci = parsedLine.uciMoves.getOrNull(currentPly) ?: return null
    val currentScene = buildBoardRenderScene(
        position = lineController.getBoardPosition(),
        orientation = lineController.getSide(),
        lastMoveHighlight = lineController.getLastMoveHighlight(),
    )

    return buildTrainingEditorSimpleMoveActionOrNull(
        scene = currentScene,
        moveUci = nextMoveUci,
        logicalPlyAfter = currentPly + 1,
        durationMs = TrainingEditorSimpleMoveDurationMs,
    )
}

internal fun buildTrainingEditorSimpleMoveActionOrNull(
    scene: BoardRenderScene,
    moveUci: String,
    logicalPlyAfter: Int,
    durationMs: Int,
): AnimateSimpleMoveAction? {
    if (moveUci.length != 4) {
        return null
    }

    val from = moveUci.substring(0, 2)
    val to = moveUci.substring(2, 4)
    val movingPiece = scene.pieces.find { piece -> piece.square == from } ?: return null
    if (scene.pieces.any { piece -> piece.square == to }) {
        return null
    }
    if (isTrainingEditorCastlingMove(movingPiece, from, to)) {
        return null
    }
    if (isTrainingEditorEnPassantLikeMove(movingPiece, from, to)) {
        return null
    }

    return AnimateSimpleMoveAction(
        from = from,
        to = to,
        lastMoveHighlight = LastMoveHighlight(from = from, to = to),
        logicalPlyAfter = logicalPlyAfter,
        durationMs = durationMs,
    )
}

private fun isTrainingEditorCastlingMove(
    movingPiece: BoardRenderPiece,
    from: String,
    to: String,
): Boolean {
    val lowerLetter = movingPiece.letter.lowercaseChar()
    if (lowerLetter != 'k') {
        return false
    }

    return kotlin.math.abs(resolveTrainingEditorSquareFileIndex(from) - resolveTrainingEditorSquareFileIndex(to)) == 2
}

private fun isTrainingEditorEnPassantLikeMove(
    movingPiece: BoardRenderPiece,
    from: String,
    to: String,
): Boolean {
    val lowerLetter = movingPiece.letter.lowercaseChar()
    if (lowerLetter != 'p') {
        return false
    }

    return resolveTrainingEditorSquareFileIndex(from) != resolveTrainingEditorSquareFileIndex(to)
}

private fun resolveTrainingEditorSquareFileIndex(square: String): Int {
    return square[0] - 'a'
}
