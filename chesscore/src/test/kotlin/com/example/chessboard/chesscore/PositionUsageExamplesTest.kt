package com.example.chessboard.chesscore

/**
 * File role: demonstrates position interfaces and SAN/PGN parsing with a recorded opening.
 * Checks the supplied FEN sequence, independent positions, and explicit expected UCI output.
 * Does not validate a full chess engine, variations, or negative parsing scenarios.
 * Validation date: 2026-11-14.
 */

import com.example.chessboard.chesscore.fixtures.OpeningSequence
import com.example.chessboard.chesscore.fixtures.OpeningSequencePositionFactory
import com.example.chessboard.chesscore.model.Piece
import com.example.chessboard.chesscore.model.PieceType
import com.example.chessboard.chesscore.model.Side
import com.example.chessboard.chesscore.model.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PositionUsageExamplesTest {
    @Test
    fun appliesRecordedMovesAndExposesPositionState() {
        val factory: PositionFactory = OpeningSequencePositionFactory()
        val position: Position = factory.create()
        val movingPieces = listOf(
            Piece(Side.WHITE, PieceType.KNIGHT),
            Piece(Side.BLACK, PieceType.KNIGHT),
            Piece(Side.WHITE, PieceType.PAWN),
            Piece(Side.BLACK, PieceType.PAWN),
            Piece(Side.WHITE, PieceType.BISHOP),
            Piece(Side.BLACK, PieceType.PAWN),
        )

        assertEquals(OpeningSequence.fens.first(), position.getFen())
        assertEquals(Side.WHITE, position.getSideToMove())
        assertEquals(Piece(Side.WHITE, PieceType.KNIGHT), position.getPiece(Square('g', 1)))
        assertNull(position.getPiece(Square('f', 3)))

        for ((index, move) in OpeningSequence.moves.withIndex()) {
            val description = "Recorded half-move ${index + 1}: $move"
            val movingPiece = movingPieces[index]
            assertEquals(description, movingPiece.side, position.getSideToMove())
            assertEquals(description, movingPiece, position.getPiece(move.from))
            assertTrue(description, position.getLegalMoves().contains(move))

            assertTrue(description, position.applyMove(move))

            var expectedSide = Side.WHITE
            if (movingPiece.side == Side.WHITE) {
                expectedSide = Side.BLACK
            }
            assertEquals(description, OpeningSequence.fens[index + 1], position.getFen())
            assertEquals(description, expectedSide, position.getSideToMove())
            assertNull(description, position.getPiece(move.from))
            assertEquals(description, movingPiece, position.getPiece(move.to))
        }
    }

    @Test
    fun createsIndependentPositionsFromTheSameFactory() {
        val factory: PositionFactory = OpeningSequencePositionFactory()
        val firstPosition = factory.create()
        val secondPosition = factory.create()

        for (move in OpeningSequence.moves) {
            assertTrue(firstPosition.applyMove(move))
            assertEquals(OpeningSequence.fens.first(), secondPosition.getFen())
            assertEquals(Side.WHITE, secondPosition.getSideToMove())
        }

        assertEquals(OpeningSequence.fens.last(), firstPosition.getFen())
        assertEquals(Piece(Side.WHITE, PieceType.KNIGHT), secondPosition.getPiece(Square('g', 1)))
        assertNull(secondPosition.getPiece(Square('f', 3)))

        assertTrue(secondPosition.applyMove(OpeningSequence.moves.first()))
        assertEquals(OpeningSequence.fens[1], secondPosition.getFen())
        assertEquals(OpeningSequence.fens.last(), firstPosition.getFen())
    }

    @Test
    fun parsesSanTokensThroughThePositionFactory() {
        val factory: PositionFactory = OpeningSequencePositionFactory()

        val uciMoves = parseSanLineToUci(
            positionFactory = factory,
            sanTokens = listOf("Nf3", "Nf6", "g3", "d5", "Bg2", "c6"),
        )

        assertEquals(
            listOf("g1f3", "g8f6", "g2g3", "d7d5", "f1g2", "c7c6"),
            uciMoves,
        )
    }

    @Test
    fun parsesTheMainPgnLineThroughThePositionFactory() {
        val factory: PositionFactory = OpeningSequencePositionFactory()

        val uciMoves = parsePgnMainLineToUci(
            positionFactory = factory,
            pgnText = "1. Nf3 Nf6 2. g3 d5 3. Bg2 c6",
        )

        assertEquals(
            listOf("g1f3", "g8f6", "g2g3", "d7d5", "f1g2", "c7c6"),
            uciMoves,
        )
    }

    @Test
    fun parsesAContinuationFromABlackToMoveFen() {
        val factory: PositionFactory = OpeningSequencePositionFactory()
        val startFen = "rnbqkb1r/pppppppp/5n2/8/8/5NP1/PPPPPP1P/RNBQKB1R b KQkq - 0 2"
        val position = factory.create(startFen)

        assertEquals(startFen, position.getFen())
        assertEquals(Side.BLACK, position.getSideToMove())

        val uciMoves = parsePgnMainLineToUci(
            positionFactory = factory,
            pgnText = "2... d5 3. Bg2 c6",
            startFen = startFen,
        )

        assertEquals(listOf("d7d5", "f1g2", "c7c6"), uciMoves)
        assertEquals(startFen, position.getFen())
    }
}
