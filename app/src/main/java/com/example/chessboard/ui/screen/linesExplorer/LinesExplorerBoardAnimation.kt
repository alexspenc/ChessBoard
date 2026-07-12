package com.example.chessboard.ui.screen.linesExplorer

/**
 * Screen-local playback wiring helpers for the Lines Explorer board.
 * Keep only explorer-specific reset and forward-playback adapters here.
 * Do not add generic board animation abstractions, Room access, or unrelated screen flow code to this file.
 * Validation date: 2026-07-10
 */

import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.service.ParsedLine
import com.example.chessboard.ui.boardanimation.BoardAnimationQueueController
import com.example.chessboard.ui.boardanimation.replay.moveReplayBoardForward
import com.example.chessboard.ui.boardanimation.replay.resetAnimatedReplayBoard

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

internal fun moveLinesExplorerBoardForward(
    parsedLine: ParsedLine,
    lineController: LineController,
    boardAnimationController: BoardAnimationQueueController,
): Boolean {
    return moveReplayBoardForward(
        uciMoves = parsedLine.uciMoves,
        lineController = lineController,
        boardAnimationController = boardAnimationController,
    )
}
