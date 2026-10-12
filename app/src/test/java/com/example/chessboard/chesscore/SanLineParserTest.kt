package com.example.chessboard.chesscore

/**
 * Tests the chesscore SAN-line parser contract directly.
 * Covers SAN token replay from standard and supplied positions, parser errors, and promotions.
 * Does not contain PGN tokenization, variation parsing, UI, or persistence workflows.
 * Validation date: 2026-09-08.
 */

import com.example.chessboard.chesscore.model.Side
import com.example.chessboard.chesscorechesslib.ChesslibPositionFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SanLineParserTest {
    private val positionFactory = ChesslibPositionFactory()

    @Test
    fun parsesStandardSanLineFromInitialPosition() {
        val sanTokens = listOf("e4", "e5", "Nf3", "Nc6", "Bb5", "a6")

        val uciMoves = parseSanLineToUci(
            positionFactory = positionFactory,
            sanTokens = sanTokens,
        )

        assertEquals(
            listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1b5", "a7a6"),
            uciMoves,
        )
    }

    @Test
    fun parsesSanLineFromSuppliedFen() {
        val afterWhiteE4Fen =
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"

        val uciMoves = parseSanLineToUci(
            positionFactory = positionFactory,
            sanTokens = listOf("c5", "Nf3", "Nc6"),
            startFen = afterWhiteE4Fen,
        )

        assertEquals(listOf("c7c5", "g1f3", "b8c6"), uciMoves)
    }

    @Test
    fun throwsSanLineParseExceptionForUnrecognizedSanToken() {
        val error = assertThrows(SanLineParseException::class.java) {
            parseSanLineToUci(
                positionFactory = positionFactory,
                sanTokens = listOf("e4", "Qa5"),
            )
        }

        assertEquals("Qa5", error.token)
        assertEquals(1, error.localMoveNumber)
        assertEquals(Side.BLACK, error.sideToMove)
        assertEquals(SanLineParseErrorReason.UNRECOGNIZED_NOTATION, error.reason)
    }

    @Test
    fun reportsLocalMoveNumberIndependentlyFromFenFullmoveNumber() {
        val afterWhiteE4Fen =
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 23"

        val error = assertThrows(SanLineParseException::class.java) {
            parseSanLineToUci(
                positionFactory = positionFactory,
                sanTokens = listOf("Qa5"),
                startFen = afterWhiteE4Fen,
            )
        }

        assertEquals("Qa5", error.token)
        assertEquals(1, error.localMoveNumber)
        assertEquals(Side.BLACK, error.sideToMove)
    }

    @Test
    fun parsesSanPromotion() {
        val promotionFen = "k5r1/5P2/8/8/8/8/8/7K w - - 0 1"

        val uciMoves = parseSanLineToUci(
            positionFactory = positionFactory,
            sanTokens = listOf("fxg8=Q"),
            startFen = promotionFen,
        )

        assertEquals(listOf("f7g8q"), uciMoves)
    }

    @Test
    fun letsPositionFactoryRejectInvalidStartFen() {
        assertThrows(IllegalArgumentException::class.java) {
            parseSanLineToUci(
                positionFactory = positionFactory,
                sanTokens = listOf("e4"),
                startFen = "invalid FEN",
            )
        }
    }
}
