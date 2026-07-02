package com.example.chessboard.runtimecontext

/*
 * File role: stores in-memory game-opening analysis screen state for the current app process.
 * Allowed here:
 * - imported-game runtime models, filters, paging offsets, selections, and analysis result state
 * - pure runtime helpers for deduplication, filtering, paging, and result-type matching
 * Not allowed here:
 * - Compose UI rendering, navigation routing, database access, PGN file reading, or analyzer execution
 * Validation date: 2026-07-02
 */

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.chessboard.analysis.GameOpeningAnalysisResult
import com.example.chessboard.analysis.GameOpeningBookTooShort
import com.example.chessboard.analysis.GameOpeningDeviation
import com.example.chessboard.analysis.GameOpeningInvalidGameMove
import com.example.chessboard.analysis.GameOpeningInvalidInitialPosition
import com.example.chessboard.analysis.GameOpeningMatchesKnownOpening
import com.example.chessboard.analysis.GameOpeningNoMatchingOpening
import com.example.chessboard.analysis.GameOpeningOpponentLeftBook
import com.example.chessboard.analysis.OpeningMatchMode
import com.example.chessboard.analysis.OpeningSide
import com.example.chessboard.service.ParsedPgnGame

data class ImportedGameItem(
    val id: Long,
    val sourceIndex: Int,
    val headers: Map<String, String>,
    val mainLineMoves: List<String>,
    val mainLineHash: Int,
)

data class GameOpeningAnalysisFilter(
    val side: OpeningSide = OpeningSide.WHITE,
    val playerNameQuery: String = "",
    val isCaseSensitive: Boolean = false,
    val playerNameMatchMode: PlayerNameMatchMode = PlayerNameMatchMode.CONTAINS,
    val minPly: Int? = null,
) {
    enum class PlayerNameMatchMode {
        CONTAINS,
        EXACT,
    }
}

data class GameOpeningAnalysisOptions(
    val resultTypes: Set<ResultFilter> = defaultResultTypes(),
    val minimumKnownPrefixPly: Int = 0,
    val matchMode: OpeningMatchMode = OpeningMatchMode.MOVE_SEQUENCE,
) {
    enum class ResultFilter {
        DEVIATION,
        OPPONENT_LEFT_BOOK,
        BOOK_TOO_SHORT,
        MATCHES_KNOWN_OPENING,
        NO_MATCHING_OPENING,
        INVALID_GAMES,
    }

    companion object {
        fun defaultResultTypes(): Set<ResultFilter> =
            setOf(
                ResultFilter.DEVIATION,
                ResultFilter.OPPONENT_LEFT_BOOK,
                ResultFilter.BOOK_TOO_SHORT,
                ResultFilter.NO_MATCHING_OPENING,
                ResultFilter.INVALID_GAMES,
            )
    }
}

data class ImportedGameAnalysisResult(
    val gameId: Long,
    val game: ImportedGameItem,
    val result: GameOpeningAnalysisResult,
)

enum class GameOpeningAnalysisView {
    IMPORTED_GAMES,
    ANALYSIS_RESULTS,
    ANALYSIS_RESULT_DETAIL,
}

data class ImportGamesSummary(
    val scannedCount: Int,
    val addedCount: Int,
    val skippedDuplicateCount: Int,
    val skippedParseErrorCount: Int,
)

data class GameOpeningAnalysisProgress(
    val analyzedCount: Int,
    val totalCount: Int,
    val stage: Stage = Stage.ANALYZING_GAMES,
    val parallelism: Int? = null,
) {
    enum class Stage {
        BUILDING_BOOK,
        ANALYZING_GAMES,
    }
}

sealed interface ImportedGameCandidate {
    data class Parsed(
        val game: ParsedPgnGame,
    ) : ImportedGameCandidate

    data object ParseError : ImportedGameCandidate
}

