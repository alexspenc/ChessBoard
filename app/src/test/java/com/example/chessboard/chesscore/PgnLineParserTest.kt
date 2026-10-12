package com.example.chessboard.chesscore

/**
 * Tests the shared PGN-to-UCI parser through its public contract.
 * Covers starting positions, variation order, duplicates, and technical parsing errors.
 * Uses the chesslib adapter for replay; does not contain app error formatting, UI, or persistence.
 * Validation date: 2026-10-12.
 */

import com.example.chessboard.chesscore.model.Move
import com.example.chessboard.chesscore.model.Side
import com.example.chessboard.chesscorechesslib.ChesslibPositionFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class PgnLineParserTest {
    private val positionFactory = ChesslibPositionFactory()
    private val afterE4Fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3"

    @Test
    fun `parses main line and independent numbered variation from standard position`() {
        val lines = parsePgnToUciLines(
            positionFactory,
            "[Event \"Test\"]\n1. e4 {comment} e5 (1... c5 \$1) 2. Nf3 *",
        )

        assertEquals(
            listOf(listOf("e2e4", "e7e5", "g1f3"), listOf("e2e4", "c7c5")),
            lines,
        )
    }

    @Test
    fun `nested unnumbered variations preserve existing result order`() {
        val lines = parsePgnToUciLines(positionFactory, "e4 e5 (c5 Nf3 (Nc3)) d4")

        assertEquals(
            listOf(
                listOf("e2e4", "e7e5", "d2d4"),
                listOf("e2e4", "c7c5", "g1f3"),
                listOf("e2e4", "c7c5", "b1c3"),
            ),
            lines,
        )
    }

    @Test
    fun `preserves duplicate lines for caller-owned filtering`() {
        val lines = parsePgnToUciLines(positionFactory, "1. e4 (1. e4 e5) e5")

        assertEquals(listOf(listOf("e2e4", "e7e5"), listOf("e2e4", "e7e5")), lines)
    }

    @Test
    fun `normalizes four and six field FEN for black-to-move variations`() {
        for (fen in listOf(afterE4Fen, "$afterE4Fen 17 23")) {
            val lines = parsePgnToUciLines(
                positionFactory,
                "23... c5 24. Nf3 d6 (24... Nc6 25. d4) 25. d4 *",
                fen,
            )

            assertEquals(
                listOf(
                    listOf("c7c5", "g1f3", "d7d6", "d2d4"),
                    listOf("c7c5", "g1f3", "b8c6", "d2d4"),
                ),
                lines,
            )
        }
    }

    @Test
    fun `null and blank FEN retain standard-position import behavior`() {
        for (fen in listOf(null, "", " \t ")) {
            assertEquals(listOf(listOf("e2e4")), parsePgnToUciLines(positionFactory, "e4", fen))
        }
    }

    @Test
    fun `empty move text returns no lines`() {
        assertEquals(
            emptyList<List<String>>(),
            parsePgnToUciLines(positionFactory, "[Event \"Empty\"]\n{comment} *"),
        )
    }

    @Test
    fun `FEN shape error precedes even empty move text and is not wrapped`() {
        val error = assertThrows(FenParseException::class.java) {
            parsePgnToUciLines(positionFactory, "", "$afterE4Fen 0")
        }

        assertEquals(FenParseErrorReason.MISSING_FULLMOVE_NUMBER, error.reason)
        assertEquals(5, error.fieldCount)
    }

    @Test
    fun `variation structure error retains its token context without a line wrapper`() {
        val error = assertThrows(PgnVariationParseException::class.java) {
            parsePgnToUciLines(positionFactory, "e4 e5 ()")
        }

        assertEquals(PgnVariationParseErrorReason.EMPTY_VARIATION, error.reason)
        assertEquals(")", error.token)
        assertEquals(3, error.tokenIndex)
    }

    @Test
    fun `main line error retains original SAN failure`() {
        val error = assertThrows(PgnLineParseException::class.java) {
            parsePgnToUciLines(positionFactory, "e4 Qa5 (c5)")
        }

        assertEquals(0, error.lineIndex)
        assertEquals("Qa5", error.sanError.token)
        assertEquals(1, error.sanError.localMoveNumber)
        assertEquals(Side.BLACK, error.sanError.sideToMove)
        assertEquals(SanLineParseErrorReason.UNRECOGNIZED_NOTATION, error.sanError.reason)
        assertSame(error.sanError, error.cause)
    }

    @Test
    fun `variation error index includes duplicate lines and aborts the parse`() {
        val error = assertThrows(PgnLineParseException::class.java) {
            parsePgnToUciLines(positionFactory, "1. e4 (1. e4 Qa5) (1. e4 e5) e5")
        }

        assertEquals(2, error.lineIndex)
        assertEquals("Qa5", error.sanError.token)
        assertSame(error.sanError, error.cause)
    }

    @Test
    fun `FEN counters do not change local SAN error numbering`() {
        val error = assertThrows(PgnLineParseException::class.java) {
            parsePgnToUciLines(positionFactory, "23... Qa5", "$afterE4Fen 0 23")
        }

        assertEquals(0, error.lineIndex)
        assertEquals(1, error.sanError.localMoveNumber)
        assertEquals(Side.BLACK, error.sanError.sideToMove)
    }

    @Test
    fun `move application rejection retains illegal move reason`() {
        val rejectingFactory = object : PositionFactory {
            override fun create(fen: String?): Position {
                val position = positionFactory.create(fen)
                return object : Position by position {
                    override fun applyMove(move: Move): Boolean {
                        return false
                    }
                }
            }
        }
        val error = assertThrows(PgnLineParseException::class.java) {
            parsePgnToUciLines(rejectingFactory, "e4")
        }

        assertEquals(0, error.lineIndex)
        assertEquals("e4", error.sanError.token)
        assertEquals(SanLineParseErrorReason.ILLEGAL_MOVE, error.sanError.reason)
    }

    @Test
    fun `factory failure during replay is propagated unchanged`() {
        val failure = IllegalArgumentException("Cannot create replay position")
        val failingFactory = object : PositionFactory {
            var createdPositions = 0

            override fun create(fen: String?): Position {
                if (createdPositions > 0) {
                    throw failure
                }
                createdPositions++
                return positionFactory.create(fen)
            }
        }
        val error = assertThrows(IllegalArgumentException::class.java) {
            parsePgnToUciLines(failingFactory, "e4")
        }

        assertSame(failure, error)
    }
}
