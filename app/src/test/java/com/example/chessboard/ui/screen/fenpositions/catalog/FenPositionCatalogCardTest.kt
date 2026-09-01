package com.example.chessboard.ui.screen.fenpositions.catalog

/*
 * File role: verifies pure board setup rules shared by FEN position screens.
 * Allowed here:
 * - four-field FEN conversion and side-to-move orientation tests
 * Not allowed here:
 * - Compose rendering, Room access, service behavior, or navigation tests
 * Validation date: 2026-09-01
 */

import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.screen.fenpositions.resolveFenPositionBoardOrientation
import com.example.chessboard.ui.screen.fenpositions.toLoadableFenPosition
import org.junit.Assert.assertEquals
import org.junit.Test

class FenPositionCatalogCardTest {
    @Test
    fun `white side to move places white pieces at the bottom`() {
        val fen = "8/8/8/8/8/8/8/8 w - -"

        assertEquals(BoardOrientation.WHITE, resolveFenPositionBoardOrientation(fen))
    }

    @Test
    fun `black side to move places black pieces at the bottom`() {
        val fen = "8/8/8/8/8/8/8/8 b - -"

        assertEquals(BoardOrientation.BLACK, resolveFenPositionBoardOrientation(fen))
    }

    @Test
    fun `loadable FEN adds only technical move counters`() {
        val fen = "8/8/8/8/8/8/8/8 b Kq e3"

        assertEquals(
            "8/8/8/8/8/8/8/8 b Kq e3 0 1",
            toLoadableFenPosition(fen),
        )
    }
}
