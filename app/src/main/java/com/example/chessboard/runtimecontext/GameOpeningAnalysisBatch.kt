package com.example.chessboard.runtimecontext

/*
 * File role: orchestrates batch analysis of imported games against opening-analysis results.
 * Allowed here:
 * - runtime batch-analysis flow over imported games, result filtering, progress, and cancellation
 * - thin adapter from imported games to the pure GameOpeningAnalyzer API
 * Not allowed here:
 * - Compose UI, dialog state, database access, file import, or PGN parsing
 * Validation date: 2026-06-29
 */

import com.example.chessboard.analysis.GameOpeningAnalysisResult
import com.example.chessboard.analysis.GameOpeningAnalyzer
import com.example.chessboard.analysis.OpeningSide
import com.example.chessboard.concurrency.BoundedParallelTaskRunner
import com.example.chessboard.concurrency.CompletedTask
import com.example.chessboard.entity.LineEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

data class GameOpeningBatchAnalysisInput(
    val game: ImportedGameItem,
    val selectedSide: OpeningSide,
    val options: GameOpeningAnalysisOptions,
)

data class GameOpeningBatchAnalysisSummary(
    val analyzedCount: Int,
    val keptResultCount: Int,
    val wasCancelled: Boolean,
)

/**
 * Runs batch analysis for games matching the current runtime filter.
 * On cancellation, partial results are discarded because the UI should only show complete analysis output.
 */
fun analyzeImportedGameOpenings(
    runtimeContext: GameOpeningAnalysisRuntimeContext,
    options: GameOpeningAnalysisOptions,
    analyzeGame: (GameOpeningBatchAnalysisInput) -> GameOpeningAnalysisResult,
    shouldCancel: () -> Boolean = { false },
): GameOpeningBatchAnalysisSummary {
    val games = runtimeContext.filteredGames()
    val selectedSide = runtimeContext.filter.side
    val keptResults = mutableListOf<ImportedGameAnalysisResult>()
    var analyzedCount = 0

    runtimeContext.setAnalysisOptions(options)
    runtimeContext.startAnalysis(totalCount = games.size)

    for (game in games) {
        if (shouldCancel()) {
            runtimeContext.cancelAnalysis()
            return GameOpeningBatchAnalysisSummary(
                analyzedCount = analyzedCount,
                keptResultCount = 0,
                wasCancelled = true,
            )
        }

        val result =
            analyzeGame(
                GameOpeningBatchAnalysisInput(
                    game = game,
                    selectedSide = selectedSide,
                    options = options,
                ),
            )
        analyzedCount++

        if (runtimeContext.shouldKeepResult(result)) {
            keptResults.add(
                ImportedGameAnalysisResult(
                    gameId = game.id,
                    game = game,
                    result = result,
                ),
            )
        }

        runtimeContext.updateAnalysisProgress(
            analyzedCount = analyzedCount,
            totalCount = games.size,
        )
    }

    runtimeContext.replaceAnalysisResults(keptResults)
    return GameOpeningBatchAnalysisSummary(
        analyzedCount = analyzedCount,
        keptResultCount = keptResults.size,
        wasCancelled = false,
    )
}

/** Runs batch analysis using the real opening analyzer and the provided saved opening lines. */
suspend fun analyzeImportedGameOpeningsAgainstBook(
    runtimeContext: GameOpeningAnalysisRuntimeContext,
    options: GameOpeningAnalysisOptions,
    gameInitialFen: String,
    bookLines: List<LineEntity>,
    parallelism: Int = resolveGameOpeningAnalysisParallelism(),
    analyzer: GameOpeningAnalyzer = GameOpeningAnalyzer(),
    shouldCancel: () -> Boolean = { false },
): GameOpeningBatchAnalysisSummary {
    runtimeContext.setAnalysisOptions(options)
    runtimeContext.startAnalysisBookBuild()
    if (shouldCancel()) {
        runtimeContext.cancelAnalysis()
        return GameOpeningBatchAnalysisSummary(
            analyzedCount = 0,
            keptResultCount = 0,
            wasCancelled = true,
        )
    }

    val preparedBook =
        analyzer.prepareBook(
            bookLines = bookLines,
            matchMode = options.matchMode,
        )
    return analyzeImportedGameOpeningsInParallel(
        runtimeContext = runtimeContext,
        options = options,
        analyzeGame = { input ->
            preparedBook.analyze(
                gameMoves = input.game.mainLineMoves,
                gameInitialFen = gameInitialFen,
                selectedSide = input.selectedSide,
                minimumKnownPrefixPly = input.options.minimumKnownPrefixPly,
            )
        },
        parallelism = parallelism,
        shouldCancel = shouldCancel,
    )
}

