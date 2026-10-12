package com.example.chessboard.chesscore

/**
 * Tests chesscore PGN move-text helpers before full PGN variation parsing is moved from the app.
 * Covers tokenization, main-line extraction, move-number parsing, and result-token recognition.
 * Does not contain SAN move replay, chesslib adapter tests, UI, or persistence workflows.
 * Validation date: 2026-09-09.
 */

import com.example.chessboard.chesscore.model.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PgnMoveTextParserTest {
    @Test
    fun tokenizesPgnMoveTextWithoutHeadersOrComments() {
        val pgnBom = "\uFEFF"
        val pgn = """
            ${pgnBom}[Event "Token test"]
            [Result "1-0"]

            1. e4 {central pawn} e5 (1... c5 ${'$'}1) 2. Nf3; line comment
            Nc6 1-0
        """.trimIndent()

        val tokens = tokenizePgnMoveText(pgn)

        assertEquals(
            listOf(
                "1.",
                "e4",
                "e5",
                "(",
                "1...",
                "c5",
                "${'$'}1",
                ")",
                "2.",
                "Nf3",
                "Nc6",
                "1-0",
            ),
            tokens,
        )
    }

    @Test
    fun extractsMainSanTokensWithoutMoveNumbersVariationsNagsOrResult() {
        val pgnTokens = listOf(
            "1.",
            "e4",
            "${'$'}1",
            "e5",
            "(",
            "1...",
            "c5",
            "2.",
            "Nf3",
            ")",
            "2.",
            "Nf3",
            "Nc6",
            "*",
        )

        val sanTokens = extractMainSanTokens(pgnTokens)

        assertEquals(listOf("e4", "e5", "Nf3", "Nc6"), sanTokens)
    }

    @Test
    fun parsesSideSpecificMoveNumbers() {
        assertEquals(PgnMoveNumber(number = 1, side = Side.WHITE), parsePgnMoveNumber("1."))
        assertEquals(PgnMoveNumber(number = 23, side = Side.BLACK), parsePgnMoveNumber("23..."))
        assertNull(parsePgnMoveNumber("23"))
        assertNull(parsePgnMoveNumber("e4"))
    }

    @Test
    fun recognizesMoveNumberTokens() {
        assertTrue(isPgnMoveNumberToken("1"))
        assertTrue(isPgnMoveNumberToken("1."))
        assertTrue(isPgnMoveNumberToken("1..."))
        assertFalse(isPgnMoveNumberToken("e4"))
    }

    @Test
    fun recognizesResultTokens() {
        assertTrue(isPgnResultToken("*"))
        assertTrue(isPgnResultToken("1-0"))
        assertTrue(isPgnResultToken("0-1"))
        assertTrue(isPgnResultToken("1/2-1/2"))
        assertFalse(isPgnResultToken("e4"))
    }
}
