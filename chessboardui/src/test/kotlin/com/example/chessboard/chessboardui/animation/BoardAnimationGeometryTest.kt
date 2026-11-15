package com.example.chessboard.chessboardui.animation

/**
 * Tests piece-center interpolation, orientation, progress bounds, and input validation.
 * Compare all coordinates with a square-size-relative tolerance, including endpoints.
 * Keep numeric animation-geometry checks here; do not add rendering, timing, or application scenes.
 * Validation date: 2026-11-14.
 */

import androidx.compose.ui.geometry.Offset
import com.example.chessboard.chessboardui.TestSquareSizePx
import com.example.chessboard.chessboardui.model.BoardOrientation
import com.example.chessboard.chesscore.model.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.math.nextUp

private const val CoordinateToleranceFraction = 0.00001f

class BoardAnimationGeometryTest {

    @Test
    fun returnsEndpointCentersInBothOrientations() {
        // h1 -> a8 spans the board. A non-binary square size also exercises endpoint rounding.
        val squareSizes = listOf(TestSquareSizePx / 8, TestSquareSizePx, TestSquareSizePx * 0.013f)
        for (orientation in BoardOrientation.entries) {
            for (squareSizePx in squareSizes) {
                var fromCenter = Offset(7 * squareSizePx + squareSizePx / 2, 7 * squareSizePx + squareSizePx / 2)
                var toCenter = Offset(squareSizePx / 2, squareSizePx / 2)
                if (orientation == BoardOrientation.BLACK) {
                    fromCenter = Offset(squareSizePx / 2, squareSizePx / 2)
                    toCenter = Offset(7 * squareSizePx + squareSizePx / 2, 7 * squareSizePx + squareSizePx / 2)
                }

                assertCenterClose(
                    "Start center, view=$orientation, size=$squareSizePx",
                    fromCenter,
                    calculateAnimatedPieceCenter(Square('h', 1), Square('a', 8), 0f, orientation, squareSizePx),
                    squareSizePx,
                )
                assertCenterClose(
                    "End center, view=$orientation, size=$squareSizePx",
                    toCenter,
                    calculateAnimatedPieceCenter(Square('h', 1), Square('a', 8), 1f, orientation, squareSizePx),
                    squareSizePx,
                )
            }
        }
    }

    @Test
    fun interpolatesVerticalMotionAtQuarterMidpointAndThreeQuarters() {
        // e2 -> e4: White sees two cells upward; Black sees two cells downward.
        assertFrames(
            Square('e', 2), Square('e', 4),
            listOf(
                ExpectedFrame(0.25f, Offset(4.5f, 6f), Offset(3.5f, 2f)),
                ExpectedFrame(0.5f, Offset(4.5f, 5.5f), Offset(3.5f, 2.5f)),
                ExpectedFrame(0.75f, Offset(4.5f, 5f), Offset(3.5f, 3f)),
            ),
        )
    }

    @Test
    fun interpolatesHorizontalMotionAtQuarterMidpointAndThreeQuarters() {
        // b3 -> f3: four cells right in White's view, left in Black's view.
        assertFrames(
            Square('b', 3), Square('f', 3),
            listOf(
                ExpectedFrame(0.25f, Offset(2.5f, 5.5f), Offset(5.5f, 2.5f)),
                ExpectedFrame(0.5f, Offset(3.5f, 5.5f), Offset(4.5f, 2.5f)),
                ExpectedFrame(0.75f, Offset(4.5f, 5.5f), Offset(3.5f, 2.5f)),
            ),
        )
    }

    @Test
    fun interpolatesUnequalAxisDistancesWithoutRequiringLegalChessMove() {
        // b2 -> f5 travels four files and three ranks; no normal chess piece makes this move.
        assertFrames(
            Square('b', 2), Square('f', 5),
            listOf(
                ExpectedFrame(0.25f, Offset(2.5f, 5.75f), Offset(5.5f, 2.25f)),
                ExpectedFrame(0.5f, Offset(3.5f, 5f), Offset(4.5f, 3f)),
                ExpectedFrame(0.75f, Offset(4.5f, 4.25f), Offset(3.5f, 3.75f)),
            ),
        )
    }

