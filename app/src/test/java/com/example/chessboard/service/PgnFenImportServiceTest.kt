package com.example.chessboard.service

/*
 * File role: verifies PGN/SAN line parsing from arbitrary FEN positions.
 * Allowed here:
 * - parser behavior for side to move, relative numbering, variations, and four-field FEN
 * Not allowed here:
 * - Compose UI, database integration, or continuation duplicate rules
 * Validation date: 2026-09-02
 */

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PgnFenImportServiceTest {
    private val afterWhiteE4Fen =
        "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3"

    @Test
    fun `parses black-to-move line from four-field FEN and ignores absolute move numbers`() {
        val pgn = """
            [Event "Copied fragment"]
            [Result "*"]

            23... c5 { Sicilian structure } 24. Nf3 Nc6 *
        """.trimIndent()

        val lines = parsePgnToUciLines(
            pgnText = pgn,
            startFen = afterWhiteE4Fen,
        )

        assertEquals(
            listOf(
                listOf("c7c5", "g1f3", "b8c6"),
            ),
            lines,
        )
    }

    @Test
    fun `parses numbered variations relative to black-to-move FEN`() {
        val pgn = """
            23... c5 24. Nf3 d6 (24... Nc6 25. d4) 25. d4 *
        """.trimIndent()

        val lines = parsePgnToUciLines(
            pgnText = pgn,
            startFen = afterWhiteE4Fen,
        )

        assertEquals(
            listOf(
                listOf("c7c5", "g1f3", "d7d6", "d2d4"),
                listOf("c7c5", "g1f3", "b8c6", "d2d4"),
            ),
            lines,
        )
    }

    @Test
    fun `uses en passant field from four-field FEN`() {
        val fen = "rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6"

        val lines = parsePgnToUciLines(
            pgnText = "18. exf6 *",
            startFen = fen,
        )

        assertEquals(listOf(listOf("e5f6")), lines)
    }

    @Test
    fun `rejects all lines when a variation is illegal from supplied FEN`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            parsePgnToUciLines(
                pgnText = "23... c5 24. Nf3 d6 (24... Qh4) 25. d4 *",
                startFen = afterWhiteE4Fen,
            )
        }

        assertTrue(error.message?.contains("Qh4") == true)
        assertTrue(error.message?.contains("variation") == true)
    }

    @Test
    fun `reports local move number and side from supplied FEN`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            parsePgnToUciLines(
                pgnText = "23... Qa5 *",
                startFen = afterWhiteE4Fen,
            )
        }

        assertTrue(error.message?.contains("move 1, Black") == true)
    }

    @Test
    fun `parses from-position PGN with two variations into three lines`() {
        val headerFen = "rnbqkbnr/ppp1pppp/8/3p4/8/5NP1/PPPPPP1P/RNBQKB1R b KQkq"
        val startFen = "$headerFen -"
        val pgn = """
            [Variant "From Position"]
            [FEN "$headerFen"]

            1... Nf6 2. Bg2 g6 (2... c5 3. O-O Nc6 4. d4 e6 5. c4) 3. O-O Bg7
            (3... c6 4. c4 Bg7 5. cxd5 cxd5 6. d4) 4. c4 c6
        """.trimIndent()

        val lines = parsePgnToUciLines(
            pgnText = pgn,
            startFen = startFen,
        )

        val expectedLines = setOf(
            listOf("g8f6","f1g2","g7g6","e1g1","f8g7","c2c4","c7c6",),
            listOf("g8f6","f1g2","c7c5","e1g1","b8c6","d2d4","e7e6","c2c4",),
            listOf("g8f6","f1g2","g7g6","e1g1","c7c6","c2c4","f8g7","c4d5","c6d5","d2d4",),
        )
        assertEquals(3, lines.size)
        assertEquals(expectedLines, lines.toSet())
    }
}
