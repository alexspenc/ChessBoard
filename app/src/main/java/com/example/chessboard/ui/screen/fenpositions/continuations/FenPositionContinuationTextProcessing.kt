package com.example.chessboard.ui.screen.fenpositions.continuations

/*
 * File role: transforms pasted PGN/SAN text into new FEN-position continuations.
 * Allowed here:
 * - parsing from the source FEN, batch filtering, stored-line comparison, and SAN previews
 * Not allowed here:
 * - Compose state, debounce timing, Room access, dialog rendering, or persistence mutations
 * Validation date: 2026-09-02
 */

import com.example.chessboard.service.FenPositionContinuationBatchPreparation
import com.example.chessboard.service.PgnParseErrorStrings
import com.example.chessboard.service.compareFenPositionContinuationBatchWithStoredLines
import com.example.chessboard.service.parsePgnToUciLinesPreservingDuplicates
import com.example.chessboard.service.prepareFenPositionContinuationBatch

internal data class FenPositionContinuationTextProcessingResult(
    val newUciLines: List<List<String>>,
    val newSanLines: List<String>,
    val recognizedLinesCount: Int,
    val exactDuplicateLinesCount: Int,
    val coveredPrefixLinesCount: Int,
    val coveredByStoredLinesCount: Int,
)

internal fun parseFenPositionContinuationText(
    text: String,
    startFen: String,
    errorStrings: PgnParseErrorStrings,
    noValidLinesMessage: String,
): List<List<String>> {
    val parsedLines = parsePgnToUciLinesPreservingDuplicates(
        pgnText = text,
        startFen = startFen,
        errorStrings = errorStrings,
    )
    if (parsedLines.isEmpty()) {
        throw IllegalArgumentException(noValidLinesMessage)
    }

    return parsedLines
}

internal fun prepareFenPositionContinuationText(
    parsedUciLines: List<List<String>>,
): FenPositionContinuationBatchPreparation {
    return prepareFenPositionContinuationBatch(parsedUciLines)
}

internal fun buildFenPositionContinuationTextProcessingResult(
    preparation: FenPositionContinuationBatchPreparation,
    storedUciLines: List<List<String>>,
    startFen: String,
): FenPositionContinuationTextProcessingResult {
    val comparison = compareFenPositionContinuationBatchWithStoredLines(
        preparation = preparation,
        storedUciLines = storedUciLines,
    )
    val sanLines = comparison.uciLinesToInsert.map { uciMoves ->
        buildFenPositionContinuationSanLine(
            uciMoves = uciMoves,
            startFen = startFen,
        )
    }

    return FenPositionContinuationTextProcessingResult(
        newUciLines = comparison.uciLinesToInsert,
        newSanLines = sanLines,
        recognizedLinesCount = preparation.sourceLinesCount,
        exactDuplicateLinesCount = preparation.exactDuplicateLinesCount,
        coveredPrefixLinesCount = preparation.coveredPrefixLinesCount,
        coveredByStoredLinesCount = comparison.coveredByStoredLinesCount,
    )
}
