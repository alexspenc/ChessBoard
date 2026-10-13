package com.example.chessboard.ui.boardrender

/**
 * Hosts chessboardui drawing in the application's shared Compose Canvas.
 * Keep the application render style, scene adaptation, and board layout here.
 * Do not add drawing primitives, gesture handling, animation calculation, or chess-rule mutations.
 * Validation date: 2026-10-13.
 */

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.chessboard.chessboardui.model.BoardRenderStyle
import com.example.chessboard.chessboardui.render.drawBoardScene
import com.example.chessboard.ui.theme.ChessDark
import com.example.chessboard.ui.theme.ChessLight
import com.example.chessboard.ui.theme.ChessPieceDark

private val AppBoardRenderStyle = BoardRenderStyle(
    lightSquareColor = ChessLight,
    darkSquareColor = ChessDark,
    whitePieceColor = Color.White,
    blackPieceColor = ChessPieceDark,
    whitePieceOutlineColor = Color.Black,
    blackPieceOutlineColor = Color.White,
    coordinateColor = Color.Black.copy(alpha = 100f / 255f),
)

/**
 * Draws an application scene through its adapter and the chessboardui renderer in a square Canvas.
 * Keeps the application's board palette and coordinate opacity for static and moving pieces.
 * The caller supplies the pixel square size and a modifier sizing the Canvas to eight such squares.
 * Scene conversion runs for each draw so current inputs are validated without caching mutable lists.
 *
 * @param scene the application placement, orientation, highlights, and current moving centers.
 * @param squareSizePx the positive finite width and height of one square in pixels.
 * @param modifier the Canvas layout and presentation modifiers, followed by a square aspect ratio.
 * @throws IllegalArgumentException if the scene fails adaptation or squareSizePx is invalid.
 * Adapter and library validation exceptions propagate unchanged during drawing.
 */
@Composable
fun BoardSceneRenderer(
    scene: BoardRenderScene,
    squareSizePx: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        val libraryScene = adaptBoardRenderScene(scene)
        drawBoardScene(
            drawScope = this,
            scene = libraryScene,
            squareSizePx = squareSizePx,
            style = AppBoardRenderStyle,
        )
    }
}
