package com.example.chessboard.chesscore

/**
 * Tests chesscore FEN field-count normalization before concrete position validation.
 * Covers four-field completion, six-field preservation, five-field rejection, blank input,
 * and whitespace normalization.
 * Does not contain chesslib legality checks, UI, persistence, or PGN/SAN replay tests.
 * Validation date: 2026-09-09.
 */

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FenParserTest {
    @Test
    fun normalizesFourFieldFenToSixFields() {
        val fen = "8/8/8/8/8/8/4K3/6k1 b - -"

        val normalizedFen = normalizeFenForPositionLoad(fen)

        assertEquals(
            "8/8/8/8/8/8/4K3/6k1 b - - 0 1",
            normalizedFen,
        )
    }

    @Test
    fun preservesSixFieldFenCounters() {
        val fen = "8/8/8/8/8/8/4K3/6k1 b - - 7 23"

        val normalizedFen = normalizeFenForPositionLoad(fen)

        assertEquals(
            "8/8/8/8/8/8/4K3/6k1 b - - 7 23",
            normalizedFen,
        )
    }

    @Test
    fun normalizesWhitespaceBetweenFields() {
        val fen = "  8/8/8/8/8/8/4K3/6k1   b\t-\n-\t0   23  "

        val normalizedFen = normalizeFenForPositionLoad(fen)

        assertEquals(
            "8/8/8/8/8/8/4K3/6k1 b - - 0 23",
            normalizedFen,
        )
    }

    @Test
    fun rejectsFiveFieldFenWithoutGuessingFullmoveNumber() {
        val error = assertThrows(FenParseException::class.java) {
            normalizeFenForPositionLoad("8/8/8/8/8/8/4K3/6k1 b - - 0")
        }

        assertEquals(5, error.fieldCount)
        assertEquals(FenParseErrorReason.MISSING_FULLMOVE_NUMBER, error.reason)
    }

    @Test
    fun rejectsBlankFen() {
        val error = assertThrows(FenParseException::class.java) {
            normalizeFenForPositionLoad(" \t\n ")
        }

        assertEquals(0, error.fieldCount)
        assertEquals(FenParseErrorReason.EMPTY_FEN, error.reason)
    }

    @Test
    fun rejectsUnsupportedFieldCount() {
        val error = assertThrows(FenParseException::class.java) {
            normalizeFenForPositionLoad("not a fen")
        }

        assertEquals(3, error.fieldCount)
        assertEquals(FenParseErrorReason.UNSUPPORTED_FIELD_COUNT, error.reason)
    }
}
