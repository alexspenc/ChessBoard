package com.example.chessboard.ui.boardanimation

/**
 * Focused JVM coverage for projected in-flight board scenes.
 * Keep library-geometry integration and forward-move scene-application tests here.
 * Do not add queue lifecycle or screen integration tests to this file.
 * Validation date: 2026-11-14.
 */

import androidx.compose.ui.geometry.Offset
import com.example.chessboard.boardmodel.LastMoveHighlight
import com.example.chessboard.chessboardui.TestSquareSizePx
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.boardrender.BoardRenderAnimatedPiece
import com.example.chessboard.ui.boardrender.BoardRenderPiece
import com.example.chessboard.ui.boardrender.BoardRenderScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

private const val CoordinateToleranceFraction = 0.00001f

class BoardAnimationSceneProjectorTest {

    @Test
    fun buildAnimatedBoardRenderScene_setsDragStateFromSimpleMoveProgress() {
        val baseScene = BoardRenderScene(
            pieces = listOf(BoardRenderPiece(letter = 'P', square = "e2")),
            orientation = BoardOrientation.WHITE,
        )
        val action = AnimateSimpleMoveAction(
            from = "e2",
            to = "e4",
            lastMoveHighlight = LastMoveHighlight(from = "e2", to = "e4"),
            logicalPlyAfter = 1,
            durationMs = 250,
        )

        val projectedScene = buildAnimatedBoardRenderScene(
            baseScene = baseScene,
            activeAction = action,
            progress = 0.5f,
            squareSizePx = TestSquareSizePx,
        )

        assertEquals("e2", projectedScene.dragFromSquare)
        assertCenterClose(
            Offset((4 + 0.5f) * TestSquareSizePx, (5 + 0.5f) * TestSquareSizePx),
            projectedScene.dragOffset,
            TestSquareSizePx,
        )
        assertEquals(baseScene.pieces, projectedScene.pieces)
        assertEquals(LastMoveHighlight(from = "e2", to = "e4"), projectedScene.lastMoveHighlight)
    }

    @Test
    fun applyAnimatedSimpleMove_movesPieceIntoTargetSquare() {
        val baseScene = BoardRenderScene(
            pieces = listOf(BoardRenderPiece(letter = 'P', square = "e2")),
            orientation = BoardOrientation.WHITE,
            dragFromSquare = "e2",
            dragOffset = Offset((4 + 0.5f) * TestSquareSizePx, (6 + 0.5f) * TestSquareSizePx),
        )
        val action = AnimateSimpleMoveAction(
            from = "e2",
            to = "e4",
            lastMoveHighlight = LastMoveHighlight(from = "e2", to = "e4"),
            logicalPlyAfter = 1,
            durationMs = 250,
        )

        val appliedScene = applyAnimatedSimpleMove(
            scene = baseScene,
            action = action,
        )

        assertEquals(listOf(BoardRenderPiece(letter = 'P', square = "e4")), appliedScene.pieces)
        assertEquals(LastMoveHighlight(from = "e2", to = "e4"), appliedScene.lastMoveHighlight)
        assertNull(appliedScene.dragFromSquare)
        assertCenterClose(Offset.Zero, appliedScene.dragOffset, TestSquareSizePx)
    }

