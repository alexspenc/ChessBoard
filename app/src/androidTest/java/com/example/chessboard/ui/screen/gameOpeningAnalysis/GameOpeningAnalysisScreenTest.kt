@file:Suppress("FunctionName")

package com.example.chessboard.ui.screen.gameOpeningAnalysis

/*
 * File role: verifies the initial game-opening analysis screen shell.
 * Allowed here:
 * - Compose tests for the imported-games screen shell, top bar, and back callback
 * Not allowed here:
 * - Home navigation coverage, PGN import tests, database access, or analyzer execution tests
 * Validation date: 2026-06-26
 */

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.chessboard.runtimecontext.GameOpeningAnalysisRuntimeContext
import com.example.chessboard.runtimecontext.ImportedGameCandidate
import com.example.chessboard.service.ParsedPgnGame
import com.example.chessboard.ui.GameOpeningAnalysisContentTestTag
import com.example.chessboard.ui.GameOpeningAnalysisEmptyStateTestTag
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Rule
import org.junit.Test

class GameOpeningAnalysisScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun gameOpeningAnalysisScreen_emptyStateShowsSummaryAndHandlesBackClick() {
        // Scenario: an empty runtime context shows the imported-games empty state and wires top-bar back.
        var backClicks = 0

        setScreenContent(
            runtimeContext = GameOpeningAnalysisRuntimeContext(),
            onBackClick = { backClicks++ },
        )

        composeRule.onNodeWithTag(GameOpeningAnalysisContentTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(GameOpeningAnalysisEmptyStateTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("Games: 0 • Showing: 0").assertIsDisplayed()
        composeRule.onNodeWithText("No imported games.").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.runOnIdle {
            check(backClicks == 1) {
                "Expected one back click, got $backClicks"
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

        composeRule.onNodeWithText("Games: 2 • Showing: 2").assertIsDisplayed()
        composeRule.onNodeWithText("Imported games are shown in import order.").assertIsDisplayed()
        composeRule.onNodeWithText("London System").assertIsDisplayed()
        composeRule.onNodeWithText("Alice - Bob").assertIsDisplayed()
        composeRule.onNodeWithText("Ply: 3").assertIsDisplayed()
        composeRule.onNodeWithText("Sicilian Game").assertIsDisplayed()
        composeRule.onNodeWithText("Carol - Dave").assertIsDisplayed()
        composeRule.onNodeWithText("Ply: 2").assertIsDisplayed()
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

        composeRule.onNodeWithText("Games: 25 • Showing: 20").assertIsDisplayed()
        composeRule.onNodeWithText("Imported Game 1").assertIsDisplayed()
        composeRule.onAllNodesWithText("Imported Game 20").fetchSemanticsNodes().let { nodes ->
            check(nodes.isNotEmpty()) {
                "Expected Imported Game 20 to be part of the visible page"
            }
        }
        composeRule.onAllNodesWithText("Imported Game 21").fetchSemanticsNodes().let { nodes ->
            check(nodes.isEmpty()) {
                "Expected Imported Game 21 to stay outside the visible page"
            }
        }
    }

    private fun setScreenContent(
        runtimeContext: GameOpeningAnalysisRuntimeContext,
        onBackClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            ChessBoardTheme {
                GameOpeningAnalysisScreen(
                    runtimeContext = runtimeContext,
                    onBackClick = onBackClick,
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
