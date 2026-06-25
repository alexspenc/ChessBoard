package com.example.chessboard.ui.components

/*
 * UI tests for AppTopBar system-back wiring.
 *
 * Keep tests here focused on the top bar's own BackHandler opt-in behavior.
 * Do not add screen navigation or MainActivity integration coverage here.
 * Validation date: 2026-06-24
 */

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppTopBarBackHandlerTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun appTopBar_systemBackCallsOnBackClick_whenSystemBackHandlingEnabled() {
        var backClicks = 0

        composeRule.setContent {
            ChessBoardTheme {
                AppTopBar(
                    title = "Test",
                    onBackClick = { backClicks += 1 },
                    handleSystemBack = true,
                )
            }
        }

        pressSystemBack()
        composeRule.waitForIdle()

        assertEquals(1, backClicks)
    }

    @Test
    fun appTopBar_systemBackDoesNotCallOnBackClick_whenSystemBackHandlingDisabled() {
        var topBarBackClicks = 0
        var fallbackBackClicks = 0

        composeRule.setContent {
            ChessBoardTheme {
                BackHandler {
                    fallbackBackClicks += 1
                }
                AppTopBar(
                    title = "Test",
                    onBackClick = { topBarBackClicks += 1 },
                    handleSystemBack = false,
                )
            }
        }

        pressSystemBack()
        composeRule.waitForIdle()

        assertEquals(0, topBarBackClicks)
        assertEquals(1, fallbackBackClicks)
    }

    private fun pressSystemBack() {
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
    }
}
