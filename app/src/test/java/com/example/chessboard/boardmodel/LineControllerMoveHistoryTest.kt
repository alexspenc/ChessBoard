package com.example.chessboard.boardmodel

/**
 * Verifies narrow access to the move applied at the current LineController position.
 * Keep move-history cursor behavior here. Do not add UI, animation, or persistence tests.
 * Validation date: 2026-07-26
 */

import com.github.bhlangonijr.chesslib.move.Move
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LineControllerMoveHistoryTest {

    @Test
    fun getLastAppliedMove_returnsNullAtInitialPosition() {
        val lineController = LineController()

        assertNull(lineController.getLastAppliedMove())
    }

    @Test
    fun getLastAppliedMove_returnsMoveBeforeCurrentPosition() {
        val lineController = LineController()
        lineController.loadFromUciMoves(
            uciMoves = TestMoves,
            targetPly = 1,
        )

        val lastAppliedMove = lineController.getLastAppliedMove()

        assertEquals("e2e4", buildUciOrNull(lastAppliedMove))
    }

    @Test
    fun getLastAppliedMove_tracksUndoPosition() {
        val lineController = LineController()
        lineController.loadFromUciMoves(
            uciMoves = TestMoves,
            targetPly = TestMoves.size,
        )

        lineController.undoMove()
        val lastAppliedMove = lineController.getLastAppliedMove()

        assertEquals("e2e4", buildUciOrNull(lastAppliedMove))
    }

    private fun buildUciOrNull(move: Move?): String? {
        if (move == null) {
            return null
        }

        return buildUciFromChesslibMove(move)
    }

    private companion object {
        val TestMoves = listOf("e2e4", "e7e5")
    }
}
