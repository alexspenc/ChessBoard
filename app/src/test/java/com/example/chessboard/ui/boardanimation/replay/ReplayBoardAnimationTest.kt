package com.example.chessboard.ui.boardanimation.replay

/**
 * Focused JVM coverage for shared replay-board animation helpers.
 * Keep only classifier/reset checks for the common non-interactive replay animation layer here.
 * Do not add Compose UI tests, screen-specific policies, or queue-engine behavior to this file.
 * Validation date: 2026-07-10
 */

import com.example.chessboard.boardmodel.LastMoveHighlight
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.boardanimation.AnimateCaptureMoveAction
import com.example.chessboard.ui.boardanimation.AnimateSimpleMoveAction
import com.example.chessboard.ui.boardanimation.DefaultBoardMoveAnimationDurationMs
import com.example.chessboard.ui.boardrender.BoardRenderPiece
import com.example.chessboard.ui.boardrender.BoardRenderScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReplayBoardAnimationTest {

    @Test
    fun buildReplayForwardMoveActionOrNull_returnsActionForQuietMove() {
        val action = buildReplayForwardMoveActionOrNull(
            scene = buildScene(
                pieces = listOf(
                    BoardRenderPiece(letter = 'P', square = "e2"),
                ),
            ),
            moveUci = "e2e4",
            logicalPlyAfter = 1,
            durationMs = DefaultBoardMoveAnimationDurationMs,
        )

        assertEquals(
            AnimateSimpleMoveAction(
                from = "e2",
                to = "e4",
                lastMoveHighlight = LastMoveHighlight(from = "e2", to = "e4"),
                logicalPlyAfter = 1,
                durationMs = DefaultBoardMoveAnimationDurationMs,
            ),
            action,
        )
    }

    @Test
    fun buildReplayForwardMoveActionOrNull_returnsCaptureActionForOccupiedTarget() {
        val action = buildReplayForwardMoveActionOrNull(
            scene = buildScene(
                pieces = listOf(
                    BoardRenderPiece(letter = 'P', square = "e4"),
                    BoardRenderPiece(letter = 'p', square = "d5"),
                ),
            ),
            moveUci = "e4d5",
            logicalPlyAfter = 2,
            durationMs = DefaultBoardMoveAnimationDurationMs,
        )

        assertEquals(
            AnimateCaptureMoveAction(
                from = "e4",
                to = "d5",
                capturedSquare = "d5",
                lastMoveHighlight = LastMoveHighlight(from = "e4", to = "d5"),
                logicalPlyAfter = 2,
                durationMs = DefaultBoardMoveAnimationDurationMs,
            ),
            action,
        )
    }

    @Test
    fun buildReplayForwardMoveActionOrNull_returnsNullForPromotion() {
        val action = buildReplayForwardMoveActionOrNull(
            scene = buildScene(
                pieces = listOf(
                    BoardRenderPiece(letter = 'P', square = "e7"),
                ),
            ),
            moveUci = "e7e8q",
            logicalPlyAfter = 1,
            durationMs = DefaultBoardMoveAnimationDurationMs,
        )

        assertNull(action)
    }

    @Test
    fun buildReplayForwardMoveActionOrNull_returnsNullForCastling() {
        val action = buildReplayForwardMoveActionOrNull(
            scene = buildScene(
                pieces = listOf(
                    BoardRenderPiece(letter = 'K', square = "e1"),
                    BoardRenderPiece(letter = 'R', square = "h1"),
                ),
            ),
            moveUci = "e1g1",
            logicalPlyAfter = 1,
            durationMs = DefaultBoardMoveAnimationDurationMs,
        )

        assertNull(action)
    }

    @Test
    fun buildReplayForwardMoveActionOrNull_returnsNullForEnPassantLikeMove() {
        val action = buildReplayForwardMoveActionOrNull(
            scene = buildScene(
                pieces = listOf(
                    BoardRenderPiece(letter = 'P', square = "e5"),
                ),
            ),
            moveUci = "e5d6",
            logicalPlyAfter = 1,
            durationMs = DefaultBoardMoveAnimationDurationMs,
        )

        assertNull(action)
    }

    private fun buildScene(
        pieces: List<BoardRenderPiece>,
    ): BoardRenderScene {
        return BoardRenderScene(
            pieces = pieces,
            orientation = BoardOrientation.WHITE,
        )
    }
}
