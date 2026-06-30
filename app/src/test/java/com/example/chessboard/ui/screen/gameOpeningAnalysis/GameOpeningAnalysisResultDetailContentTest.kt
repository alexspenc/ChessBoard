package com.example.chessboard.ui.screen.gameOpeningAnalysis

/*
 * File role: verifies non-Compose helpers used by the game-opening analysis result detail UI.
 * Allowed here:
 * - JVM tests for detail-screen controller setup and presentation-facing helper behavior
 * Not allowed here:
 * - Compose rendering, Android instrumentation, database access, or analyzer execution
 * Validation date: 2026-06-30
 */

import com.example.chessboard.analysis.GameOpeningInvalidInitialPosition
import com.example.chessboard.analysis.GameOpeningMatchesKnownOpening
import com.example.chessboard.analysis.OpeningMatchMode
import com.example.chessboard.analysis.OpeningSide
import com.example.chessboard.boardmodel.InitialBoardFen
import com.example.chessboard.ui.BoardOrientation
import org.junit.Assert.assertEquals
import org.junit.Test

class GameOpeningAnalysisResultDetailContentTest {
    // Checks that the detail board controller is ready with the result position before first Compose draw.
    @Test
    fun `createAnalysisResultBoardController loads preview fen immediately`() {
        val result =
            GameOpeningMatchesKnownOpening(
                selectedSide = OpeningSide.BLACK,
                matchMode = OpeningMatchMode.MOVE_SEQUENCE,
                matchedPly = 1,
                finalPositionFen = AfterE4Fen,
                matchingLineRefs = emptyList(),
            )

        val controller = createAnalysisResultBoardController(result)

        assertEquals(
            normalizeFenForAssertion(AfterE4Fen),
            normalizeFenForAssertion(controller.getFen()),
        )
        assertEquals(BoardOrientation.BLACK, controller.getSide())
    }

    // Checks that results without a preview keep the controller on the default initial board.
    @Test
    fun `createAnalysisResultBoardController keeps initial board when preview fen is unavailable`() {
        val result =
            GameOpeningInvalidInitialPosition(
                selectedSide = OpeningSide.WHITE,
                matchMode = OpeningMatchMode.MOVE_SEQUENCE,
                initialFen = "bad fen",
            )

        val controller = createAnalysisResultBoardController(result)

        assertEquals(
            normalizeFenForAssertion(InitialBoardFen),
            normalizeFenForAssertion(controller.getFen()),
        )
        assertEquals(BoardOrientation.WHITE, controller.getSide())
    }

    private companion object {
        fun normalizeFenForAssertion(fen: String): String {
            return fen.split(" ").take(4).joinToString(" ")
        }

        const val AfterE4Fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
    }
}
