package com.example.chessboard.chesscore

/**
 * Tests PGN variation expansion independently of position implementations.
 * Covers SAN line ordering, numbering, duplicate preservation, and variation structure errors.
 * Does not contain chesslib, legal move replay, UI, or persistence tests.
 * Validation date: 2026-10-12.
 */

import com.example.chessboard.chesscore.model.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PgnVariationParserTest {
    @Test
    fun `expands supplied tokens with main line last`() {
        val lines = extractSanLines(
            pgnTokens = listOf("e4", "e5", "(", "c5", ")", "d4"),
            startingSide = Side.WHITE,
        )

        assertEquals(listOf(listOf("e4", "c5"), listOf("e4", "e5", "d4")), lines)
    }

    @Test
    fun `nested and sibling branches preserve variation closing order`() {
        val lines = extractSanLines(
            tokenizePgnMoveText("e4 e5 (c5 Nf3 (Nc3) (Bc4)) d4"),
            Side.WHITE,
        )

        assertEquals(
            listOf(
                listOf("e4", "c5", "Nc3"),
                listOf("e4", "c5", "Bc4"),
                listOf("e4", "c5", "Nf3"),
                listOf("e4", "e5", "d4"),
            ),
            lines,
        )
    }

    @Test
    fun `numbered variations use numbering relative to black starting side`() {
        val lines = extractSanLines(
            tokenizePgnMoveText("23... c5 24. Nf3 d6 (24... Nc6 25. d4) 25. d4 *"),
            Side.BLACK,
        )

        assertEquals(
            listOf(listOf("c5", "Nf3", "Nc6", "d4"), listOf("c5", "Nf3", "d6", "d4")),
            lines,
        )
    }

    @Test
    fun `explicit number can start a branch after the preceding move`() {
        val lines = extractSanLines(tokenizePgnMoveText("1. e4 e5 (2. Nf3)"), Side.WHITE)

        assertEquals(listOf(listOf("e4", "e5", "Nf3"), listOf("e4", "e5")), lines)
    }

    @Test
    fun `duplicate branches are preserved`() {
        val lines = extractSanLines(tokenizePgnMoveText("1. e4 (1. e4 e5) e5"), Side.WHITE)

        assertEquals(listOf(listOf("e4", "e5"), listOf("e4", "e5")), lines)
    }

    @Test
    fun `filters numbers NAGs and results without checking move legality`() {
        val lines = extractSanLines(
            listOf("1.", "e4", "e5", "(", "Nf3", "\$1", ")", "*"),
            Side.WHITE,
        )

        assertEquals(listOf(listOf("e4", "Nf3"), listOf("e4", "e5")), lines)
    }

    @Test
    fun `empty tokens produce no lines`() {
        assertEquals(emptyList<List<String>>(), extractSanLines(emptyList(), Side.WHITE))
    }

    @Test
    fun `variations before first main line move are rejected even with move numbers`() {
        for (pgn in listOf("(d4 d5) e4 e5", "(1. d4 d5) 1. e4 e5")) {
            val error = assertThrows(PgnVariationParseException::class.java) {
                extractSanLines(tokenizePgnMoveText(pgn), Side.WHITE)
            }
            assertEquals(PgnVariationParseErrorReason.MISSING_PRECEDING_MOVE, error.reason)
            assertEquals("(", error.token)
            assertEquals(0, error.tokenIndex)
        }
    }

    @Test
    fun `variations without own moves are rejected`() {
        for (pgn in listOf(
            "e4 e5 () Nf3",
            "e4 e5 ()",
            "e4 e5 (1...)",
            "e4 e5 ({comment})",
            "e4 e5 (1... {comment})",
            "e4 e5 (c5 Nf3 ())",
        )) {
            val error = assertThrows(PgnVariationParseException::class.java) {
                extractSanLines(tokenizePgnMoveText(pgn), Side.WHITE)
            }
            assertEquals(PgnVariationParseErrorReason.EMPTY_VARIATION, error.reason)
            assertEquals(")", error.token)
        }
    }

    @Test
    fun `NAG before first variation move is rejected`() {
        for (pgn in listOf(
            "e4 e5 (\$1 c5)",
            "e4 e5 (1... \$1 c5)",
            "e4 e5 (c5 Nf3 (\$1 Nc3))",
        )) {
            val error = assertThrows(PgnVariationParseException::class.java) {
                extractSanLines(tokenizePgnMoveText(pgn), Side.WHITE)
            }
            assertEquals(PgnVariationParseErrorReason.NAG_BEFORE_FIRST_MOVE, error.reason)
            assertEquals("\$1", error.token)
        }
    }

    @Test
    fun `nested variation before first own move is rejected`() {
        for (pgn in listOf("e4 e5 ((c5) d5)", "e4 e5 (1... (1... c5) d5)")) {
            val error = assertThrows(PgnVariationParseException::class.java) {
                extractSanLines(tokenizePgnMoveText(pgn), Side.WHITE)
            }
            assertEquals(PgnVariationParseErrorReason.MISSING_PRECEDING_MOVE, error.reason)
            assertEquals("(", error.token)
        }
    }
}
