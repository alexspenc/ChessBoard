package com.example.chessboard.ui.boardanimation.replay

/**
 * Focused JVM coverage for shared replay-board animation helpers.
 * Keep only classifier/reset checks for the common non-interactive replay animation layer here.
 * Do not add Compose UI tests, screen-specific policies, or queue-engine behavior to this file.
 * Validation date: 2026-07-10
 */

import com.example.chessboard.boardmodel.LastMoveHighlight
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.boardanimation.AnimateSimpleMoveAction
import com.example.chessboard.ui.boardrender.BoardRenderPiece
import com.example.chessboard.ui.boardrender.BoardRenderScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReplayBoardAnimationTest {

    @Test
    fun buildReplaySimpleMoveActionOrNull_returnsActionForQuietMove() {
        val action = buildReplaySimpleMoveActionOrNull(
            scene = buildScene(
                pieces = listOf(
                    BoardRenderPiece(letter = 'P', square = "e2"),
                ),
            ),
            moveUci = "e2e4",
            logicalPlyAfter = 1,
            durationMs = 80,
        )

        assertEquals(
            AnimateSimpleMoveAction(
                from = "e2",
                to = "e4",
                lastMoveHighlight = LastMoveHighlight(from = "e2", to = "e4"),
                logicalPlyAfter = 1,
                durationMs = 80,
            ),
            action,
        )
    }

    @Test
    fun buildReplaySimpleMoveActionOrNull_returnsNullForCapture() {
        val action = buildReplaySimpleMoveActionOrNull(
            scene = buildScene(
                pieces = listOf(
                    BoardRenderPiece(letter = 'P', square = "e4"),
                    BoardRenderPiece(letter = 'p', square = "d5"),
                ),
            ),
            moveUci = "e4d5",
            logicalPlyAfter = 2,
            durationMs = 80,
        )

        assertNull(action)
    }

    @Test
    fun buildReplaySimpleMoveActionOrNull_returnsNullForPromotion() {
        val action = buildReplaySimpleMoveActionOrNull(
            scene = buildScene(
                pieces = listOf(
                    BoardRenderPiece(letter = 'P', square = "e7"),
                ),
            ),
            moveUci = "e7e8q",
            logicalPlyAfter = 1,
            durationMs = 80,
        )

        assertNull(action)
    }

    @Test
    fun buildReplaySimpleMoveActionOrNull_returnsNullForCastling() {
        val action = buildReplaySimpleMoveActionOrNull(
            scene = buildScene(
                pieces = listOf(
                    BoardRenderPiece(letter = 'K', square = "e1"),
                    BoardRenderPiece(letter = 'R', square = "h1"),
                ),
            ),
            moveUci = "e1g1",
            logicalPlyAfter = 1,
            durationMs = 80,
        )

        assertNull(action)
    }

    @Test
    fun buildReplaySimpleMoveActionOrNull_returnsNullForEnPassantLikeMove() {
        val action = buildReplaySimpleMoveActionOrNull(
            scene = buildScene(
                pieces = listOf(
                    BoardRenderPiece(letter = 'P', square = "e5"),
                ),
            ),
            moveUci = "e5d6",
            logicalPlyAfter = 1,
            durationMs = 80,
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
