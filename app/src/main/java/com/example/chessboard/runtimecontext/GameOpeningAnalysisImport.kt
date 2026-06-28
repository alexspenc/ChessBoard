package com.example.chessboard.runtimecontext

/*
 * File role: imports PGN text into the in-memory game-opening analysis runtime context.
 * Allowed here:
 * - runtime import orchestration for PGN text used by the game-opening analysis UI
 * - conversion from parsed PGN records into ImportedGameCandidate values
 * Not allowed here:
 * - Compose UI, file picker code, database access, or opening analysis execution
 * Validation date: 2026-06-26
 */

import com.example.chessboard.service.ParsedPgnGame
import com.example.chessboard.service.PgnRecord
import com.example.chessboard.service.parsePgnMainLineToUci
import com.example.chessboard.service.splitPgnRecords
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Parses [pgnText] and adds its valid main-line games to [runtimeContext]. */
fun importGameOpeningAnalysisPgnText(
    pgnText: String,
    runtimeContext: GameOpeningAnalysisRuntimeContext,
): ImportGamesSummary {
    val candidates = parseGameOpeningAnalysisPgnCandidates(pgnText)
    return runtimeContext.addImportedGames(candidates)
}

/** Converts each PGN record into a parsed game candidate or a parse-error candidate. */
fun parseGameOpeningAnalysisPgnCandidates(pgnText: String): List<ImportedGameCandidate> {
    return splitPgnRecords(pgnText).map(::parseGameOpeningAnalysisPgnCandidate)
}

/** Converts PGN records into import candidates while reporting progress and honoring coroutine cancellation. */
suspend fun parseGameOpeningAnalysisPgnCandidatesWithProgress(
    pgnText: String,
    onProgress: suspend (processedCount: Int, totalCount: Int) -> Unit,
): List<ImportedGameCandidate> {
    val records = splitPgnRecords(pgnText)
    onProgress(0, records.size)

    return records.mapIndexed { index, record ->
        currentCoroutineContext().ensureActive()
        val candidate = parseGameOpeningAnalysisPgnCandidate(record)

        onProgress(index + 1, records.size)
        candidate
    }
}

private fun parseGameOpeningAnalysisPgnCandidate(record: PgnRecord): ImportedGameCandidate {
    try {
        val mainLineMoves = parsePgnMainLineToUci(record.text)
        if (mainLineMoves.isEmpty()) {
            return ImportedGameCandidate.ParseError
        }

        return ImportedGameCandidate.Parsed(
            ParsedPgnGame(
                sourceIndex = record.sourceIndex,
                headers = record.headers,
                mainLineMoves = mainLineMoves,
            ),
        )
    } catch (_: IllegalArgumentException) {
        return ImportedGameCandidate.ParseError
    }
}
