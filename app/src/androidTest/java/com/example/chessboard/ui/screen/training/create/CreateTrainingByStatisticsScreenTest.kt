package com.example.chessboard.ui.screen.training.create

/*
 * UI tests for statistics-based training creation.
 *
 * Keep screen-level selection, settings, and save-flow behavior tests here.
 * Do not add formula calculation tests, Room migration tests, or unrelated training editor tests here.
 *
 * Validation date: 2026-05-18
 */

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.runtimecontext.StatisticsTrainingRuntimeContext
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CreateTrainingByStatisticsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val dbProvider: DatabaseProvider
        get() = DatabaseProvider.createInstance(composeRule.activity)

    @Before
    fun setUp() {
        dbProvider.clearAllData()
    }

    @Test
    fun createTrainingByStatisticsScreen_settingsChangeShowsSelectionOutOfDateMessage() {
        setCreateTrainingByStatisticsContent()

        waitForTextDisplayed("Max lines")
        composeRule.onNodeWithContentDescription("Increase Min days since last training").performClick()

        waitForTextDisplayed("Selection is out of date")
        composeRule.onNodeWithText(
            "Save will refresh the selected lines before creating the training."
        ).assertIsDisplayed()
    }

    private fun setCreateTrainingByStatisticsContent() {
        composeRule.setContent {
            ChessBoardTheme {
                CreateTrainingByStatisticsScreenContainer(
                    screenContext = ScreenContainerContext(inDbProvider = dbProvider),
                    statisticsTrainingRuntimeContext = StatisticsTrainingRuntimeContext(),
                    onOpenFormulaSettings = {},
                )
            }
        }
    }

    private fun waitForTextDisplayed(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }
}
