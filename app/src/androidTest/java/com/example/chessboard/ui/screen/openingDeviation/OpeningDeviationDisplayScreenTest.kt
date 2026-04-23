package com.example.chessboard.ui.screen.openingDeviation

/**
 * UI coverage for the opening deviation display screen.
 *
 * Keep tests here focused on final-screen rendering, board loading, and local callbacks.
 * Do not add navigation flow tests from Saved Positions to this file.
 */
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.chessboard.testing.fenStateDescriptionMatcher
import com.example.chessboard.ui.InteractiveChessBoardTestTag
import com.example.chessboard.ui.OpeningDeviationDisplayContentTestTag
import com.example.chessboard.ui.OpeningDeviationEmptyStateTestTag
import com.example.chessboard.ui.OpeningDeviationSourceBoardCardTestTag
import com.example.chessboard.ui.openingDeviationBranchCardTestTag
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OpeningDeviationDisplayScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun displayScreen_showsDeviationPositionAndBranches() {
        composeRule.setContent {
            ChessBoardTheme {
                OpeningDeviationDisplayScreen(
                    item = OpeningDeviationItem(
                        positionFen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6",
                        branches = listOf(
                            OpeningDeviationBranch(
                                moveUci = "g1f3",
                                resultFen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2",
                                gamesCount = 2,
                            ),
                            OpeningDeviationBranch(
                                moveUci = "f1c4",
                                resultFen = "rnbqkbnr/pppp1ppp/8/4p3/2B1P3/8/PPPP1PPP/RNBQK1NR b KQkq - 1 2",
                                gamesCount = 1,
                            ),
                        ),
                    ),
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag(OpeningDeviationDisplayContentTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(OpeningDeviationSourceBoardCardTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(openingDeviationBranchCardTestTag(0)).assertIsDisplayed()
        composeRule.onNodeWithTag(openingDeviationBranchCardTestTag(1)).assertIsDisplayed()
        composeRule.onNodeWithText("Deviation Position").assertIsDisplayed()
        composeRule.onNodeWithText("Move: g1f3").assertIsDisplayed()
        composeRule.onNodeWithText("Move: f1c4").assertIsDisplayed()
        composeRule.onNodeWithText("Games: 2").assertIsDisplayed()
        composeRule.onNodeWithText("Games: 1").assertIsDisplayed()

        composeRule.onAllNodesWithTag(InteractiveChessBoardTestTag).assertCountEquals(3)
        composeRule.onAllNodesWithTag(InteractiveChessBoardTestTag)[0].assert(
            fenStateDescriptionMatcher(
                "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 1"
            )
        )
        composeRule.onAllNodesWithTag(InteractiveChessBoardTestTag)[1].assert(
            fenStateDescriptionMatcher(
                "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2"
            )
        )
        composeRule.onAllNodesWithTag(InteractiveChessBoardTestTag)[2].assert(
            fenStateDescriptionMatcher(
                "rnbqkbnr/pppp1ppp/8/4p3/2B1P3/8/PPPP1PPP/RNBQK1NR b KQkq - 1 2"
            )
        )
    }

    @Test
    fun displayScreen_showsEmptyStateWhenNoBranchesAvailable() {
        composeRule.setContent {
            ChessBoardTheme {
                OpeningDeviationDisplayScreen(
                    item = OpeningDeviationItem(
                        positionFen = "8/8/8/8/8/8/8/8 w - - 0 1",
                        branches = emptyList(),
                    ),
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag(OpeningDeviationEmptyStateTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("No deviation branches available.").assertIsDisplayed()
        composeRule.onAllNodesWithTag(InteractiveChessBoardTestTag).assertCountEquals(1)
    }

    @Test
    fun displayScreen_backButtonCallsOnBackClick() {
        var backClicks = 0

        composeRule.setContent {
            ChessBoardTheme {
                OpeningDeviationDisplayScreen(
                    item = OpeningDeviationItem(
                        positionFen = "8/8/8/8/8/8/8/8 w - - 0 1",
                        branches = emptyList(),
                    ),
                    onBackClick = {
                        backClicks += 1
                    },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backClicks)
    }
}
