package com.example.chessboard.service

/*
 * File role: prepares parsed UCI continuations for persistence in the FEN positions feature.
 * Allowed here:
 * - exact duplicate removal and directional prefix coverage inside one parsed batch
 * - comparison of a prepared batch with already stored continuations
 * - preparation statistics that describe removed continuation lines
 * Not allowed here:
 * - PGN parsing, Room access, Compose state, or continuation insertion
 * Validation date: 2026-09-02
 */

data class FenPositionContinuationBatchPreparation(
    val preparedUciLines: List<List<String>>,
    val sourceLinesCount: Int,
    val exactDuplicateLinesCount: Int,
    val coveredPrefixLinesCount: Int,
)

data class FenPositionContinuationStoredComparison(
    val uciLinesToInsert: List<List<String>>,
    val coveredByStoredLinesCount: Int,
)

fun prepareFenPositionContinuationBatch(
    parsedUciLines: List<List<String>>,
): FenPositionContinuationBatchPreparation {
    val distinctLines = mutableListOf<List<String>>()
    var exactDuplicateLinesCount = 0

    for (line in parsedUciLines) {
        if (distinctLines.contains(line)) {
            exactDuplicateLinesCount++
            continue
        }

        distinctLines += line.toList()
    }

    val preparedLines = distinctLines.filterNot { candidate ->
        distinctLines.any { otherLine -> candidate.isStrictPrefixOf(otherLine) }
    }

    return FenPositionContinuationBatchPreparation(
        preparedUciLines = preparedLines,
        sourceLinesCount = parsedUciLines.size,
        exactDuplicateLinesCount = exactDuplicateLinesCount,
        coveredPrefixLinesCount = distinctLines.size - preparedLines.size,
    )
}

fun compareFenPositionContinuationBatchWithStoredLines(
    preparation: FenPositionContinuationBatchPreparation,
    storedUciLines: List<List<String>>,
): FenPositionContinuationStoredComparison {
    val linesToInsert = preparation.preparedUciLines.filterNot { candidate ->
        storedUciLines.any { storedLine -> candidate.isPrefixOf(storedLine) }
    }

    return FenPositionContinuationStoredComparison(
        uciLinesToInsert = linesToInsert,
        coveredByStoredLinesCount = preparation.preparedUciLines.size - linesToInsert.size,
    )
}

private fun List<String>.isStrictPrefixOf(otherLine: List<String>): Boolean {
    if (size >= otherLine.size) {
        return false
    }

    return otherLine.take(size) == this
}

private fun List<String>.isPrefixOf(otherLine: List<String>): Boolean {
    if (size > otherLine.size) {
        return false
    }

    return otherLine.take(size) == this
}
