package com.example.chessboard.ui.boardanimation

/**
 * Focused JVM coverage for projected in-flight board scenes.
 * Keep interpolation and forward-move scene-application tests here for the animation layer.
 * Do not add queue lifecycle or screen integration tests to this file.
 * Validation date: 2026-07-10
 */

import androidx.compose.ui.geometry.Offset
import com.example.chessboard.boardmodel.LastMoveHighlight
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.boardrender.BoardRenderAnimatedPiece
import com.example.chessboard.ui.boardrender.BoardRenderPiece
import com.example.chessboard.ui.boardrender.BoardRenderScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
            squareSizePx = 100f,
        )

        assertEquals("e2", projectedScene.dragFromSquare)
        assertEquals(Offset(450f, 550f), projectedScene.dragOffset)
        assertEquals(baseScene.pieces, projectedScene.pieces)
        assertEquals(LastMoveHighlight(from = "e2", to = "e4"), projectedScene.lastMoveHighlight)
    }

    @Test
    fun applyAnimatedSimpleMove_movesPieceIntoTargetSquare() {
        val baseScene = BoardRenderScene(
            pieces = listOf(BoardRenderPiece(letter = 'P', square = "e2")),
            orientation = BoardOrientation.WHITE,
            dragFromSquare = "e2",
            dragOffset = Offset(450f, 650f),
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
        assertEquals(Offset.Zero, appliedScene.dragOffset)
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
            squareSizePx = 100f,
        )

        assertEquals(listOf(BoardRenderPiece(letter = 'P', square = "e4")), projectedScene.pieces)
        assertEquals("e4", projectedScene.dragFromSquare)
        assertEquals(Offset(400f, 400f), projectedScene.dragOffset)
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
            dragOffset = Offset(450f, 450f),
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
        assertEquals(Offset.Zero, appliedScene.dragOffset)
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
            squareSizePx = 100f,
        )

        assertEquals(
            listOf(
                BoardRenderAnimatedPiece(fromSquare = "e1", centerOffset = Offset(550f, 750f)),
                BoardRenderAnimatedPiece(fromSquare = "h1", centerOffset = Offset(650f, 750f)),
            ),
            projectedScene.animatedPieces,
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
                BoardRenderAnimatedPiece(fromSquare = "e1", centerOffset = Offset(650f, 750f)),
                BoardRenderAnimatedPiece(fromSquare = "h1", centerOffset = Offset(550f, 750f)),
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
