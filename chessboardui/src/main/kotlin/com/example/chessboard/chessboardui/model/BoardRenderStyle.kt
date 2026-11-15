package com.example.chessboard.chessboardui.model

/**
 * Defines caller-supplied colors for the board renderer.
 * Do not import application themes or add interaction state and decoration semantics here.
 * Validation date: 2026-10-13.
 */

import androidx.compose.ui.graphics.Color

/**
 * Supplies board, piece, and coordinate colors independently from an application theme.
 * Square-decoration colors are supplied separately through BoardSquareDecoration.
 *
 * @param lightSquareColor the light-square color.
 * @param darkSquareColor the dark-square color.
 * @param whitePieceColor the fill color of white pieces.
 * @param blackPieceColor the fill color of black pieces.
 * @param whitePieceOutlineColor the outline color of white pieces.
 * @param blackPieceOutlineColor the outline color of black pieces.
 * @param coordinateColor the rank/file label color, including its opacity.
 */
data class BoardRenderStyle(
    val lightSquareColor: Color,
    val darkSquareColor: Color,
    val whitePieceColor: Color,
    val blackPieceColor: Color,
    val whitePieceOutlineColor: Color,
    val blackPieceOutlineColor: Color,
    val coordinateColor: Color,
)
