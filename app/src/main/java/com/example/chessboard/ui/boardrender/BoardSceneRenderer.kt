package com.example.chessboard.ui.boardrender

/**
 * Shared chess-board scene renderer.
 * Keep common board drawing, highlights, and piece placement here for both static and animated boards.
 * Do not add gesture handling, screen-specific behavior, or chess-rule state changes to this file.
 * Validation date: 2026-07-10
 */

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.drawPieceGlyph
import com.example.chessboard.ui.theme.ChessDark
import com.example.chessboard.ui.theme.ChessLight

private const val CellCount = 8

@Composable
fun BoardSceneRenderer(
    scene: BoardRenderScene,
    squareSizePx: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        drawBoardSquares(squareSizePx)
        drawCoordinates(scene.orientation, squareSizePx)
        drawHighlights(scene, squareSizePx)
        drawPieces(scene, squareSizePx)
    }
}

private fun DrawScope.drawBoardSquares(squareSizePx: Float) {
    for (row in 0 until CellCount) {
        for (col in 0 until CellCount) {
            drawRect(
                color = getColor(row, col),
                topLeft = Offset(col * squareSizePx, row * squareSizePx),
                size = Size(squareSizePx, squareSizePx)
            )
        }
    }
}

private fun DrawScope.drawCoordinates(
    orientation: BoardOrientation,
    squareSizePx: Float,
) {
    val ranks: IntProgression
    val files: List<String>
    if (orientation == BoardOrientation.WHITE) {
        ranks = CellCount downTo 1
        files = listOf("a", "b", "c", "d", "e", "f", "g", "h")
    } else {
        ranks = 1..CellCount
        files = listOf("h", "g", "f", "e", "d", "c", "b", "a")
    }

    for (rank in ranks) {
        val yPos = resolveRankLabelYPosition(
            orientation = orientation,
            rank = rank,
            squareSizePx = squareSizePx,
        )
        drawContext.canvas.nativeCanvas.drawText(
            rank.toString(),
            squareSizePx * 0.1f,
            yPos,
            Paint().apply {
                textAlign = Paint.Align.LEFT
                textSize = squareSizePx * 0.25f
                isAntiAlias = true
                alpha = 100
            }
        )
    }

    for ((index, file) in files.withIndex()) {
        drawContext.canvas.nativeCanvas.drawText(
            file,
            (index * squareSizePx) + squareSizePx * 0.8f,
            (CellCount * squareSizePx) - squareSizePx * 0.2f,
            Paint().apply {
                textAlign = Paint.Align.CENTER
                textSize = squareSizePx * 0.25f
                isAntiAlias = true
                alpha = 100
            }
        )
    }
}

private fun resolveRankLabelYPosition(
    orientation: BoardOrientation,
    rank: Int,
    squareSizePx: Float,
): Float {
    if (orientation == BoardOrientation.WHITE) {
        return ((CellCount - rank) * squareSizePx) + squareSizePx * 0.3f
    }

    return ((rank - 1) * squareSizePx) + squareSizePx * 0.3f
}

private fun DrawScope.drawHighlights(
    scene: BoardRenderScene,
    squareSizePx: Float,
) {
    val lastMoveFromColor = Color(0xFFFFD600).copy(alpha = 0.55f)
    val lastMoveToColor = Color(0xFFFFFF80).copy(alpha = 0.50f)
    val highlightColor = Color.Yellow.copy(alpha = 0.4f)
    val wrongMoveColor = Color.Red.copy(alpha = 0.45f)

    drawHighlight(scene.lastMoveHighlight?.from, scene.orientation, squareSizePx, lastMoveFromColor)
    drawHighlight(scene.lastMoveHighlight?.to, scene.orientation, squareSizePx, lastMoveToColor)
    drawHighlight(scene.selectedSquare, scene.orientation, squareSizePx, highlightColor)
    drawHighlight(scene.dragFromSquare, scene.orientation, squareSizePx, highlightColor)
    drawHighlight(scene.wrongMoveSquare, scene.orientation, squareSizePx, wrongMoveColor)
    drawHintHighlight(scene.hintSquare, scene.orientation, squareSizePx)
}

private fun DrawScope.drawPieces(
    scene: BoardRenderScene,
    squareSizePx: Float,
) {
    val animatedFromSquares = scene.animatedPieces
        .map { animatedPiece -> animatedPiece.fromSquare }
        .toSet()
    scene.pieces.forEach { piece ->
        if (piece.square == scene.dragFromSquare || piece.square in animatedFromSquares) {
            return@forEach
        }

        drawFigure(piece.letter, piece.square, squareSizePx, scene.orientation)
    }

    scene.animatedPieces.forEach { animatedPiece ->
        val piece = scene.pieces.find { piece -> piece.square == animatedPiece.fromSquare }
            ?: return@forEach
        drawFigureAtCenter(
            letter = piece.letter,
            centerOffset = animatedPiece.centerOffset,
            squareSize = squareSizePx,
        )
    }

    val dragFromSquare = scene.dragFromSquare ?: return
    val draggedPiece = scene.pieces.find { piece -> piece.square == dragFromSquare } ?: return
    drawFigureAtCenter(
        letter = draggedPiece.letter,
        centerOffset = scene.dragOffset,
        squareSize = squareSizePx,
    )
}

private fun getColor(row: Int, col: Int): Color {
    val isLight = (row + col) % 2 == 0
    return if (isLight) ChessLight else ChessDark
}

private fun DrawScope.drawHighlight(
    square: String?,
    orientation: BoardOrientation,
    squareSizePx: Float,
    color: Color
) {
    square ?: return
    val (row, col) = squareToBoardCoords(square, orientation)
    drawRect(
        color = color,
        topLeft = Offset(col * squareSizePx, row * squareSizePx),
        size = Size(squareSizePx, squareSizePx)
    )
}

private fun DrawScope.drawHintHighlight(
    square: String?,
    orientation: BoardOrientation,
    squareSizePx: Float,
) {
    square ?: return
    val (row, col) = squareToBoardCoords(square, orientation)
    val hintColor = Color(0xFF1DB584)
    drawRect(
        color = hintColor.copy(alpha = 0.22f),
        topLeft = Offset(col * squareSizePx, row * squareSizePx),
        size = Size(squareSizePx, squareSizePx)
    )
    drawRect(
        color = hintColor.copy(alpha = 0.85f),
        topLeft = Offset(col * squareSizePx, row * squareSizePx),
        size = Size(squareSizePx, squareSizePx),
        style = Stroke(width = squareSizePx * 0.07f)
    )
}

private fun DrawScope.drawFigure(
    letter: Char,
    square: String,
    squareSize: Float,
    orientation: BoardOrientation
) {
    val (row, col) = fieldToBoardCoords(square)
    val displayRow = if (orientation == BoardOrientation.WHITE) row else 7 - row
    val displayCol = if (orientation == BoardOrientation.WHITE) col else 7 - col

    drawPieceAt(
        letter = letter,
        left = displayCol * squareSize,
        top = displayRow * squareSize,
        squareSize = squareSize,
    )
}

private fun DrawScope.drawFigureAtCenter(
    letter: Char,
    centerOffset: Offset,
    squareSize: Float
) {
    drawPieceAt(
        letter = letter,
        left = centerOffset.x - squareSize / 2,
        top = centerOffset.y - squareSize / 2,
        squareSize = squareSize,
    )
}

private fun DrawScope.drawPieceAt(
    letter: Char,
    left: Float,
    top: Float,
    squareSize: Float
) {
    drawPieceGlyph(letter, left, top, squareSize)
}
