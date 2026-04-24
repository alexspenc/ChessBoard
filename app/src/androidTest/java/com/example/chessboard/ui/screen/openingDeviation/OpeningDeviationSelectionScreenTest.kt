package com.example.chessboard.ui.screen.openingDeviation

/**
 * UI coverage for choosing a deviation start position before opening the final display screen.
 *
 * Keep tests here focused on selection, preview loading, and local callbacks.
 * Do not add Saved Positions integration tests to this file.
 */
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.chessboard.ui.OpeningDeviationSelectionEmptyStateTestTag
import com.example.chessboard.ui.OpeningDeviationSelectionPreviewBoardCardTestTag
import com.example.chessboard.ui.OpeningDeviationSelectionStartTestTag
import com.example.chessboard.ui.openingDeviationSelectionCardTestTag
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OpeningDeviationSelectionScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun selectionScreen_showsEmptyStateWhenNoDeviationPositionsAvailable() {
        composeRule.setContent {
            ChessBoardTheme {
                OpeningDeviationSelectionScreen(
                    deviationItems = emptyList(),
                    selectedDeviationIndex = null,
                    onDeviationSelected = {},
                    onStartClick = {},
                    onBackClick = {},
                )
            }
        }

        composeRule.onNodeWithTag(OpeningDeviationSelectionEmptyStateTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("No deviation positions available.").assertIsDisplayed()
        composeRule.onNodeWithTag(OpeningDeviationSelectionStartTestTag).assertIsNotEnabled()
    }

    @Test
    fun selectionScreen_selectsPositionShowsPreviewAndStartsDisplay() {
        var selectedDeviationIndex: Int? by mutableStateOf(null)
        var startedWithFen: String? = null

        composeRule.setContent {
            ChessBoardTheme {
                OpeningDeviationSelectionScreen(
                    deviationItems = deviationItemsForSelection(),
                    selectedDeviationIndex = selectedDeviationIndex,
                    onDeviationSelected = { index ->
                        selectedDeviationIndex = index
                    },
                    onStartClick = { deviationItem ->
                        startedWithFen = deviationItem.positionFen
                    },
                    onBackClick = {},
                )
            }
        }

        composeRule.onNodeWithTag(OpeningDeviationSelectionStartTestTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(openingDeviationSelectionCardTestTag(1)).performClick()

        composeRule.onNodeWithTag(openingDeviationSelectionCardTestTag(1)).assertIsSelected()
        composeRule.onNodeWithTag(OpeningDeviationSelectionPreviewBoardCardTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(OpeningDeviationSelectionStartTestTag).assertIsEnabled()

        composeRule.onNodeWithTag(OpeningDeviationSelectionStartTestTag).performClick()

        assertEquals(
            "rnbqkbnr/ppp1pppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq d6",
            startedWithFen,
        )
    }

    @Test
    fun selectionScreen_backButtonCallsOnBackClick() {
        var backClicks = 0

        composeRule.setContent {
            ChessBoardTheme {
                OpeningDeviationSelectionScreen(
                    deviationItems = deviationItemsForSelection(),
                    selectedDeviationIndex = null,
                    onDeviationSelected = {},
                    onStartClick = {},
                    onBackClick = {
                        backClicks += 1
                    },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backClicks)
    }

    private fun deviationItemsForSelection(): List<OpeningDeviationItem> {
        return listOf(
            OpeningDeviationItem(
                positionFen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6",
                branches = listOf(
                    OpeningDeviationBranch(
                        moveUci = "g1f3",
                        resultFen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq -",
                        gamesCount = 2,
                    ),
                ),
            ),
            OpeningDeviationItem(
                positionFen = "rnbqkbnr/ppp1pppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq d6",
                branches = listOf(
                    OpeningDeviationBranch(
                        moveUci = "c2c4",
                        resultFen = "rnbqkbnr/ppp1pppp/8/3p4/2PP4/8/PP2PPPP/RNBQKBNR b KQkq c3",
                        gamesCount = 1,
                    ),
                ),
            ),
        )
    }
}
