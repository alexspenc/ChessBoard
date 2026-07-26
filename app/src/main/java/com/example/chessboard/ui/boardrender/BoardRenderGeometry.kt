package com.example.chessboard.ui.boardrender

/**
 * Shared board-render geometry helpers.
 * Keep square-to-canvas coordinate translation here so renderers and animation projectors stay aligned.
 * Do not add drawing code, controller state, or screen orchestration to this file.
 * Validation date: 2026-07-10
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

fun fieldToBoardCoords(square: String): Pair<Int, Int> {
    val col = square[0] - 'a'
    val row = CellCount - (square[1] - '0')
    return row to col
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
