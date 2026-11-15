package com.example.chessboard.chessboardui.geometry

/**
 * Tests square placement and pointer hit testing in both board orientations.
 * Keep numeric and boundary checks here; do not add rendering or gesture workflows.
 * Validation date: 2026-10-13.
 */

import androidx.compose.ui.geometry.Offset
import com.example.chessboard.chesscore.model.Square
import com.example.chessboard.chessboardui.TestBoardSizePx
import com.example.chessboard.chessboardui.TestSquareSizePx
import com.example.chessboard.chessboardui.model.BoardOrientation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.math.nextDown

class BoardGeometryTest {

    @Test
    fun locatesCornersAndInteriorSquareInBothOrientations() {
        data class ExpectedPosition(
            val square: Square,
            val orientation: BoardOrientation,
            val column: Int,
            val row: Int,
        )

        val positions = listOf(
            ExpectedPosition(Square('a', 8), BoardOrientation.WHITE, column = 0, row = 0),
            ExpectedPosition(Square('h', 8), BoardOrientation.WHITE, column = 7, row = 0),
            ExpectedPosition(Square('a', 1), BoardOrientation.WHITE, column = 0, row = 7),
            ExpectedPosition(Square('h', 1), BoardOrientation.WHITE, column = 7, row = 7),
            ExpectedPosition(Square('e', 4), BoardOrientation.WHITE, column = 4, row = 4),
            ExpectedPosition(Square('a', 8), BoardOrientation.BLACK, column = 7, row = 7),
            ExpectedPosition(Square('h', 8), BoardOrientation.BLACK, column = 0, row = 7),
            ExpectedPosition(Square('a', 1), BoardOrientation.BLACK, column = 7, row = 0),
            ExpectedPosition(Square('h', 1), BoardOrientation.BLACK, column = 0, row = 0),
            ExpectedPosition(Square('e', 4), BoardOrientation.BLACK, column = 3, row = 3),
        )

        for (expected in positions) {
            assertEquals(
                Offset(expected.column * TestSquareSizePx, expected.row * TestSquareSizePx),
                calculateSquareTopLeftOffset(expected.square, expected.orientation, TestSquareSizePx),
            )
            assertEquals(
                Offset(
                    (expected.column + 0.5f) * TestSquareSizePx,
                    (expected.row + 0.5f) * TestSquareSizePx,
                ),
                calculateSquareCenterOffset(expected.square, expected.orientation, TestSquareSizePx),
            )
        }
    }

    /**
     * Converts each of the 64 squares to its pixel center, then locates the square at that center.
     * The result must be the original square in both orientations and at each tested square size.
     * This checks that placement and hit testing agree across the whole board and different scales.
     */
    @Test
    fun mapsCentersBackToAll64SquaresAtMultipleScales() {
        val squares = mutableListOf<Square>()
        for (file in 'a'..'h') {
            for (rank in 1..8) {
                squares.add(Square(file, rank))
            }
        }

        for (orientation in BoardOrientation.entries) {
            for (squareSizePx in listOf(TestSquareSizePx * 0.01f, TestSquareSizePx / 8, TestSquareSizePx)) {
                for (square in squares) {
                    val center = calculateSquareCenterOffset(square, orientation, squareSizePx)

                    assertEquals(square, getSquareFromOffset(center, orientation, squareSizePx))
                }
            }
        }
    }

    @Test
    fun includesTopLeftAndPointsJustBeforeBottomRight() {
        val justBeforeBoardEdge = TestBoardSizePx.nextDown()
        assertEquals(Square('a', 8), getSquareFromOffset(Offset.Zero, BoardOrientation.WHITE, TestSquareSizePx))
        assertEquals(Square('h', 1), getSquareFromOffset(Offset.Zero, BoardOrientation.BLACK, TestSquareSizePx))
        assertEquals(
            Square('h', 1),
            getSquareFromOffset(
                Offset(justBeforeBoardEdge, justBeforeBoardEdge),
                BoardOrientation.WHITE,
                TestSquareSizePx,
            ),
        )
        assertEquals(
            Square('a', 8),
            getSquareFromOffset(
                Offset(justBeforeBoardEdge, justBeforeBoardEdge),
                BoardOrientation.BLACK,
                TestSquareSizePx,
            ),
        )
    }

    @Test
    fun internalEdgesBelongToCellsOnTheirRightAndBelow() {
        val rightEdge = Offset(TestSquareSizePx, 0f)
        val bottomEdge = Offset(0f, TestSquareSizePx)
        val bottomRightCorner = Offset(TestSquareSizePx, TestSquareSizePx)
        val justBeforeSquareEdge = TestSquareSizePx.nextDown()
        assertEquals(Square('b', 8), getSquareFromOffset(rightEdge, BoardOrientation.WHITE, TestSquareSizePx))
        assertEquals(Square('a', 7), getSquareFromOffset(bottomEdge, BoardOrientation.WHITE, TestSquareSizePx))
        assertEquals(Square('b', 7), getSquareFromOffset(bottomRightCorner, BoardOrientation.WHITE, TestSquareSizePx))
        assertEquals(Square('g', 1), getSquareFromOffset(rightEdge, BoardOrientation.BLACK, TestSquareSizePx))
        assertEquals(Square('h', 2), getSquareFromOffset(bottomEdge, BoardOrientation.BLACK, TestSquareSizePx))
        assertEquals(Square('g', 2), getSquareFromOffset(bottomRightCorner, BoardOrientation.BLACK, TestSquareSizePx))
        assertEquals(
            Square('a', 8),
            getSquareFromOffset(
                Offset(justBeforeSquareEdge, justBeforeSquareEdge),
                BoardOrientation.WHITE,
                TestSquareSizePx,
            ),
        )
    }

    @Test
    fun excludesBottomRightEdgesAndAllFiniteOutsideCoordinates() {
        val outsideOffsets = listOf(
            Offset(TestBoardSizePx, 0f),
            Offset(0f, TestBoardSizePx),
            Offset(TestBoardSizePx, TestBoardSizePx),
            Offset(TestBoardSizePx + TestSquareSizePx * 0.01f, TestSquareSizePx),
            Offset(TestSquareSizePx, TestBoardSizePx + TestSquareSizePx * 0.01f),
            Offset(-TestSquareSizePx * 0.001f, TestSquareSizePx),
            Offset(TestSquareSizePx, -TestSquareSizePx * 0.001f),
            Offset(-Float.MIN_VALUE, 0f),
            Offset(0f, -Float.MIN_VALUE),
            Offset(Float.MAX_VALUE, 0f),
            Offset(0f, Float.MAX_VALUE),
        )
        for (orientation in BoardOrientation.entries) {
            for (offset in outsideOffsets) {
                assertNull(getSquareFromOffset(offset, orientation, TestSquareSizePx))
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
                        getSquareFromOffset(offset, orientation, TestSquareSizePx)
                    }
                }
            }
        }
    }
}
