package com.example.chessboard.runtimecontext

/*
 * File role: verifies PGN text import orchestration for game-opening analysis runtime state.
 * Allowed here:
 * - tests for PGN record conversion into runtime import candidates
 * - tests for import summaries and in-memory context updates caused by PGN text import
 * Not allowed here:
 * - Compose rendering, file picker tests, database access, or opening analyzer execution tests
 * Validation date: 2026-06-29
 */

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameOpeningAnalysisImportTest {
    // Checks that import parallelism uses half of available processors, rounded up, with a minimum of one.
    @Test
    fun `resolveGameOpeningAnalysisImportParallelism uses half of available processors rounded up`() {
        assertEquals(1, resolveGameOpeningAnalysisImportParallelism(availableProcessors = 0))
        assertEquals(1, resolveGameOpeningAnalysisImportParallelism(availableProcessors = 1))
        assertEquals(1, resolveGameOpeningAnalysisImportParallelism(availableProcessors = 2))
        assertEquals(2, resolveGameOpeningAnalysisImportParallelism(availableProcessors = 3))
        assertEquals(2, resolveGameOpeningAnalysisImportParallelism(availableProcessors = 4))
        assertEquals(3, resolveGameOpeningAnalysisImportParallelism(availableProcessors = 5))
        assertEquals(3, resolveGameOpeningAnalysisImportParallelism(availableProcessors = 6))
    }

    // Checks that every valid PGN record is imported as one runtime game in source order.
    @Test
    fun `importGameOpeningAnalysisPgnText imports valid PGN records`() {
        val context = GameOpeningAnalysisRuntimeContext()
        val pgn =
            """
            [Event "Game 1"]
            [White "Alice"]
            [Black "Bob"]
            [Result "1-0"]

            1. e4 e5 2. Nf3 Nc6 *
            [Event "Game 2"]
            [White "Carol"]
            [Black "Dave"]
            [Result "0-1"]

            1. d4 Nf6 2. c4 e6 *
            """.trimIndent()

        val summary = importGameOpeningAnalysisPgnText(pgnText = pgn, runtimeContext = context)

        assertEquals(ImportGamesSummary(scannedCount = 2, addedCount = 2, skippedDuplicateCount = 0, skippedParseErrorCount = 0), summary)
        assertEquals(listOf("Game 1", "Game 2"), context.importedGames.map { game -> game.headers["Event"] })
        assertEquals(listOf(0, 1), context.importedGames.map { game -> game.sourceIndex })
        assertEquals(
            listOf("e2e4", "e7e5", "g1f3", "b8c6"),
            context.importedGames[0].mainLineMoves,
        )
        assertEquals(
            listOf("d2d4", "g8f6", "c2c4", "e7e6"),
            context.importedGames[1].mainLineMoves,
        )
    }

    // Checks that pasted move text without PGN headers is still treated as one game.
    @Test
    fun `importGameOpeningAnalysisPgnText imports one headerless PGN record`() {
        val context = GameOpeningAnalysisRuntimeContext()

        val summary =
            importGameOpeningAnalysisPgnText(
                pgnText = "1. e4 e5 2. Nf3 Nc6 *",
                runtimeContext = context,
            )

        assertEquals(ImportGamesSummary(scannedCount = 1, addedCount = 1, skippedDuplicateCount = 0, skippedParseErrorCount = 0), summary)
        assertTrue(
            context.importedGames
                .single()
                .headers
                .isEmpty(),
        )
        assertEquals(listOf("e2e4", "e7e5", "g1f3", "b8c6"), context.importedGames.single().mainLineMoves)
    }

    // Checks that duplicate main lines are reported by the runtime context summary.
    @Test
    fun `importGameOpeningAnalysisPgnText reports duplicate games`() {
        val context = GameOpeningAnalysisRuntimeContext()
        val pgn =
            """
            [Event "Original"]
            [Result "*"]

            1. e4 e5 *
            [Event "Duplicate"]
            [Result "*"]

            1. e4 e5 *
            """.trimIndent()

        val summary = importGameOpeningAnalysisPgnText(pgnText = pgn, runtimeContext = context)

        assertEquals(ImportGamesSummary(scannedCount = 2, addedCount = 1, skippedDuplicateCount = 1, skippedParseErrorCount = 0), summary)
        assertEquals(1, context.importedGames.size)
        assertEquals("Original", context.importedGames.single().headers["Event"])
    }

    // Checks that invalid and empty records are counted as parse errors while valid records still import.
    @Test
    fun `importGameOpeningAnalysisPgnText reports parse errors and imports valid records`() {
        val context = GameOpeningAnalysisRuntimeContext()
        val pgn =
            """
            [Event "Invalid"]
            [Result "*"]

            1. e4 e5 2. Qa5 *
            [Event "Empty"]
            [Result "*"]

            *
            [Event "Valid"]
            [Result "*"]

            1. d4 d5 *
            """.trimIndent()

        val summary = importGameOpeningAnalysisPgnText(pgnText = pgn, runtimeContext = context)

        assertEquals(ImportGamesSummary(scannedCount = 3, addedCount = 1, skippedDuplicateCount = 0, skippedParseErrorCount = 2), summary)
        assertEquals(listOf("Valid"), context.importedGames.map { game -> game.headers["Event"] })
        assertEquals(listOf("d2d4", "d7d5"), context.importedGames.single().mainLineMoves)
    }

    // Checks candidate conversion without mutating runtime state.
    @Test
    fun `parseGameOpeningAnalysisPgnCandidates returns parsed and failed candidates`() {
        val pgn =
            """
            [Event "Valid"]
            [Result "*"]

            1. c4 e5 *
            [Event "Invalid"]
            [Result "*"]

            1. e4 e5 2. Qa5 *
            """.trimIndent()

        val candidates = parseGameOpeningAnalysisPgnCandidates(pgn)

        assertEquals(2, candidates.size)
        val parsedCandidate = candidates[0] as ImportedGameCandidate.Parsed
        assertEquals("Valid", parsedCandidate.game.headers["Event"])
        assertEquals(listOf("c2c4", "e7e5"), parsedCandidate.game.mainLineMoves)
        assertEquals(ImportedGameCandidate.ParseError, candidates[1])
    }

    // Checks that the cancellable parser reports progress without depending on parallel completion order.
    @Test
    fun `parseGameOpeningAnalysisPgnCandidatesWithProgress reports progress`() = runBlocking {
        val pgn =
            """
            [Event "First"]
            [Result "*"]

            1. e4 e5 *
            [Event "Second"]
            [Result "*"]

            1. d4 d5 *
            """.trimIndent()
        val progressUpdates = mutableListOf<Pair<Int, Int>>()

        val candidates =
            parseGameOpeningAnalysisPgnCandidatesWithProgress(
                pgnText = pgn,
                parallelism = 2,
                onProgress = { processedCount, totalCount ->
                    progressUpdates.add(processedCount to totalCount)
                },
            )

        assertEquals(2, candidates.size)
        assertEquals(0 to 2, progressUpdates.first())
        assertEquals(2 to 2, progressUpdates.last())
        assertEquals(setOf(0, 1, 2), progressUpdates.map { progress -> progress.first }.toSet())
        assertTrue(progressUpdates.all { progress -> progress.second == 2 })
        Unit
    }

    // Checks that parallel record parsing returns candidates in source PGN order.
    @Test
    fun `parseGameOpeningAnalysisPgnCandidatesWithProgress preserves source order when parallel`() = runBlocking {
        val pgn =
            """
            [Event "Game 1"]
            [Result "*"]

            1. e4 e5 *
            [Event "Game 2"]
            [Result "*"]

            1. d4 d5 *
            [Event "Game 3"]
            [Result "*"]

            1. c4 e5 *
            [Event "Game 4"]
            [Result "*"]

            1. Nf3 d5 *
            """.trimIndent()

        val candidates =
            parseGameOpeningAnalysisPgnCandidatesWithProgress(
                pgnText = pgn,
                parallelism = 2,
                onProgress = { _, _ -> },
            )

        val events =
            candidates.map { candidate ->
                val parsed = candidate as ImportedGameCandidate.Parsed
                parsed.game.headers["Event"]
            }

        assertEquals(listOf("Game 1", "Game 2", "Game 3", "Game 4"), events)
        Unit
    }
}
