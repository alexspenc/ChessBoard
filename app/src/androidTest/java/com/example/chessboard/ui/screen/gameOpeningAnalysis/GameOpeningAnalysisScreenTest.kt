@file:Suppress("FunctionName")

package com.example.chessboard.ui.screen.gameOpeningAnalysis

/*
 * File role: verifies the game-opening analysis screen shell, paste import flow, and first results view.
 * Allowed here:
 * - Compose tests for the imported-games screen shell, top bar, back callback, paste import dialog, filter dialog, analysis run dialog, and results list view
 * Not allowed here:
 * - Home navigation coverage, database access, file-picker behavior, or analyzer execution tests
 * Validation date: 2026-06-26
 */

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.chessboard.analysis.GameOpeningMatchesKnownOpening
import com.example.chessboard.analysis.OpeningMatchMode
import com.example.chessboard.analysis.OpeningSide
import com.example.chessboard.runtimecontext.GameOpeningAnalysisRuntimeContext
import com.example.chessboard.runtimecontext.GameOpeningAnalysisView
import com.example.chessboard.runtimecontext.GameOpeningBatchAnalysisSummary
import com.example.chessboard.runtimecontext.ImportedGameAnalysisResult
import com.example.chessboard.runtimecontext.ImportedGameCandidate
import com.example.chessboard.service.ParsedPgnGame
import com.example.chessboard.testing.fenStateDescriptionMatcher
import com.example.chessboard.testing.normalizeFenForAssertion
import com.example.chessboard.ui.GameOpeningAnalysisAddGamesTestTag
import com.example.chessboard.ui.GameOpeningAnalysisAnalyzeActionTestTag
import com.example.chessboard.ui.GameOpeningAnalysisContentTestTag
import com.example.chessboard.ui.GameOpeningAnalysisEmptyStateTestTag
import com.example.chessboard.ui.GameOpeningAnalysisFilterBlackSideTestTag
import com.example.chessboard.ui.GameOpeningAnalysisFilterCaseSensitiveTestTag
import com.example.chessboard.ui.GameOpeningAnalysisFilterExactMatchTestTag
import com.example.chessboard.ui.GameOpeningAnalysisFilterMinPlyTestTag
import com.example.chessboard.ui.GameOpeningAnalysisFilterPlayerNameTestTag
import com.example.chessboard.ui.GameOpeningAnalysisGameListTestTag
import com.example.chessboard.ui.GameOpeningAnalysisImportConfirmTestTag
import com.example.chessboard.ui.GameOpeningAnalysisImportDialogTestTag
import com.example.chessboard.ui.GameOpeningAnalysisImportFromFileTestTag
import com.example.chessboard.ui.GameOpeningAnalysisImportSummaryDialogTestTag
import com.example.chessboard.ui.GameOpeningAnalysisImportTextInputTestTag
import com.example.chessboard.ui.GameOpeningAnalysisNextGamesPageTestTag
import com.example.chessboard.ui.GameOpeningAnalysisNextMoveTestTag
import com.example.chessboard.ui.GameOpeningAnalysisOptionsAnalyzeTestTag
import com.example.chessboard.ui.GameOpeningAnalysisOptionsDialogTestTag
import com.example.chessboard.ui.GameOpeningAnalysisPreviewTestTag
import com.example.chessboard.ui.GameOpeningAnalysisPreviousGamesPageTestTag
import com.example.chessboard.ui.GameOpeningAnalysisPreviousMoveTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultDetailActionTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultDetailBoardTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultDetailContentTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultListTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultPreviewBoardTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultPreviewTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultsContentTestTag
import com.example.chessboard.ui.GameOpeningAnalysisSearchActionTestTag
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Rule
import org.junit.Test

class GameOpeningAnalysisScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun gameOpeningAnalysisScreen_emptyStateShowsSummaryAndHandlesBackClick() {
        // Scenario: an empty runtime context shows the imported-games empty state and wires top-bar navigation.
        var backClicks = 0
        var homeClicks = 0

