package com.example.chessboard.boardmodel

/**
 * File role: verifies board-controller replay behavior for special move cases.
 * Allowed here:
 * - pure JVM tests for LineController move replay, FEN, and move history behavior
 * Not allowed here:
 * - Compose UI assertions, Room persistence checks, or import-parser behavior
 * Validation date: 2026-06-16
 */

import org.junit.Assert.assertEquals
import org.junit.Test

class LineControllerPromotionTest {

    @Test
    fun `loadFromUciMoves replays pawn promotion`() {
        val lineController = LineController()

        lineController.loadFromUciMoves(PromotionUciMoves, targetPly = PromotionUciMoves.size)

        assertEquals(
            normalizeFenForAssertion(AfterPromotionFen),
            normalizeFenForAssertion(lineController.getFen()),
        )
    }

    private companion object {
        fun normalizeFenForAssertion(fen: String): String {
            return fen.split(" ").take(4).joinToString(" ")
        }

        val PromotionUciMoves = listOf(
            "e2e4",
            "c7c5",
            "e4e5",
            "d7d6",
            "e5e6",
            "b8c6",
            "e6f7",
            "e8d7",
            "f7g8q",
        )
        const val AfterPromotionFen = "r1bq1bQr/pp1kp1pp/2np4/2p5/8/8/PPPP1PPP/RNBQKBNR b KQ - 0 5"
    }
}
