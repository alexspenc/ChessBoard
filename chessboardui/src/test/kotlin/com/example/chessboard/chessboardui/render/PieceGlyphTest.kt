package com.example.chessboard.chessboardui.render

/**
 * Checks the Unicode glyph contract for all chesscore piece types.
 * Keep glyph mapping checks here; Android drawing and pixel checks belong in androidTest.
 * Validation date: 2026-10-13.
 */

import com.example.chessboard.chesscore.model.PieceType
import org.junit.Assert.assertEquals
import org.junit.Test

class PieceGlyphTest {

    /** Both sides share these six shapes; drawing colors distinguish the sides. */
    @Test
    fun resolvesUnicodeGlyphForEveryPieceType() {
        data class ExpectedGlyph(val type: PieceType, val glyph: String)

        val expectedGlyphs = listOf(
            ExpectedGlyph(PieceType.KING, "\u265A"),
            ExpectedGlyph(PieceType.QUEEN, "\u265B"),
            ExpectedGlyph(PieceType.ROOK, "\u265C"),
            ExpectedGlyph(PieceType.BISHOP, "\u265D"),
            ExpectedGlyph(PieceType.KNIGHT, "\u265E"),
            ExpectedGlyph(PieceType.PAWN, "\u265F"),
        )
        for (expected in expectedGlyphs) {
            assertEquals("Glyph for ${expected.type}", expected.glyph, resolvePieceGlyph(expected.type))
        }
    }
}
