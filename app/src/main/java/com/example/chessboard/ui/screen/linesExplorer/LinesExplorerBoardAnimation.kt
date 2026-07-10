package com.example.chessboard.ui.screen.linesExplorer

/**
 * Screen-local animation wiring helpers for the Lines Explorer board.
 * Keep only the first-screen integration logic here for deciding when explorer moves can use simple queued animation.
 * Do not add generic board animation abstractions, Room access, or unrelated screen flow code to this file.
 * Validation date: 2026-07-10
 */

import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.boardmodel.LastMoveHighlight
import com.example.chessboard.service.ParsedLine
import com.example.chessboard.ui.boardanimation.AnimateSimpleMoveAction
import com.example.chessboard.ui.boardanimation.BoardAnimationQueueController
import com.example.chessboard.ui.boardanimation.ResetBoardSceneAction
import com.example.chessboard.ui.boardrender.BoardRenderPiece
import com.example.chessboard.ui.boardrender.BoardRenderScene
import com.example.chessboard.ui.boardrender.buildBoardRenderScene

private const val LinesExplorerSimpleMoveDurationMs = 80

internal fun resetLinesExplorerAnimatedBoard(
    boardAnimationController: BoardAnimationQueueController,
    lineController: LineController,
    selectedLine: ParsedLine?,
) {
    lineController.setOrientation(resolveLinesExplorerBoardOrientation(selectedLine))
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

internal fun buildLinesExplorerNextMoveAnimationAction(
    parsedLine: ParsedLine,
    lineController: LineController,
): AnimateSimpleMoveAction? {
    val currentPly = lineController.currentMoveIndex
    val nextMoveUci = parsedLine.uciMoves.getOrNull(currentPly) ?: return null
    val currentScene = buildBoardRenderScene(
        position = lineController.getBoardPosition(),
        orientation = lineController.getSide(),
        lastMoveHighlight = lineController.getLastMoveHighlight(),
    )

    return buildLinesExplorerSimpleMoveActionOrNull(
        scene = currentScene,
        moveUci = nextMoveUci,
        logicalPlyAfter = currentPly + 1,
        durationMs = LinesExplorerSimpleMoveDurationMs,
    )
}

internal fun buildLinesExplorerSimpleMoveActionOrNull(
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
    if (isLinesExplorerCastlingMove(movingPiece, from, to)) {
        return null
    }
    if (isLinesExplorerEnPassantLikeMove(movingPiece, from, to)) {
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

private fun isLinesExplorerCastlingMove(
    movingPiece: BoardRenderPiece,
    from: String,
    to: String,
): Boolean {
    val lowerLetter = movingPiece.letter.lowercaseChar()
    if (lowerLetter != 'k') {
        return false
    }

    return kotlin.math.abs(resolveSquareFileIndex(from) - resolveSquareFileIndex(to)) == 2
}

private fun isLinesExplorerEnPassantLikeMove(
    movingPiece: BoardRenderPiece,
    from: String,
    to: String,
): Boolean {
    val lowerLetter = movingPiece.letter.lowercaseChar()
    if (lowerLetter != 'p') {
        return false
    }

    return resolveSquareFileIndex(from) != resolveSquareFileIndex(to)
}

private fun resolveSquareFileIndex(square: String): Int {
    return square[0] - 'a'
}
