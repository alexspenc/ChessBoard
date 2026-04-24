package com.example.chessboard.ui.screen.openingDeviation

/**
 * UI coverage for the opening deviation display screen.
 *
 * Keep tests here focused on final-screen rendering, board loading, and local callbacks.
 * Do not add navigation flow tests from Saved Positions to this file.
 */
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.chessboard.testing.fenStateDescriptionMatcher
import com.example.chessboard.testing.normalizeFenForAssertion
import com.example.chessboard.ui.OpeningDeviationDisplayContentTestTag
import com.example.chessboard.ui.OpeningDeviationEmptyStateTestTag
import com.example.chessboard.ui.OpeningDeviationSourceBoardTestTag
import com.example.chessboard.ui.OpeningDeviationSourceBoardCardTestTag
import com.example.chessboard.ui.openingDeviationBranchBoardTestTag
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
                    deviationItem = OpeningDeviationItem(
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
        composeRule.onNodeWithText("Deviation Position").assertIsDisplayed()
        assertBoardFenEventually(
            boardTag = OpeningDeviationSourceBoardTestTag,
            expectedFen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 1",
        )

        scrollToTag(openingDeviationBranchCardTestTag(0))
        composeRule.onNodeWithTag(openingDeviationBranchCardTestTag(0)).assertIsDisplayed()
        composeRule.onNodeWithText("Move: g1f3").assertIsDisplayed()
        composeRule.onNodeWithText("Games: 2").assertIsDisplayed()
        assertBoardFenEventually(
            boardTag = openingDeviationBranchBoardTestTag(0),
            expectedFen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2",
        )

        scrollToTag(openingDeviationBranchCardTestTag(1))
        composeRule.onNodeWithTag(openingDeviationBranchCardTestTag(1)).assertIsDisplayed()
        composeRule.onNodeWithText("Move: f1c4").assertIsDisplayed()
        composeRule.onNodeWithText("Games: 1").assertIsDisplayed()
        assertBoardFenEventually(
            boardTag = openingDeviationBranchBoardTestTag(1),
            expectedFen = "rnbqkbnr/pppp1ppp/8/4p3/2B1P3/8/PPPP1PPP/RNBQK1NR b KQkq - 1 2",
        )
    }

    @Test
    fun displayScreen_showsEmptyStateWhenNoBranchesAvailable() {
        composeRule.setContent {
            ChessBoardTheme {
                OpeningDeviationDisplayScreen(
                    deviationItem = OpeningDeviationItem(
                        positionFen = "8/8/8/8/8/8/8/8 w - - 0 1",
                        branches = emptyList(),
                    ),
                )
            }
        }

        composeRule.waitForIdle()

        scrollToTag(OpeningDeviationEmptyStateTestTag)
        composeRule.onNodeWithTag(OpeningDeviationEmptyStateTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("No deviation branches available.").assertIsDisplayed()
        assertBoardFenEventually(
            boardTag = OpeningDeviationSourceBoardTestTag,
            expectedFen = "8/8/8/8/8/8/8/8 w - - 0 1",
        )
    }

    @Test
    fun displayScreen_backButtonCallsOnBackClick() {
        var backClicks = 0

        composeRule.setContent {
            ChessBoardTheme {
                OpeningDeviationDisplayScreen(
                    deviationItem = OpeningDeviationItem(
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

    private fun scrollToTag(tag: String) {
        composeRule.onNodeWithTag(OpeningDeviationDisplayContentTestTag)
            .performScrollToNode(hasTestTag(tag))
        composeRule.waitForIdle()
    }

    private fun assertBoardFenEventually(boardTag: String, expectedFen: String) {
        val normalizedExpectedFen = normalizeFenForAssertion(expectedFen)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            currentBoardFen(boardTag)?.let(::normalizeFenForAssertion) == normalizedExpectedFen
        }
        composeRule.onNodeWithTag(boardTag, useUnmergedTree = true).assert(
            fenStateDescriptionMatcher(expectedFen)
        )
    }

    private fun currentBoardFen(boardTag: String): String? {
        return runCatching {
            composeRule.onNodeWithTag(boardTag, useUnmergedTree = true)
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.StateDescription)
        }.getOrNull()
    }
}
