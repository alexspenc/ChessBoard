package com.example.chessboard.service

/*
 * Unit tests for pure dubious-line filtering used by the lines explorer.
 *
 * Keep only marker/name matching coverage here. Do not add Room integration
 * or Compose search-dialog behavior to this file.
 *
 * Validation date: 2026-05-27
 */

import com.example.chessboard.entity.DubiousLineEntity
import com.example.chessboard.entity.LineEntity
import com.example.chessboard.entity.SideMask
import org.junit.Assert.assertEquals
import org.junit.Test

class DubiousLineFilterTest {

    @Test
    fun `empty query returns all existing dubious lines in marker order`() {
        val lineIds = filterDubiousLineIdsByName(
            dubiousLines = listOf(
                dubiousLine(lineId = 3L),
                dubiousLine(lineId = 99L),
                dubiousLine(lineId = 1L),
            ),
            lines = listOf(
                line(id = 1L, name = "Caro-Kann"),
                line(id = 3L, name = null),
            ),
            query = "",
            isCaseSensitive = false,
        )

        assertEquals(listOf(3L, 1L), lineIds)
    }

    @Test
    fun `query filters dubious lines by name case-insensitively`() {
        val lineIds = filterDubiousLineIdsByName(
            dubiousLines = listOf(
                dubiousLine(lineId = 1L),
                dubiousLine(lineId = 2L),
                dubiousLine(lineId = 3L),
            ),
            lines = listOf(
                line(id = 1L, name = "Sicilian Najdorf"),
                line(id = 2L, name = "French Defense"),
                line(id = 3L, name = "Anti-Sicilian"),
            ),
            query = "sicilian",
            isCaseSensitive = false,
        )

        assertEquals(listOf(1L, 3L), lineIds)
    }

    @Test
    fun `case-sensitive query keeps only exact case matches`() {
        val lineIds = filterDubiousLineIdsByName(
            dubiousLines = listOf(
                dubiousLine(lineId = 1L),
                dubiousLine(lineId = 2L),
            ),
            lines = listOf(
                line(id = 1L, name = "Queen Gambit"),
                line(id = 2L, name = "queen pawn"),
            ),
            query = "Queen",
            isCaseSensitive = true,
        )

        assertEquals(listOf(1L), lineIds)
    }

    private fun dubiousLine(lineId: Long): DubiousLineEntity {
        return DubiousLineEntity(
            lineId = lineId,
            weight = 1,
        )
    }

    @Test
    fun `side filter keeps matching side and both-side dubious lines`() {
        val lineIds = filterDubiousLineIdsByName(
            dubiousLines = listOf(
                dubiousLine(lineId = 1L),
                dubiousLine(lineId = 2L),
                dubiousLine(lineId = 3L),
            ),
            lines = listOf(
                line(id = 1L, name = "White line", sideMask = SideMask.WHITE),
                line(id = 2L, name = "Both line", sideMask = SideMask.BOTH),
                line(id = 3L, name = "Black line", sideMask = SideMask.BLACK),
            ),
            query = "",
            isCaseSensitive = false,
            sideMask = SideMask.WHITE,
        )

        assertEquals(listOf(1L, 2L), lineIds)
    }

    private fun line(
        id: Long,
        name: String?,
        sideMask: Int = SideMask.BOTH,
    ): LineEntity {
        return LineEntity(
            id = id,
            event = name,
            pgn = "",
            initialFen = "",
            sideMask = sideMask,
        )
    }
}
