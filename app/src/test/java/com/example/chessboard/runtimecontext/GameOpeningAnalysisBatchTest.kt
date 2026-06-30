package com.example.chessboard.runtimecontext

/*
 * File role: verifies batch analysis orchestration for imported game-opening analysis games.
 * Allowed here:
 * - tests for runtime batch flow, game filtering, result filtering, progress, and cancellation
 * - thin adapter tests that call the pure GameOpeningAnalyzer with in-memory line records
 * Not allowed here:
 * - Compose rendering, database access, file picker tests, or PGN import parsing tests
 * Validation date: 2026-06-26
 */

import com.example.chessboard.analysis.GameOpeningDeviation
import com.example.chessboard.analysis.GameOpeningInvalidGameMove
import com.example.chessboard.analysis.GameOpeningInvalidInitialPosition
import com.example.chessboard.analysis.GameOpeningMatchesKnownOpening
import com.example.chessboard.analysis.GameOpeningNoMatchingOpening
import com.example.chessboard.analysis.OpeningMatchMode
import com.example.chessboard.analysis.OpeningSide
import com.example.chessboard.boardmodel.InitialBoardFen
import com.example.chessboard.entity.LineEntity
import com.example.chessboard.service.ParsedPgnGame
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class GameOpeningAnalysisBatchTest {
    // Checks that only games passing the current runtime filter are analyzed in import order.
    @Test
    fun `analyzeImportedGameOpenings analyzes filtered games in import order`() {
        val context = GameOpeningAnalysisRuntimeContext()
        context.addImportedGames(
            listOf(
                parsedCandidate(sourceIndex = 0, white = "Alice", moves = listOf("a")),
                parsedCandidate(sourceIndex = 1, white = "Bob", moves = listOf("b")),
                parsedCandidate(sourceIndex = 2, white = "Alice", moves = listOf("c")),
            ),
        )
        context.updateFilter(GameOpeningAnalysisFilter(playerNameQuery = "Alice"))
        val analyzedGameIds = mutableListOf<Long>()

        val summary =
            analyzeImportedGameOpenings(
                runtimeContext = context,
                options = GameOpeningAnalysisOptions(),
                analyzeGame = { input ->
                    analyzedGameIds.add(input.game.id)
                    noMatchingOpeningResult(selectedSide = input.selectedSide)
                },
            )

        assertEquals(GameOpeningBatchAnalysisSummary(analyzedCount = 2, keptResultCount = 2, wasCancelled = false), summary)
        assertEquals(listOf(1L, 3L), analyzedGameIds)
        assertEquals(listOf(1L, 3L), context.analysisResults.map { result -> result.gameId })
        assertNull(context.analysisProgress)
    }

    // Checks that the selected side used for analysis comes from the current runtime filter.
    @Test
    fun `analyzeImportedGameOpenings passes selected side from filter`() {
        val context = GameOpeningAnalysisRuntimeContext()
        context.addImportedGames(listOf(parsedCandidate(sourceIndex = 0, moves = listOf("a"))))
        context.updateFilter(GameOpeningAnalysisFilter(side = OpeningSide.BLACK))
        var receivedSide: OpeningSide? = null

        analyzeImportedGameOpenings(
            runtimeContext = context,
            options = GameOpeningAnalysisOptions(),
            analyzeGame = { input ->
                receivedSide = input.selectedSide
                noMatchingOpeningResult(selectedSide = input.selectedSide)
            },
        )

        assertEquals(OpeningSide.BLACK, receivedSide)
        assertEquals(
            OpeningSide.BLACK,
            context.analysisResults
                .single()
                .result.selectedSide,
        )
    }

    // Checks that result filters keep only selected result types.
    @Test
    fun `analyzeImportedGameOpenings keeps only enabled result filters`() {
        val context = GameOpeningAnalysisRuntimeContext()
        context.addImportedGames(
            listOf(
                parsedCandidate(sourceIndex = 0, moves = listOf("a")),
                parsedCandidate(sourceIndex = 1, moves = listOf("b")),
            ),
        )
        val options =
            GameOpeningAnalysisOptions(
                resultTypes = setOf(GameOpeningAnalysisOptions.ResultFilter.DEVIATION),
            )

        val summary =
            analyzeImportedGameOpenings(
                runtimeContext = context,
                options = options,
                analyzeGame = { input ->
                    if (input.game.id == 1L) {
                        deviationResult()
                    } else {
                        noMatchingOpeningResult()
                    }
                },
            )

        assertEquals(GameOpeningBatchAnalysisSummary(analyzedCount = 2, keptResultCount = 1, wasCancelled = false), summary)
        assertEquals(listOf(1L), context.analysisResults.map { result -> result.gameId })
        assertTrue(context.analysisResults.single().result is GameOpeningDeviation)
    }

    // Checks that default options do not keep MatchesKnownOpening results.
    @Test
    fun `analyzeImportedGameOpenings default options exclude matches known opening`() {
        val context = GameOpeningAnalysisRuntimeContext()
        context.addImportedGames(listOf(parsedCandidate(sourceIndex = 0, moves = listOf("a"))))

        val summary =
            analyzeImportedGameOpenings(
                runtimeContext = context,
                options = GameOpeningAnalysisOptions(),
                analyzeGame = { matchesKnownOpeningResult() },
            )

        assertEquals(GameOpeningBatchAnalysisSummary(analyzedCount = 1, keptResultCount = 0, wasCancelled = false), summary)
        assertTrue(context.analysisResults.isEmpty())
        assertNull(context.analysisProgress)
    }

    // Checks that invalid move and invalid initial position both use the INVALID_GAMES result filter.
    @Test
    fun `analyzeImportedGameOpenings maps invalid results to invalid games filter`() {
        val context = GameOpeningAnalysisRuntimeContext()
        context.addImportedGames(
            listOf(
                parsedCandidate(sourceIndex = 0, moves = listOf("a")),
                parsedCandidate(sourceIndex = 1, moves = listOf("b")),
            ),
        )
        val options =
            GameOpeningAnalysisOptions(
                resultTypes = setOf(GameOpeningAnalysisOptions.ResultFilter.INVALID_GAMES),
            )

        val summary =
            analyzeImportedGameOpenings(
                runtimeContext = context,
                options = options,
                analyzeGame = { input ->
                    if (input.game.id == 1L) {
                        invalidMoveResult()
                    } else {
                        invalidInitialPositionResult()
                    }
                },
            )

        assertEquals(GameOpeningBatchAnalysisSummary(analyzedCount = 2, keptResultCount = 2, wasCancelled = false), summary)
        assertTrue(context.analysisResults[0].result is GameOpeningInvalidGameMove)
        assertTrue(context.analysisResults[1].result is GameOpeningInvalidInitialPosition)
    }

    // Checks that cancellation discards partial results and clears progress.
    @Test
    fun `analyzeImportedGameOpenings cancels without saving partial results`() {
        val context = GameOpeningAnalysisRuntimeContext()
        context.addImportedGames(
            listOf(
                parsedCandidate(sourceIndex = 0, moves = listOf("a")),
                parsedCandidate(sourceIndex = 1, moves = listOf("b")),
            ),
        )
        var cancelCheckCount = 0

        val summary =
            analyzeImportedGameOpenings(
                runtimeContext = context,
                options = GameOpeningAnalysisOptions(),
                analyzeGame = { noMatchingOpeningResult() },
                shouldCancel = {
                    cancelCheckCount++
                    cancelCheckCount == 2
                },
            )

        assertEquals(GameOpeningBatchAnalysisSummary(analyzedCount = 1, keptResultCount = 0, wasCancelled = true), summary)
        assertTrue(context.analysisResults.isEmpty())
        assertNull(context.analysisProgress)
    }

    // Checks that parallel analysis stores results in filtered-game order, not completion order.
    @Test
    fun `analyzeImportedGameOpeningsInParallel preserves result order when tasks finish out of order`() = runBlocking {
        val context = GameOpeningAnalysisRuntimeContext()
        context.addImportedGames(
            listOf(
                parsedCandidate(sourceIndex = 0, moves = listOf("a")),
                parsedCandidate(sourceIndex = 1, moves = listOf("b")),
                parsedCandidate(sourceIndex = 2, moves = listOf("c")),
            ),
        )
        val firstTaskStarted = CompletableDeferred<Unit>()
        val secondTaskFinished = CompletableDeferred<Unit>()
        val allowFirstTaskToFinish = CompletableDeferred<Unit>()

        val summaryDeferred =
            async {
                analyzeImportedGameOpeningsInParallel(
                    runtimeContext = context,
                    options = GameOpeningAnalysisOptions(),
                    analyzeGame = { input ->
                        if (input.game.id == 1L) {
                            firstTaskStarted.complete(Unit)
                            allowFirstTaskToFinish.await()
                        }
                        if (input.game.id == 2L) {
                            secondTaskFinished.complete(Unit)
                        }

                        noMatchingOpeningResult(selectedSide = input.selectedSide)
                    },
                    parallelism = 2,
                    shouldCancel = { false },
                )
            }

        firstTaskStarted.await()
        secondTaskFinished.await()
        allowFirstTaskToFinish.complete(Unit)

        val summary = summaryDeferred.await()

        assertEquals(GameOpeningBatchAnalysisSummary(analyzedCount = 3, keptResultCount = 3, wasCancelled = false), summary)
        assertEquals(listOf(1L, 2L, 3L), context.analysisResults.map { result -> result.gameId })
        assertNull(context.analysisProgress)
        Unit
    }

    // Checks that parallel analysis progress exposes worker count before final results replace progress state.
    @Test
    fun `analyzeImportedGameOpeningsInParallel stores parallelism in active progress`() = runBlocking {
        val context = GameOpeningAnalysisRuntimeContext()
        context.addImportedGames(
            listOf(
                parsedCandidate(sourceIndex = 0, moves = listOf("a")),
                parsedCandidate(sourceIndex = 1, moves = listOf("b")),
            ),
        )
        val activeProgress = CompletableDeferred<GameOpeningAnalysisProgress?>()

        val summary =
            analyzeImportedGameOpeningsInParallel(
                runtimeContext = context,
                options = GameOpeningAnalysisOptions(),
                analyzeGame = { input ->
                    activeProgress.complete(context.analysisProgress)
                    noMatchingOpeningResult(selectedSide = input.selectedSide)
                },
                parallelism = 2,
                shouldCancel = { false },
            )

        assertEquals(
            GameOpeningAnalysisProgress(
                analyzedCount = 0,
                totalCount = 2,
                stage = GameOpeningAnalysisProgress.Stage.ANALYZING_GAMES,
                parallelism = 2,
            ),
            activeProgress.await(),
        )
        assertEquals(GameOpeningBatchAnalysisSummary(analyzedCount = 2, keptResultCount = 2, wasCancelled = false), summary)
        assertNull(context.analysisProgress)
        Unit
    }

    // Checks that parallel analysis cancellation discards already completed partial results.
    @Test
    fun `analyzeImportedGameOpeningsInParallel cancels without saving partial results`() = runBlocking {
        val context = GameOpeningAnalysisRuntimeContext()
        context.addImportedGames(
            listOf(
                parsedCandidate(sourceIndex = 0, moves = listOf("a")),
                parsedCandidate(sourceIndex = 1, moves = listOf("b")),
            ),
        )
        val cancelAfterFirstCompletion = AtomicBoolean(false)

        val summary =
            analyzeImportedGameOpeningsInParallel(
                runtimeContext = context,
                options = GameOpeningAnalysisOptions(),
                analyzeGame = { input ->
                    cancelAfterFirstCompletion.set(true)
                    noMatchingOpeningResult(selectedSide = input.selectedSide)
                },
                parallelism = 1,
                shouldCancel = { cancelAfterFirstCompletion.get() },
            )

        assertEquals(GameOpeningBatchAnalysisSummary(analyzedCount = 1, keptResultCount = 0, wasCancelled = true), summary)
        assertTrue(context.analysisResults.isEmpty())
        assertNull(context.analysisProgress)
        Unit
    }

    // Checks that the real analyzer adapter uses imported game moves, selected side, options, and book lines.
    @Test
    fun `analyzeImportedGameOpeningsAgainstBook runs real analyzer adapter`() {
        val context = GameOpeningAnalysisRuntimeContext()
        context.addImportedGames(listOf(parsedCandidate(sourceIndex = 0, moves = listOf("d2d4"))))
        context.updateFilter(GameOpeningAnalysisFilter(side = OpeningSide.BLACK))
        val options =
            GameOpeningAnalysisOptions(
                resultTypes = setOf(GameOpeningAnalysisOptions.ResultFilter.NO_MATCHING_OPENING),
                matchMode = OpeningMatchMode.MOVE_SEQUENCE,
            )

        val summary =
            runBlocking {
                analyzeImportedGameOpeningsAgainstBook(
                    runtimeContext = context,
                    options = options,
                    gameInitialFen = InitialBoardFen,
                    bookLines = listOf(line(id = 10, moves = listOf("e2e4"))),
                )
            }

        assertEquals(GameOpeningBatchAnalysisSummary(analyzedCount = 1, keptResultCount = 1, wasCancelled = false), summary)
        val result = context.analysisResults.single().result as GameOpeningNoMatchingOpening
        assertEquals(OpeningSide.BLACK, result.selectedSide)
        assertEquals(OpeningMatchMode.MOVE_SEQUENCE, result.matchMode)
        assertEquals("d2d4", result.playedMoveUci)
        assertEquals(listOf("e2e4"), result.knownMoves.map { move -> move.moveUci })
    }

    private fun parsedCandidate(
        sourceIndex: Int,
        white: String = "White $sourceIndex",
        black: String = "Black $sourceIndex",
        moves: List<String>,
    ): ImportedGameCandidate =
        ImportedGameCandidate.Parsed(
            ParsedPgnGame(
                sourceIndex = sourceIndex,
                headers =
                    mapOf(
                        "Event" to "Game $sourceIndex",
                        "White" to white,
                        "Black" to black,
                    ),
                mainLineMoves = moves,
            ),
        )

    private fun line(
        id: Long,
        moves: List<String>,
    ): LineEntity =
        LineEntity(
            id = id,
            pgn = storedPgn(moves),
            initialFen = InitialBoardFen,
        )

    private fun storedPgn(moves: List<String>): String =
        buildString {
            append("[Event \"Test\"]\n")
            append("[White \"White\"]\n")
            append("[Black \"Black\"]\n")
            append("[Result \"*\"]\n\n")

            moves.forEachIndexed { index, move ->
                if (index % 2 == 0) {
                    append("${index / 2 + 1}. ")
                }
                append(move)
                append(" ")
            }

            append("*")
        }

    private fun noMatchingOpeningResult(selectedSide: OpeningSide = OpeningSide.WHITE): GameOpeningNoMatchingOpening =
        GameOpeningNoMatchingOpening(
            selectedSide = selectedSide,
            matchMode = OpeningMatchMode.MOVE_SEQUENCE,
            positionFen = INITIAL_POSITION_FEN,
            ply = 0,
            playedMoveUci = "d2d4",
            knownMoves = emptyList(),
        )

    private fun deviationResult(): GameOpeningDeviation =
        GameOpeningDeviation(
            selectedSide = OpeningSide.WHITE,
            matchMode = OpeningMatchMode.MOVE_SEQUENCE,
            positionFen = INITIAL_POSITION_FEN,
            ply = 0,
            playedMoveUci = "d2d4",
            playedResultFen = INITIAL_POSITION_FEN,
            expectedMoves = emptyList(),
            matchingLineRefs = emptyList(),
        )

    private fun invalidMoveResult(): GameOpeningInvalidGameMove =
        GameOpeningInvalidGameMove(
            selectedSide = OpeningSide.WHITE,
            matchMode = OpeningMatchMode.MOVE_SEQUENCE,
            positionFen = INITIAL_POSITION_FEN,
            ply = 0,
            moveUci = "e2e5",
            reason = GameOpeningInvalidGameMove.Reason.ILLEGAL_MOVE,
        )

    private fun invalidInitialPositionResult(): GameOpeningInvalidInitialPosition =
        GameOpeningInvalidInitialPosition(
            selectedSide = OpeningSide.WHITE,
            matchMode = OpeningMatchMode.MOVE_SEQUENCE,
            initialFen = "bad fen",
        )

    private fun matchesKnownOpeningResult(): GameOpeningMatchesKnownOpening =
        GameOpeningMatchesKnownOpening(
            selectedSide = OpeningSide.WHITE,
            matchMode = OpeningMatchMode.MOVE_SEQUENCE,
            matchedPly = 1,
            finalPositionFen = INITIAL_POSITION_FEN,
            matchingLineRefs = emptyList(),
        )

    private companion object {
        const val INITIAL_POSITION_FEN = "start"
    }
}
