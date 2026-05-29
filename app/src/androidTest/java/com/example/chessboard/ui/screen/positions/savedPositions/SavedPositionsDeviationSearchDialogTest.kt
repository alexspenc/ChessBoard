package com.example.chessboard.ui.screen.positions.savedPositions

/**
 * UI coverage for the saved-positions deviation-search progress dialog. Validated 2026-05-07.
 *
 * Keep tests here focused on dialog rendering and cancellation wiring.
 * Do not add full Saved Positions navigation flows or database-backed deviation analysis here.
 */
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.chessboard.ui.SavedPositionsDeviationSearchCancelTestTag
import com.example.chessboard.ui.SavedPositionsDeviationSearchDialogTestTag
import com.example.chessboard.ui.SavedPositionsDeviationSearchMessageTestTag
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SavedPositionsDeviationSearchDialogTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun deviationSearchDialog_rendersProgressMessage() {
        composeRule.setContent {
            ChessBoardTheme {
                RenderSavedPositionsDeviationSearchDialog(
                    dialogState = SavedPositionsDeviationSearchDialog(
                        positionName = "Italian Position",
                    ),
                    strings = savedPositionsStrings(),
                    onCancel = {},
                )
            }
        }

        composeRule.onNodeWithTag(SavedPositionsDeviationSearchDialogTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("Searching Deviations").assertIsDisplayed()
        composeRule.onNodeWithTag(
            SavedPositionsDeviationSearchMessageTestTag,
            useUnmergedTree = true,
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            "This can take a while. Cancel to stop the analysis."
        ).assertIsDisplayed()
        composeRule.onNodeWithTag(SavedPositionsDeviationSearchCancelTestTag).assertIsDisplayed()
    }

    @Test
    fun deviationSearchDialog_cancelCallsCallback() {
        var cancelClicks = 0

        composeRule.setContent {
            ChessBoardTheme {
                RenderSavedPositionsDeviationSearchDialog(
                    dialogState = SavedPositionsDeviationSearchDialog(
                        positionName = "French Structure",
                    ),
                    strings = savedPositionsStrings(),
                    onCancel = {
                        cancelClicks += 1
                    },
                )
            }
        }

        composeRule.onNodeWithTag(SavedPositionsDeviationSearchCancelTestTag).performClick()

        assertEquals(1, cancelClicks)
    }

    @Test
    fun deviationSearchDialog_hiddenWhenStateIsNull() {
        composeRule.setContent {
            ChessBoardTheme {
                RenderSavedPositionsDeviationSearchDialog(
                    dialogState = null,
                    strings = savedPositionsStrings(),
                    onCancel = {},
                )
            }
        }

        composeRule.onNodeWithTag(SavedPositionsDeviationSearchDialogTestTag).assertDoesNotExist()
    }
}
