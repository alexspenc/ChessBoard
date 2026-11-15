package com.example.chessboard.chessboardui.model

/**
 * Tests generic border validation and fill-only/border-only decoration inputs.
 * Do not add drawing, training semantics, or application-theme tests here.
 * Validation date: 2026-10-13.
 */

import androidx.compose.ui.graphics.Color
import com.example.chessboard.chesscore.model.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class BoardSquareDecorationTest {

    @Test
    fun acceptsPositiveFiniteBorderWidthsWithoutUpperLimit() {
        for (fraction in listOf(Float.MIN_VALUE, 0.07f, 1.5f, Float.MAX_VALUE)) {
            val border = BoardSquareBorder(Color.Green, fraction)

            assertEquals(fraction, border.widthFraction, 0f)
        }
    }

    @Test
    fun rejectsNonPositiveAndNonFiniteBorderWidths() {
        val invalidFractions = listOf(
            0f,
            -0f,
            -0.07f,
            Float.NaN,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
        )
        for (fraction in invalidFractions) {
            assertThrows(IllegalArgumentException::class.java) {
                BoardSquareBorder(Color.Green, fraction)
            }
        }
    }

    @Test
    fun supportsFillOnlyAndBorderOnlyDecorations() {
        val square = Square('e', 4)
        val fillOnly = BoardSquareDecoration(square, Color.Red)
        val border = BoardSquareBorder(Color.Green, 0.07f)
        val borderOnly = BoardSquareDecoration(square, Color.Transparent, border)

        assertNull(fillOnly.border)
        assertEquals(Color.Red, fillOnly.fillColor)
        assertEquals(Color.Transparent, borderOnly.fillColor)
        assertEquals(border, borderOnly.border)
    }
}
