@file:Suppress("FunctionName")

package com.example.chessboard.ui.screen.gameOpeningAnalysis

/*
 * File role: verifies the game-opening analysis screen shell, paste import flow, and first results view.
 * Allowed here:
 * - Compose tests for the imported-games screen shell, top bar, back callback, paste import dialog, filter dialog, analysis run dialog, results list, and result detail actions
 * Not allowed here:
 * - Home navigation coverage, database access, file-picker behavior, or analyzer execution tests
 * Validation date: 2026-07-02
 */

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.chessboard.analysis.GameOpeningAnalysisResult
import com.example.chessboard.analysis.GameOpeningDeviation
import com.example.chessboard.analysis.GameOpeningExpectedMove
import com.example.chessboard.analysis.GameOpeningMatchesKnownOpening
import com.example.chessboard.analysis.OpeningBookLineRef
import com.example.chessboard.analysis.OpeningMatchMode
import com.example.chessboard.analysis.OpeningSide
import com.example.chessboard.runtimecontext.GameOpeningAnalysisFilter
import com.example.chessboard.runtimecontext.GameOpeningAnalysisRuntimeContext
import com.example.chessboard.runtimecontext.GameOpeningAnalysisView
import com.example.chessboard.runtimecontext.GameOpeningBatchAnalysisSummary
import com.example.chessboard.runtimecontext.ImportedGameAnalysisResult
import com.example.chessboard.runtimecontext.ImportedGameCandidate
import com.example.chessboard.runtimecontext.ImportedGameItem
import com.example.chessboard.service.ParsedPgnGame
import com.example.chessboard.testing.fenStateDescriptionMatcher
import com.example.chessboard.testing.normalizeFenForAssertion
import com.example.chessboard.ui.GameOpeningAnalysisAddGamesTestTag
import com.example.chessboard.ui.GameOpeningAnalysisAnalyzeActionTestTag
import com.example.chessboard.ui.GameOpeningAnalysisClearFilterTestTag
import com.example.chessboard.ui.GameOpeningAnalysisContentTestTag
import com.example.chessboard.ui.GameOpeningAnalysisDeleteGameConfirmTestTag
import com.example.chessboard.ui.GameOpeningAnalysisDeleteGameTestTag
import com.example.chessboard.ui.GameOpeningAnalysisEmptyStateTestTag
import com.example.chessboard.ui.GameOpeningAnalysisFilterBlackSideTestTag
import com.example.chessboard.ui.GameOpeningAnalysisFilterCaseSensitiveTestTag
import com.example.chessboard.ui.GameOpeningAnalysisFilterDialogTestTag
import com.example.chessboard.ui.GameOpeningAnalysisFilterExactMatchTestTag
import com.example.chessboard.ui.GameOpeningAnalysisFilterMinPlyTestTag
import com.example.chessboard.ui.GameOpeningAnalysisFilterPlayerNameTestTag
import com.example.chessboard.ui.GameOpeningAnalysisGameListTestTag
import com.example.chessboard.ui.GameOpeningAnalysisGameActionsTestTag
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
import com.example.chessboard.ui.GameOpeningAnalysisRecordDeviationMistakeTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultCopyPgnActionTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultDetailActionTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultDetailBoardTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultDetailContentTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultDeleteActionTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultListTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultPgnCopiedDialogTestTag
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
        assertGamesPageSubtitle(gamesCount = 0, currentPage = 1, totalPages = 1)
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
    fun gameOpeningAnalysisScreen_emptyStateHidesImportedGameTopActions() {
        // Scenario: with no imported games, top actions show only navigation and hide filter and paging controls.
        setScreenContent(runtimeContext = GameOpeningAnalysisRuntimeContext())

        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Home").assertIsDisplayed()
        assertTagIsAbsent(GameOpeningAnalysisSearchActionTestTag)
        assertTagIsAbsent(GameOpeningAnalysisClearFilterTestTag)
        assertTagIsAbsent(GameOpeningAnalysisPreviousGamesPageTestTag)
        assertTagIsAbsent(GameOpeningAnalysisNextGamesPageTestTag)
    }

    @Test
    fun gameOpeningAnalysisScreen_emptyStateShowsOnlyAddInBottomBar() {
        // Scenario: with no imported games, the bottom bar keeps only the add-games action.
        setScreenContent(runtimeContext = GameOpeningAnalysisRuntimeContext())

        composeRule.onNodeWithTag(GameOpeningAnalysisAddGamesTestTag).assertIsDisplayed()
        assertTagIsAbsent(GameOpeningAnalysisDeleteGameTestTag)
        assertTagIsAbsent(GameOpeningAnalysisAnalyzeActionTestTag)
        assertTagIsAbsent(GameOpeningAnalysisGameActionsTestTag)
        assertTagIsAbsent(GameOpeningAnalysisPreviousMoveTestTag)
        assertTagIsAbsent(GameOpeningAnalysisNextMoveTestTag)
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

        assertGamesPageSubtitle(gamesCount = 2, currentPage = 1, totalPages = 1)
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
    fun gameOpeningAnalysisScreen_deletesSelectedGameAfterConfirmation() {
        // Scenario: delete is unavailable without a selection, then confirms and removes the selected imported game.
        val runtimeContext = GameOpeningAnalysisRuntimeContext()
        runtimeContext.addImportedGames(
            listOf(
                parsedCandidate(
                    sourceIndex = 0,
                    event = "Keep Game",
                    moves = listOf("e2e4"),
                ),
                parsedCandidate(
                    sourceIndex = 1,
                    event = "Remove Target",
                    moves = listOf("d2d4"),
                ),
            ),
        )

        setScreenContent(runtimeContext = runtimeContext)

        composeRule.onNodeWithTag(GameOpeningAnalysisDeleteGameTestTag).assertIsNotEnabled()

        composeRule.onAllNodesWithTag(GameOpeningAnalysisGameListTestTag)[1].performClick()

        composeRule.onNodeWithTag(GameOpeningAnalysisDeleteGameTestTag).assertIsEnabled()
        composeRule.onNodeWithTag(GameOpeningAnalysisDeleteGameTestTag).performClick()
        composeRule.onNodeWithText("Delete Game").assertIsDisplayed()
        composeRule.onNodeWithText("Delete \"Remove Target\"?").assertIsDisplayed()
        composeRule.onNodeWithTag(GameOpeningAnalysisDeleteGameConfirmTestTag).performClick()

        composeRule.onNodeWithText("Keep Game").assertIsDisplayed()
        assertTextIsAbsent("Remove Target")
        composeRule.runOnIdle {
            check(runtimeContext.importedGames.map { game -> game.headers["Event"] } == listOf("Keep Game")) {
                "Expected only Keep Game after deletion"
            }
            check(runtimeContext.selectedGameId == null) {
                "Expected selected game to be cleared after deletion"
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
        composeRule
            .onNodeWithText(
                """
                Scanned: 1
                Added: 1
                Skipped duplicates: 0
                Skipped parse errors: 0
                """.trimIndent(),
            )
            .assertIsDisplayed()

        composeRule.onNodeWithText("OK").performClick()

        assertGamesPageSubtitle(gamesCount = 1, currentPage = 1, totalPages = 1)
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
        composeRule.onNodeWithText("Apply").assertIsNotEnabled()
        composeRule.onNodeWithTag(GameOpeningAnalysisFilterBlackSideTestTag).performClick()
        composeRule.onNodeWithTag(GameOpeningAnalysisFilterPlayerNameTestTag).performTextInput("Bob")
        composeRule.onNodeWithText("Apply").assertIsEnabled()
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
    fun gameOpeningAnalysisScreen_analyzeImmediatelyAfterApplyingFilterOpensOptionsDialog() {
        // Scenario: a freshly applied player filter must be visible to the analyze action without leaving the screen.
        val runtimeContext = GameOpeningAnalysisRuntimeContext()
        runtimeContext.addImportedGames(
            listOf(
                parsedCandidate(
                    sourceIndex = 0,
                    event = "Target Game",
                    white = "Alice",
                    black = "Bob",
                    moves = listOf("e2e4", "e7e5"),
                ),
            ),
        )

        setScreenContent(runtimeContext = runtimeContext)

        composeRule.onNodeWithTag(GameOpeningAnalysisSearchActionTestTag).performClick()
        composeRule.onNodeWithTag(GameOpeningAnalysisFilterPlayerNameTestTag).performTextInput("Alice")
        composeRule.onNodeWithText("Apply").performClick()
        composeRule.onNodeWithTag(GameOpeningAnalysisAnalyzeActionTestTag).performClick()

        composeRule.onNodeWithTag(GameOpeningAnalysisOptionsDialogTestTag).assertIsDisplayed()
        assertTextIsAbsent("No Filtered Games")
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
        runtimeContext.updateFilter(GameOpeningAnalysisFilter(playerNameQuery = "White 0"))

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
        assertResultsSubtitle(resultsCount = 1, showingCount = 1)
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
        assertGamesPageSubtitle(gamesCount = 1, currentPage = 1, totalPages = 1)
    }

    @Test
    fun gameOpeningAnalysisScreen_deviationDetailShowsRecordMistakeAction() {
        // Scenario: deviation details expose the action that records a training mistake for affected opening lines.
        val runtimeContext = GameOpeningAnalysisRuntimeContext()
        runtimeContext.addImportedGames(
            listOf(parsedCandidate(sourceIndex = 0, event = "Deviation Game", moves = listOf("e2e4", "e7e5"))),
        )
        val result = resultForGame(runtimeContext.importedGames.single(), deviationResult())
        runtimeContext.replaceAnalysisResults(listOf(result))
        runtimeContext.selectResult(result.gameId)
        runtimeContext.openSelectedResultDetail()

        setScreenContent(runtimeContext = runtimeContext)

        composeRule.onNodeWithTag(GameOpeningAnalysisResultCopyPgnActionTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(GameOpeningAnalysisResultDeleteActionTestTag).assertIsDisplayed()
        composeRule
            .onNodeWithTag(GameOpeningAnalysisResultDetailContentTestTag)
            .performScrollToNode(hasTestTag(GameOpeningAnalysisRecordDeviationMistakeTestTag))
        composeRule.onNodeWithTag(GameOpeningAnalysisRecordDeviationMistakeTestTag).assertIsDisplayed()
    }

    @Test
    fun gameOpeningAnalysisScreen_nonDeviationDetailDoesNotShowRecordMistakeAction() {
        // Scenario: non-deviation details do not expose the training mistake action.
        val runtimeContext = GameOpeningAnalysisRuntimeContext()
        runtimeContext.addImportedGames(
            listOf(parsedCandidate(sourceIndex = 0, event = "Known Game", moves = listOf("e2e4", "e7e5"))),
        )
        val result = resultForGame(runtimeContext.importedGames.single(), matchesKnownOpeningResult())
        runtimeContext.replaceAnalysisResults(listOf(result))
        runtimeContext.selectResult(result.gameId)
        runtimeContext.openSelectedResultDetail()

        setScreenContent(runtimeContext = runtimeContext)

        composeRule.onNodeWithTag(GameOpeningAnalysisResultCopyPgnActionTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(GameOpeningAnalysisResultDeleteActionTestTag).assertIsDisplayed()
        assertTagIsAbsent(GameOpeningAnalysisRecordDeviationMistakeTestTag)
    }

    @Test
    fun gameOpeningAnalysisScreen_resultDetailCopiesCompleteGamePgn() {
        // Scenario: every result detail can copy its complete analyzed game as normalized PGN.
        val runtimeContext = GameOpeningAnalysisRuntimeContext()
        runtimeContext.addImportedGames(
            listOf(
                parsedCandidate(
                    sourceIndex = 0,
                    event = "Copy Complete Game",
                    white = "Alice",
                    black = "Bob",
                    moves = listOf("e2e4", "e7e5", "g1f3", "b8c6"),
                ),
            ),
        )
        val result = resultForGame(runtimeContext.importedGames.single(), matchesKnownOpeningResult())
        runtimeContext.replaceAnalysisResults(listOf(result))
        runtimeContext.selectResult(result.gameId)
        runtimeContext.openSelectedResultDetail()

        setScreenContent(runtimeContext = runtimeContext)

        composeRule.onNodeWithTag(GameOpeningAnalysisResultCopyPgnActionTestTag).performClick()

        composeRule
            .onNodeWithTag(GameOpeningAnalysisResultPgnCopiedDialogTestTag)
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("The complete game PGN was copied to the clipboard.")
            .assertIsDisplayed()
    }

    @Test
    fun gameOpeningAnalysisScreen_deleteResultDetailOpensNextResultDetail() {
        // Scenario: deleting from the analyzed-game card removes that game and keeps the next result open.
        val runtimeContext = GameOpeningAnalysisRuntimeContext()
        runtimeContext.addImportedGames(
            listOf(
                parsedCandidate(sourceIndex = 0, event = "Delete Detail", moves = listOf("e2e4", "e7e5")),
                parsedCandidate(sourceIndex = 1, event = "Next Detail", moves = listOf("d2d4", "d7d5")),
            ),
        )
        val deleteResult = resultForGame(runtimeContext.importedGames.first(), matchesKnownOpeningResult())
        val nextResult = resultForGame(runtimeContext.importedGames.last(), matchesKnownOpeningResult())
        runtimeContext.replaceAnalysisResults(listOf(deleteResult, nextResult))
        runtimeContext.selectResult(deleteResult.gameId)
        runtimeContext.openSelectedResultDetail()

        setScreenContent(runtimeContext = runtimeContext)

        composeRule.onNodeWithTag(GameOpeningAnalysisResultDeleteActionTestTag).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runtimeContext.currentView == GameOpeningAnalysisView.ANALYSIS_RESULT_DETAIL &&
                runtimeContext.selectedResultGameId == nextResult.gameId &&
                runtimeContext.analysisResults.map { result -> result.gameId } == listOf(nextResult.gameId)
        }
        composeRule.onNodeWithTag(GameOpeningAnalysisResultDetailContentTestTag).assertIsDisplayed()
        composeRule
            .onNode(
                hasText("Next Detail") and
                    hasAnyAncestor(hasTestTag(GameOpeningAnalysisResultDetailContentTestTag)),
            )
            .assertIsDisplayed()
        assertTextIsAbsent("Delete Detail")
    }

    @Test
    fun gameOpeningAnalysisScreen_recordMistakeRemovesAnalyzedGameAndKeepsRemainingResults() {
        // Scenario: recording a deviation mistake removes only the analyzed game/result and keeps remaining results visible.
        val runtimeContext = GameOpeningAnalysisRuntimeContext()
        runtimeContext.addImportedGames(
            listOf(
                parsedCandidate(sourceIndex = 0, event = "Recorded Mistake", moves = listOf("e2e4", "e7e5")),
                parsedCandidate(sourceIndex = 1, event = "Remaining Result", moves = listOf("d2d4", "d7d5")),
            ),
        )
        runtimeContext.updateFilter(GameOpeningAnalysisFilter(playerNameQuery = "White"))
        val deviation = resultForGame(runtimeContext.importedGames.first(), deviationResult())
        val remaining = resultForGame(runtimeContext.importedGames.last(), matchesKnownOpeningResult())
        runtimeContext.replaceAnalysisResults(listOf(deviation, remaining))
        runtimeContext.selectResult(deviation.gameId)
        runtimeContext.openSelectedResultDetail()
        var recordedLineIds: List<Long> = emptyList()
        var recordedMistakesCount = 0

        setScreenContent(
            runtimeContext = runtimeContext,
            recordDeviationMistake = { lineIds, mistakesCount ->
                recordedLineIds = lineIds
                recordedMistakesCount = mistakesCount
                lineIds.size
            },
        )

        composeRule
            .onNodeWithTag(GameOpeningAnalysisResultDetailContentTestTag)
            .performScrollToNode(hasTestTag(GameOpeningAnalysisRecordDeviationMistakeTestTag))
        composeRule.onNodeWithTag(GameOpeningAnalysisRecordDeviationMistakeTestTag).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runtimeContext.currentView == GameOpeningAnalysisView.ANALYSIS_RESULTS &&
                runtimeContext.analysisResults.map { result -> result.gameId } == listOf(remaining.gameId)
        }

        composeRule.onNodeWithTag(GameOpeningAnalysisResultsContentTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("Remaining Result").assertIsDisplayed()
        assertTextIsAbsent("Recorded Mistake")
        composeRule.runOnIdle {
            check(recordedLineIds == listOf(101L, 202L)) {
                "Expected recorded line ids [101, 202], got $recordedLineIds"
            }
            check(recordedMistakesCount == 2) {
                "Expected mistakes count 2, got $recordedMistakesCount"
            }
            check(runtimeContext.importedGames.map { game -> game.headers["Event"] } == listOf("Remaining Result")) {
                "Expected only the remaining imported game"
            }
            check(runtimeContext.hasAppliedFilter) {
                "Expected applied filter to be preserved"
            }
        }
    }

    @Test
    fun gameOpeningAnalysisScreen_recordMistakeOpensNextDeviationDetail() {
        // Scenario: after recording one deviation mistake, the screen advances to the next deviation detail.
        val runtimeContext = GameOpeningAnalysisRuntimeContext()
        runtimeContext.addImportedGames(
            listOf(
                parsedCandidate(sourceIndex = 0, event = "First Deviation", moves = listOf("e2e4", "e7e5")),
                parsedCandidate(sourceIndex = 1, event = "Known Result", moves = listOf("d2d4", "d7d5")),
                parsedCandidate(sourceIndex = 2, event = "Next Deviation", moves = listOf("c2c4", "e7e5")),
            ),
        )
        runtimeContext.updateFilter(GameOpeningAnalysisFilter(playerNameQuery = "White"))
        val firstDeviation = resultForGame(runtimeContext.importedGames[0], deviationResult())
        val knownResult = resultForGame(runtimeContext.importedGames[1], matchesKnownOpeningResult())
        val nextDeviation = resultForGame(runtimeContext.importedGames[2], deviationResult())
        runtimeContext.replaceAnalysisResults(listOf(firstDeviation, knownResult, nextDeviation))
        runtimeContext.selectResult(firstDeviation.gameId)
        runtimeContext.openSelectedResultDetail()

        setScreenContent(runtimeContext = runtimeContext)

        composeRule
            .onNodeWithTag(GameOpeningAnalysisResultDetailContentTestTag)
            .performScrollToNode(hasTestTag(GameOpeningAnalysisRecordDeviationMistakeTestTag))
        composeRule.onNodeWithTag(GameOpeningAnalysisRecordDeviationMistakeTestTag).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runtimeContext.currentView == GameOpeningAnalysisView.ANALYSIS_RESULT_DETAIL &&
                runtimeContext.selectedResultGameId == nextDeviation.gameId &&
                runtimeContext.analysisResults.map { result -> result.gameId } ==
                    listOf(knownResult.gameId, nextDeviation.gameId)
        }
        composeRule.runOnIdle {
            check(runtimeContext.importedGames.map { game -> game.headers["Event"] } == listOf("Known Result", "Next Deviation")) {
                "Expected the first deviation game to be removed"
            }
        }
    }

    @Test
    fun gameOpeningAnalysisScreen_deleteSelectedResultCardSelectsNextResult() {
        // Scenario: deleting the selected result card removes that compared game and selects the next result in order.
        val runtimeContext = GameOpeningAnalysisRuntimeContext()
        runtimeContext.addImportedGames(
            listOf(
                parsedCandidate(sourceIndex = 0, event = "Delete Result", moves = listOf("e2e4", "e7e5")),
                parsedCandidate(sourceIndex = 1, event = "Next Result", moves = listOf("d2d4", "d7d5")),
            ),
        )
        runtimeContext.updateFilter(GameOpeningAnalysisFilter(playerNameQuery = "White"))
        val deleteResult = resultForGame(runtimeContext.importedGames.first(), matchesKnownOpeningResult())
        val nextResult = resultForGame(runtimeContext.importedGames.last(), matchesKnownOpeningResult())
        runtimeContext.replaceAnalysisResults(listOf(deleteResult, nextResult))
        runtimeContext.openAnalysisResults()
        runtimeContext.selectResult(deleteResult.gameId)

        setScreenContent(runtimeContext = runtimeContext)

        composeRule.onNodeWithTag(GameOpeningAnalysisResultDeleteActionTestTag).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runtimeContext.currentView == GameOpeningAnalysisView.ANALYSIS_RESULTS &&
                runtimeContext.selectedResultGameId == nextResult.gameId &&
                runtimeContext.analysisResults.map { result -> result.gameId } == listOf(nextResult.gameId)
        }
        composeRule.onNodeWithText("Next Result").assertIsDisplayed()
        assertTextIsAbsent("Delete Result")
        composeRule.runOnIdle {
            check(runtimeContext.importedGames.map { game -> game.headers["Event"] } == listOf("Next Result")) {
                "Expected only the next imported game"
            }
        }
    }

    @Test
    fun gameOpeningAnalysisScreen_analyzeWithoutAppliedFilterShowsFilterRequiredDialog() {
        // Scenario: analysis is visible but refuses to run until the player filter has been applied.
        val runtimeContext = GameOpeningAnalysisRuntimeContext()
        runtimeContext.addImportedGames(
            listOf(
                parsedCandidate(
                    sourceIndex = 0,
                    event = "Unfiltered Game",
                    moves = listOf("e2e4", "e7e5"),
                ),
            ),
        )

        setScreenContent(runtimeContext = runtimeContext)

        composeRule.onNodeWithTag(GameOpeningAnalysisAnalyzeActionTestTag).performClick()

        composeRule.onNodeWithText("Filter Required").assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "Apply a player filter before analysis so only one player's games are compared with the library.",
            )
            .assertIsDisplayed()

        composeRule.onNodeWithText("OK").performClick()

        composeRule.onNodeWithTag(GameOpeningAnalysisFilterDialogTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(GameOpeningAnalysisFilterPlayerNameTestTag).assertIsDisplayed()
        assertTextIsAbsent("Filter Required")
        assertTagIsAbsent(GameOpeningAnalysisOptionsDialogTestTag)
    }

    @Test
    fun gameOpeningAnalysisScreen_analyzeWithEmptyFilteredGamesShowsNoFilteredGamesDialog() {
        // Scenario: an applied player filter is still rejected when no imported games match it.
        val runtimeContext = GameOpeningAnalysisRuntimeContext()
        runtimeContext.addImportedGames(
            listOf(
                parsedCandidate(
                    sourceIndex = 0,
                    event = "Alice Game",
                    white = "Alice",
                    moves = listOf("e2e4", "e7e5"),
                ),
            ),
        )
        runtimeContext.updateFilter(GameOpeningAnalysisFilter(playerNameQuery = "Carol"))

        setScreenContent(runtimeContext = runtimeContext)

        composeRule.onNodeWithTag(GameOpeningAnalysisAnalyzeActionTestTag).performClick()

        composeRule.onNodeWithText("No Filtered Games").assertIsDisplayed()
        composeRule.onNodeWithText("The applied player filter has no games to analyze.").assertIsDisplayed()
        assertTagIsAbsent(GameOpeningAnalysisOptionsDialogTestTag)
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
        runtimeContext.updateFilter(GameOpeningAnalysisFilter(playerNameQuery = "White 0"))

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

        assertGamesPageSubtitle(gamesCount = 25, currentPage = 1, totalPages = 2)
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

        assertGamesPageSubtitle(gamesCount = 25, currentPage = 2, totalPages = 2)
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

    private fun assertGamesPageSubtitle(
        gamesCount: Int,
        currentPage: Int,
        totalPages: Int,
    ) {
        composeRule.onNodeWithText("Games: $gamesCount").assertIsDisplayed()
        composeRule.onNodeWithText("Page $currentPage/$totalPages").assertIsDisplayed()
    }

    private fun assertResultsSubtitle(
        resultsCount: Int,
        showingCount: Int,
    ) {
        composeRule.onNodeWithText("Results: $resultsCount").assertIsDisplayed()
        composeRule.onNodeWithText("Showing: $showingCount").assertIsDisplayed()
    }

    private fun assertTagIsAbsent(tag: String) {
        composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().let { nodes ->
            check(nodes.isEmpty()) {
                "Expected $tag to be absent"
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
        recordDeviationMistake: GameOpeningAnalysisDeviationMistakeRecorder = { lineIds, _ -> lineIds.size },
    ) {
        composeRule.setContent {
            ChessBoardTheme {
                GameOpeningAnalysisScreen(
                    runtimeContext = runtimeContext,
                    onBackClick = onBackClick,
                    onHomeClick = onHomeClick,
                    analysisRunner = analysisRunner,
                    recordDeviationMistake = recordDeviationMistake,
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

    private fun resultForGame(
        game: ImportedGameItem,
        result: GameOpeningAnalysisResult,
    ): ImportedGameAnalysisResult =
        ImportedGameAnalysisResult(
            gameId = game.id,
            game = game,
            result = result,
        )

    private fun deviationResult(): GameOpeningDeviation =
        GameOpeningDeviation(
            selectedSide = OpeningSide.WHITE,
            matchMode = OpeningMatchMode.MOVE_SEQUENCE,
            positionFen = INITIAL_POSITION_FEN,
            ply = 0,
            playedMoveUci = "e2e4",
            playedResultFen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
            expectedMoves =
                listOf(
                    GameOpeningExpectedMove(
                        moveUci = "d2d4",
                        resultFen = "rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq - 0 1",
                        lineRefs =
                            listOf(
                                OpeningBookLineRef(lineIndex = 0, stableLineId = 101L, inputIndex = null, ply = 0),
                            ),
                    ),
                ),
            matchingLineRefs =
                listOf(
                    OpeningBookLineRef(lineIndex = 0, stableLineId = 101L, inputIndex = null, ply = 0),
                    OpeningBookLineRef(lineIndex = 1, stableLineId = 202L, inputIndex = null, ply = 0),
                    OpeningBookLineRef(lineIndex = 2, stableLineId = 101L, inputIndex = null, ply = 0),
                    OpeningBookLineRef(lineIndex = 3, stableLineId = null, inputIndex = 3, ply = 0),
                ),
        )

    private fun matchesKnownOpeningResult(): GameOpeningMatchesKnownOpening =
        GameOpeningMatchesKnownOpening(
            selectedSide = OpeningSide.WHITE,
            matchMode = OpeningMatchMode.MOVE_SEQUENCE,
            matchedPly = 2,
            finalPositionFen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
            matchingLineRefs = emptyList(),
        )

    private companion object {
        const val INITIAL_POSITION_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    }
}
