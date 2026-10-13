package com.example.chessboard.chessboardui.render

/**
 * Provides the public board-drawing entry point and fixes the order of the library's rendering layers.
 * Keep layer assembly here; do not add Canvas ownership, gestures, playback, or application adapters.
 * Validation date: 2026-10-13.
 */

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.chessboard.chessboardui.model.BoardRenderScene
import com.example.chessboard.chessboardui.model.BoardRenderStyle

/**
 * Draws a complete board: square background, coordinate labels, ordered decorations, then scene pieces.
 * Decorations may cover labels; pieces are drawn above decorations, with dragging above animations.
 * Coordinates are in pixels from the supplied drawing context's current origin.
 * The board spans eight square sizes on each axis; the caller owns the Canvas, layout, and clipping.
 * An empty placement still draws the board, its labels, and any decorations.
 *
 * @param drawScope the drawing context receiving all board layers.
 * @param scene the validated piece placement, orientation, decorations, and current moving centers.
 * @param squareSizePx the positive finite width and height of one square in pixels.
 * @param style the caller-supplied background, coordinate, and piece colors.
 * @throws IllegalArgumentException if squareSizePx is non-positive or non-finite, before drawing.
 * Component validation exceptions propagate unchanged.
 */
fun drawBoardScene(
    drawScope: DrawScope,
    scene: BoardRenderScene,
    squareSizePx: Float,
    style: BoardRenderStyle,
) {
    drawBoardSquares(drawScope, squareSizePx, style.lightSquareColor, style.darkSquareColor)
    drawCoordinates(drawScope, scene.orientation, squareSizePx, style.coordinateColor)
    drawSquareDecorations(drawScope, scene.decorations, scene.orientation, squareSizePx)
    drawScenePieces(drawScope, scene, squareSizePx, style)
}
