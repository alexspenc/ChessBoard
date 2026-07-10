package com.example.chessboard.ui.boardanimation

/**
 * Focused JVM coverage for projected in-flight board scenes.
 * Keep interpolation and simple move scene-application tests here for the animation layer.
 * Do not add queue lifecycle or screen integration tests to this file.
 * Validation date: 2026-07-10
 */

import androidx.compose.ui.geometry.Offset
import com.example.chessboard.boardmodel.LastMoveHighlight
import com.example.chessboard.ui.BoardOrientation
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
}
