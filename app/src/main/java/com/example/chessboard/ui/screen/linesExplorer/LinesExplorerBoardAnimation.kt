package com.example.chessboard.ui.screen.linesExplorer

/**
 * Screen-local animation wiring helpers for the Lines Explorer board.
 * Keep only the first-screen integration logic here for deciding when explorer moves can use simple queued animation.
 * Do not add generic board animation abstractions, Room access, or unrelated screen flow code to this file.
 * Validation date: 2026-07-10
 */

import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.service.ParsedLine
import com.example.chessboard.ui.boardanimation.AnimateSimpleMoveAction
import com.example.chessboard.ui.boardanimation.BoardAnimationQueueController
import com.example.chessboard.ui.boardanimation.replay.buildReplayNextMoveAnimationAction
import com.example.chessboard.ui.boardanimation.replay.resetAnimatedReplayBoard

private const val LinesExplorerSimpleMoveDurationMs = 80

internal fun resetLinesExplorerAnimatedBoard(
    boardAnimationController: BoardAnimationQueueController,
    lineController: LineController,
    selectedLine: ParsedLine?,
) {
    lineController.setOrientation(resolveLinesExplorerBoardOrientation(selectedLine))
    resetAnimatedReplayBoard(
        boardAnimationController = boardAnimationController,
        lineController = lineController,
    )
}

internal fun buildLinesExplorerNextMoveAnimationAction(
    parsedLine: ParsedLine,
    lineController: LineController,
): AnimateSimpleMoveAction? {
    return buildReplayNextMoveAnimationAction(
        uciMoves = parsedLine.uciMoves,
        lineController = lineController,
        durationMs = LinesExplorerSimpleMoveDurationMs,
    )
}
