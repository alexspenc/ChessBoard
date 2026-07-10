package com.example.chessboard.ui.screen.training

/**
 * Focused JVM coverage for training-editor replay animation gating.
 * Keep only editor-local decisions about which next moves can use simple queued animation here.
 * Do not add Compose UI tests, queue-engine tests, or unrelated editor behavior.
 * Validation date: 2026-07-10
 */

import com.example.chessboard.boardmodel.LastMoveHighlight
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.boardanimation.AnimateSimpleMoveAction
import com.example.chessboard.ui.boardrender.BoardRenderPiece
import com.example.chessboard.ui.boardrender.BoardRenderScene
import com.example.chessboard.ui.screen.training.common.buildTrainingEditorSimpleMoveActionOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrainingEditorBoardAnimationTest {

    @Test
    fun buildTrainingEditorSimpleMoveActionOrNull_returnsActionForQuietMove() {
        val action = buildTrainingEditorSimpleMoveActionOrNull(
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
    fun buildTrainingEditorSimpleMoveActionOrNull_returnsNullForCapture() {
        val action = buildTrainingEditorSimpleMoveActionOrNull(
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
    fun buildTrainingEditorSimpleMoveActionOrNull_returnsNullForPromotion() {
        val action = buildTrainingEditorSimpleMoveActionOrNull(
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
    fun buildTrainingEditorSimpleMoveActionOrNull_returnsNullForCastling() {
        val action = buildTrainingEditorSimpleMoveActionOrNull(
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
    fun buildTrainingEditorSimpleMoveActionOrNull_returnsNullForEnPassantLikeMove() {
        val action = buildTrainingEditorSimpleMoveActionOrNull(
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
