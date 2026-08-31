package com.example.chessboard.service.fenpositions

/*
 * File role: verifies FEN validation and canonicalization for the FEN positions feature.
 * Allowed here:
 * - four-field and six-field FEN normalization assertions
 * - malformed FEN rejection assertions
 * Not allowed here:
 * - Room integration, Compose behavior, or other FEN consumers
 * Validation date: 2026-08-31
 */

import com.example.chessboard.service.normalizeValidFenPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FenPositionFenTest {
    @Test
    fun `four-field position FEN remains unchanged`() {
        assertEquals(
            InitialPositionFen,
            normalizeValidFenPosition(InitialPositionFen),
        )
    }

    @Test
    fun `six-field FEN is normalized to position fields`() {
        assertEquals(
            InitialPositionFen,
            normalizeValidFenPosition("$InitialPositionFen 17 42"),
        )
    }

    @Test
    fun `malformed FEN is rejected`() {
        assertNull(normalizeValidFenPosition("not a fen"))
    }

    private companion object {
        const val InitialPositionFen =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -"
    }
}
