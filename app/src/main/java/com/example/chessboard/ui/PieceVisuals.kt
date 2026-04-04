package com.example.chessboard.ui

import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.chessboard.ui.theme.ChessPieceDark

internal fun resolvePieceGlyph(letter: Char): String? = when (letter) {
    'K' -> "\u265A"
    'Q' -> "\u265B"
    'R' -> "\u265C"
    'B' -> "\u265D"
    'N' -> "\u265E"
    'P' -> "\u265F"
    'k' -> "\u265A"
    'q' -> "\u265B"
    'r' -> "\u265C"
    'b' -> "\u265D"
    'n' -> "\u265E"
    'p' -> "\u265F"
    else -> null
}

internal fun resolvePieceTint(letter: Char): Color =
    if (letter.isUpperCase()) Color.White else ChessPieceDark

internal fun resolvePieceOutline(letter: Char): Color =
    if (letter.isUpperCase()) Color.Black else Color.White

internal fun DrawScope.drawPieceGlyph(
    letter: Char,
    left: Float,
    top: Float,
    squareSize: Float
) {
    val glyph = resolvePieceGlyph(letter) ?: return
    val centerX = left + squareSize / 2
    val baselineY = top + squareSize * 0.76f
    val glyphTextSize = squareSize * 0.82f
    val nativeCanvas = drawContext.canvas.nativeCanvas

    listOf(
        Offset(-1f, 0f),
        Offset(1f, 0f),
        Offset(0f, -1f),
        Offset(0f, 1f)
    ).forEach { offset ->
        nativeCanvas.drawText(
            glyph,
            centerX + offset.x,
            baselineY + offset.y,
            Paint().apply {
                color = resolvePieceOutline(letter).toArgb()
                textAlign = Paint.Align.CENTER
                textSize = glyphTextSize
                isAntiAlias = true
            }
        )
    }

    nativeCanvas.drawText(
        glyph,
        centerX,
        baselineY,
        Paint().apply {
            color = resolvePieceTint(letter).toArgb()
            textAlign = Paint.Align.CENTER
            textSize = glyphTextSize
            isAntiAlias = true
        }
    )
}
