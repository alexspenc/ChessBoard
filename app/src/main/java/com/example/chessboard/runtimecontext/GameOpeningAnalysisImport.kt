package com.example.chessboard.runtimecontext

/*
 * File role: imports PGN text into the in-memory game-opening analysis runtime context.
 * Allowed here:
 * - runtime import orchestration for PGN text used by the game-opening analysis UI
 * - conversion from parsed PGN records into ImportedGameCandidate values
 * - bounded parallel conversion of independent PGN records during import
 * Not allowed here:
 * - Compose UI, file picker code, database access, or opening analysis execution
 * Validation date: 2026-06-26
 */

import com.example.chessboard.concurrency.BoundedParallelTaskRunner
import com.example.chessboard.concurrency.CompletedTask
import com.example.chessboard.service.ParsedPgnGame
import com.example.chessboard.service.PgnRecord
import com.example.chessboard.service.parsePgnMainLineToUci
import com.example.chessboard.service.splitPgnRecords
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
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
@OptIn(ExperimentalCoroutinesApi::class)
suspend fun parseGameOpeningAnalysisPgnCandidatesWithProgress(
    pgnText: String,
    parallelism: Int = resolveGameOpeningAnalysisParallelism(),
    onProgress: suspend (processedCount: Int, totalCount: Int) -> Unit,
): List<ImportedGameCandidate> {
    val records = splitPgnRecords(pgnText)
    onProgress(0, records.size)
    if (records.isEmpty()) {
        return emptyList()
    }

    return coroutineScope {
        val safeParallelism = parallelism.coerceAtLeast(1)
        val runner =
            BoundedParallelTaskRunner<ImportedGameCandidate>(
                parallelism = safeParallelism,
                dispatcher = Dispatchers.Default.limitedParallelism(safeParallelism),
                scope = this,
            )
        val candidates = MutableList<ImportedGameCandidate?>(records.size) { null }
        var nextRecordIndex = 0
        var completedCount = 0

        try {
            while (completedCount < records.size) {
                while (nextRecordIndex < records.size && runner.hasFreeSlot()) {
                    val recordIndex = nextRecordIndex
                    val record = records[recordIndex]
                    val accepted =
                        runner.trySubmit(taskId = recordIndex) {
                            currentCoroutineContext().ensureActive()
                            parseGameOpeningAnalysisPgnCandidate(record)
                        }
                    if (!accepted) {
                        currentCoroutineContext().ensureActive()
                        break
                    }

                    nextRecordIndex++
                }

                when (val completedTask = runner.receiveCompleted()) {
                    is CompletedTask.Success -> {
                        candidates[completedTask.taskId] = completedTask.value
                    }

                    is CompletedTask.Failure -> {
                        candidates[completedTask.taskId] = ImportedGameCandidate.ParseError
                    }
                }

                completedCount++
                onProgress(completedCount, records.size)
            }
        } finally {
            runner.cancelActiveTasks()
        }

        candidates.map { candidate -> candidate ?: ImportedGameCandidate.ParseError }
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
