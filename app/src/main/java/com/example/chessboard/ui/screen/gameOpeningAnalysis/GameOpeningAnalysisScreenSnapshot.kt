package com.example.chessboard.ui.screen.gameOpeningAnalysis

/*
 * File role: builds immutable per-composition snapshots for the game-opening analysis screen.
 * Allowed here:
 * - derived read-only values calculated from GameOpeningAnalysisRuntimeContext
 * - boolean view helpers that describe the currently visible screen mode
 * Not allowed here:
 * - mutable Compose state, runtime-context mutation, UI rendering, or long-running work
 * Validation date: 2026-07-01
 */

import com.example.chessboard.runtimecontext.GameOpeningAnalysisFilter
import com.example.chessboard.runtimecontext.GameOpeningAnalysisRuntimeContext
import com.example.chessboard.runtimecontext.GameOpeningAnalysisView
import com.example.chessboard.runtimecontext.ImportedGameAnalysisResult
import com.example.chessboard.runtimecontext.ImportedGameItem

internal data class GameOpeningAnalysisScreenSnapshot(
    val importedGames: List<ImportedGameItem>,
    val visibleGames: List<ImportedGameItem>,
    val filteredGamesCount: Int,
    val selectedGame: ImportedGameItem?,
    val visibleResults: List<ImportedGameAnalysisResult>,
    val selectedAnalysisResult: ImportedGameAnalysisResult?,
    val currentView: GameOpeningAnalysisView,
    val showingResults: Boolean,
    val showingResultDetail: Boolean,
    val hasActiveFilter: Boolean,
)

internal fun GameOpeningAnalysisRuntimeContext.toScreenSnapshot(): GameOpeningAnalysisScreenSnapshot {
    val visibleGames = visibleGames()
    val filteredGamesCount = filteredGames().size
    val currentView = currentView
    return GameOpeningAnalysisScreenSnapshot(
        importedGames = importedGames,
        visibleGames = visibleGames,
        filteredGamesCount = filteredGamesCount,
        selectedGame = visibleGames.firstOrNull { game -> game.id == selectedGameId },
        visibleResults = visibleResults(),
        selectedAnalysisResult = selectedAnalysisResult(),
        currentView = currentView,
        showingResults = currentView == GameOpeningAnalysisView.ANALYSIS_RESULTS,
        showingResultDetail = currentView == GameOpeningAnalysisView.ANALYSIS_RESULT_DETAIL,
        hasActiveFilter = filter != GameOpeningAnalysisFilter(),
    )
}
