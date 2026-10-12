package com.example.chessboard.chesscore

/**
 * Tests chesscore UCI token recognition, extraction, and syntactic move parsing.
 * Covers plain moves, promotion moves, non-UCI token rejection, and mixed stored-PGN-like text.
 * Does not contain chesslib legality checks, app stored-PGN serialization, UI, or persistence tests.
 * Validation date: 2026-09-09.
 */

import com.example.chessboard.chesscore.model.Move
import com.example.chessboard.chesscore.model.PromotionPiece
import com.example.chessboard.chesscore.model.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class UciMoveParserTest {
    @Test
    fun recognizesUciMoveTokens() {
        assertTrue(isUciMoveToken("e2e4"))
        assertTrue(isUciMoveToken("e7e8q"))
        assertTrue(isUciMoveToken("e7e8Q"))
        assertFalse(isUciMoveToken("Nf3"))
        assertFalse(isUciMoveToken("i2e4"))
        assertFalse(isUciMoveToken("e2e9"))
    }

    @Test
    fun parsesPlainUciMove() {
        val move = parseUciMove("e2e4")

        assertEquals(
            Move(
                from = Square('e', 2),
                to = Square('e', 4),
            ),
            move,
        )
    }

    @Test
    fun parsesPromotionUciMove() {
        val move = parseUciMove("e7e8Q")

        assertEquals(
            Move(
                from = Square('e', 7),
                to = Square('e', 8),
                promotion = PromotionPiece.QUEEN,
            ),
            move,
        )
    }

    @Test
    fun rejectsInvalidUciMoveToken() {
        val error = assertThrows(UciMoveParseException::class.java) {
            parseUciMove("Nf3")
        }

        assertEquals("Nf3", error.token)
        assertEquals(UciMoveParseErrorReason.INVALID_FORMAT, error.reason)
    }

    @Test
    fun extractsUciMoveTokensFromMixedText() {
        val text = """
            [Event "Test"]
            [Result "*"]

            1. e2e4 e7e5 2. Nf3 g8f6 3. e7e8Q *
        """.trimIndent()

        val moves = extractUciMoveTokens(text)

        assertEquals(
            listOf("e2e4", "e7e5", "g8f6", "e7e8Q"),
            moves,
        )
    }
}
