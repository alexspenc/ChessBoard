package com.example.chessboard.chessboardui.render

/**
 * Draws rank and file labels using viewing orientation and caller-supplied text color.
 * Keep coordinate drawing here; do not add board squares, pieces, decorations, or app themes.
 * Validation date: 2026-10-13.
 */

import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.chessboard.chessboardui.model.BoardOrientation

private const val CellCount = 8
private const val TextSizeFraction = 0.25f
private const val RankLeftFraction = 0.1f
private const val RankBaselineFraction = 0.3f
private const val FileCenterFraction = 0.8f
private const val FileBottomInsetFraction = 0.2f

/**
 * Draws ranks along the left edge and files along the bottom edge, inside the board squares.
 * White's view uses ranks 8 through 1 and files a through h; Black's view reverses both sequences.
 * Text size and positions scale with the square size, preserving the existing application layout.
 *
 * @param drawScope the drawing context receiving the labels.
 * @param orientation the viewing orientation determining the label sequences.
 * @param squareSizePx the positive finite square size in pixels from the current drawing origin.
 * @param coordinateColor the text color, including its complete opacity; no extra alpha is applied.
 * @throws IllegalArgumentException if squareSizePx is non-positive or non-finite, before drawing.
 */
internal fun drawCoordinates(
    drawScope: DrawScope,
    orientation: BoardOrientation,
    squareSizePx: Float,
    coordinateColor: Color,
) {
    require(squareSizePx.isFinite() && squareSizePx > 0f) {
        "squareSizePx must be positive and finite: $squareSizePx"
    }

    val rankPaint = Paint()
    rankPaint.color = coordinateColor.toArgb()
    rankPaint.textAlign = Paint.Align.LEFT
    rankPaint.textSize = squareSizePx * TextSizeFraction
    rankPaint.isAntiAlias = true
    val filePaint = Paint(rankPaint)
    filePaint.textAlign = Paint.Align.CENTER
    val canvas = drawScope.drawContext.canvas.nativeCanvas

    for (row in 0 until CellCount) {
        var rank = CellCount - row
        if (orientation == BoardOrientation.BLACK) {
            rank = row + 1
        }
        canvas.drawText(
            rank.toString(),
            squareSizePx * RankLeftFraction,
            row * squareSizePx + squareSizePx * RankBaselineFraction,
            rankPaint,
        )
    }

    for (column in 0 until CellCount) {
        var file = 'a' + column
        if (orientation == BoardOrientation.BLACK) {
            file = 'h' - column
        }
        canvas.drawText(
            file.toString(),
            column * squareSizePx + squareSizePx * FileCenterFraction,
            CellCount * squareSizePx - squareSizePx * FileBottomInsetFraction,
            filePaint,
        )
    }
}
