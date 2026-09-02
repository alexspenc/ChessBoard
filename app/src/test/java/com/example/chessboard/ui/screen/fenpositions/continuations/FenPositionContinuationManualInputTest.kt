package com.example.chessboard.ui.screen.fenpositions.continuations

/*
 * File role: verifies one manually authored continuation from an arbitrary stored FEN position.
 * Allowed here:
 * - source-position setup, legal move tracking, SAN numbering, undo, and clear
 * Not allowed here:
 * - Compose rendering, gesture recognition, pasted-text parsing, Room, or navigation tests
 * Validation date: 2026-09-02
 */

import com.example.chessboard.ui.BoardOrientation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FenPositionContinuationManualInputTest {

    @Test
    fun `manual input starts from FEN side and builds black-first SAN`() {
        val input = FenPositionContinuationManualInput(BlackToMoveFen)

        assertEquals(BoardOrientation.BLACK, input.lineController.getSide())
        assertTrue(input.lineController.tryMove("g8", "f6"))
        assertTrue(input.lineController.tryMove("f1", "g2"))

        assertEquals(listOf("g8f6", "f1g2"), input.uciMoves)
        assertEquals("1... Nf6 2. Bg2", input.sanLine)
        assertTrue(input.canUndo)
        assertTrue(input.canClear)
        assertTrue(input.canSave)
    }

    @Test
    fun `undo exposes only the currently applied move prefix`() {
        val input = FenPositionContinuationManualInput(WhiteToMoveFen)
        assertTrue(input.lineController.tryMove("e2", "e4"))
        assertTrue(input.lineController.tryMove("e7", "e5"))

        assertTrue(input.undo())

        assertEquals(listOf("e2e4"), input.uciMoves)
        assertEquals("1. e4", input.sanLine)
        assertTrue(input.canUndo)
        assertTrue(input.canClear)
    }

    @Test
    fun `clear restores source position and empties the manual line`() {
        val input = FenPositionContinuationManualInput(BlackToMoveFen)
        assertTrue(input.lineController.tryMove("g8", "f6"))

        input.clear()

        assertEquals("", input.sanLine)
        assertTrue(input.uciMoves.isEmpty())
        assertFalse(input.canUndo)
        assertFalse(input.canClear)
        assertFalse(input.canSave)
        assertEquals("$BlackToMoveFen 0 1", input.lineController.getFen())
    }

    @Test
    fun `white-first SAN numbers complete move pairs from one`() {
        assertEquals(
            "1. e4 e5 2. Nf3",
            buildFenPositionContinuationSanLine(
                uciMoves = listOf("e2e4", "e7e5", "g1f3"),
                startFen = WhiteToMoveFen,
            ),
        )
    }

    private companion object {
        const val WhiteToMoveFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -"
        const val BlackToMoveFen =
            "rnbqkbnr/ppp1pppp/8/3p4/8/5NP1/PPPPPP1P/RNBQKB1R b KQkq -"
    }
}
