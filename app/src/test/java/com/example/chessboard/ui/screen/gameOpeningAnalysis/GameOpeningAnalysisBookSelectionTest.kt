package com.example.chessboard.ui.screen.gameOpeningAnalysis

/*
 * File role: verifies screen-level book-line selection before game-opening analysis starts.
 * Allowed here:
 * - JVM tests for side-based filtering of saved opening lines passed into analysis
 * - assertions about how WHITE, BLACK, and BOTH side masks participate in book building
 * Not allowed here:
 * - Compose rendering, database access, or analyzer result semantics
 * Validation date: 2026-07-09
 */

import com.example.chessboard.analysis.OpeningSide
import com.example.chessboard.entity.LineEntity
import com.example.chessboard.entity.SideMask
import org.junit.Assert.assertEquals
import org.junit.Test

class GameOpeningAnalysisBookSelectionTest {

    @Test
    fun `filterGameOpeningAnalysisBookLinesBySide keeps white and both lines for white analysis`() {
        val lines = listOf(
            line(id = 1L, sideMask = SideMask.WHITE),
            line(id = 2L, sideMask = SideMask.BLACK),
            line(id = 3L, sideMask = SideMask.BOTH),
        )

        val filtered = filterGameOpeningAnalysisBookLinesBySide(
            lines = lines,
            selectedSide = OpeningSide.WHITE,
        )

        assertEquals(listOf(1L, 3L), filtered.map { line -> line.id })
    }

    @Test
    fun `filterGameOpeningAnalysisBookLinesBySide keeps black and both lines for black analysis`() {
        val lines = listOf(
            line(id = 1L, sideMask = SideMask.WHITE),
            line(id = 2L, sideMask = SideMask.BLACK),
            line(id = 3L, sideMask = SideMask.BOTH),
        )

        val filtered = filterGameOpeningAnalysisBookLinesBySide(
            lines = lines,
            selectedSide = OpeningSide.BLACK,
        )

        assertEquals(listOf(2L, 3L), filtered.map { line -> line.id })
    }

    private fun line(id: Long, sideMask: Int): LineEntity {
        return LineEntity(
            id = id,
            event = "Line $id",
            eco = "A00",
            pgn = "1. e2e4 *",
            initialFen = "",
            sideMask = sideMask,
        )
    }
}
