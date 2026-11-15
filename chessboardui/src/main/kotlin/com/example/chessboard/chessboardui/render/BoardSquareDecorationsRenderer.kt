package com.example.chessboard.chessboardui.render

/**
 * Draws ordered square fills and borders using library decorations and board geometry.
 * Do not add selection, hints, move validation, pieces, or application-theme dependencies here.
 * Validation date: 2026-10-13.
 */

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.chessboard.chessboardui.geometry.calculateSquareTopLeftOffset
import com.example.chessboard.chessboardui.model.BoardOrientation
import com.example.chessboard.chessboardui.model.BoardSquareDecoration

/**
 * Draws decorations in their list order, painting each fill before its optional border.
 * Coordinates use pixels from the current drawing origin and the supplied viewing orientation.
 * Borders are centered on square edges, with half their width outside the square,
 * preserving the application's existing stroke placement. They are not clipped to the square.
 * An empty list does not change the drawing target.
 *
 * @param drawScope the drawing context receiving the fills and borders.
 * @param decorations the ordered decorations; repeated and empty squares are allowed.
 * @param orientation the viewing orientation used to locate each decorated square.
 * @param squareSizePx the positive finite width and height of one square in pixels.
 * @throws IllegalArgumentException if squareSizePx is non-positive or non-finite,
 * including when decorations is empty.
 */
internal fun drawSquareDecorations(
    drawScope: DrawScope,
    decorations: List<BoardSquareDecoration>,
    orientation: BoardOrientation,
    squareSizePx: Float,
) {
    require(squareSizePx.isFinite() && squareSizePx > 0f) {
        "squareSizePx must be positive and finite: $squareSizePx"
    }

    val squareSize = Size(squareSizePx, squareSizePx)
    for (decoration in decorations) {
        val topLeft = calculateSquareTopLeftOffset(decoration.square, orientation, squareSizePx)
        drawScope.drawRect(color = decoration.fillColor, topLeft = topLeft, size = squareSize)

        val border = decoration.border ?: continue
        drawScope.drawRect(
            color = border.color,
            topLeft = topLeft,
            size = squareSize,
            style = Stroke(width = squareSizePx * border.widthFraction),
        )
    }
}
