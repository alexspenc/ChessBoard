package com.example.chessboard.chessboardui.render

/**
 * Draws one chesscore piece at a supplied pixel center using caller-owned render colors.
 * Keep glyph selection and drawing here; do not add scene placement, animations, or app themes.
 * Validation date: 2026-10-13.
 */

import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.chessboard.chesscore.model.Piece
import com.example.chessboard.chesscore.model.PieceType
import com.example.chessboard.chesscore.model.Side
import com.example.chessboard.chessboardui.model.BoardRenderStyle

private const val GlyphSizeFraction = 0.82f
private const val GlyphBaselineFraction = 0.76f
private const val KingScaleX = 1.25f
private const val QueenScaleX = 1.15f
private const val OutlineOffsetPx = 1f

/**
 * Draws a piece using the application's existing glyph proportions and four-pass outline.
 * The center locates the square-sized placement box, rather than the glyph's ink bounds.
 * Both sides use the same glyph shapes; their fill and outline colors come from the style.
 * Outline offsets stay one pixel at every square size, preserving existing drawing behavior.
 *
 * @param drawScope the drawing context receiving the glyph.
 * @param piece the chesscore piece whose type selects the glyph and side selects the colors.
 * @param centerOffset the finite placement center in pixels from the current drawing origin;
 * it may lie outside the board or drawing target.
 * @param squareSizePx the positive finite size of the placement box in pixels.
 * @param style the caller-supplied colors; only the four piece colors are used.
 * @throws IllegalArgumentException if squareSizePx is non-positive or non-finite,
 * or either center coordinate is non-finite. Validation happens before drawing.
 */
internal fun drawPiece(
    drawScope: DrawScope,
    piece: Piece,
    centerOffset: Offset,
    squareSizePx: Float,
    style: BoardRenderStyle,
) {
    require(squareSizePx.isFinite() && squareSizePx > 0f) {
        "squareSizePx must be positive and finite: $squareSizePx"
    }
    require(centerOffset.x.isFinite() && centerOffset.y.isFinite()) {
        "centerOffset must have finite coordinates: $centerOffset"
    }

    var fillColor = style.whitePieceColor
    var outlineColor = style.whitePieceOutlineColor
    if (piece.side == Side.BLACK) {
        fillColor = style.blackPieceColor
        outlineColor = style.blackPieceOutlineColor
    }

    var scaleX = 1f
    if (piece.type == PieceType.KING) {
        scaleX = KingScaleX
    }
    if (piece.type == PieceType.QUEEN) {
        scaleX = QueenScaleX
    }

    val glyph = resolvePieceGlyph(piece.type)
    val top = centerOffset.y - squareSizePx / 2
    val baselineY = top + squareSizePx * GlyphBaselineFraction
    val fillPaint = Paint()
    fillPaint.color = fillColor.toArgb()
    fillPaint.textAlign = Paint.Align.CENTER
    fillPaint.textSize = squareSizePx * GlyphSizeFraction
    fillPaint.textScaleX = scaleX
    fillPaint.isAntiAlias = true
    val outlinePaint = Paint(fillPaint)
    outlinePaint.color = outlineColor.toArgb()

    val canvas = drawScope.drawContext.canvas.nativeCanvas
    val outlineOffsets = listOf(
        Offset(-OutlineOffsetPx, 0f),
        Offset(OutlineOffsetPx, 0f),
        Offset(0f, -OutlineOffsetPx),
        Offset(0f, OutlineOffsetPx),
    )
    for (offset in outlineOffsets) {
        canvas.drawText(glyph, centerOffset.x + offset.x, baselineY + offset.y, outlinePaint)
    }
    canvas.drawText(glyph, centerOffset.x, baselineY, fillPaint)
}

/**
 * Returns the shared Unicode glyph shape for a piece type, independent of its side.
 *
 * @param pieceType the chesscore type to render.
 * @return the corresponding glyph, for example U+265A for a king.
 */
internal fun resolvePieceGlyph(pieceType: PieceType): String {
    return when (pieceType) {
        PieceType.KING -> "\u265A"
        PieceType.QUEEN -> "\u265B"
        PieceType.ROOK -> "\u265C"
        PieceType.BISHOP -> "\u265D"
        PieceType.KNIGHT -> "\u265E"
        PieceType.PAWN -> "\u265F"
    }
}
