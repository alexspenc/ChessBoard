package com.example.chessboard.chessboardui.model

/**
 * Describes the current drawing position of an animated piece.
 * Keep frame inputs here; do not add animation timing, queues, or move application.
 * Validation date: 2026-10-13.
 */

import androidx.compose.ui.geometry.Offset
import com.example.chessboard.chesscore.model.Square

/**
 * References a scene piece and overrides its drawing position for an animation frame.
 * BoardRenderScene validates the reference and coordinate finiteness when accepting this input.
 *
 * @param fromSquare the square identifying the piece in the scene's pieces list.
 * @param centerOffset the current center in pixels relative to the board's top left;
 * it may be outside the board.
 */
data class BoardRenderAnimatedPiece(
    val fromSquare: Square,
    val centerOffset: Offset,
)
