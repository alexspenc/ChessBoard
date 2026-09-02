package com.example.chessboard.service

/*
 * File role: verifies pure preparation of parsed FEN-position continuation batches.
 * Allowed here:
 * - exact duplicate, directional prefix, statistics, and result-order scenarios
 * Not allowed here:
 * - PGN parsing, Room integration, Compose UI, or continuation insertion
 * Validation date: 2026-09-02
 */

import org.junit.Assert.assertEquals
import org.junit.Test

class FenPositionContinuationImportTest {
    @Test
    fun `removes exact duplicate and keeps first occurrence`() {
        val line = listOf("e2e4", "e7e5")

        val result = prepareFenPositionContinuationBatch(
            parsedUciLines = listOf(line, line),
        )

        assertEquals(listOf(line), result.preparedUciLines)
        assertEquals(2, result.sourceLinesCount)
        assertEquals(1, result.exactDuplicateLinesCount)
        assertEquals(0, result.coveredPrefixLinesCount)
    }

    @Test
    fun `removes short line when it precedes longer line`() {
        val shortLine = listOf("e2e4", "e7e5")
        val longLine = shortLine + listOf("g1f3")

        val result = prepareFenPositionContinuationBatch(
            parsedUciLines = listOf(shortLine, longLine),
        )

        assertEquals(listOf(longLine), result.preparedUciLines)
        assertEquals(0, result.exactDuplicateLinesCount)
        assertEquals(1, result.coveredPrefixLinesCount)
    }

    @Test
    fun `removes short line when it follows longer line`() {
        val shortLine = listOf("e2e4", "e7e5")
        val longLine = shortLine + listOf("g1f3")

        val result = prepareFenPositionContinuationBatch(
            parsedUciLines = listOf(longLine, shortLine),
        )

        assertEquals(listOf(longLine), result.preparedUciLines)
        assertEquals(0, result.exactDuplicateLinesCount)
        assertEquals(1, result.coveredPrefixLinesCount)
    }

    @Test
    fun `keeps lines that diverge after common moves`() {
        val kingPawnLine = listOf("e2e4", "e7e5", "g1f3")
        val sicilianLine = listOf("e2e4", "c7c5", "g1f3")

        val result = prepareFenPositionContinuationBatch(
            parsedUciLines = listOf(kingPawnLine, sicilianLine),
        )

        assertEquals(listOf(kingPawnLine, sicilianLine), result.preparedUciLines)
        assertEquals(0, result.exactDuplicateLinesCount)
        assertEquals(0, result.coveredPrefixLinesCount)
    }

    @Test
    fun `removes common short prefix and keeps every longer branch`() {
        val commonPrefix = listOf("e2e4")
        val kingPawnLine = commonPrefix + listOf("e7e5")
        val sicilianLine = commonPrefix + listOf("c7c5")

        val result = prepareFenPositionContinuationBatch(
            parsedUciLines = listOf(commonPrefix, kingPawnLine, sicilianLine),
        )

        assertEquals(listOf(kingPawnLine, sicilianLine), result.preparedUciLines)
        assertEquals(1, result.coveredPrefixLinesCount)
    }

    @Test
    fun `preserves source order of surviving lines`() {
        val firstLine = listOf("d2d4", "d7d5")
        val coveredLine = listOf("e2e4")
        val secondLine = listOf("c2c4", "e7e5")
        val longerLine = coveredLine + listOf("c7c5")

        val result = prepareFenPositionContinuationBatch(
            parsedUciLines = listOf(firstLine, coveredLine, secondLine, longerLine),
        )

        assertEquals(
            listOf(firstLine, secondLine, longerLine),
            result.preparedUciLines,
        )
    }
}
