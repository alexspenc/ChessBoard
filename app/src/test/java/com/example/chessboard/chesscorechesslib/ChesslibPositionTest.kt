package com.example.chessboard.chesscorechesslib

/**
 * Tests the factory and position contracts against the actual chesslib implementation.
 * Covers position independence, move legality, and special moves.
 * Does not contain SAN/PGN parsing, UI, or database access.
 * Validation date: 2026-09-08.
 */

import com.example.chessboard.chesscore.model.Move
import com.example.chessboard.chesscore.model.Piece
import com.example.chessboard.chesscore.model.PieceType
import com.example.chessboard.chesscore.PositionFactory
import com.example.chessboard.chesscore.model.PromotionPiece
import com.example.chessboard.chesscore.model.Side
import com.example.chessboard.chesscore.model.Square
import org.junit.Assert.*
import org.junit.Test

class ChesslibPositionTest {
    private val factory: PositionFactory = ChesslibPositionFactory()
    private val initialFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    @Test
    fun createsIndependentStandardPositions() {
        var pos = factory.create()
        assertEquals(initialFen, pos.getFen())
        assertTrue(pos.applyMove(Move(Square('e', 2), Square('e', 4))))
        assertEquals(Side.BLACK, pos.getSideToMove())

        pos = factory.create()
        assertEquals(initialFen, pos.getFen())
        assertEquals(Side.WHITE, pos.getSideToMove())
    }

    @Test
    fun loadsSixFieldsAndPreservesCounters() {
        val fen = "4k3/8/8/8/8/8/8/4K3 b - - 17 42"
        val position = factory.create("  " + fen.replace(" ", " \t ") + "  ")
        assertEquals(fen, position.getFen())
        assertEquals(Side.BLACK, position.getSideToMove())
        assertTrue(position.applyMove(Move(Square('e', 8), Square('d', 8))))
        assertEquals("3k4/8/8/8/8/8/8/4K3 w - - 18 43", position.getFen())
    }

    @Test
    fun rejectsWrongFieldCount() {
        for (fen in listOf("", " ", initialFen.substringBeforeLast(" 0 1"), "$initialFen extra")) {
            assertThrows(IllegalArgumentException::class.java) { factory.create(fen) }
        }
    }

    @Test
    fun wrapsChesslibLoadingFailure() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            factory.create("4k3/8/8/8/8/8/8/4K3 w - - invalid 1")
        }
        assertNotNull(error.cause)
    }

    @Test
    fun mapsPiecesOfBothColorsAndEmptySquare() {
        val position = factory.create()
        val backRank = listOf(PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP,
            PieceType.QUEEN, PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK)
        for ((index, type) in backRank.withIndex()) {
            val file = 'a' + index
            assertEquals(Piece(Side.WHITE, type), position.getPiece(Square(file, 1)))
            assertEquals(Piece(Side.BLACK, type), position.getPiece(Square(file, 8)))
            assertEquals(Piece(Side.WHITE, PieceType.PAWN), position.getPiece(Square(file, 2)))
            assertEquals(Piece(Side.BLACK, PieceType.PAWN), position.getPiece(Square(file, 7)))
        }
        assertNull(position.getPiece(Square('e', 4)))
    }

    @Test
    fun legalMovesAreAnIndependentSnapshot() {
        val position = factory.create()
        val moves = position.getLegalMoves()
        val expected = ('a'..'h').flatMap { file ->
            listOf(Move(Square(file, 2), Square(file, 3)), Move(Square(file, 2), Square(file, 4)))
        }.toSet() + setOf(
            Move(Square('b', 1), Square('a', 3)), Move(Square('b', 1), Square('c', 3)),
            Move(Square('g', 1), Square('f', 3)), Move(Square('g', 1), Square('h', 3)),
        )
        assertEquals(expected, moves.toSet())
        assertTrue(position.applyMove(Move(Square('e', 2), Square('e', 4))))
        assertEquals(expected, moves.toSet())
        assertEquals(Piece(Side.WHITE, PieceType.PAWN), position.getPiece(Square('e', 4)))
        assertNull(position.getPiece(Square('e', 2)))
    }

    @Test
    fun illegalMoveDoesNotChangePosition() {
        val position = factory.create()
        assertFalse(position.applyMove(Move(Square('e', 2), Square('e', 5))))
        assertEquals(initialFen, position.getFen())
    }

    @Test
    fun rejectsMoveExposingOwnKing() {
        val fen = "k3r3/8/8/8/8/8/4R3/4K3 w - - 0 1"
        val position = factory.create(fen)
        val move = Move(Square('e', 2), Square('d', 2))
        assertFalse(position.getLegalMoves().contains(move))
        assertFalse(position.applyMove(move))
        assertEquals(fen, position.getFen())
    }

    @Test
    fun appliesBothCastlesAndUpdatesRights() {
        val position = factory.create("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        assertTrue(position.applyMove(Move(Square('e', 1), Square('g', 1))))
        assertTrue(position.applyMove(Move(Square('e', 8), Square('c', 8))))
        assertEquals("2kr3r/8/8/8/8/8/8/R4RK1 w - - 2 2", position.getFen())
    }

    @Test
    fun appliesEnPassantCapture() {
        val fen = "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2"
        val position = factory.create(fen)
        val move = Move(Square('e', 5), Square('d', 6))
        assertTrue(position.getLegalMoves().contains(move))
        assertTrue(position.applyMove(move))
        assertEquals("4k3/8/3P4/8/8/8/8/4K3 b - - 0 2", position.getFen())
    }

    @Test
    fun mapsAndAppliesAllPromotionsForBothSides() {
        val types = mapOf(PromotionPiece.QUEEN to PieceType.QUEEN,
            PromotionPiece.ROOK to PieceType.ROOK, PromotionPiece.BISHOP to PieceType.BISHOP,
            PromotionPiece.KNIGHT to PieceType.KNIGHT)
        for (side in Side.entries) {
            val fen = if (side == Side.WHITE) "7k/P7/8/8/8/8/8/7K w - - 0 1"
                else "7k/8/8/8/8/8/p7/7K b - - 0 1"
            val from = Square('a', if (side == Side.WHITE) 7 else 2)
            val to = Square('a', if (side == Side.WHITE) 8 else 1)
            for ((promotion, type) in types) {
                val position = factory.create(fen)
                val move = Move(from, to, promotion)
                assertTrue(position.getLegalMoves().contains(move))
                assertFalse(position.applyMove(Move(from, to)))
                assertEquals(fen, position.getFen())
                assertTrue(position.applyMove(move))
                assertEquals(Piece(side, type), position.getPiece(to))
                assertNull(position.getPiece(from))
            }
        }
    }
}
