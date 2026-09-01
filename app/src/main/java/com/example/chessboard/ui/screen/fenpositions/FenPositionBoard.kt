package com.example.chessboard.ui.screen.fenpositions

/*
 * File role: defines shared board setup rules for screens in the FEN positions feature.
 * Allowed here:
 * - converting stored four-field FEN to a board-loadable FEN
 * - resolving board orientation from the stored side to move
 * Not allowed here:
 * - Compose rendering, persistence access, or screen workflow state
 * Validation date: 2026-09-01
 */

import com.example.chessboard.ui.BoardOrientation

internal fun resolveFenPositionBoardOrientation(fen: String): BoardOrientation {
    val sideToMove = fen.trim().split(Regex("\\s+")).getOrNull(1)
    if (sideToMove == "b") {
        return BoardOrientation.BLACK
    }

    return BoardOrientation.WHITE
}

internal fun toLoadableFenPosition(fen: String): String {
    return "${fen.trim()} 0 1"
}