    @Test
    fun buildAnimatedBoardRenderScene_hidesCapturedPieceDuringCaptureProgress() {
        val baseScene = BoardRenderScene(
            pieces = listOf(
                BoardRenderPiece(letter = 'P', square = "e4"),
                BoardRenderPiece(letter = 'p', square = "d5"),
            ),
            orientation = BoardOrientation.WHITE,
        )
        val action = AnimateCaptureMoveAction(
            from = "e4",
            to = "d5",
            capturedSquare = "d5",
            lastMoveHighlight = LastMoveHighlight(from = "e4", to = "d5"),
            logicalPlyAfter = 2,
            durationMs = 250,
        )

        val projectedScene = buildAnimatedBoardRenderScene(
            baseScene = baseScene,
            activeAction = action,
            progress = 0.5f,
            squareSizePx = TestSquareSizePx,
        )

        assertEquals(listOf(BoardRenderPiece(letter = 'P', square = "e4")), projectedScene.pieces)
        assertEquals("e4", projectedScene.dragFromSquare)
        // Halfway from e4 to d5, between the centers of both cells on each axis.
        assertCenterClose(Offset(4 * TestSquareSizePx, 4 * TestSquareSizePx), projectedScene.dragOffset, TestSquareSizePx)
        assertEquals(LastMoveHighlight(from = "e4", to = "d5"), projectedScene.lastMoveHighlight)
    }

    @Test
    fun applyAnimatedCaptureMove_removesCapturedPieceAndMovesAttackerIntoTargetSquare() {
        val baseScene = BoardRenderScene(
            pieces = listOf(
                BoardRenderPiece(letter = 'P', square = "e4"),
                BoardRenderPiece(letter = 'p', square = "d5"),
            ),
            orientation = BoardOrientation.WHITE,
            dragFromSquare = "e4",
            dragOffset = Offset((4 + 0.5f) * TestSquareSizePx, (4 + 0.5f) * TestSquareSizePx),
        )
        val action = AnimateCaptureMoveAction(
            from = "e4",
            to = "d5",
            capturedSquare = "d5",
            lastMoveHighlight = LastMoveHighlight(from = "e4", to = "d5"),
            logicalPlyAfter = 2,
            durationMs = 250,
        )

        val appliedScene = applyAnimatedCaptureMove(
            scene = baseScene,
            action = action,
        )

        assertEquals(listOf(BoardRenderPiece(letter = 'P', square = "d5")), appliedScene.pieces)
        assertEquals(LastMoveHighlight(from = "e4", to = "d5"), appliedScene.lastMoveHighlight)
        assertNull(appliedScene.dragFromSquare)
        assertCenterClose(Offset.Zero, appliedScene.dragOffset, TestSquareSizePx)
    }

    @Test
    fun buildAnimatedBoardRenderScene_projectsKingAndRookAtSharedCastlingProgress() {
        val baseScene = BoardRenderScene(
            pieces = listOf(
                BoardRenderPiece(letter = 'K', square = "e1"),
                BoardRenderPiece(letter = 'R', square = "h1"),
            ),
            orientation = BoardOrientation.WHITE,
        )
        val action = buildWhiteKingSideCastlingAction()

        val projectedScene = buildAnimatedBoardRenderScene(
            baseScene = baseScene,
            activeAction = action,
            progress = 0.5f,
            squareSizePx = TestSquareSizePx,
        )

        assertEquals(listOf("e1", "h1"), projectedScene.animatedPieces.map { piece -> piece.fromSquare })
        assertCenterClose(
            Offset((5 + 0.5f) * TestSquareSizePx, (7 + 0.5f) * TestSquareSizePx),
            projectedScene.animatedPieces[0].centerOffset,
            TestSquareSizePx,
        )
        assertCenterClose(
            Offset((6 + 0.5f) * TestSquareSizePx, (7 + 0.5f) * TestSquareSizePx),
            projectedScene.animatedPieces[1].centerOffset,
            TestSquareSizePx,
        )
        assertEquals(LastMoveHighlight(from = "e1", to = "g1"), projectedScene.lastMoveHighlight)
    }

