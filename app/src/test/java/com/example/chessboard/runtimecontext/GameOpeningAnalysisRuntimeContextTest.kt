package com.example.chessboard.runtimecontext

/*
 * File role: verifies in-memory game-opening analysis runtime state behavior.
 * Allowed here:
 * - tests for imported-game deduplication, filters, paging, selection, and result filtering
 * - tests for runtime-only state invalidation rules used by the future analysis UI
 * Not allowed here:
 * - Compose rendering, database access, file import, PGN parsing, or analyzer integration tests
 * Validation date: 2026-07-02
 */

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameOpeningAnalysisRuntimeContextTest {
    // Checks that successful imports preserve order and receive stable in-memory ids.
    @Test
    fun `addImportedGames adds parsed games in order and assigns ids`() {
        val context = GameOpeningAnalysisRuntimeContext()

        val summary =
            context.addImportedGames(
                listOf(
                    parsedCandidate(sourceIndex = 4, event = "Game A", moves = listOf("e2e4")),
                    parsedCandidate(sourceIndex = 7, event = "Game B", moves = listOf("d2d4")),
                ),
            )

        assertEquals(ImportGamesSummary(2, 2, 0, 0), summary)
        assertEquals(listOf(1L, 2L), context.importedGames.map { game -> game.id })
        assertEquals(listOf(4, 7), context.importedGames.map { game -> game.sourceIndex })
        assertEquals(listOf("Game A", "Game B"), context.importedGames.map { game -> game.headers["Event"] })
    }

    // Checks that identical main-line move lists are treated as duplicates.
    @Test
    fun `addImportedGames skips duplicate main lines`() {
        val context = GameOpeningAnalysisRuntimeContext()
        val moves = listOf("e2e4", "e7e5")

        val summary =
            context.addImportedGames(
                listOf(
                    parsedCandidate(sourceIndex = 0, event = "Original", moves = moves),
                    parsedCandidate(sourceIndex = 1, event = "Duplicate", moves = moves),
                ),
            )

        assertEquals(ImportGamesSummary(2, 1, 1, 0), summary)
        assertEquals(1, context.importedGames.size)
        assertEquals("Original", context.importedGames.single().headers["Event"])
    }

    // Checks that duplicate games keep the best available important PGN headers.
    @Test
    fun `addImportedGames updates duplicate headers when important headers are more complete`() {
        val context = GameOpeningAnalysisRuntimeContext()
        val moves = listOf("e2e4", "e7e5")

        context.addImportedGames(
            listOf(parsedCandidate(sourceIndex = 0, headers = mapOf("Event" to "Original"), moves = moves)),
        )
        val summary =
            context.addImportedGames(
                listOf(
                    parsedCandidate(
                        sourceIndex = 1,
                        headers =
                            mapOf(
                                "Event" to "Improved",
                                "White" to "Alice",
                                "Black" to "Bob",
                                "Date" to "2026.06.26",
                                "Result" to "1-0",
                            ),
                        moves = moves,
                    ),
                ),
            )

        assertEquals(ImportGamesSummary(1, 0, 1, 0), summary)
        assertEquals("Improved", context.importedGames.single().headers["Event"])
        assertEquals("Alice", context.importedGames.single().headers["White"])
    }

    // Checks the import summary shown after adding parsed and failed PGN records.
    @Test
    fun `addImportedGames reports scanned added duplicate and parse error counts`() {
        val context = GameOpeningAnalysisRuntimeContext()
        val moves = listOf("e2e4")

        val summary =
            context.addImportedGames(
                listOf(
                    parsedCandidate(sourceIndex = 0, event = "Original", moves = moves),
                    parsedCandidate(sourceIndex = 1, event = "Duplicate", moves = moves),
                    ImportedGameCandidate.ParseError,
                    parsedCandidate(sourceIndex = 2, event = "Empty", moves = emptyList()),
                    parsedCandidate(sourceIndex = 3, event = "Other", moves = listOf("d2d4")),
                ),
            )

        assertEquals(ImportGamesSummary(5, 2, 1, 2), summary)
        assertEquals(listOf("Original", "Other"), context.importedGames.map { game -> game.headers["Event"] })
    }

    // Checks that changing the imported game set invalidates screen selections and old analysis.
    @Test
    fun `addImportedGames resets filter selection and analysis results`() {
        val context = GameOpeningAnalysisRuntimeContext()
        context.addImportedGames(listOf(parsedCandidate(sourceIndex = 0, event = "Initial", moves = listOf("e2e4"))))
        context.updateFilter(GameOpeningAnalysisFilter(playerNameQuery = "Alice"))
        context.selectGame(1L)
        context.replaceAnalysisResults(listOf(resultForGame(context.importedGames.single(), invalidMoveResult())))

        context.addImportedGames(listOf(parsedCandidate(sourceIndex = 1, event = "Next", moves = listOf("d2d4"))))

        assertEquals(GameOpeningAnalysisFilter(), context.filter)
        assertEquals(0, context.gamesOffset)
        assertNull(context.selectedGameId)
        assertTrue(context.analysisResults.isEmpty())
        assertFalse(context.hasAppliedFilter)
    }

    // Checks that applying a filter marks the imported-game list as ready for analysis.
    @Test
    fun `updateFilter marks player filter as applied`() {
        val context = GameOpeningAnalysisRuntimeContext()

        context.updateFilter(GameOpeningAnalysisFilter(playerNameQuery = "Alice"))

        assertTrue(context.hasAppliedFilter)
    }

    // Checks that clearing a filter removes the analysis precondition marker.
    @Test
    fun `clearFilter resets applied filter marker`() {
        val context = GameOpeningAnalysisRuntimeContext()
        context.updateFilter(GameOpeningAnalysisFilter(playerNameQuery = "Alice"))

        context.clearFilter()

        assertFalse(context.hasAppliedFilter)
    }

    // Checks that a player-name query uses the White header when white is selected.
    @Test
    fun `updateFilter filters player by selected white side`() {
        val context = GameOpeningAnalysisRuntimeContext()
        context.addImportedGames(
            listOf(
                parsedCandidate(sourceIndex = 0, white = "Alice", black = "Carol", moves = listOf("e2e4")),
                parsedCandidate(sourceIndex = 1, white = "Bob", black = "Alice", moves = listOf("d2d4")),
            ),
        )

        context.updateFilter(GameOpeningAnalysisFilter(side = OpeningSide.WHITE, playerNameQuery = "Ali"))

        assertEquals(listOf("Alice"), context.filteredGames().map { game -> game.headers["White"] })
    }

    // Checks that a player-name query uses the Black header when black is selected.
    @Test
    fun `updateFilter filters player by selected black side`() {
        val context = GameOpeningAnalysisRuntimeContext()
        context.addImportedGames(
            listOf(
                parsedCandidate(sourceIndex = 0, white = "Alice", black = "Carol", moves = listOf("e2e4")),
                parsedCandidate(sourceIndex = 1, white = "Bob", black = "Alice", moves = listOf("d2d4")),
            ),
        )

        context.updateFilter(GameOpeningAnalysisFilter(side = OpeningSide.BLACK, playerNameQuery = "Ali"))

        assertEquals(listOf("Alice"), context.filteredGames().map { game -> game.headers["Black"] })
    }

    // Checks the narrow case-sensitive partial-name search mode.
    @Test
    fun `player filter supports case-sensitive contains`() {
        val context = GameOpeningAnalysisRuntimeContext()
        context.addImportedGames(
            listOf(
                parsedCandidate(sourceIndex = 0, white = "Magnus Carlsen", moves = listOf("e2e4")),
                parsedCandidate(sourceIndex = 1, white = "MAGNUS CARLSEN", moves = listOf("d2d4")),
            ),
        )

        context.updateFilter(
            GameOpeningAnalysisFilter(
                playerNameQuery = "Car",
                isCaseSensitive = true,
                playerNameMatchMode = GameOpeningAnalysisFilter.PlayerNameMatchMode.CONTAINS,
            ),
        )

        assertEquals(listOf("Magnus Carlsen"), context.filteredGames().map { game -> game.headers["White"] })
    }

    // Checks exact player-name search while ignoring case.
    @Test
    fun `player filter supports case-insensitive exact`() {
        val context = GameOpeningAnalysisRuntimeContext()
        context.addImportedGames(
            listOf(
                parsedCandidate(sourceIndex = 0, white = "Magnus Carlsen", moves = listOf("e2e4")),
                parsedCandidate(sourceIndex = 1, white = "Carlsen", moves = listOf("d2d4")),
            ),
        )

        context.updateFilter(
            GameOpeningAnalysisFilter(
                playerNameQuery = "magnus carlsen",
                playerNameMatchMode = GameOpeningAnalysisFilter.PlayerNameMatchMode.EXACT,
            ),
        )

        assertEquals(listOf("Magnus Carlsen"), context.filteredGames().map { game -> game.headers["White"] })
    }

    // Checks that minimum game length is measured in half-moves.
    @Test
    fun `minPly filters by half move count`() {
        val context = GameOpeningAnalysisRuntimeContext()
        context.addImportedGames(
            listOf(
                parsedCandidate(sourceIndex = 0, event = "Short", moves = listOf("e2e4", "e7e5")),
                parsedCandidate(sourceIndex = 1, event = "Long", moves = listOf("d2d4", "d7d5", "c2c4")),
            ),
        )

        context.updateFilter(GameOpeningAnalysisFilter(minPly = 3))

        assertEquals(listOf("Long"), context.filteredGames().map { game -> game.headers["Event"] })
    }

    // Checks that clearing removes every filtered game and invalidates dependent state.
    @Test
    fun `clearFilteredGames removes all filtered games and clears results`() {
        val context = GameOpeningAnalysisRuntimeContext(pageLimit = 1)
        context.addImportedGames(
            listOf(
                parsedCandidate(sourceIndex = 0, white = "Alice", moves = listOf("e2e4")),
                parsedCandidate(sourceIndex = 1, white = "Alice", moves = listOf("d2d4")),
                parsedCandidate(sourceIndex = 2, white = "Bob", moves = listOf("c2c4")),
            ),
        )
        context.updateFilter(GameOpeningAnalysisFilter(playerNameQuery = "Alice"))
        context.openNextGamesPage()
        context.selectGame(2L)
        context.replaceAnalysisResults(listOf(resultForGame(context.importedGames.first(), invalidMoveResult())))

        context.clearFilteredGames()

        assertEquals(listOf("Bob"), context.importedGames.map { game -> game.headers["White"] })
        assertEquals(0, context.gamesOffset)
        assertNull(context.selectedGameId)
        assertTrue(context.analysisResults.isEmpty())
        assertFalse(context.hasAppliedFilter)
    }

    // Checks that deleting the selected imported game keeps the applied filter and unrelated analysis results.
    @Test
    fun `deleteSelectedGame removes selected game and keeps filter with remaining results`() {
        val context = GameOpeningAnalysisRuntimeContext(pageLimit = 1)
        context.addImportedGames(
            listOf(
                parsedCandidate(sourceIndex = 0, event = "First", moves = listOf("e2e4")),
                parsedCandidate(sourceIndex = 1, event = "Second", moves = listOf("d2d4")),
            ),
        )
        context.updateFilter(GameOpeningAnalysisFilter(playerNameQuery = "White"))
        context.openNextGamesPage()
        context.selectGame(2L)
        val firstGameResult = resultForGame(context.importedGames.first(), invalidMoveResult())
        val secondGameResult = resultForGame(context.importedGames.last(), invalidMoveResult())
        context.replaceAnalysisResults(listOf(firstGameResult, secondGameResult))

        val wasDeleted = context.deleteSelectedGame()

        assertTrue(wasDeleted)
        assertEquals(listOf("First"), context.importedGames.map { game -> game.headers["Event"] })
        assertEquals(0, context.gamesOffset)
        assertNull(context.selectedGameId)
        assertEquals(listOf(firstGameResult), context.analysisResults)
        assertTrue(context.hasAppliedFilter)
        assertEquals(listOf("First"), context.filteredGames().map { game -> game.headers["Event"] })
    }

    // Checks that deleting without a selected game is a no-op.
    @Test
    fun `deleteSelectedGame returns false when no game is selected`() {
        val context = GameOpeningAnalysisRuntimeContext()
        context.addImportedGames(listOf(parsedCandidate(sourceIndex = 0, event = "Only", moves = listOf("e2e4"))))

        val wasDeleted = context.deleteSelectedGame()

        assertFalse(wasDeleted)
        assertEquals(listOf("Only"), context.importedGames.map { game -> game.headers["Event"] })
        assertNull(context.selectedGameId)
    }

    // Checks that imported games are paged using the configured page limit.
    @Test
    fun `visibleGames returns one page using configured limit`() {
        val context = GameOpeningAnalysisRuntimeContext(pageLimit = 20)
        context.addImportedGames(
            (0 until 25).map { index ->
                parsedCandidate(sourceIndex = index, event = "Game $index", moves = listOf("move-$index"))
            },
        )

        val firstPage = context.visibleGames()
        val firstCurrentPage = context.currentGamesPage()
        val firstTotalPages = context.totalGamesPages()
        context.openNextGamesPage()
        val secondPage = context.visibleGames()

        assertEquals(20, firstPage.size)
        assertEquals(1, firstCurrentPage)
        assertEquals(2, firstTotalPages)
        assertEquals(5, secondPage.size)
        assertEquals(2, context.currentGamesPage())
        assertEquals(2, context.totalGamesPages())
        assertEquals("Game 20", secondPage.first().headers["Event"])
    }

    // Checks the default analysis dialog options agreed for the first UI version.
    @Test
    fun `analysis options default excludes matches known opening`() {
        val options = GameOpeningAnalysisOptions()

        assertEquals(OpeningMatchMode.MOVE_SEQUENCE, options.matchMode)
        assertEquals(0, options.minimumKnownPrefixPly)
        assertFalse(GameOpeningAnalysisOptions.ResultFilter.MATCHES_KNOWN_OPENING in options.resultTypes)
        assertTrue(GameOpeningAnalysisOptions.ResultFilter.DEVIATION in options.resultTypes)
    }

    // Checks that new analysis output replaces old result paging and selection state.
    @Test
    fun `replaceAnalysisResults stores results and resets result paging selection`() {
        val context = GameOpeningAnalysisRuntimeContext(pageLimit = 1)
        context.addImportedGames(
            listOf(
                parsedCandidate(sourceIndex = 0, event = "A", moves = listOf("e2e4")),
                parsedCandidate(sourceIndex = 1, event = "B", moves = listOf("d2d4")),
            ),
        )
        val initialResults = context.importedGames.map { game -> resultForGame(game, invalidMoveResult()) }
        context.replaceAnalysisResults(initialResults)
        context.openNextResultsPage()
        context.selectResult(initialResults.last().gameId)

        context.replaceAnalysisResults(listOf(initialResults.first()))

        assertEquals(listOf(initialResults.first()), context.analysisResults)
        assertEquals(0, context.resultsOffset)
        assertNull(context.selectedResultGameId)
    }

    // Checks that results view opens only for stored results and resets when results are cleared.
    @Test
    fun `analysis results view opens only for stored results`() {
        val context = GameOpeningAnalysisRuntimeContext()

        context.openAnalysisResults()

        assertEquals(GameOpeningAnalysisView.IMPORTED_GAMES, context.currentView)

        context.addImportedGames(listOf(parsedCandidate(sourceIndex = 0, event = "A", moves = listOf("e2e4"))))
        context.replaceAnalysisResults(listOf(resultForGame(context.importedGames.single(), invalidMoveResult())))
        context.openAnalysisResults()

        assertEquals(GameOpeningAnalysisView.ANALYSIS_RESULTS, context.currentView)

        context.clearAnalysisResults()

        assertEquals(GameOpeningAnalysisView.IMPORTED_GAMES, context.currentView)
    }

    // Checks that progress updates store current counters and clamp invalid negative values.
    @Test
    fun `updateAnalysisProgress stores progress and clamps negative values`() {
        val context = GameOpeningAnalysisRuntimeContext()

        context.startAnalysis(totalCount = 10)
        context.updateAnalysisProgress(analyzedCount = 3, totalCount = 10)

        assertEquals(GameOpeningAnalysisProgress(3, 10), context.analysisProgress)

        context.updateAnalysisProgress(analyzedCount = -1, totalCount = -5)

        assertEquals(GameOpeningAnalysisProgress(0, 0), context.analysisProgress)
    }

    // Checks that analysis progress can represent the book-building stage before game counters start.
    @Test
    fun `startAnalysisBookBuild stores book building progress stage`() {
        val context = GameOpeningAnalysisRuntimeContext()

        context.startAnalysisBookBuild()

        assertEquals(
            GameOpeningAnalysisProgress(
                analyzedCount = 0,
                totalCount = 0,
                stage = GameOpeningAnalysisProgress.Stage.BUILDING_BOOK,
            ),
            context.analysisProgress,
        )
    }

    // Checks that selected analysis result detail view is opened only from a valid selected result.
    @Test
    fun `openSelectedResultDetail opens detail for selected analysis result`() {
        val context = GameOpeningAnalysisRuntimeContext()
        context.addImportedGames(listOf(parsedCandidate(sourceIndex = 0, event = "A", moves = listOf("e2e4"))))
        val result = resultForGame(context.importedGames.single(), matchesKnownOpeningResult())
        context.replaceAnalysisResults(listOf(result))
        context.openAnalysisResults()
        context.selectResult(result.gameId)

        context.openSelectedResultDetail()

        assertEquals(GameOpeningAnalysisView.ANALYSIS_RESULT_DETAIL, context.currentView)
        assertEquals(result, context.selectedAnalysisResult())
    }

    // Checks that next result selection advances by result order and wraps to the first result.
    @Test
    fun `selectNextResult selects following result and wraps`() {
        val context = GameOpeningAnalysisRuntimeContext()
        context.addImportedGames(
            listOf(
                parsedCandidate(sourceIndex = 0, event = "A", moves = listOf("e2e4")),
                parsedCandidate(sourceIndex = 1, event = "B", moves = listOf("d2d4")),
                parsedCandidate(sourceIndex = 2, event = "C", moves = listOf("c2c4")),
            ),
        )
        val firstResult = resultForGame(context.importedGames[0], matchesKnownOpeningResult())
        val secondResult = resultForGame(context.importedGames[1], matchesKnownOpeningResult())
        val thirdResult = resultForGame(context.importedGames[2], matchesKnownOpeningResult())
        context.replaceAnalysisResults(listOf(firstResult, secondResult, thirdResult))

        assertTrue(context.selectNextResult(firstResult.gameId))
        assertEquals(secondResult.gameId, context.selectedResultGameId)

        assertTrue(context.selectNextResult(thirdResult.gameId))
        assertEquals(firstResult.gameId, context.selectedResultGameId)
    }

    // Checks that missing start result selection falls back to the first result.
    @Test
    fun `selectNextResult selects first result when supplied result is missing`() {
        val context = GameOpeningAnalysisRuntimeContext()
        context.addImportedGames(
            listOf(parsedCandidate(sourceIndex = 0, event = "A", moves = listOf("e2e4"))),
        )
        val result = resultForGame(context.importedGames.single(), matchesKnownOpeningResult())
        context.replaceAnalysisResults(listOf(result))

        assertTrue(context.selectNextResult(gameId = 999L))
        assertEquals(result.gameId, context.selectedResultGameId)
    }

    // Checks that deleting a result game keeps detail mode focused on the following result.
    @Test
    fun `deleteAnalysisResultGame removes game and opens next result detail`() {
        val context = GameOpeningAnalysisRuntimeContext()
        context.addImportedGames(
            listOf(
                parsedCandidate(sourceIndex = 0, event = "A", moves = listOf("e2e4")),
                parsedCandidate(sourceIndex = 1, event = "B", moves = listOf("d2d4")),
            ),
        )
        val firstResult = resultForGame(context.importedGames[0], matchesKnownOpeningResult())
        val secondResult = resultForGame(context.importedGames[1], matchesKnownOpeningResult())
        context.replaceAnalysisResults(listOf(firstResult, secondResult))
        context.selectResult(firstResult.gameId)
        context.openSelectedResultDetail()

        val wasDeleted = context.deleteAnalysisResultGame(firstResult.gameId)

        assertTrue(wasDeleted)
        assertEquals(listOf(secondResult.gameId), context.importedGames.map { game -> game.id })
        assertEquals(listOf(secondResult.gameId), context.analysisResults.map { result -> result.gameId })
        assertEquals(secondResult.gameId, context.selectedResultGameId)
        assertEquals(GameOpeningAnalysisView.ANALYSIS_RESULT_DETAIL, context.currentView)
    }

    // Checks that deleting the final result game leaves no stale detail selection.
    @Test
    fun `deleteAnalysisResultGame returns to imported games after final result`() {
        val context = GameOpeningAnalysisRuntimeContext()
        context.addImportedGames(
            listOf(parsedCandidate(sourceIndex = 0, event = "A", moves = listOf("e2e4"))),
        )
        val result = resultForGame(context.importedGames.single(), matchesKnownOpeningResult())
        context.replaceAnalysisResults(listOf(result))
        context.selectResult(result.gameId)
        context.openSelectedResultDetail()

        val wasDeleted = context.deleteAnalysisResultGame(result.gameId)

        assertTrue(wasDeleted)
        assertTrue(context.importedGames.isEmpty())
        assertTrue(context.analysisResults.isEmpty())
        assertNull(context.selectedResultGameId)
        assertEquals(GameOpeningAnalysisView.IMPORTED_GAMES, context.currentView)
    }

    // Checks that the next deviation result is selected after the supplied result id, skipping non-deviation results.
    @Test
    fun `selectNextDeviation selects next deviation after supplied result`() {
        val context = GameOpeningAnalysisRuntimeContext()
        context.addImportedGames(
            listOf(
                parsedCandidate(sourceIndex = 0, event = "A", moves = listOf("e2e4")),
                parsedCandidate(sourceIndex = 1, event = "B", moves = listOf("d2d4")),
                parsedCandidate(sourceIndex = 2, event = "C", moves = listOf("c2c4")),
            ),
        )
        val firstDeviation = resultForGame(context.importedGames[0], deviationResult())
        val nonDeviation = resultForGame(context.importedGames[1], matchesKnownOpeningResult())
        val nextDeviation = resultForGame(context.importedGames[2], deviationResult())
        context.replaceAnalysisResults(listOf(firstDeviation, nonDeviation, nextDeviation))

        val wasSelected = context.selectNextDeviation(firstDeviation.gameId)

        assertTrue(wasSelected)
        assertEquals(nextDeviation.gameId, context.selectedResultGameId)
        assertEquals(GameOpeningAnalysisView.ANALYSIS_RESULT_DETAIL, context.currentView)
    }

    // Checks that next deviation selection wraps to the beginning and returns false when no other deviation exists.
    @Test
    fun `selectNextDeviation wraps and ignores the supplied result`() {
        val context = GameOpeningAnalysisRuntimeContext()
        context.addImportedGames(
            listOf(
                parsedCandidate(sourceIndex = 0, event = "A", moves = listOf("e2e4")),
                parsedCandidate(sourceIndex = 1, event = "B", moves = listOf("d2d4")),
            ),
        )
        val firstDeviation = resultForGame(context.importedGames[0], deviationResult())
        val lastDeviation = resultForGame(context.importedGames[1], deviationResult())
        context.replaceAnalysisResults(listOf(firstDeviation, lastDeviation))

        val wasSelected = context.selectNextDeviation(lastDeviation.gameId)

        assertTrue(wasSelected)
        assertEquals(firstDeviation.gameId, context.selectedResultGameId)

        context.replaceAnalysisResults(listOf(lastDeviation))

        assertFalse(context.selectNextDeviation(lastDeviation.gameId))
    }

    // Checks that a missing start result falls back to the first available deviation result.
    @Test
    fun `selectNextDeviation selects first deviation when supplied result is missing`() {
        val context = GameOpeningAnalysisRuntimeContext()
        context.addImportedGames(
            listOf(
                parsedCandidate(sourceIndex = 0, event = "A", moves = listOf("e2e4")),
                parsedCandidate(sourceIndex = 1, event = "B", moves = listOf("d2d4")),
            ),
        )
        val nonDeviation = resultForGame(context.importedGames[0], matchesKnownOpeningResult())
        val deviation = resultForGame(context.importedGames[1], deviationResult())
        context.replaceAnalysisResults(listOf(nonDeviation, deviation))

        val wasSelected = context.selectNextDeviation(gameId = 999L)

        assertTrue(wasSelected)
        assertEquals(deviation.gameId, context.selectedResultGameId)
        assertEquals(GameOpeningAnalysisView.ANALYSIS_RESULT_DETAIL, context.currentView)
    }

    // Checks that result clearing also removes selected result and progress state.
    @Test
    fun `clearAnalysisResults clears selected result and progress`() {
        val context = GameOpeningAnalysisRuntimeContext()
        context.addImportedGames(listOf(parsedCandidate(sourceIndex = 0, event = "A", moves = listOf("e2e4"))))
        val result = resultForGame(context.importedGames.single(), invalidMoveResult())
        context.startAnalysis(totalCount = 1)
        context.replaceAnalysisResults(listOf(result))
        context.selectResult(result.gameId)

        context.clearAnalysisResults()

        assertTrue(context.analysisResults.isEmpty())
        assertNull(context.selectedResultGameId)
        assertNull(context.analysisProgress)
    }

    // Checks that both invalid-game result models are controlled by one UI result checkbox.
    @Test
    fun `result filter maps invalid move and invalid initial position to invalid games`() {
        val context = GameOpeningAnalysisRuntimeContext()
        context.setAnalysisOptions(
            GameOpeningAnalysisOptions(
                resultTypes = setOf(GameOpeningAnalysisOptions.ResultFilter.INVALID_GAMES),
            ),
        )

        assertTrue(context.shouldKeepResult(invalidMoveResult()))
        assertTrue(context.shouldKeepResult(invalidInitialPositionResult()))
        assertFalse(context.shouldKeepResult(deviationResult()))
    }

    // Checks mapping from analyzer result classes to selectable result filters.
    @Test
    fun `result filter maps each regular result type`() {
        val context = GameOpeningAnalysisRuntimeContext()
        context.setAnalysisOptions(
            GameOpeningAnalysisOptions(
                resultTypes =
                    setOf(
                        GameOpeningAnalysisOptions.ResultFilter.DEVIATION,
                        GameOpeningAnalysisOptions.ResultFilter.BOOK_TOO_SHORT,
                        GameOpeningAnalysisOptions.ResultFilter.MATCHES_KNOWN_OPENING,
                    ),
            ),
        )

        assertTrue(context.shouldKeepResult(deviationResult()))
        assertTrue(context.shouldKeepResult(bookTooShortResult()))
        assertTrue(context.shouldKeepResult(matchesKnownOpeningResult()))
        assertFalse(context.shouldKeepResult(opponentLeftBookResult()))
        assertFalse(context.shouldKeepResult(noMatchingOpeningResult()))
    }

    private fun parsedCandidate(
        sourceIndex: Int,
        event: String = "Game $sourceIndex",
        white: String = "White $sourceIndex",
        black: String = "Black $sourceIndex",
        headers: Map<String, String> =
            mapOf(
                "Event" to event,
                "White" to white,
                "Black" to black,
            ),
        moves: List<String>,
    ): ImportedGameCandidate =
        ImportedGameCandidate.Parsed(
            ParsedPgnGame(
                sourceIndex = sourceIndex,
                headers = headers,
                mainLineMoves = moves,
            ),
        )

    private fun resultForGame(
        game: ImportedGameItem,
        result: GameOpeningAnalysisResult,
    ): ImportedGameAnalysisResult =
        ImportedGameAnalysisResult(
            gameId = game.id,
            game = game,
            result = result,
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

    private fun opponentLeftBookResult(): GameOpeningOpponentLeftBook =
        GameOpeningOpponentLeftBook(
            selectedSide = OpeningSide.WHITE,
            matchMode = OpeningMatchMode.MOVE_SEQUENCE,
            positionFen = INITIAL_POSITION_FEN,
            ply = 1,
            playedMoveUci = "c7c5",
            playedResultFen = INITIAL_POSITION_FEN,
            expectedMoves = emptyList(),
            matchingLineRefs = emptyList(),
        )

    private fun bookTooShortResult(): GameOpeningBookTooShort =
        GameOpeningBookTooShort(
            selectedSide = OpeningSide.WHITE,
            matchMode = OpeningMatchMode.MOVE_SEQUENCE,
            lastKnownPositionFen = INITIAL_POSITION_FEN,
            matchedPly = 2,
            minimumKnownPrefixPly = 8,
            nextGameMoveUci = "g1f3",
            endedLineRefs = emptyList(),
        )

    private fun matchesKnownOpeningResult(): GameOpeningMatchesKnownOpening =
        GameOpeningMatchesKnownOpening(
            selectedSide = OpeningSide.WHITE,
            matchMode = OpeningMatchMode.MOVE_SEQUENCE,
            matchedPly = 2,
            finalPositionFen = INITIAL_POSITION_FEN,
            matchingLineRefs = emptyList(),
        )

    private fun noMatchingOpeningResult(): GameOpeningNoMatchingOpening =
        GameOpeningNoMatchingOpening(
            selectedSide = OpeningSide.WHITE,
            matchMode = OpeningMatchMode.MOVE_SEQUENCE,
            positionFen = INITIAL_POSITION_FEN,
            ply = 0,
            playedMoveUci = "d2d4",
            knownMoves = emptyList(),
        )

    private companion object {
        const val INITIAL_POSITION_FEN = "start"
    }
}