        setScreenContent(
            runtimeContext = GameOpeningAnalysisRuntimeContext(),
            onBackClick = { backClicks++ },
            onHomeClick = { homeClicks++ },
        )

        composeRule.onNodeWithTag(GameOpeningAnalysisContentTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(GameOpeningAnalysisEmptyStateTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("Games: 0 • Page 1/1").assertIsDisplayed()
        composeRule.onNodeWithText("No imported games.").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.runOnIdle {
            check(backClicks == 1) {
                "Expected one back click, got $backClicks"
            }
        }

        composeRule.onNodeWithContentDescription("Home").performClick()

        composeRule.runOnIdle {
            check(homeClicks == 1) {
                "Expected one home click, got $homeClicks"
            }
        }
    }

    @Test
    fun gameOpeningAnalysisScreen_rendersImportedGames() {
        // Scenario: imported games are rendered with event, players, and half-move count metadata.
        val runtimeContext = GameOpeningAnalysisRuntimeContext()
        runtimeContext.addImportedGames(
            listOf(
                parsedCandidate(
                    sourceIndex = 0,
                    event = "London System",
                    white = "Alice",
                    black = "Bob",
                    moves = listOf("d2d4", "d7d5", "g1f3"),
                ),
                parsedCandidate(
                    sourceIndex = 1,
                    event = "Sicilian Game",
                    white = "Carol",
                    black = "Dave",
                    moves = listOf("e2e4", "c7c5"),
                ),
            ),
        )

        setScreenContent(runtimeContext = runtimeContext)

        composeRule.onNodeWithText("Games: 2 • Page 1/1").assertIsDisplayed()
        composeRule.onNodeWithText("Imported games are shown in import order.").assertIsDisplayed()
        composeRule.onNodeWithText("London System").assertIsDisplayed()
        composeRule.onNodeWithText("Alice - Bob").assertIsDisplayed()
        composeRule.onNodeWithText("Ply: 3").assertIsDisplayed()
        composeRule.onNodeWithText("Sicilian Game").assertIsDisplayed()
        composeRule.onNodeWithText("Carol - Dave").assertIsDisplayed()
        composeRule.onNodeWithText("Ply: 2").assertIsDisplayed()
    }

    @Test
    fun gameOpeningAnalysisScreen_selectingGameShowsPreviewAndStoresSelection() {
        // Scenario: selecting a game stores it in runtime context and expands the inline preview.
        val runtimeContext = GameOpeningAnalysisRuntimeContext()
        runtimeContext.addImportedGames(
            listOf(
                parsedCandidate(
                    sourceIndex = 0,
                    event = "London System",
                    white = "Alice",
                    black = "Bob",
                    moves = listOf("d2d4", "d7d5"),
                ),
            ),
        )

        setScreenContent(runtimeContext = runtimeContext)

        composeRule.onAllNodesWithTag(GameOpeningAnalysisGameListTestTag)[0].performClick()

        composeRule.onNodeWithTag(GameOpeningAnalysisPreviewTestTag).assertIsDisplayed()
        composeRule.runOnIdle {
            check(runtimeContext.selectedGameId == 1L) {
                "Expected selected game id 1, got ${runtimeContext.selectedGameId}"
            }
        }
    }

    @Test
    fun gameOpeningAnalysisScreen_previewMoveControlsNavigateSelectedGame() {
        // Scenario: the preview bottom bar navigates the selected game through its move list.
        val runtimeContext = GameOpeningAnalysisRuntimeContext()
        runtimeContext.addImportedGames(
            listOf(
                parsedCandidate(
                    sourceIndex = 0,
                    event = "Control Game",
                    moves = listOf("e2e4", "e7e5"),
                ),
            ),
        )

        setScreenContent(runtimeContext = runtimeContext)

        composeRule.onNodeWithTag(GameOpeningAnalysisPreviousMoveTestTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(GameOpeningAnalysisNextMoveTestTag).assertIsNotEnabled()

        composeRule.onAllNodesWithTag(GameOpeningAnalysisGameListTestTag)[0].performClick()

        composeRule.onNodeWithTag(GameOpeningAnalysisPreviousMoveTestTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(GameOpeningAnalysisNextMoveTestTag).assertIsEnabled()

        composeRule.onNodeWithTag(GameOpeningAnalysisNextMoveTestTag).performClick()

        composeRule.onNodeWithTag(GameOpeningAnalysisPreviousMoveTestTag).assertIsEnabled()
        composeRule.onNodeWithTag(GameOpeningAnalysisNextMoveTestTag).assertIsEnabled()

        composeRule.onNodeWithTag(GameOpeningAnalysisNextMoveTestTag).performClick()

        composeRule.onNodeWithTag(GameOpeningAnalysisPreviousMoveTestTag).assertIsEnabled()
        composeRule.onNodeWithTag(GameOpeningAnalysisNextMoveTestTag).assertIsNotEnabled()

        composeRule.onNodeWithTag(GameOpeningAnalysisPreviousMoveTestTag).performClick()

        composeRule.onNodeWithTag(GameOpeningAnalysisNextMoveTestTag).assertIsEnabled()
    }

    @Test
    fun gameOpeningAnalysisScreen_addGamesOpensImportDialog() {
        // Scenario: the add-games action opens the paste import dialog with a file import action.
        val runtimeContext = GameOpeningAnalysisRuntimeContext()

        setScreenContent(runtimeContext = runtimeContext)

        composeRule.onNodeWithTag(GameOpeningAnalysisAddGamesTestTag).performClick()

        composeRule.onNodeWithTag(GameOpeningAnalysisImportDialogTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("Add Games").assertIsDisplayed()
        composeRule.onNodeWithText("PGN text").assertIsDisplayed()
        composeRule.onNodeWithTag(GameOpeningAnalysisImportConfirmTestTag).assertIsNotEnabled()
        composeRule
            .onNodeWithTag(GameOpeningAnalysisImportFromFileTestTag)
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun gameOpeningAnalysisScreen_importPgnClosesDialogShowsSummaryAndAddsGame() {
        // Scenario: pasted PGN is imported into runtime context, closes the import dialog, and shows summary counts.
        val runtimeContext = GameOpeningAnalysisRuntimeContext()
        val pgnText =
            """
            [Event "Imported Event"]
            [White "Alice"]
            [Black "Bob"]

            1. e4 e5 *
            """.trimIndent()

        setScreenContent(runtimeContext = runtimeContext)

        composeRule.onNodeWithTag(GameOpeningAnalysisAddGamesTestTag).performClick()
        composeRule.onNodeWithTag(GameOpeningAnalysisImportTextInputTestTag).performTextInput(pgnText)
        composeRule.onNodeWithTag(GameOpeningAnalysisImportConfirmTestTag).performClick()

        waitForNodeWithTag(GameOpeningAnalysisImportSummaryDialogTestTag)
        composeRule.onAllNodesWithTag(GameOpeningAnalysisImportDialogTestTag).fetchSemanticsNodes().let { nodes ->
            check(nodes.isEmpty()) {
                "Expected import dialog to close after import"
            }
        }
        composeRule.onNodeWithTag(GameOpeningAnalysisImportSummaryDialogTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("Import Summary").assertIsDisplayed()

        composeRule.onNodeWithText("OK").performClick()

        composeRule.onNodeWithText("Games: 1 • Page 1/1").assertIsDisplayed()
        composeRule.onNodeWithText("Imported Event").assertIsDisplayed()
        composeRule.onNodeWithText("Alice - Bob").assertIsDisplayed()
        composeRule.runOnIdle {
            check(runtimeContext.importedGames.size == 1) {
                "Expected one imported game, got ${runtimeContext.importedGames.size}"
            }
        }
    }

    @Test
    fun gameOpeningAnalysisScreen_filterDialogFiltersByPlayerSideNameMatchModeCaseAndMinPly() {
        // Scenario: applying the filter uses selected player color, exact case-sensitive name, and minimum ply.
        val runtimeContext = GameOpeningAnalysisRuntimeContext()
        runtimeContext.addImportedGames(
            listOf(
                parsedCandidate(
                    sourceIndex = 0,
                    event = "Target Game",
                    white = "Alice",
                    black = "Bob",
                    moves = listOf("e2e4", "e7e5", "g1f3"),
                ),
                parsedCandidate(
                    sourceIndex = 1,
                    event = "Case Miss",
                    white = "Alice",
                    black = "bob",
                    moves = listOf("e2e4", "e7e5", "g1f3"),
                ),
                parsedCandidate(
                    sourceIndex = 2,
                    event = "Contains Miss",
                    white = "Alice",
                    black = "Bobby",
                    moves = listOf("e2e4", "e7e5", "g1f3"),
                ),
                parsedCandidate(
                    sourceIndex = 3,
                    event = "Short Miss",
                    white = "Alice",
                    black = "Bob",
                    moves = listOf("e2e4", "e7e5"),
                ),
            ),
        )

        setScreenContent(runtimeContext = runtimeContext)

        composeRule.onNodeWithTag(GameOpeningAnalysisSearchActionTestTag).performClick()
        composeRule.onNodeWithTag(GameOpeningAnalysisFilterBlackSideTestTag).performClick()
        composeRule.onNodeWithTag(GameOpeningAnalysisFilterPlayerNameTestTag).performTextInput("Bob")
        composeRule.onNodeWithTag(GameOpeningAnalysisFilterExactMatchTestTag).performClick()
        composeRule.onNodeWithTag(GameOpeningAnalysisFilterCaseSensitiveTestTag).performClick()
        composeRule.onNodeWithTag(GameOpeningAnalysisFilterMinPlyTestTag).performTextInput("3")
        composeRule.onNodeWithText("Apply").performClick()

        composeRule.onNodeWithText("Target Game").assertIsDisplayed()
        assertTextIsAbsent("Case Miss")
        assertTextIsAbsent("Contains Miss")
        assertTextIsAbsent("Short Miss")
    }

    @Test
    fun gameOpeningAnalysisScreen_analyzeDialogRunsAnalysisAndStoresResults() {
        // Scenario: analyze stores results, opens result detail with board, and returns through results to imported games.
        val runtimeContext = GameOpeningAnalysisRuntimeContext()
        val expectedFinalFen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
        runtimeContext.addImportedGames(
            listOf(
                parsedCandidate(
                    sourceIndex = 0,
                    event = "Analysis Game",
                    moves = listOf("e2e4", "e7e5"),
                ),
            ),
        )

        setScreenContent(
            runtimeContext = runtimeContext,
            analysisRunner = { context, options, _ ->
                val game = context.filteredGames().single()
                context.setAnalysisOptions(options)
                context.replaceAnalysisResults(
                    listOf(
                        ImportedGameAnalysisResult(
                            gameId = game.id,
                            game = game,
                            result =
                                GameOpeningMatchesKnownOpening(
                                    selectedSide = OpeningSide.WHITE,
                                    matchMode = OpeningMatchMode.MOVE_SEQUENCE,
                                    matchedPly = game.mainLineMoves.size,
                                    finalPositionFen = expectedFinalFen,
                                    matchingLineRefs = emptyList(),
                                ),
                        ),
                    ),
                )
                GameOpeningBatchAnalysisSummary(
                    analyzedCount = 1,
                    keptResultCount = 1,
                    wasCancelled = false,
                )
            },
        )

        composeRule.onNodeWithTag(GameOpeningAnalysisAnalyzeActionTestTag).performClick()
        composeRule.onNodeWithTag(GameOpeningAnalysisOptionsDialogTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(GameOpeningAnalysisOptionsAnalyzeTestTag).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runtimeContext.analysisResults.size == 1 &&
                runtimeContext.currentView == GameOpeningAnalysisView.ANALYSIS_RESULTS
        }
        composeRule.onNodeWithTag(GameOpeningAnalysisResultsContentTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("Analysis Results").assertIsDisplayed()
        composeRule.onNodeWithText("Results: 1 • Showing: 1").assertIsDisplayed()
        composeRule.onNodeWithText("Analysis Game").assertIsDisplayed()
        composeRule.onNodeWithText("Matches known opening").assertIsDisplayed()
        composeRule.onNodeWithText("Matched ply: 2").assertIsDisplayed()

        composeRule.onAllNodesWithTag(GameOpeningAnalysisResultListTestTag)[0].performClick()

        composeRule.onNodeWithTag(GameOpeningAnalysisResultPreviewTestTag).assertIsDisplayed()
        assertBoardFenEventually(
            boardTag = GameOpeningAnalysisResultPreviewBoardTestTag,
            expectedFen = expectedFinalFen,
        )

        composeRule.onNodeWithTag(GameOpeningAnalysisResultDetailActionTestTag).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runtimeContext.currentView == GameOpeningAnalysisView.ANALYSIS_RESULT_DETAIL
        }
        composeRule.onNodeWithTag(GameOpeningAnalysisResultDetailContentTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("Analysis Result").assertIsDisplayed()
        composeRule.onNodeWithText("Analyzed game").assertIsDisplayed()
        assertBoardFenEventually(
            boardTag = GameOpeningAnalysisResultDetailBoardTestTag,
            expectedFen = expectedFinalFen,
        )
        composeRule
            .onNodeWithTag(GameOpeningAnalysisResultDetailContentTestTag)
            .performScrollToNode(hasText("Possible continuations"))
        composeRule.onNodeWithText("Possible continuations").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithTag(GameOpeningAnalysisResultsContentTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("Analysis Results").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithText("Compare").assertIsDisplayed()
        composeRule.onNodeWithText("Games: 1 • Page 1/1").assertIsDisplayed()
    }

    @Test
    fun gameOpeningAnalysisScreen_analyzeWithoutResultsShowsNoResultsDialog() {
        // Scenario: a completed analysis with no kept results stays on imported games and explains the empty output.
        val runtimeContext = GameOpeningAnalysisRuntimeContext()
        runtimeContext.addImportedGames(
            listOf(
                parsedCandidate(
                    sourceIndex = 0,
                    event = "No Result Game",
                    moves = listOf("e2e4", "e7e5"),
                ),
            ),
        )

        setScreenContent(runtimeContext = runtimeContext)

        composeRule.onNodeWithTag(GameOpeningAnalysisAnalyzeActionTestTag).performClick()
        composeRule.onNodeWithTag(GameOpeningAnalysisOptionsAnalyzeTestTag).performClick()

        composeRule.onNodeWithText("No Results").assertIsDisplayed()
        composeRule.onNodeWithText("No results matched selected result filters.").assertIsDisplayed()
        composeRule.runOnIdle {
            check(runtimeContext.currentView == GameOpeningAnalysisView.IMPORTED_GAMES) {
                "Expected imported-games view after empty analysis"
            }
        }
    }

    @Test
    fun gameOpeningAnalysisScreen_limitsVisibleGamesToFirstTwenty() {
        // Scenario: the screen uses runtime paging and renders only the first page of imported games.
        val runtimeContext = GameOpeningAnalysisRuntimeContext()
        runtimeContext.addImportedGames(
            (1..25).map { index ->
                parsedCandidate(
                    sourceIndex = index,
                    event = "Imported Game $index",
                    moves = listOf("move-$index"),
                )
            },
        )

        setScreenContent(runtimeContext = runtimeContext)

        composeRule.onNodeWithText("Games: 25 • Page 1/2").assertIsDisplayed()
        composeRule.onNodeWithTag(GameOpeningAnalysisPreviousGamesPageTestTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(GameOpeningAnalysisNextGamesPageTestTag).assertIsEnabled()
        composeRule.onNodeWithText("Imported Game 1").assertIsDisplayed()
        composeRule.onAllNodesWithText("Imported Game 20").fetchSemanticsNodes().let { nodes ->
            check(nodes.isNotEmpty()) {
                "Expected Imported Game 20 to be part of the visible page"
            }
        }
        assertTextIsAbsent("Imported Game 21")

        composeRule.onNodeWithTag(GameOpeningAnalysisNextGamesPageTestTag).performClick()

        composeRule.onNodeWithText("Games: 25 • Page 2/2").assertIsDisplayed()
        composeRule.onNodeWithTag(GameOpeningAnalysisPreviousGamesPageTestTag).assertIsEnabled()
        composeRule.onNodeWithTag(GameOpeningAnalysisNextGamesPageTestTag).assertIsNotEnabled()
        composeRule.onNodeWithText("Imported Game 21").assertIsDisplayed()
        composeRule.onNodeWithText("Imported Game 25").assertIsDisplayed()
        assertTextIsAbsent("Imported Game 1")
    }

    private fun assertTextIsAbsent(text: String) {
        composeRule.onAllNodesWithText(text).fetchSemanticsNodes().let { nodes ->
            check(nodes.isEmpty()) {
                "Expected $text to be absent"
            }
        }
    }

    private fun waitForNodeWithTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag(tag).fetchSemanticsNode()
                true
            }.getOrDefault(false)
        }
    }

    private fun assertBoardFenEventually(
        boardTag: String,
        expectedFen: String,
    ) {
        val normalizedExpectedFen = normalizeFenForAssertion(expectedFen)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            currentBoardFen(boardTag)?.let(::normalizeFenForAssertion) == normalizedExpectedFen
        }
        composeRule.onNodeWithTag(boardTag, useUnmergedTree = true).assert(
            fenStateDescriptionMatcher(expectedFen),
        )
    }

    private fun currentBoardFen(boardTag: String): String? =
        runCatching {
            composeRule
                .onNodeWithTag(boardTag, useUnmergedTree = true)
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.StateDescription)
        }.getOrNull()

    private fun setScreenContent(
        runtimeContext: GameOpeningAnalysisRuntimeContext,
        onBackClick: () -> Unit = {},
        onHomeClick: () -> Unit = {},
        analysisRunner: GameOpeningAnalysisRunner = { context, options, _ ->
            context.setAnalysisOptions(options)
            context.replaceAnalysisResults(emptyList())
            GameOpeningBatchAnalysisSummary(
                analyzedCount = 0,
                keptResultCount = 0,
                wasCancelled = false,
            )
        },
    ) {
        composeRule.setContent {
            ChessBoardTheme {
                GameOpeningAnalysisScreen(
                    runtimeContext = runtimeContext,
                    onBackClick = onBackClick,
                    onHomeClick = onHomeClick,
                    analysisRunner = analysisRunner,
                )
            }
        }
    }

    private fun parsedCandidate(
        sourceIndex: Int,
        event: String,
        white: String = "White $sourceIndex",
        black: String = "Black $sourceIndex",
        moves: List<String>,
    ): ImportedGameCandidate =
        ImportedGameCandidate.Parsed(
            ParsedPgnGame(
                sourceIndex = sourceIndex,
                headers =
                    mapOf(
                        "Event" to event,
                        "White" to white,
                        "Black" to black,
                    ),
                mainLineMoves = moves,
            ),
        )
}