    @Test
    fun applyAnimatedCastlingMove_movesKingAndRookAndClearsAnimatedPieces() {
        val baseScene = BoardRenderScene(
            pieces = listOf(
                BoardRenderPiece(letter = 'K', square = "e1"),
                BoardRenderPiece(letter = 'R', square = "h1"),
            ),
            orientation = BoardOrientation.WHITE,
            animatedPieces = listOf(
                BoardRenderAnimatedPiece(
                    fromSquare = "e1",
                    centerOffset = Offset((6 + 0.5f) * TestSquareSizePx, (7 + 0.5f) * TestSquareSizePx),
                ),
                BoardRenderAnimatedPiece(
                    fromSquare = "h1",
                    centerOffset = Offset((5 + 0.5f) * TestSquareSizePx, (7 + 0.5f) * TestSquareSizePx),
                ),
            ),
        )

        val appliedScene = applyAnimatedCastlingMove(
            scene = baseScene,
            action = buildWhiteKingSideCastlingAction(),
        )

        assertEquals(
            listOf(
                BoardRenderPiece(letter = 'K', square = "g1"),
                BoardRenderPiece(letter = 'R', square = "f1"),
            ),
            appliedScene.pieces,
        )
        assertEquals(emptyList<BoardRenderAnimatedPiece>(), appliedScene.animatedPieces)
        assertEquals(LastMoveHighlight(from = "e1", to = "g1"), appliedScene.lastMoveHighlight)
    }

    @Test
    fun clampsMovementAtEndpointsInBothOrientationsAndAtMultipleScales() {
        data class ExpectedFrame(val progress: Float, val whiteCenter: Offset, val blackCenter: Offset)

        val frames = listOf(
            ExpectedFrame(-0.5f, Offset(4.5f, 6.5f), Offset(3.5f, 1.5f)),
            ExpectedFrame(1.5f, Offset(4.5f, 4.5f), Offset(3.5f, 3.5f)),
        )
        val action = AnimateSimpleMoveAction("e2", "e4", LastMoveHighlight("e2", "e4"), 1, 250)
        for (orientation in BoardOrientation.entries) {
            val scene = BoardRenderScene(listOf(BoardRenderPiece('P', "e2")), orientation)
            for (squareSizePx in listOf(TestSquareSizePx / 2, TestSquareSizePx)) {
                for (frame in frames) {
                    val projected = buildAnimatedBoardRenderScene(scene, action, frame.progress, squareSizePx)
                    var expectedCenter = frame.whiteCenter
                    if (orientation == BoardOrientation.BLACK) {
                        expectedCenter = frame.blackCenter
                    }
                    assertCenterClose(
                        Offset(expectedCenter.x * squareSizePx, expectedCenter.y * squareSizePx),
                        projected.dragOffset,
                        squareSizePx,
                    )
                    assertEquals(scene.pieces, projected.pieces)
                    assertEquals(orientation, projected.orientation)
                    assertEquals("e2", projected.dragFromSquare)
                    assertEquals(action.lastMoveHighlight, projected.lastMoveHighlight)
                }
            }
        }
    }

    @Test
    fun propagatesNonFiniteProgressErrorsForAllMoveTypes() {
        for ((scene, action) in projectionCases()) {
            for (progress in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
                val error = assertThrows(IllegalArgumentException::class.java) {
                    buildAnimatedBoardRenderScene(scene, action, progress, TestSquareSizePx)
                }
                assertTrue(error.message.orEmpty().contains("progress"))
            }
        }
    }

    @Test
    fun propagatesInvalidSquareSizeErrorsForAllMoveTypes() {
        val invalidSizes = listOf(
            0f, -0f, -TestSquareSizePx, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
        )
        for ((scene, action) in projectionCases()) {
            for (squareSizePx in invalidSizes) {
                val error = assertThrows(IllegalArgumentException::class.java) {
                    buildAnimatedBoardRenderScene(scene, action, 0.5f, squareSizePx)
                }
                assertTrue(error.message.orEmpty().contains("squareSizePx"))
            }
        }
    }

