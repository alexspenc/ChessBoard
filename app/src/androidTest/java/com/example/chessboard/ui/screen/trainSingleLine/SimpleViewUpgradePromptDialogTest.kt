package com.example.chessboard.ui.screen.trainSingleLine

/*
 * UI tests for the simple-to-full-view suggestion dialog.
 *
 * Keep only prompt-level action wiring here. Do not add database-backed training
 * completion or settings-screen tests to this file.
 */

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.chessboard.ui.SimpleViewUpgradePromptCancelTestTag
import com.example.chessboard.ui.SimpleViewUpgradePromptOpenSettingsTestTag
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SimpleViewUpgradePromptDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun promptDialog_openSettingsActionInvokesCallback() {
        var openSettingsClicks = 0
        var dismissClicks = 0

        composeRule.setContent {
            ChessBoardTheme {
                SimpleViewUpgradePromptDialog(
                    trainingsCount = 20,
                    onOpenSettingsClick = { openSettingsClicks++ },
                    onDismiss = { dismissClicks++ },
                )
            }
        }

        composeRule.onNodeWithText("Try Full View?").assertIsDisplayed()
        composeRule.onNodeWithTag(SimpleViewUpgradePromptOpenSettingsTestTag).performClick()

        assertEquals(1, openSettingsClicks)
        assertEquals(0, dismissClicks)
    }

    @Test
    fun promptDialog_cancelActionOnlyDismissesDialog() {
        var openSettingsClicks = 0
        var dismissClicks = 0

        composeRule.setContent {
            ChessBoardTheme {
                SimpleViewUpgradePromptDialog(
                    trainingsCount = 20,
                    onOpenSettingsClick = { openSettingsClicks++ },
                    onDismiss = { dismissClicks++ },
                )
            }
        }

        composeRule.onNodeWithTag(SimpleViewUpgradePromptCancelTestTag).performClick()

        assertEquals(0, openSettingsClicks)
        assertEquals(1, dismissClicks)
    }
}
