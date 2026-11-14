package com.example.chessboard.chessboardui.model

/**
 * Defines generic square fills and borders for board rendering.
 * Do not add training concepts, screen state, or application-theme dependencies here.
 * Validation date: 2026-10-13.
 */

import androidx.compose.ui.graphics.Color
import com.example.chessboard.chesscore.model.Square

/**
 * Describes a square's fill followed by an optional border.
 * Scenes preserve decoration order, including multiple decorations on the same square.
 *
 * @param square the decorated square; it may be empty.
 * @param fillColor the fill, or Color.Transparent for a border-only decoration.
 * @param border the border drawn after the fill, or null for a fill-only decoration.
 */
data class BoardSquareDecoration(
    val square: Square,
    val fillColor: Color,
    val border: BoardSquareBorder? = null,
)

/**
 * Defines a border's color and width relative to the size of a square.
 *
 * @param color the border color.
 * @param widthFraction a positive finite fraction of the square size;
 * for example, 0.07f gives a 7-pixel border on a 100-pixel square. There is no upper limit.
 * @throws IllegalArgumentException if widthFraction is non-positive or non-finite.
 */
data class BoardSquareBorder(
    val color: Color,
    val widthFraction: Float,
) {
    init {
        require(widthFraction.isFinite() && widthFraction > 0f) {
            "widthFraction must be positive and finite: $widthFraction"
        }
    }
}
