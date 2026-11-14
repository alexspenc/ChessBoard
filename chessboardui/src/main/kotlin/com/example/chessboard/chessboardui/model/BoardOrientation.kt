package com.example.chessboard.chessboardui.model

/**
 * Defines board viewing orientation independently from the side to move.
 * Do not add position state, screen policies, or application UI dependencies here.
 * Validation date: 2026-10-13.
 */

/** Defines which side of the board is shown at the bottom of the rendered surface. */
enum class BoardOrientation {
    /** White's view: a8 is at the top left and h1 is at the bottom right. */
    WHITE,

    /** Black's view: h1 is at the top left and a8 is at the bottom right. */
    BLACK,
}
