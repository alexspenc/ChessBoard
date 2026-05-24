package com.example.chessboard.ui.screen.linesExplorer

/**
 * Screen-level tests for the Lines Explorer bulk-delete interaction.
 *
 * Keep here:
 * - tests that need the LinesExplorerScreen shell, action menu, and confirm dialog wiring
 *
 * Do not add here:
 * - database-backed delete behavior
 * - render-only tests for individual dialog actions
 */
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.ui.LinesExplorerBulkDeleteConfirmTestTag
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LinesExplorerBulkDeleteTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun linesExplorer_bulkDeleteActionRequiresConfirmation() {
        var deleteClicks = 0

        composeRule.setContent {
            ChessBoardTheme {
                LinesExplorerScreen(
                    lineController = LineController(),
                    totalLinesCount = 3,
                    copyLinesPgnAction = CallbackWithCfg(canUse = false, onClick = {}),
                    deleteExplorerLinesAction = CallbackWithCfg(
                        canUse = true,
                        onClick = { deleteClicks += 1 },
                    ),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Line actions").performClick()
        composeRule.onNodeWithContentDescription("Delete explorer lines").performClick()

        composeRule.runOnIdle {
            assertEquals(0, deleteClicks)
        }

        composeRule.onNodeWithText("Delete Lines").assertIsDisplayed()
        composeRule.onNodeWithTag(LinesExplorerBulkDeleteConfirmTestTag).performClick()
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(1, deleteClicks)
        }
    }
}
