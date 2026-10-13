package com.example.chessboard.chessboardui.geometry

/**
 * Tests square placement and pointer hit testing in both board orientations.
 * Keep numeric and boundary checks here; do not add rendering or gesture workflows.
 * Validation date: 2026-10-13.
 */

import androidx.compose.ui.geometry.Offset
import com.example.chessboard.chesscore.model.Square
import com.example.chessboard.chessboardui.model.BoardOrientation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class BoardGeometryTest {

    @Test
    fun locatesCornersAndInteriorSquareInBothOrientations() {
        data class ExpectedPosition(
            val square: Square,
            val orientation: BoardOrientation,
            val topLeft: Offset,
            val center: Offset,
        )

        val positions = listOf(
            ExpectedPosition(Square('a', 8), BoardOrientation.WHITE, Offset(0f, 0f), Offset(50f, 50f)),
            ExpectedPosition(Square('h', 8), BoardOrientation.WHITE, Offset(700f, 0f), Offset(750f, 50f)),
            ExpectedPosition(Square('a', 1), BoardOrientation.WHITE, Offset(0f, 700f), Offset(50f, 750f)),
            ExpectedPosition(Square('h', 1), BoardOrientation.WHITE, Offset(700f, 700f), Offset(750f, 750f)),
            ExpectedPosition(Square('e', 4), BoardOrientation.WHITE, Offset(400f, 400f), Offset(450f, 450f)),
            ExpectedPosition(Square('a', 8), BoardOrientation.BLACK, Offset(700f, 700f), Offset(750f, 750f)),
            ExpectedPosition(Square('h', 8), BoardOrientation.BLACK, Offset(0f, 700f), Offset(50f, 750f)),
            ExpectedPosition(Square('a', 1), BoardOrientation.BLACK, Offset(700f, 0f), Offset(750f, 50f)),
            ExpectedPosition(Square('h', 1), BoardOrientation.BLACK, Offset(0f, 0f), Offset(50f, 50f)),
            ExpectedPosition(Square('e', 4), BoardOrientation.BLACK, Offset(300f, 300f), Offset(350f, 350f)),
        )

        for (expected in positions) {
            assertEquals(
                expected.topLeft,
                calculateSquareTopLeftOffset(expected.square, expected.orientation, 100f),
            )
            assertEquals(
                expected.center,
                calculateSquareCenterOffset(expected.square, expected.orientation, 100f),
            )
        }
    }

    @Test
    fun mapsCentersBackToAll64SquaresAtMultipleScales() {
        val squares = mutableListOf<Square>()
        for (file in 'a'..'h') {
            for (rank in 1..8) {
                squares.add(Square(file, rank))
            }
        }

        for (orientation in BoardOrientation.entries) {
            for (squareSizePx in listOf(1f, 12.5f, 100f)) {
                for (square in squares) {
                    val center = calculateSquareCenterOffset(square, orientation, squareSizePx)

                    assertEquals(square, getSquareFromOffset(center, orientation, squareSizePx))
                }
            }
        }
    }

    @Test
    fun includesTopLeftAndPointsJustBeforeBottomRight() {
        assertEquals(Square('a', 8), getSquareFromOffset(Offset.Zero, BoardOrientation.WHITE, 100f))
        assertEquals(Square('h', 1), getSquareFromOffset(Offset.Zero, BoardOrientation.BLACK, 100f))
        assertEquals(
            Square('h', 1),
            getSquareFromOffset(Offset(799.99994f, 799.99994f), BoardOrientation.WHITE, 100f),
        )
        assertEquals(
            Square('a', 8),
            getSquareFromOffset(Offset(799.99994f, 799.99994f), BoardOrientation.BLACK, 100f),
        )
    }

    @Test
    fun internalEdgesBelongToCellsOnTheirRightAndBelow() {
        assertEquals(Square('b', 8), getSquareFromOffset(Offset(100f, 0f), BoardOrientation.WHITE, 100f))
        assertEquals(Square('a', 7), getSquareFromOffset(Offset(0f, 100f), BoardOrientation.WHITE, 100f))
        assertEquals(Square('b', 7), getSquareFromOffset(Offset(100f, 100f), BoardOrientation.WHITE, 100f))
        assertEquals(Square('g', 1), getSquareFromOffset(Offset(100f, 0f), BoardOrientation.BLACK, 100f))
        assertEquals(Square('h', 2), getSquareFromOffset(Offset(0f, 100f), BoardOrientation.BLACK, 100f))
        assertEquals(Square('g', 2), getSquareFromOffset(Offset(100f, 100f), BoardOrientation.BLACK, 100f))
        assertEquals(
            Square('a', 8),
            getSquareFromOffset(Offset(99.99999f, 99.99999f), BoardOrientation.WHITE, 100f),
        )
    }

    @Test
    fun excludesBottomRightEdgesAndAllFiniteOutsideCoordinates() {
        val outsideOffsets = listOf(
            Offset(800f, 0f),
            Offset(0f, 800f),
            Offset(800f, 800f),
            Offset(801f, 100f),
            Offset(100f, 801f),
            Offset(-0.1f, 100f),
            Offset(100f, -0.1f),
            Offset(-Float.MIN_VALUE, 0f),
            Offset(0f, -Float.MIN_VALUE),
            Offset(Float.MAX_VALUE, 0f),
            Offset(0f, Float.MAX_VALUE),
        )
        for (orientation in BoardOrientation.entries) {
            for (offset in outsideOffsets) {
                assertNull(getSquareFromOffset(offset, orientation, 100f))
            }
        }
    }

    @Test
    fun allFunctionsRejectNonPositiveAndNonFiniteSquareSizes() {
        val invalidSizes = listOf(
            0f,
            -0f,
            -1f,
            Float.NaN,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
        )
        for (orientation in BoardOrientation.entries) {
            for (size in invalidSizes) {
                assertThrows(IllegalArgumentException::class.java) {
                    calculateSquareTopLeftOffset(Square('e', 4), orientation, size)
                }
                assertThrows(IllegalArgumentException::class.java) {
                    calculateSquareCenterOffset(Square('e', 4), orientation, size)
                }
                assertThrows(IllegalArgumentException::class.java) {
                    getSquareFromOffset(Offset.Zero, orientation, size)
                }
            }
        }
    }

    @Test
    fun rejectsNonFiniteCoordinatesOnEitherAxis() {
        for (orientation in BoardOrientation.entries) {
            for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
                for (offset in listOf(Offset(value, 0f), Offset(0f, value))) {
                    assertThrows(IllegalArgumentException::class.java) {
                        getSquareFromOffset(offset, orientation, 100f)
                    }
                }
            }
        }
    }
}
