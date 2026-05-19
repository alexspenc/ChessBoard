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
import com.example.chessboard.entity.LineEntity
import com.example.chessboard.entity.SideMask
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.runtimecontext.StatisticsTrainingRuntimeContext
import com.example.chessboard.service.StatisticsTrainingRecommendationSettings
import com.example.chessboard.service.uciMovesToMoves
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.theme.ChessBoardTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CreateTrainingByStatisticsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val dbProvider: DatabaseProvider
        get() = DatabaseProvider.createInstance(composeRule.activity)

    private lateinit var statisticsRuntimeContext: StatisticsTrainingRuntimeContext

    @Before
    fun setUp() {
        dbProvider.clearAllData()
        statisticsRuntimeContext = StatisticsTrainingRuntimeContext()
    }

    @Test
    fun createTrainingByStatisticsScreen_limitsDialogCancelKeepsSelectionUnchanged() {
        setCreateTrainingByStatisticsContent()

        waitForTextDisplayed("Statistics limits")
        composeRule.onNodeWithContentDescription("Change limits").performClick()
        waitForTextDisplayed("Training Limits")
        composeRule.onNodeWithContentDescription("Increase Min days since last training").performClick()
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.waitForIdle()
        check(statisticsRuntimeContext.loadedRecommendationSettings?.minDaysSinceLastTraining == 0) {
            "Expected canceled limit changes to keep loaded recommendation settings unchanged"
        }
        assertTextDoesNotExist("Selection is out of date")
    }

    @Test
    fun createTrainingByStatisticsScreen_limitChangeRefreshesBeforeSave() {
        saveLine()
        setCreateTrainingByStatisticsContent()

        waitForTextDisplayed("Lines selected by statistics: 1")
        composeRule.onNodeWithContentDescription("Change limits").performClick()
        waitForTextDisplayed("Training Limits")
        composeRule.onNodeWithContentDescription("Increase Max lines").performClick()
        composeRule.onNodeWithText("OK").performClick()

        waitForRecommendationSettingsLoaded { it.limit == 51 }
        waitForTextDisplayed("Lines selected by statistics: 1")
        composeRule.onNodeWithContentDescription("Save").performClick()

        waitForTextDisplayed("Training Created")
        assertTextDoesNotExist("Selection Refreshed")
    }

    private fun setCreateTrainingByStatisticsContent() {
        composeRule.setContent {
            ChessBoardTheme {
                CreateTrainingByStatisticsScreenContainer(
                    screenContext = ScreenContainerContext(inDbProvider = dbProvider),
                    statisticsTrainingRuntimeContext = statisticsRuntimeContext,
                    onOpenFormulaSettings = {},
                )
            }
        }
    }

    private fun waitForRecommendationSettingsLoaded(
        matches: (StatisticsTrainingRecommendationSettings) -> Boolean,
    ) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            val settings = statisticsRuntimeContext.loadedRecommendationSettings
            settings != null && matches(settings)
        }
    }

    private fun waitForTextDisplayed(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }

    private fun assertTextDoesNotExist(text: String) {
        composeRule.waitForIdle()
        check(composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()) {
            "Expected text '$text' to be absent"
        }
    }

    private fun saveLine(): Long {
        return runBlocking {
            val line = LineEntity(
                event = "Statistics Training Source",
                pgn = storedPgn(listOf("e2e4", "e7e5")),
                initialFen = "",
                sideMask = SideMask.BOTH,
            )

            val lineId = dbProvider.createLineSaver().saveLine(
                line = line,
                moves = uciMovesToMoves(listOf("e2e4", "e7e5")),
                sideMask = line.sideMask,
            )
            checkNotNull(lineId) {
                "Expected test line to be saved"
            }
        }
    }

    private fun storedPgn(moves: List<String>): String {
        return buildString {
            append("[Event \"Statistics Training Source\"]\n")
            append("[White \"White\"]\n")
            append("[Black \"Black\"]\n")
            append("[Result \"*\"]\n\n")

            moves.forEachIndexed { index, move ->
                if (index % 2 == 0) {
                    append("${index / 2 + 1}. ")
                }

                append(move)
                append(" ")
            }

            append("*")
        }
    }
}
