package com.example.chessboard.ui.screen.linesExplorer

/**
 * Focused JVM coverage for the first Lines Explorer animation integration rules.
 * Keep only screen-local decisions about which next moves can use simple queued animation here.
 * Do not add Compose UI tests, queue engine tests, or unrelated screen behavior to this file.
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

class LinesExplorerBoardAnimationTest {

    @Test
    fun buildLinesExplorerSimpleMoveActionOrNull_returnsActionForQuietMove() {
        val action = buildLinesExplorerSimpleMoveActionOrNull(
            scene = buildScene(
                pieces = listOf(
                    BoardRenderPiece(letter = 'P', square = "e2"),
                ),
            ),
            moveUci = "e2e4",
            logicalPlyAfter = 1,
            durationMs = 250,
        )

        assertEquals(
            AnimateSimpleMoveAction(
                from = "e2",
                to = "e4",
                lastMoveHighlight = LastMoveHighlight(from = "e2", to = "e4"),
                logicalPlyAfter = 1,
                durationMs = 250,
            ),
            action,
        )
    }

    @Test
    fun buildLinesExplorerSimpleMoveActionOrNull_returnsNullForCapture() {
        val action = buildLinesExplorerSimpleMoveActionOrNull(
            scene = buildScene(
                pieces = listOf(
                    BoardRenderPiece(letter = 'P', square = "e4"),
                    BoardRenderPiece(letter = 'p', square = "d5"),
                ),
            ),
            moveUci = "e4d5",
            logicalPlyAfter = 2,
            durationMs = 250,
        )

        assertNull(action)
    }

    @Test
    fun buildLinesExplorerSimpleMoveActionOrNull_returnsNullForPromotion() {
        val action = buildLinesExplorerSimpleMoveActionOrNull(
            scene = buildScene(
                pieces = listOf(
                    BoardRenderPiece(letter = 'P', square = "e7"),
                ),
            ),
            moveUci = "e7e8q",
            logicalPlyAfter = 1,
            durationMs = 250,
        )

        assertNull(action)
    }

    @Test
    fun buildLinesExplorerSimpleMoveActionOrNull_returnsNullForCastling() {
        val action = buildLinesExplorerSimpleMoveActionOrNull(
            scene = buildScene(
                pieces = listOf(
                    BoardRenderPiece(letter = 'K', square = "e1"),
                    BoardRenderPiece(letter = 'R', square = "h1"),
                ),
            ),
            moveUci = "e1g1",
            logicalPlyAfter = 1,
            durationMs = 250,
        )

        assertNull(action)
    }

    @Test
    fun buildLinesExplorerSimpleMoveActionOrNull_returnsNullForEnPassantLikeMove() {
        val action = buildLinesExplorerSimpleMoveActionOrNull(
            scene = buildScene(
                pieces = listOf(
                    BoardRenderPiece(letter = 'P', square = "e5"),
                ),
            ),
            moveUci = "e5d6",
            logicalPlyAfter = 1,
            durationMs = 250,
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
