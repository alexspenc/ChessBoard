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
import com.example.chessboard.entity.LineEntity

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
fun analyzeImportedGameOpeningsAgainstBook(
    runtimeContext: GameOpeningAnalysisRuntimeContext,
    options: GameOpeningAnalysisOptions,
    gameInitialFen: String,
    bookLines: List<LineEntity>,
    analyzer: GameOpeningAnalyzer = GameOpeningAnalyzer(),
    shouldCancel: () -> Boolean = { false },
): GameOpeningBatchAnalysisSummary {
    val preparedBook by lazy {
        analyzer.prepareBook(
            bookLines = bookLines,
            matchMode = options.matchMode,
        )
    }
    return analyzeImportedGameOpenings(
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
        shouldCancel = shouldCancel,
    )
}