    @Test
    fun rejectsMalformedMovementSquaresWithoutSubstitutingA1() {
        val action = AnimateSimpleMoveAction("e2", "e4", LastMoveHighlight("e2", "e4"), 1, 250)
        val scene = BoardRenderScene(listOf(BoardRenderPiece('P', "e2")), BoardOrientation.WHITE)
        val castlingScene = BoardRenderScene(
            listOf(BoardRenderPiece('K', "e1"), BoardRenderPiece('R', "h1")), BoardOrientation.WHITE,
        )
        for (value in listOf("", "e", "E2", "i2", "e0", "e22", " e2", "e2 ")) {
            // The source piece exists so projection must reach the conversion boundary.
            val invalidSourceScene = scene.copy(pieces = listOf(BoardRenderPiece('P', value)))
            val fromError = assertThrows(IllegalArgumentException::class.java) {
                buildAnimatedBoardRenderScene(invalidSourceScene, action.copy(from = value), 0.5f, TestSquareSizePx)
            }
            assertTrue(fromError.message.orEmpty().contains("from"))
            assertTrue(fromError.message.orEmpty().contains("'$value'"))

            // Destination conversion is strict even when progress is at the starting endpoint.
            val toError = assertThrows(IllegalArgumentException::class.java) {
                buildAnimatedBoardRenderScene(scene, action.copy(to = value), 0f, TestSquareSizePx)
            }
            assertTrue(toError.message.orEmpty().contains("to"))
            assertTrue(toError.message.orEmpty().contains("'$value'"))

            val rookError = assertThrows(IllegalArgumentException::class.java) {
                buildAnimatedBoardRenderScene(
                    castlingScene, buildWhiteKingSideCastlingAction().copy(rookTo = value), 0.5f, TestSquareSizePx,
                )
            }
            assertTrue(rookError.message.orEmpty().contains("'$value'"))
        }
    }

    @Test
    fun keepsBaseSceneWhenRequiredSourcePieceIsMissingWithoutCalculatingCenters() {
        for ((scene, action) in projectionCases()) {
            val emptyScene = scene.copy(pieces = emptyList())
            assertSame(emptyScene, buildAnimatedBoardRenderScene(emptyScene, action, Float.NaN, 0f))
        }
        val kingOnlyScene = BoardRenderScene(listOf(BoardRenderPiece('K', "e1")), BoardOrientation.WHITE)
        assertSame(
            kingOnlyScene,
            buildAnimatedBoardRenderScene(kingOnlyScene, buildWhiteKingSideCastlingAction(), Float.NaN, 0f),
        )
    }

    private fun assertCenterClose(expected: Offset, actual: Offset, squareSizePx: Float) {
        val tolerancePx = squareSizePx * CoordinateToleranceFraction
        assertEquals("Center X", expected.x, actual.x, tolerancePx)
        assertEquals("Center Y", expected.y, actual.y, tolerancePx)
    }

    private fun projectionCases(): List<Pair<BoardRenderScene, AnimatedBoardMoveAction>> {
        return listOf(
            BoardRenderScene(listOf(BoardRenderPiece('P', "e2")), BoardOrientation.WHITE) to
                AnimateSimpleMoveAction("e2", "e4", LastMoveHighlight("e2", "e4"), 1, 250),
            BoardRenderScene(
                listOf(BoardRenderPiece('P', "e4"), BoardRenderPiece('p', "d5")), BoardOrientation.WHITE,
            ) to AnimateCaptureMoveAction("e4", "d5", "d5", LastMoveHighlight("e4", "d5"), 2, 250),
            BoardRenderScene(
                listOf(BoardRenderPiece('K', "e1"), BoardRenderPiece('R', "h1")), BoardOrientation.WHITE,
            ) to buildWhiteKingSideCastlingAction(),
        )
    }

    private fun buildWhiteKingSideCastlingAction(): AnimateCastlingMoveAction {
        return AnimateCastlingMoveAction(
            from = "e1",
            to = "g1",
            rookFrom = "h1",
            rookTo = "f1",
            lastMoveHighlight = LastMoveHighlight(from = "e1", to = "g1"),
            logicalPlyAfter = 1,
            durationMs = 250,
        )
    }
}
