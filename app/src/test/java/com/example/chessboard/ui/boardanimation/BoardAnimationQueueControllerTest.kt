package com.example.chessboard.ui.boardanimation

/**
 * Focused JVM coverage for the board animation queue state machine.
 * Keep FIFO, reset, and render-scene progression tests here for the isolated animation layer.
 * Do not add screen integration or Compose UI interaction tests to this file.
 * Validation date: 2026-07-10
 */

import com.example.chessboard.boardmodel.LastMoveHighlight
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.boardrender.BoardRenderPiece
import com.example.chessboard.ui.boardrender.BoardRenderScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class BoardAnimationQueueControllerTest {

    @Test
    fun submit_twoMoves_startsFirstAndQueuesSecond() {
        val controller = BoardAnimationQueueController()
        controller.submit(ResetBoardSceneAction(scene = buildScene("e2"), renderPly = 0))

        val firstAction = AnimateSimpleMoveAction(
            from = "e2",
            to = "e4",
            lastMoveHighlight = LastMoveHighlight(from = "e2", to = "e4"),
            logicalPlyAfter = 1,
            durationMs = 250,
        )
        val secondAction = AnimateSimpleMoveAction(
            from = "e4",
            to = "e5",
            lastMoveHighlight = LastMoveHighlight(from = "e4", to = "e5"),
            logicalPlyAfter = 2,
            durationMs = 250,
        )

        controller.submit(firstAction)
        controller.submit(secondAction)

        assertEquals(firstAction, controller.state.activeAction)
        assertEquals(listOf(secondAction), controller.state.pendingActions)
    }

    @Test
    fun completeActiveAction_appliesMoveAndStartsNextPendingAction() {
        val controller = BoardAnimationQueueController()
        controller.submit(ResetBoardSceneAction(scene = buildScene("e2"), renderPly = 0))

        val firstAction = AnimateSimpleMoveAction(
            from = "e2",
            to = "e4",
            lastMoveHighlight = LastMoveHighlight(from = "e2", to = "e4"),
            logicalPlyAfter = 1,
            durationMs = 250,
        )
        val secondAction = AnimateSimpleMoveAction(
            from = "e4",
            to = "e5",
            lastMoveHighlight = LastMoveHighlight(from = "e4", to = "e5"),
            logicalPlyAfter = 2,
            durationMs = 250,
        )

        controller.submit(firstAction)
        controller.submit(secondAction)
        controller.completeActiveAction()

        assertEquals(listOf(BoardRenderPiece(letter = 'P', square = "e4")), controller.state.currentScene?.pieces)
        assertEquals(LastMoveHighlight(from = "e2", to = "e4"), controller.state.currentScene?.lastMoveHighlight)
        assertEquals(1, controller.state.renderPly)
        assertEquals(secondAction, controller.state.activeAction)
        assertEquals(emptyList<AnimateSimpleMoveAction>(), controller.state.pendingActions)
    }

    @Test
    fun resetSceneAction_clearsQueueAndActiveAnimation() {
        val controller = BoardAnimationQueueController()
        controller.submit(ResetBoardSceneAction(scene = buildScene("e2"), renderPly = 0))
        controller.submit(
            AnimateSimpleMoveAction(
                from = "e2",
                to = "e4",
                lastMoveHighlight = LastMoveHighlight(from = "e2", to = "e4"),
                logicalPlyAfter = 1,
                durationMs = 250,
            )
        )
        controller.submit(
            AnimateSimpleMoveAction(
                from = "e4",
                to = "e5",
                lastMoveHighlight = LastMoveHighlight(from = "e4", to = "e5"),
                logicalPlyAfter = 2,
                durationMs = 250,
            )
        )

        val resetScene = buildScene("a1")
        controller.submit(ResetBoardSceneAction(scene = resetScene, renderPly = 12))

        assertEquals(resetScene, controller.state.currentScene)
        assertEquals(12, controller.state.renderPly)
        assertNull(controller.state.activeAction)
        assertEquals(emptyList<AnimateSimpleMoveAction>(), controller.state.pendingActions)
        assertFalse(controller.state.isAnimating)
    }

    private fun buildScene(square: String): BoardRenderScene {
        return BoardRenderScene(
            pieces = listOf(BoardRenderPiece(letter = 'P', square = square)),
            orientation = BoardOrientation.WHITE,
        )
    }
}
