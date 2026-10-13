package com.example.chessboard.chessboardui.geometry

/**
 * Converts chesscore squares and board-local pixel coordinates in both viewing orientations.
 * Keep coordinate calculations here; do not add drawing, gestures, or application controllers.
 * Validation date: 2026-10-13.
 */

import androidx.compose.ui.geometry.Offset
import com.example.chessboard.chesscore.model.Square
import com.example.chessboard.chessboardui.model.BoardOrientation

private const val CellCount = 8

/**
 * Returns a square's top-left corner in pixels from the board's top left.
 * X increases rightward and Y increases downward.
 *
 * @param square the square to locate.
 * @param orientation the viewing orientation.
 * @param squareSizePx the positive finite size of one square in pixels.
 * @return the top-left offset; a1 is (0, 700) in White's view with 100-pixel squares.
 * @throws IllegalArgumentException if squareSizePx is non-positive or non-finite.
 */
fun calculateSquareTopLeftOffset(
    square: Square,
    orientation: BoardOrientation,
    squareSizePx: Float,
): Offset {
    requireSquareSize(squareSizePx)

    var column = square.file - 'a'
    var row = CellCount - square.rank
    if (orientation == BoardOrientation.BLACK) {
        column = CellCount - 1 - column
        row = CellCount - 1 - row
    }

    return Offset(column * squareSizePx, row * squareSizePx)
}

/**
 * Returns a square's center in pixels from the board's top left.
 *
 * @param square the square to locate.
 * @param orientation the viewing orientation.
 * @param squareSizePx the positive finite size of one square in pixels.
 * @return the center offset; a1 is (50, 750) in White's view with 100-pixel squares.
 * @throws IllegalArgumentException if squareSizePx is non-positive or non-finite.
 */
fun calculateSquareCenterOffset(
    square: Square,
    orientation: BoardOrientation,
    squareSizePx: Float,
): Offset {
    val topLeft = calculateSquareTopLeftOffset(square, orientation, squareSizePx)
    return Offset(topLeft.x + squareSizePx / 2f, topLeft.y + squareSizePx / 2f)
}

/**
 * Locates the square under a board-local pixel offset.
 * The top/left edges are included and the bottom/right edges are excluded.
 * An internal boundary belongs to the square on its right or below.
 *
 * @param offset the finite pixel coordinates from the board's top left.
 * @param orientation the viewing orientation.
 * @param squareSizePx the positive finite size of one square in pixels.
 * @return the square, or null for finite coordinates outside the board.
 * @throws IllegalArgumentException if squareSizePx is non-positive or non-finite,
 * or either offset coordinate is non-finite.
 */
fun getSquareFromOffset(
    offset: Offset,
    orientation: BoardOrientation,
    squareSizePx: Float,
): Square? {
    requireSquareSize(squareSizePx)
    require(offset.x.isFinite() && offset.y.isFinite()) {
        "offset must have finite coordinates: $offset"
    }

    if (offset.x < 0f || offset.y < 0f) {
        return null
    }

    // Double division avoids rounding a point just before an edge into the next cell.
    var column = (offset.x.toDouble() / squareSizePx.toDouble()).toInt()
    var row = (offset.y.toDouble() / squareSizePx.toDouble()).toInt()
    if (column !in 0 until CellCount || row !in 0 until CellCount) {
        return null
    }

    if (orientation == BoardOrientation.BLACK) {
        column = CellCount - 1 - column
        row = CellCount - 1 - row
    }

    return Square(file = 'a' + column, rank = CellCount - row)
}

private fun requireSquareSize(squareSizePx: Float) {
    require(squareSizePx.isFinite() && squareSizePx > 0f) {
        "squareSizePx must be positive and finite: $squareSizePx"
    }
}
