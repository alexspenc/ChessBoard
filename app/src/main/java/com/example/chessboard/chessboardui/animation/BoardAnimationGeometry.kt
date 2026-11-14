package com.example.chessboard.chessboardui.animation

/**
 * Calculates board-local piece centers for animation frames using library geometry.
 * Keep pure movement interpolation here; do not add timing, queues, scene mutation, or chess rules.
 * Validation date: 2026-11-14.
 */

import androidx.compose.ui.geometry.Offset
import com.example.chessboard.chessboardui.geometry.calculateSquareCenterOffset
import com.example.chessboard.chessboardui.model.BoardOrientation
import com.example.chessboard.chesscore.model.Square

/**
 * Calculates a piece's current center on the straight segment between two square centers.
 * Finite progress outside 0..1 is clamped to the corresponding endpoint.
 * Endpoints are returned directly, and equal squares remain stationary.
 * The caller controls progress over time; this function applies no easing or chess-legality checks.
 * Input validation also applies at endpoints and when both squares are equal.
 *
 * @param fromSquare the starting square, without requiring a piece to occupy it.
 * @param toSquare the destination square, independent from chess-move legality.
 * @param progress the finite fraction of the movement; 0 is the start and 1 is the destination.
 * @param orientation the viewing orientation used to locate both square centers.
 * @param squareSizePx the positive finite width and height of one square in pixels.
 * @return the current center in pixels from the board's top left, with X rightward and Y downward.
 * @throws IllegalArgumentException if progress is non-finite or squareSizePx is non-positive or
 * non-finite. Library geometry validation exceptions propagate unchanged.
 */
fun calculateAnimatedPieceCenter(
    fromSquare: Square,
    toSquare: Square,
    progress: Float,
    orientation: BoardOrientation,
    squareSizePx: Float,
): Offset {
    require(progress.isFinite()) {
        "progress must be finite: $progress"
    }

    val fromCenter = calculateSquareCenterOffset(fromSquare, orientation, squareSizePx)
    if (progress <= 0f || fromSquare == toSquare) {
        return fromCenter
    }

    val toCenter = calculateSquareCenterOffset(toSquare, orientation, squareSizePx)
    if (progress >= 1f) {
        return toCenter
    }

    return Offset(
        x = fromCenter.x + (toCenter.x - fromCenter.x) * progress,
        y = fromCenter.y + (toCenter.y - fromCenter.y) * progress,
    )
}
