package com.example.chessboard.chessboardui.render

/**
 * Places scene pieces and draws stationary, animated, and dragged layers in their agreed order.
 * Do not add backgrounds, decorations, animation execution, gestures, or application controllers.
 * Validation date: 2026-10-13.
 */

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.chessboard.chesscore.model.Piece
import com.example.chessboard.chesscore.model.Square
import com.example.chessboard.chessboardui.geometry.calculateSquareCenterOffset
import com.example.chessboard.chessboardui.model.BoardRenderScene
import com.example.chessboard.chessboardui.model.BoardRenderStyle

/**
 * Draws stationary pieces in placement order, animated pieces in frame order, then the dragged piece.
 * Moving pieces are excluded from the stationary pass so no source-square copies remain.
 * Stationary centers are calculated from squares and orientation; moving centers are used as supplied.
 * BoardRenderScene already validates moving references and center coordinates.
 *
 * @param drawScope the drawing context receiving the piece layers.
 * @param scene the validated placement and current animation/drag drawing inputs.
 * @param squareSizePx the positive finite square size in pixels, including for an empty scene.
 * @param style the caller-supplied piece colors passed to drawPiece.
 * @throws IllegalArgumentException if squareSizePx is non-positive or non-finite, before drawing.
 */
internal fun drawScenePieces(
    drawScope: DrawScope,
    scene: BoardRenderScene,
    squareSizePx: Float,
    style: BoardRenderStyle,
) {
    require(squareSizePx.isFinite() && squareSizePx > 0f) {
        "squareSizePx must be positive and finite: $squareSizePx"
    }

    val animatedFromSquares = mutableSetOf<Square>()
    for (animatedPiece in scene.animatedPieces) {
        animatedFromSquares.add(animatedPiece.fromSquare)
    }

    val piecesBySquare = mutableMapOf<Square, Piece>()
    for (placedPiece in scene.pieces) {
        piecesBySquare[placedPiece.square] = placedPiece.piece
        if (placedPiece.square == scene.dragFromSquare || placedPiece.square in animatedFromSquares) {
            continue
        }

        val center = calculateSquareCenterOffset(placedPiece.square, scene.orientation, squareSizePx)
        drawPiece(
            drawScope = drawScope,
            piece = placedPiece.piece,
            centerOffset = center,
            squareSizePx = squareSizePx,
            style = style,
        )
    }

    for (animatedPiece in scene.animatedPieces) {
        val piece = piecesBySquare.getValue(animatedPiece.fromSquare)
        drawPiece(
            drawScope = drawScope,
            piece = piece,
            centerOffset = animatedPiece.centerOffset,
            squareSizePx = squareSizePx,
            style = style,
        )
    }

    val dragFromSquare = scene.dragFromSquare
    if (dragFromSquare == null) {
        return
    }

    val draggedPiece = piecesBySquare.getValue(dragFromSquare)
    drawPiece(
        drawScope = drawScope,
        piece = draggedPiece,
        centerOffset = scene.dragOffset,
        squareSizePx = squareSizePx,
        style = style,
    )
}
