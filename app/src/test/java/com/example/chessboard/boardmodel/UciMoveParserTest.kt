package com.example.chessboard.boardmodel

/**
 * Focused JVM coverage for bidirectional chesslib Move and UCI conversion.
 * Keep coordinate and promotion-token assertions here for the shared boardmodel adapter.
 * Do not add controller workflows, Compose UI checks, or PGN/SAN formatting tests to this file.
 * Validation date: 2026-07-12
 */

import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import org.junit.Assert.assertEquals
import org.junit.Test

class UciMoveParserTest {

    @Test
    fun buildUciFromChesslibMove_returnsCoordinatesForRegularMove() {
        val move = Move(
            Square.fromValue("E2"),
            Square.fromValue("E4"),
        )

        assertEquals("e2e4", buildUciFromChesslibMove(move))
    }

    @Test
    fun buildUciFromChesslibMove_appendsQueenPromotionToken() {
        val move = Move(
            Square.fromValue("E7"),
            Square.fromValue("E8"),
            Piece.WHITE_QUEEN,
        )

        assertEquals("e7e8q", buildUciFromChesslibMove(move))
    }

    @Test
    fun buildUciFromChesslibMove_usesKnightUciPromotionToken() {
        val move = Move(
            Square.fromValue("E7"),
            Square.fromValue("E8"),
            Piece.WHITE_KNIGHT,
        )

        assertEquals("e7e8n", buildUciFromChesslibMove(move))
    }
}
