@file:Suppress("FunctionName")

package com.example.chessboard.ui.screen.gameOpeningAnalysis

/*
 * File role: verifies the initial game-opening analysis screen shell.
 * Allowed here:
 * - Compose tests for the placeholder screen, top bar, and back callback
 * Not allowed here:
 * - Home navigation coverage, PGN import tests, database access, or analyzer execution tests
 * Validation date: 2026-06-26
 */

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.chessboard.ui.GameOpeningAnalysisContentTestTag
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Rule
import org.junit.Test

class GameOpeningAnalysisScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun gameOpeningAnalysisScreen_showsPlaceholderAndHandlesBackClick() {
        // Scenario: the first UI slice shows a placeholder screen and wires the top-bar back action.
        var backClicks = 0

        composeRule.setContent {
            ChessBoardTheme {
                GameOpeningAnalysisScreen(
                    onBackClick = { backClicks++ },
                )
            }
        }

        composeRule.onNodeWithTag(GameOpeningAnalysisContentTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("Game Opening Analysis").assertIsDisplayed()
        composeRule.onNodeWithText("Import games to compare them with your saved opening library.").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.runOnIdle {
            check(backClicks == 1) {
                "Expected one back click, got $backClicks"
            }
        }
    }
}
