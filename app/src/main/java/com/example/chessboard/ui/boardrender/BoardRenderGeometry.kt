package com.example.chessboard.ui.boardrender

/**
 * Converts application square strings to pixel centers for existing animation frame producers.
 * Keep legacy square-to-canvas translation here while animation still uses application scene types.
 * Do not add drawing code, controller state, or screen orchestration to this file.
 * Validation date: 2026-10-13.
 */

import androidx.compose.ui.geometry.Offset
import com.example.chessboard.ui.BoardOrientation

private const val CellCount = 8

fun calculateSquareCenterOffset(
    square: String,
    orientation: BoardOrientation,
    squareSizePx: Float,
): Offset {
    val (row, col) = squareToBoardCoords(square, orientation)
    return Offset(
        x = (col * squareSizePx) + (squareSizePx / 2),
        y = (row * squareSizePx) + (squareSizePx / 2),
    )
}

fun squareToBoardCoords(
    square: String,
    orientation: BoardOrientation
): Pair<Int, Int> {
    if (square.length != 2 || square[0] !in 'a'..'h' || square[1] !in '1'..'8') {
        return squareToBoardCoords("a1", orientation)
    }

    val file = square[0] - 'a'
    val rank = square[1].digitToInt()
    val row = CellCount - rank
    val col = file
    if (orientation == BoardOrientation.WHITE) {
        return row to col
    }

    return (7 - row) to (7 - col)
}
