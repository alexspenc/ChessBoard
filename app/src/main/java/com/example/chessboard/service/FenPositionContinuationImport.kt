package com.example.chessboard.service

/*
 * File role: prepares parsed UCI continuations for persistence in the FEN positions feature.
 * Allowed here:
 * - exact duplicate removal and directional prefix coverage inside one parsed batch
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

private fun List<String>.isStrictPrefixOf(otherLine: List<String>): Boolean {
    if (size >= otherLine.size) {
        return false
    }

    return otherLine.take(size) == this
}
