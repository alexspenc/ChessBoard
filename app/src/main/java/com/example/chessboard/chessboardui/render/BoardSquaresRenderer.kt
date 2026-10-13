package com.example.chessboard.chessboardui.render

/**
 * Draws the board's alternating square background using caller-supplied colors.
 * Keep square drawing here; do not add pieces, labels, decorations, or interaction state.
 * Validation date: 2026-10-13.
 */

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

private const val CellCount = 8

/**
 * Draws an 8-by-8 board from the current drawing origin, with a light top-left square.
 * The board occupies eight square sizes along each axis, independently of the DrawScope size.
 * Both viewing orientations have the same background pattern: a8 and h1 are light.
 *
 * @param squareSizePx the positive finite width and height of one square in pixels.
 * @param lightSquareColor the color of light squares.
 * @param darkSquareColor the color of dark squares.
 * @throws IllegalArgumentException if squareSizePx is non-positive or non-finite.
 */
internal fun DrawScope.drawBoardSquares(
    squareSizePx: Float,
    lightSquareColor: Color,
    darkSquareColor: Color,
) {
    require(squareSizePx.isFinite() && squareSizePx > 0f) {
        "squareSizePx must be positive and finite: $squareSizePx"
    }

    val squareSize = Size(squareSizePx, squareSizePx)
    for (row in 0 until CellCount) {
        for (column in 0 until CellCount) {
            var color = lightSquareColor
            if ((row + column) % 2 != 0) {
                color = darkSquareColor
            }

            drawRect(
                color = color,
                topLeft = Offset(column * squareSizePx, row * squareSizePx),
                size = squareSize,
            )
        }
    }
}