class GameOpeningAnalysisRuntimeContext(
    private val pageLimit: Int = RuntimeContext.LinesExplorerPageLimit,
) {
    var importedGames by mutableStateOf<List<ImportedGameItem>>(emptyList())
        private set

    var filter by mutableStateOf(GameOpeningAnalysisFilter())
        private set

    var hasAppliedFilter by mutableStateOf(false)
        private set

    var gamesOffset by mutableStateOf(0)
        private set

    var selectedGameId by mutableStateOf<Long?>(null)
        private set

    var analysisResults by mutableStateOf<List<ImportedGameAnalysisResult>>(emptyList())
        private set

    var resultsOffset by mutableStateOf(0)
        private set

    var selectedResultGameId by mutableStateOf<Long?>(null)
        private set

    var currentView by mutableStateOf(GameOpeningAnalysisView.IMPORTED_GAMES)
        private set

    var lastAnalysisOptions by mutableStateOf(GameOpeningAnalysisOptions())
        private set

    var analysisProgress by mutableStateOf<GameOpeningAnalysisProgress?>(null)
        private set

    private var nextGameId = 1L

    fun addImportedGames(candidates: List<ImportedGameCandidate>): ImportGamesSummary {
        var addedCount = 0
        var skippedDuplicateCount = 0
        var skippedParseErrorCount = 0
        val nextGames = importedGames.toMutableList()

        candidates.forEach { candidate ->
            when (candidate) {
                ImportedGameCandidate.ParseError -> {
                    skippedParseErrorCount++
                }

                is ImportedGameCandidate.Parsed -> {
                    val mainLineMoves = candidate.game.mainLineMoves
                    if (mainLineMoves.isEmpty()) {
                        skippedParseErrorCount++
                        return@forEach
                    }

                    val duplicateIndex = nextGames.indexOfSameMainLine(mainLineMoves)
                    if (duplicateIndex >= 0) {
                        nextGames[duplicateIndex] =
                            maybeUpdateDuplicateHeaders(
                                existingGame = nextGames[duplicateIndex],
                                newHeaders = candidate.game.headers,
                            )
                        skippedDuplicateCount++
                        return@forEach
                    }

                    nextGames.add(
                        ImportedGameItem(
                            id = nextGameId++,
                            sourceIndex = candidate.game.sourceIndex,
                            headers = candidate.game.headers,
                            mainLineMoves = mainLineMoves,
                            mainLineHash = mainLineMoves.hashCode(),
                        ),
                    )
                    addedCount++
                }
            }
        }

        if (nextGames != importedGames) {
            importedGames = nextGames
            resetAfterImportedGamesChanged()
        }

        return ImportGamesSummary(
            scannedCount = candidates.size,
            addedCount = addedCount,
            skippedDuplicateCount = skippedDuplicateCount,
            skippedParseErrorCount = skippedParseErrorCount,
        )
    }

    fun updateFilter(filter: GameOpeningAnalysisFilter) {
        this.filter = filter
        hasAppliedFilter = true
        gamesOffset = 0
        clearAnalysisResults()
    }

    fun clearFilter() {
        filter = GameOpeningAnalysisFilter()
        hasAppliedFilter = false
        gamesOffset = 0
        clearAnalysisResults()
    }

    fun filteredGames(): List<ImportedGameItem> = importedGames.filter { game -> game.matches(filter) }

    fun visibleGames(): List<ImportedGameItem> = filteredGames().drop(gamesOffset).take(pageLimit)

    fun currentGamesPage(): Int {
        if (filteredGames().isEmpty()) {
            return 1
        }

        return gamesOffset / pageLimit + 1
    }

    fun totalGamesPages(): Int {
        val totalGamesCount = filteredGames().size
        if (totalGamesCount == 0) {
            return 1
        }

        return (totalGamesCount + pageLimit - 1) / pageLimit
    }

    fun canOpenPreviousGamesPage(): Boolean = gamesOffset > 0

    fun canOpenNextGamesPage(): Boolean = gamesOffset + pageLimit < filteredGames().size

    fun openPreviousGamesPage() {
        if (!canOpenPreviousGamesPage()) {
            return
        }

        gamesOffset = (gamesOffset - pageLimit).coerceAtLeast(0)
    }

    fun openNextGamesPage() {
        if (!canOpenNextGamesPage()) {
            return
        }

        gamesOffset += pageLimit
    }

    fun selectGame(gameId: Long?) {
        if (gameId == null) {
            selectedGameId = null
            return
        }

        val hasGame = importedGames.any { game -> game.id == gameId }
        if (!hasGame) {
            selectedGameId = null
            return
        }

        selectedGameId = gameId
    }

    fun deleteSelectedGame(): Boolean {
        val gameId = selectedGameId ?: return false
        val nextGames = importedGames.filterNot { game -> game.id == gameId }
        if (nextGames.size == importedGames.size) {
            selectedGameId = null
            return false
        }

        importedGames = nextGames
        gamesOffset =
            resolveOffsetAfterRemove(
                currentOffset = gamesOffset,
                nextItemsCount = filteredGames().size,
            )
        selectedGameId = null
        removeAnalysisResultForDeletedGame(gameId)
        return true
    }

    fun clearFilteredGames() {
        val filteredGameIds = filteredGames().map { game -> game.id }.toSet()
        if (filteredGameIds.isEmpty()) {
            return
        }

        importedGames = importedGames.filterNot { game -> game.id in filteredGameIds }
        gamesOffset =
            resolveOffsetAfterRemove(
                currentOffset = gamesOffset,
                nextItemsCount = filteredGames().size,
            )
        selectedGameId = null
        hasAppliedFilter = false
        clearAnalysisResults()
    }

    fun deleteAnalysisResultGames(): Int {
        val resultGameIds = analysisResults.map { result -> result.gameId }.toSet()
        if (resultGameIds.isEmpty()) {
            return 0
        }

        val nextGames = importedGames.filterNot { game -> game.id in resultGameIds }
        val deletedGamesCount = importedGames.size - nextGames.size
        if (nextGames.size == importedGames.size) {
            clearAnalysisResults()
            return 0
        }

        importedGames = nextGames
        gamesOffset =
            resolveOffsetAfterRemove(
                currentOffset = gamesOffset,
                nextItemsCount = filteredGames().size,
            )
        val selectedGameIdToDelete = selectedGameId
        if (selectedGameIdToDelete != null && selectedGameIdToDelete in resultGameIds) {
            selectedGameId = null
        }
        clearAnalysisResults()
        return deletedGamesCount
    }

    fun setAnalysisOptions(options: GameOpeningAnalysisOptions) {
        lastAnalysisOptions = options
    }

    fun startAnalysisBookBuild() {
        clearAnalysisResults()
        analysisProgress =
            GameOpeningAnalysisProgress(
                analyzedCount = 0,
                totalCount = 0,
                stage = GameOpeningAnalysisProgress.Stage.BUILDING_BOOK,
            )
    }

    fun startAnalysis(
        totalCount: Int,
        parallelism: Int? = null,
    ) {
        clearAnalysisResults()
        analysisProgress =
            GameOpeningAnalysisProgress(
                analyzedCount = 0,
                totalCount = totalCount,
                stage = GameOpeningAnalysisProgress.Stage.ANALYZING_GAMES,
                parallelism = parallelism,
            )
    }

    fun updateAnalysisProgress(
        analyzedCount: Int,
        totalCount: Int,
        parallelism: Int? = null,
    ) {
        analysisProgress =
            GameOpeningAnalysisProgress(
                analyzedCount = analyzedCount.coerceAtLeast(0),
                totalCount = totalCount.coerceAtLeast(0),
                stage = GameOpeningAnalysisProgress.Stage.ANALYZING_GAMES,
                parallelism = parallelism,
            )
    }

    fun cancelAnalysis() {
        analysisProgress = null
    }

    /** Replaces the current analysis output and resets result paging, selection, and progress state. */
    fun replaceAnalysisResults(results: List<ImportedGameAnalysisResult>) {
        analysisProgress = null
        analysisResults = results
        resultsOffset = 0
        selectedResultGameId = null
    }

    fun clearAnalysisResults() {
        analysisResults = emptyList()
        resultsOffset = 0
        selectedResultGameId = null
        analysisProgress = null
        currentView = GameOpeningAnalysisView.IMPORTED_GAMES
    }

    fun visibleResults(): List<ImportedGameAnalysisResult> = analysisResults.drop(resultsOffset).take(pageLimit)

    fun canOpenPreviousResultsPage(): Boolean = resultsOffset > 0

    fun canOpenNextResultsPage(): Boolean = resultsOffset + pageLimit < analysisResults.size

    fun openPreviousResultsPage() {
        if (!canOpenPreviousResultsPage()) {
            return
        }

        resultsOffset = (resultsOffset - pageLimit).coerceAtLeast(0)
    }

    fun openNextResultsPage() {
        if (!canOpenNextResultsPage()) {
            return
        }

        resultsOffset += pageLimit
    }

    fun selectResult(gameId: Long?) {
        if (gameId == null) {
            selectedResultGameId = null
            return
        }

        val hasResult = analysisResults.any { result -> result.gameId == gameId }
        if (!hasResult) {
            selectedResultGameId = null
            return
        }

        selectedResultGameId = gameId
    }

    fun openImportedGames() {
        currentView = GameOpeningAnalysisView.IMPORTED_GAMES
    }

    fun openAnalysisResults() {
        if (analysisResults.isEmpty()) {
            currentView = GameOpeningAnalysisView.IMPORTED_GAMES
            return
        }

        currentView = GameOpeningAnalysisView.ANALYSIS_RESULTS
    }

    fun openSelectedResultDetail() {
        val selectedResultId = selectedResultGameId ?: return
        val hasResult = analysisResults.any { result -> result.gameId == selectedResultId }
        if (!hasResult) {
            currentView = GameOpeningAnalysisView.ANALYSIS_RESULTS
            return
        }

        currentView = GameOpeningAnalysisView.ANALYSIS_RESULT_DETAIL
    }

    fun selectNextResult(gameId: Long): Boolean {
        if (analysisResults.isEmpty()) {
            return false
        }

        return selectResultByOffset(gameId = gameId, offset = 1)
    }

    fun selectPreviousResult(gameId: Long): Boolean {
        if (analysisResults.isEmpty()) {
            return false
        }

        return selectResultByOffset(gameId = gameId, offset = -1)
    }

    fun selectNextDeviation(gameId: Long): Boolean {
        if (analysisResults.isEmpty()) {
            return false
        }

        val startIndex = analysisResults.indexOfFirst { result -> result.gameId == gameId }
        if (startIndex < 0) {
            return selectFirstDeviation()
        }

        for (offset in 1 until analysisResults.size) {
            val result = analysisResults[(startIndex + offset) % analysisResults.size]
            if (result.result !is GameOpeningDeviation) {
                continue
            }

            selectedResultGameId = result.gameId
            currentView = GameOpeningAnalysisView.ANALYSIS_RESULT_DETAIL
            return true
        }

        return false
    }

    private fun selectResultByOffset(
        gameId: Long,
        offset: Int,
    ): Boolean {
        val startIndex = analysisResults.indexOfFirst { result -> result.gameId == gameId }
        if (startIndex < 0) {
            return selectResultAtIndex(0)
        }

        if (analysisResults.size == 1) {
            return false
        }

        val nextIndex = Math.floorMod(startIndex + offset, analysisResults.size)
        return selectResultAtIndex(nextIndex)
    }

    private fun selectResultAtIndex(index: Int): Boolean {
        val result = analysisResults.getOrNull(index) ?: return false
        selectedResultGameId = result.gameId
        resultsOffset = (index / pageLimit) * pageLimit
        return true
    }

    private fun selectFirstDeviation(): Boolean {
        val nextDeviation = analysisResults.firstOrNull { result -> result.result is GameOpeningDeviation }
        if (nextDeviation == null) {
            return false
        }

        selectedResultGameId = nextDeviation.gameId
        currentView = GameOpeningAnalysisView.ANALYSIS_RESULT_DETAIL
        return true
    }

    fun selectedAnalysisResult(): ImportedGameAnalysisResult? {
        val selectedResultId = selectedResultGameId ?: return null
        return analysisResults.firstOrNull { result -> result.gameId == selectedResultId }
    }

    fun shouldKeepResult(result: GameOpeningAnalysisResult): Boolean = result.matchesAny(lastAnalysisOptions.resultTypes)

    private fun resetAfterImportedGamesChanged() {
        filter = GameOpeningAnalysisFilter()
        hasAppliedFilter = false
        gamesOffset = 0
        selectedGameId = null
        clearAnalysisResults()
    }

    private fun removeAnalysisResultForDeletedGame(gameId: Long) {
        val nextResults = analysisResults.filterNot { result -> result.gameId == gameId }
        if (nextResults.size == analysisResults.size) {
            return
        }

        analysisResults = nextResults
        resultsOffset =
            resolveOffsetAfterRemove(
                currentOffset = resultsOffset,
                nextItemsCount = analysisResults.size,
            )

        val deletedSelectedResult = selectedResultGameId == gameId
        if (deletedSelectedResult) {
            selectedResultGameId = null
        }

        if (analysisResults.isEmpty()) {
            currentView = GameOpeningAnalysisView.IMPORTED_GAMES
            return
        }

        if (deletedSelectedResult && currentView == GameOpeningAnalysisView.ANALYSIS_RESULT_DETAIL) {
            currentView = GameOpeningAnalysisView.ANALYSIS_RESULTS
        }
    }

    private fun List<ImportedGameItem>.indexOfSameMainLine(mainLineMoves: List<String>): Int {
        val mainLineHash = mainLineMoves.hashCode()
        return indexOfFirst { game ->
            game.mainLineHash == mainLineHash && game.mainLineMoves == mainLineMoves
        }
    }

    private fun maybeUpdateDuplicateHeaders(
        existingGame: ImportedGameItem,
        newHeaders: Map<String, String>,
    ): ImportedGameItem {
        if (importantHeaderScore(newHeaders) <= importantHeaderScore(existingGame.headers)) {
            return existingGame
        }

        return existingGame.copy(headers = newHeaders)
    }

    private fun ImportedGameItem.matches(filter: GameOpeningAnalysisFilter): Boolean {
        if (!matchesPlayerName(filter)) {
            return false
        }

        val minPly = filter.minPly
        if (minPly != null && mainLineMoves.size < minPly) {
            return false
        }

        return true
    }

    private fun ImportedGameItem.matchesPlayerName(filter: GameOpeningAnalysisFilter): Boolean {
        if (filter.playerNameQuery.isBlank()) {
            return true
        }

        val playerName =
            when (filter.side) {
                OpeningSide.WHITE -> headers[WHITE_HEADER]
                OpeningSide.BLACK -> headers[BLACK_HEADER]
            } ?: return false

        val query = filter.playerNameQuery
        val normalizedPlayerName = normalizePlayerNameForSearch(playerName, filter.isCaseSensitive)
        val normalizedQuery = normalizePlayerNameForSearch(query, filter.isCaseSensitive)

        return when (filter.playerNameMatchMode) {
            GameOpeningAnalysisFilter.PlayerNameMatchMode.CONTAINS -> {
                normalizedPlayerName.contains(normalizedQuery)
            }

            GameOpeningAnalysisFilter.PlayerNameMatchMode.EXACT -> {
                normalizedPlayerName == normalizedQuery
            }
        }
    }

    private fun normalizePlayerNameForSearch(
        value: String,
        isCaseSensitive: Boolean,
    ): String {
        if (isCaseSensitive) {
            return value
        }

        return value.lowercase()
    }

    private fun GameOpeningAnalysisResult.matchesAny(resultTypes: Set<GameOpeningAnalysisOptions.ResultFilter>): Boolean {
        val resultFilter =
            when (this) {
                is GameOpeningDeviation -> GameOpeningAnalysisOptions.ResultFilter.DEVIATION
                is GameOpeningOpponentLeftBook -> GameOpeningAnalysisOptions.ResultFilter.OPPONENT_LEFT_BOOK
                is GameOpeningBookTooShort -> GameOpeningAnalysisOptions.ResultFilter.BOOK_TOO_SHORT
                is GameOpeningMatchesKnownOpening -> GameOpeningAnalysisOptions.ResultFilter.MATCHES_KNOWN_OPENING
                is GameOpeningNoMatchingOpening -> GameOpeningAnalysisOptions.ResultFilter.NO_MATCHING_OPENING
                is GameOpeningInvalidGameMove -> GameOpeningAnalysisOptions.ResultFilter.INVALID_GAMES
                is GameOpeningInvalidInitialPosition -> GameOpeningAnalysisOptions.ResultFilter.INVALID_GAMES
            }
        return resultFilter in resultTypes
    }

    private fun resolveOffsetAfterRemove(
        currentOffset: Int,
        nextItemsCount: Int,
    ): Int {
        if (nextItemsCount <= 0) {
            return 0
        }

        if (currentOffset < nextItemsCount) {
            return currentOffset
        }

        return ((nextItemsCount - 1) / pageLimit) * pageLimit
    }

    private fun importantHeaderScore(headers: Map<String, String>): Int =
        IMPORTANT_HEADER_KEYS.count { key ->
            val value = headers[key]
            !value.isNullOrBlank() && value != UNKNOWN_HEADER_VALUE
        }

    private companion object {
        const val WHITE_HEADER = "White"
        const val BLACK_HEADER = "Black"
        const val UNKNOWN_HEADER_VALUE = "?"
        val IMPORTANT_HEADER_KEYS = listOf("Event", WHITE_HEADER, BLACK_HEADER, "Date", "Result")
    }
}
