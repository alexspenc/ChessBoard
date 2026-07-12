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
        assertEquals(emptyList<BoardPlaybackAction>(), controller.state.pendingActions)
    }

    @Test
    fun completeActiveAction_appliesCaptureAndStartsNextPendingAction() {
        val controller = BoardAnimationQueueController()
        controller.submit(
            ResetBoardSceneAction(
                scene = BoardRenderScene(
                    pieces = listOf(
                        BoardRenderPiece(letter = 'P', square = "e4"),
                        BoardRenderPiece(letter = 'p', square = "d5"),
                    ),
                    orientation = BoardOrientation.WHITE,
                ),
                renderPly = 1,
            )
        )

        val captureAction = AnimateCaptureMoveAction(
            from = "e4",
            to = "d5",
            capturedSquare = "d5",
            lastMoveHighlight = LastMoveHighlight(from = "e4", to = "d5"),
            logicalPlyAfter = 2,
            durationMs = 250,
        )
        val secondAction = AnimateSimpleMoveAction(
            from = "g8",
            to = "f6",
            lastMoveHighlight = LastMoveHighlight(from = "g8", to = "f6"),
            logicalPlyAfter = 3,
            durationMs = 250,
        )

        controller.submit(captureAction)
        controller.submit(secondAction)
        controller.completeActiveAction()

        assertEquals(listOf(BoardRenderPiece(letter = 'P', square = "d5")), controller.state.currentScene?.pieces)
        assertEquals(LastMoveHighlight(from = "e4", to = "d5"), controller.state.currentScene?.lastMoveHighlight)
        assertEquals(2, controller.state.renderPly)
        assertEquals(secondAction, controller.state.activeAction)
        assertEquals(emptyList<BoardPlaybackAction>(), controller.state.pendingActions)
    }

    @Test
    fun submit_instantTransition_keepsActionActiveUntilCompletion() {
        val controller = BoardAnimationQueueController()
        val initialScene = buildScene("e2")
        val targetScene = buildScene("e4")
        controller.submit(ResetBoardSceneAction(scene = initialScene, renderPly = 0))

        val action = ApplyBoardSceneAction(
            scene = targetScene,
            logicalPlyAfter = 1,
            durationMs = 80,
        )
        controller.submit(action)

        assertEquals(action, controller.state.activeAction)
        assertEquals(initialScene, controller.state.currentScene)
        assertEquals(0, controller.state.renderPly)
        assertEquals(true, controller.state.isPlaying)

        controller.completeActiveAction()

        assertEquals(targetScene, controller.state.currentScene)
        assertEquals(1, controller.state.renderPly)
        assertNull(controller.state.activeAction)
        assertFalse(controller.state.isPlaying)
    }

    @Test
    fun completeActiveAction_preservesAnimatedInstantAnimatedOrder() {
        val controller = BoardAnimationQueueController()
        controller.submit(ResetBoardSceneAction(scene = buildScene("e2"), renderPly = 0))

        val firstAction = AnimateSimpleMoveAction(
            from = "e2",
            to = "e4",
            lastMoveHighlight = LastMoveHighlight(from = "e2", to = "e4"),
            logicalPlyAfter = 1,
            durationMs = 80,
        )
        val instantScene = BoardRenderScene(
            pieces = listOf(
                BoardRenderPiece(letter = 'P', square = "e4"),
                BoardRenderPiece(letter = 'p', square = "a7"),
            ),
            orientation = BoardOrientation.WHITE,
        )
        val instantAction = ApplyBoardSceneAction(
            scene = instantScene,
            logicalPlyAfter = 2,
            durationMs = 80,
        )
        val thirdAction = AnimateSimpleMoveAction(
            from = "a7",
            to = "a6",
            lastMoveHighlight = LastMoveHighlight(from = "a7", to = "a6"),
            logicalPlyAfter = 3,
            durationMs = 80,
        )

        controller.submit(firstAction)
        controller.submit(instantAction)
        controller.submit(thirdAction)

        controller.completeActiveAction()
        assertEquals(instantAction, controller.state.activeAction)
        assertEquals(1, controller.state.renderPly)

        controller.completeActiveAction()
        assertEquals(instantScene, controller.state.currentScene)
        assertEquals(thirdAction, controller.state.activeAction)
        assertEquals(2, controller.state.renderPly)
    }

    @Test
    fun completeActiveAction_appliesCastlingAndStartsNextPendingAction() {
        val controller = BoardAnimationQueueController()
        controller.submit(
            ResetBoardSceneAction(
                scene = BoardRenderScene(
                    pieces = listOf(
                        BoardRenderPiece(letter = 'K', square = "e1"),
                        BoardRenderPiece(letter = 'R', square = "h1"),
                        BoardRenderPiece(letter = 'p', square = "a7"),
                    ),
                    orientation = BoardOrientation.WHITE,
                ),
                renderPly = 0,
            )
        )
        val castlingAction = AnimateCastlingMoveAction(
            from = "e1",
            to = "g1",
            rookFrom = "h1",
            rookTo = "f1",
            lastMoveHighlight = LastMoveHighlight(from = "e1", to = "g1"),
            logicalPlyAfter = 1,
            durationMs = 80,
        )
        val nextAction = AnimateSimpleMoveAction(
            from = "a7",
            to = "a6",
            lastMoveHighlight = LastMoveHighlight(from = "a7", to = "a6"),
            logicalPlyAfter = 2,
            durationMs = 80,
        )
        controller.submit(castlingAction)
        controller.submit(nextAction)

        controller.completeActiveAction()

        assertEquals(
            listOf(
                BoardRenderPiece(letter = 'K', square = "g1"),
                BoardRenderPiece(letter = 'R', square = "f1"),
                BoardRenderPiece(letter = 'p', square = "a7"),
            ),
            controller.state.currentScene?.pieces,
        )
        assertEquals(nextAction, controller.state.activeAction)
        assertEquals(1, controller.state.renderPly)
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
        assertEquals(emptyList<BoardPlaybackAction>(), controller.state.pendingActions)
        assertFalse(controller.state.isPlaying)
    }

    private fun buildScene(square: String): BoardRenderScene {
        return BoardRenderScene(
            pieces = listOf(BoardRenderPiece(letter = 'P', square = square)),
            orientation = BoardOrientation.WHITE,
        )
    }
}