    @Test
    fun keepsSameSquareStationaryForFiniteProgressInsideAndOutsideInterval() {
        val frames = mutableListOf<ExpectedFrame>()
        for (progress in listOf(-Float.MAX_VALUE, -0.25f, 0f, 0.25f, 0.5f, 0.75f, 1f, 1.25f, Float.MAX_VALUE)) {
            frames.add(ExpectedFrame(progress, Offset(4.5f, 5.5f), Offset(3.5f, 2.5f)))
        }

        assertFrames(Square('e', 3), Square('e', 3), frames)
    }

    @Test
    fun clampsFiniteProgressOutsideIntervalWithoutExtrapolating() {
        val frames = mutableListOf<ExpectedFrame>()
        for (progress in listOf(-Float.MAX_VALUE, -1f, -Float.MIN_VALUE)) {
            frames.add(ExpectedFrame(progress, Offset(7.5f, 7.5f), Offset(0.5f, 0.5f)))
        }
        for (progress in listOf(1f.nextUp(), 2f, Float.MAX_VALUE)) {
            frames.add(ExpectedFrame(progress, Offset(0.5f, 0.5f), Offset(7.5f, 7.5f)))
        }

        assertFrames(Square('h', 1), Square('a', 8), frames)
    }

    @Test
    fun rejectsNonFiniteProgressEvenForStationaryMotion() {
        for (orientation in BoardOrientation.entries) {
            for (toSquare in listOf(Square('e', 4), Square('e', 2))) {
                assertNonFiniteProgressRejected(toSquare, orientation)
            }
        }
    }

    @Test
    fun rejectsInvalidSquareSizesAtEndpointsAndForStationaryMotion() {
        for (orientation in BoardOrientation.entries) {
            for (toSquare in listOf(Square('e', 4), Square('e', 2))) {
                assertInvalidSquareSizesRejected(toSquare, orientation)
            }
        }
    }

    // Expected centers use cell units, specified explicitly for each view rather than derived by interpolation.
    private data class ExpectedFrame(val progress: Float, val whiteCenter: Offset, val blackCenter: Offset)

    private fun assertFrames(fromSquare: Square, toSquare: Square, frames: List<ExpectedFrame>) {
        for (orientation in BoardOrientation.entries) {
            for (squareSizePx in listOf(TestSquareSizePx / 8, TestSquareSizePx / 2, TestSquareSizePx)) {
                for (frame in frames) {
                    var expectedCenter = frame.whiteCenter
                    if (orientation == BoardOrientation.BLACK) {
                        expectedCenter = frame.blackCenter
                    }
                    assertCenterClose(
                        "$fromSquare -> $toSquare, progress=${frame.progress}, view=$orientation, size=$squareSizePx",
                        Offset(expectedCenter.x * squareSizePx, expectedCenter.y * squareSizePx),
                        calculateAnimatedPieceCenter(fromSquare, toSquare, frame.progress, orientation, squareSizePx),
                        squareSizePx,
                    )
                }
            }
        }
    }

    private fun assertCenterClose(context: String, expected: Offset, actual: Offset, squareSizePx: Float) {
        val tolerancePx = squareSizePx * CoordinateToleranceFraction
        assertEquals("$context, X", expected.x, actual.x, tolerancePx)
        assertEquals("$context, Y", expected.y, actual.y, tolerancePx)
    }

    private fun assertNonFiniteProgressRejected(toSquare: Square, orientation: BoardOrientation) {
        for (progress in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertThrows(IllegalArgumentException::class.java) {
                calculateAnimatedPieceCenter(Square('e', 2), toSquare, progress, orientation, TestSquareSizePx)
            }
        }
    }

    private fun assertInvalidSquareSizesRejected(toSquare: Square, orientation: BoardOrientation) {
        val invalidSizes = listOf(
            0f, -0f, -TestSquareSizePx, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
        )
        for (progress in listOf(-0.25f, 0f, 0.5f, 1f, 1.25f)) {
            for (squareSizePx in invalidSizes) {
                assertThrows(IllegalArgumentException::class.java) {
                    calculateAnimatedPieceCenter(Square('e', 2), toSquare, progress, orientation, squareSizePx)
                }
            }
        }
    }
}
