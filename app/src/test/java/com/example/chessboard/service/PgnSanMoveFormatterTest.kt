package com.example.chessboard.service

/*
 * File role: verifies shared SAN formatting for individual moves and complete UCI lines.
 * Allowed here:
 * - SAN notation details, line replay, explicit start FEN, and invalid-prefix behavior
 * Not allowed here:
 * - PGN parsing, Room integration, Compose rendering, or app navigation
 * Validation date: 2026-09-02
 */

import com.github.bhlangonijr.chesslib.Board
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PgnSanMoveFormatterTest {
    @Test
    fun `resolvePgnSanMove formats pawn and knight moves`() {
        val board = Board()

        val pawnMove = resolvePgnSanMove(uciMove = "e2e4", board = board)
        assertEquals("e4", pawnMove.san)
        board.doMove(pawnMove.move)
        val blackMove = resolvePgnSanMove(uciMove = "e7e5", board = board)
        board.doMove(blackMove.move)

        assertEquals("Nf3", resolvePgnSanMove(uciMove = "g1f3", board = board).san)
    }

    @Test
    fun `resolvePgnSanMove formats ordinary and en passant captures`() {
        assertEquals(
            "exd5",
            resolveSan(
                uciMove = "e4d5",
                fen = "rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2",
            ),
        )
        assertEquals(
            "exd6",
            resolveSan(
                uciMove = "e5d6",
                fen = "rnbqkbnr/1pp1pppp/p7/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 3",
            ),
        )
    }

    @Test
    fun `resolvePgnSanMove formats both castling sides`() {
        val fen = "r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq - 0 1"

        assertEquals("O-O", resolveSan(uciMove = "e1g1", fen = fen))
        assertEquals("O-O-O", resolveSan(uciMove = "e1c1", fen = fen))
    }

    @Test
    fun `resolvePgnSanMove appends check and checkmate suffixes`() {
        val check = resolveSan(
            uciMove = "a1a8",
            fen = "4k3/8/8/8/8/8/8/Q3K3 w - - 0 1",
        )
        val checkmate = resolveSan(
            uciMove = "h5f7",
            fen = "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4",
        )

        assertTrue("Expected check suffix, got: $check", check.endsWith("+"))
        assertTrue("Expected checkmate suffix, got: $checkmate", checkmate.endsWith("#"))
    }

    @Test
    fun `resolvePgnSanMove includes promotion piece`() {
        val san = resolveSan(
            uciMove = "a7a8q",
            fen = "7k/P7/8/8/8/8/8/7K w - - 0 1",
        )

        assertTrue("Expected =Q in label, got: $san", san.contains("=Q"))
    }

    @Test
    fun `resolvePgnSanMove applies file rank and square disambiguation`() {
        assertEquals(
            "Nbd2",
            resolveSan(
                uciMove = "b1d2",
                fen = "7k/8/8/8/8/5N2/8/1N5K w - - 0 1",
            ),
        )
        assertEquals(
            "R1a2",
            resolveSan(
                uciMove = "a1a2",
                fen = "8/7k/8/8/8/R7/8/R6K w - - 0 1",
            ),
        )
        assertEquals(
            "Qb2d4",
            resolveSan(
                uciMove = "b2d4",
                fen = "8/7k/8/8/1Q6/8/1Q1Q4/7K w - - 0 1",
            ),
        )
    }

    @Test
    fun `buildMoveLabels returns correct SAN for standard line`() {
        val uciMoves = listOf(
            "d2d4",
            "d7d5",
            "g1f3",
            "g8f6",
            "e2e3",
            "e7e6",
            "f1d3",
            "f8e7",
        )

        assertEquals(
            listOf("d4", "d5", "Nf3", "Nf6", "e3", "e6", "Bd3", "Be7"),
            buildMoveLabels(uciMoves),
        )
    }

    @Test
    fun `buildMoveLabels adds SAN disambiguation`() {
        val uciMoves = listOf(
            "d2d4",
            "d7d5",
            "g1f3",
            "c8g4",
            "b1d2",
        )

        assertEquals(
            listOf("d4", "d5", "Nf3", "Bg4", "Nbd2"),
            buildMoveLabels(uciMoves),
        )
    }

    @Test
    fun `buildMoveLabels starts from four field FEN with black to move`() {
        val startFen =
            "rnbqkbnr/ppp1pppp/8/3p4/8/5NP1/PPPPPP1P/RNBQKB1R b KQkq -"

        val labels = buildMoveLabels(
            uciMoves = listOf("g8f6", "f1g2", "g7g6"),
            startFen = startFen,
        )

        assertEquals(listOf("Nf6", "Bg2", "g6"), labels)
    }

    @Test
    fun `buildMoveLabels accepts six field start FEN`() {
        val labels = buildMoveLabels(
            uciMoves = listOf("g8f6", "f1g2"),
            startFen =
                "rnbqkbnr/ppp1pppp/8/3p4/8/5NP1/PPPPPP1P/RNBQKB1R b KQkq - 17 23",
        )

        assertEquals(listOf("Nf6", "Bg2"), labels)
    }

    @Test
    fun `buildMoveLabels stops at first invalid move`() {
        val labels = buildMoveLabels(
            listOf("e2e4", "not-a-move", "g1f3"),
        )

        assertEquals(listOf("e4"), labels)
    }

    @Test
    fun `buildMoveLabels rejects invalid start FEN`() {
        assertThrows(IllegalArgumentException::class.java) {
            buildMoveLabels(
                uciMoves = listOf("e2e4"),
                startFen = "invalid FEN",
            )
        }
    }

    private fun resolveSan(
        uciMove: String,
        fen: String,
    ): String {
        val board = Board().also { board -> board.loadFromFen(fen) }
        return resolvePgnSanMove(
            uciMove = uciMove,
            board = board,
        ).san
    }
}
