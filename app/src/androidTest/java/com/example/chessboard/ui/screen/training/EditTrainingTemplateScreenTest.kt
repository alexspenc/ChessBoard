package com.example.chessboard.ui.screen.training
import com.example.chessboard.ui.screen.training.template.EditTrainingTemplateScreen

/*
 * UI tests for the training template editor screen.
 *
 * Keep template-editor screen regression tests here. Do not add container
 * loading, database wiring, or route-level navigation tests to this file.
 */

import com.example.chessboard.ui.screen.training.common.TrainingGameEditorItem

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.chessboard.entity.SideMask
import com.example.chessboard.testing.fenStateDescriptionMatcher
import com.example.chessboard.testing.normalizeFenForAssertion
import com.example.chessboard.ui.EditTrainingListTestTag
import com.example.chessboard.ui.EditTrainingMoveLegendSectionTestTag
import com.example.chessboard.ui.InteractiveChessBoardTestTag
import com.example.chessboard.ui.moveChipTestTag
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Rule
import org.junit.Test

class EditTrainingTemplateScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun editTrainingTemplateScreen_showsTemplateNameAndGamesCount() {
        composeRule.setContent {
            ChessBoardTheme {
                EditTrainingTemplateScreen(
                    initialTemplateName = "Sicilian Templates",
                    gamesForTemplate = listOf(TestTemplateGame),
                )
            }
        }

        composeRule.onNodeWithText("Sicilian Templates").assertIsDisplayed()
        composeRule.onNodeWithText("Games in template: 1").assertIsDisplayed()
    }


    @Test
    fun editTrainingTemplateScreen_weightButtonsUpdateVisibleWeight() {
        composeRule.setContent {
            ChessBoardTheme {
                EditTrainingTemplateScreen(
                    initialTemplateName = "Sicilian Templates",
                    gamesForTemplate = listOf(TestTemplateGame),
                )
            }
        }

        composeRule.onNodeWithText("2").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Increase").performClick()
        composeRule.onNodeWithText("3").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Decrease").performClick()
        composeRule.onNodeWithText("2").assertIsDisplayed()
    }


    @Test
    fun editTrainingTemplateScreen_moveChipUpdatesVisibleBoardPosition() {
        composeRule.setContent {
            ChessBoardTheme {
                EditTrainingTemplateScreen(
                    initialTemplateName = "Sicilian Templates",
                    gamesForTemplate = listOf(TestTemplateGame),
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditTrainingListTestTag)
            .performScrollToNode(hasTestTag(EditTrainingMoveLegendSectionTestTag))
        waitForNodeDisplayed(EditTrainingMoveLegendSectionTestTag)
        composeRule.onNodeWithTag(moveChipTestTag("1.e4")).performScrollTo()
        waitForNodeDisplayed(moveChipTestTag("1.e4"))
        composeRule.onNodeWithTag(moveChipTestTag("1.e4"))
            .performSemanticsAction(SemanticsActions.OnClick)

        assertBoardFenEventually(AfterE4Fen)
    }


    @Test
    fun editTrainingTemplateScreen_backShowsUnsavedChangesDialog_afterTemplateNameChange() {
        composeRule.setContent {
            ChessBoardTheme {
                EditTrainingTemplateScreen(
                    initialTemplateName = "Sicilian Templates",
                    gamesForTemplate = listOf(TestTemplateGame),
                )
            }
        }

        composeRule.onNodeWithText("Sicilian Templates").performTextReplacement("Updated Template")
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.onNodeWithText("Unsaved Changes").assertIsDisplayed()
    }

    @Test
    fun editTrainingTemplateScreen_backShowsUnsavedChangesDialog_afterWeightChange() {
        composeRule.setContent {
            ChessBoardTheme {
                EditTrainingTemplateScreen(
                    initialTemplateName = "Sicilian Templates",
                    gamesForTemplate = listOf(TestTemplateGame),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Increase").performClick()
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.onNodeWithText("Unsaved Changes").assertIsDisplayed()
    }


    private fun assertBoardFenEventually(expectedFen: String) {
        val normalizedExpectedFen = normalizeFenForAssertion(expectedFen)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            currentBoardFen()?.let(::normalizeFenForAssertion) == normalizedExpectedFen
        }
        composeRule.onNodeWithTag(InteractiveChessBoardTestTag).assert(
            fenStateDescriptionMatcher(expectedFen)
        )
    }

    private fun waitForNodeDisplayed(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag(tag).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun currentBoardFen(): String? {
        return runCatching {
            composeRule.onNodeWithTag(InteractiveChessBoardTestTag)
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.StateDescription)
        }.getOrNull()
    }

    private companion object {
        val TestTemplateGame = TrainingGameEditorItem(
            gameId = 1L,
            title = "Sicilian Defense",
            weight = 2,
            pgn = "1. e2e4 c7c5 *",
            sideMask = SideMask.WHITE,
        )
        const val AfterE4Fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
    }
}