/** Runs independent game-opening analysis tasks in parallel while preserving filtered-game order. */
@OptIn(ExperimentalCoroutinesApi::class)
internal suspend fun analyzeImportedGameOpeningsInParallel(
    runtimeContext: GameOpeningAnalysisRuntimeContext,
    options: GameOpeningAnalysisOptions,
    analyzeGame: suspend (GameOpeningBatchAnalysisInput) -> GameOpeningAnalysisResult,
    parallelism: Int = resolveGameOpeningAnalysisParallelism(),
    shouldCancel: () -> Boolean,
): GameOpeningBatchAnalysisSummary {
    val games = runtimeContext.filteredGames()
    val selectedSide = runtimeContext.filter.side
    val safeParallelism = parallelism.coerceAtLeast(1)

    runtimeContext.setAnalysisOptions(options)
    runtimeContext.startAnalysis(
        totalCount = games.size,
        parallelism = safeParallelism,
    )
    if (games.isEmpty()) {
        runtimeContext.replaceAnalysisResults(emptyList())
        return GameOpeningBatchAnalysisSummary(
            analyzedCount = 0,
            keptResultCount = 0,
            wasCancelled = false,
        )
    }

    return coroutineScope {
        val runner =
            BoundedParallelTaskRunner<GameOpeningAnalysisResult>(
                parallelism = safeParallelism,
                dispatcher = Dispatchers.Default.limitedParallelism(safeParallelism),
                scope = this,
            )
        val keptResults = MutableList<ImportedGameAnalysisResult?>(games.size) { null }
        var nextGameIndex = 0
        var analyzedCount = 0

        try {
            while (analyzedCount < games.size) {
                if (shouldCancel()) {
                    runtimeContext.cancelAnalysis()
                    return@coroutineScope GameOpeningBatchAnalysisSummary(
                        analyzedCount = analyzedCount,
                        keptResultCount = 0,
                        wasCancelled = true,
                    )
                }

                while (nextGameIndex < games.size && runner.hasFreeSlot()) {
                    val gameIndex = nextGameIndex
                    val game = games[gameIndex]
                    val accepted =
                        runner.trySubmit(taskId = gameIndex) {
                            currentCoroutineContext().ensureActive()
                            analyzeGame(
                                GameOpeningBatchAnalysisInput(
                                    game = game,
                                    selectedSide = selectedSide,
                                    options = options,
                                ),
                            )
                        }
                    if (!accepted) {
                        currentCoroutineContext().ensureActive()
                        break
                    }

                    nextGameIndex++
                }

                when (val completedTask = runner.receiveCompleted()) {
                    is CompletedTask.Success -> {
                        val game = games[completedTask.taskId]
                        val result = completedTask.value
                        if (runtimeContext.shouldKeepResult(result)) {
                            keptResults[completedTask.taskId] =
                                ImportedGameAnalysisResult(
                                    gameId = game.id,
                                    game = game,
                                    result = result,
                                )
                        }
                    }

                    is CompletedTask.Failure -> {
                        throw completedTask.error
                    }
                }

                analyzedCount++
                runtimeContext.updateAnalysisProgress(
                    analyzedCount = analyzedCount,
                    totalCount = games.size,
                    parallelism = safeParallelism,
                )
            }
        } finally {
            runner.cancelActiveTasks()
        }

        val finalResults = keptResults.filterNotNull()
        runtimeContext.replaceAnalysisResults(finalResults)
        GameOpeningBatchAnalysisSummary(
            analyzedCount = analyzedCount,
            keptResultCount = finalResults.size,
            wasCancelled = false,
        )
    }
}
