package com.example.chessboard.service

/*
 * File role: verifies pure preparation of parsed FEN-position continuation batches.
 * Allowed here:
 * - exact duplicate, directional prefix, stored-line comparison, statistics, and result-order scenarios
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

    @Test
    fun `stored identical line covers prepared line`() {
        val line = listOf("e2e4", "e7e5")
        val preparation = prepareFenPositionContinuationBatch(
            parsedUciLines = listOf(line),
        )

        val result = compareFenPositionContinuationBatchWithStoredLines(
            preparation = preparation,
            storedUciLines = listOf(line),
        )

        assertEquals(emptyList<List<String>>(), result.uciLinesToInsert)
        assertEquals(1, result.coveredByStoredLinesCount)
    }

    @Test
    fun `stored longer line covers prepared short prefix`() {
        val shortLine = listOf("e2e4", "e7e5")
        val longLine = shortLine + listOf("g1f3")
        val preparation = prepareFenPositionContinuationBatch(
            parsedUciLines = listOf(shortLine),
        )

        val result = compareFenPositionContinuationBatchWithStoredLines(
            preparation = preparation,
            storedUciLines = listOf(longLine),
        )

        assertEquals(emptyList<List<String>>(), result.uciLinesToInsert)
        assertEquals(1, result.coveredByStoredLinesCount)
    }

    @Test
    fun `stored short line does not cover prepared longer line`() {
        val shortLine = listOf("e2e4", "e7e5")
        val longLine = shortLine + listOf("g1f3")
        val preparation = prepareFenPositionContinuationBatch(
            parsedUciLines = listOf(longLine),
        )

        val result = compareFenPositionContinuationBatchWithStoredLines(
            preparation = preparation,
            storedUciLines = listOf(shortLine),
        )

        assertEquals(listOf(longLine), result.uciLinesToInsert)
        assertEquals(0, result.coveredByStoredLinesCount)
    }

    @Test
    fun `stored divergent line does not cover prepared line`() {
        val preparedLine = listOf("e2e4", "e7e5", "g1f3")
        val storedLine = listOf("e2e4", "c7c5", "g1f3")
        val preparation = prepareFenPositionContinuationBatch(
            parsedUciLines = listOf(preparedLine),
        )

        val result = compareFenPositionContinuationBatchWithStoredLines(
            preparation = preparation,
            storedUciLines = listOf(storedLine),
        )

        assertEquals(listOf(preparedLine), result.uciLinesToInsert)
        assertEquals(0, result.coveredByStoredLinesCount)
    }

    @Test
    fun `stored comparison preserves order and counts every covered prepared line`() {
        val exactLine = listOf("e2e4", "e7e5")
        val coveredPrefix = listOf("d2d4")
        val storedLongLine = coveredPrefix + listOf("d7d5")
        val newFirstLine = listOf("c2c4", "e7e5")
        val newSecondLine = listOf("g1f3", "d7d5")
        val preparation = prepareFenPositionContinuationBatch(
            parsedUciLines = listOf(exactLine, newFirstLine, coveredPrefix, newSecondLine),
        )

        val result = compareFenPositionContinuationBatchWithStoredLines(
            preparation = preparation,
            storedUciLines = listOf(exactLine, storedLongLine),
        )

        assertEquals(listOf(newFirstLine, newSecondLine), result.uciLinesToInsert)
        assertEquals(2, result.coveredByStoredLinesCount)
    }
}
