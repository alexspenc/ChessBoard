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
import com.example.chessboard.service.parsePgnMainLineToUci
import com.example.chessboard.service.splitPgnRecords

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
    return splitPgnRecords(pgnText).map { record ->
        try {
            val mainLineMoves = parsePgnMainLineToUci(record.text)
            if (mainLineMoves.isEmpty()) {
                return@map ImportedGameCandidate.ParseError
            }

            ImportedGameCandidate.Parsed(
                ParsedPgnGame(
                    sourceIndex = record.sourceIndex,
                    headers = record.headers,
                    mainLineMoves = mainLineMoves,
                ),
            )
        } catch (_: IllegalArgumentException) {
            ImportedGameCandidate.ParseError
        }
    }
}
