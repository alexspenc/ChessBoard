package com.example.chessboard.ui.screen.training
import com.example.chessboard.ui.screen.training.template.EditTrainingTemplateScreen

/*
 * UI tests for the training template editor screen.
 *
 * Keep template-editor screen regression tests here. Do not add container
 * loading, database wiring, or route-level navigation tests to this file.
 */

import com.example.chessboard.ui.screen.training.common.TrainingLineEditorItem

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicLong

class EditTrainingTemplateScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun editTrainingTemplateScreen_showsTemplateNameAndLinesCount() {
        composeRule.setContent {
            ChessBoardTheme {
                EditTrainingTemplateScreen(
                    initialTemplateName = "Sicilian Templates",
                    linesForTemplate = listOf(TestTemplateLine),
                )
            }
        }

        composeRule.onNodeWithText("Sicilian Templates").assertIsDisplayed()
        composeRule.onNodeWithText("Lines in template: 1").assertIsDisplayed()
    }


    @Test
    fun editTrainingTemplateScreen_weightButtonsUpdateVisibleWeight() {
        composeRule.setContent {
            ChessBoardTheme {
                EditTrainingTemplateScreen(
                    initialTemplateName = "Sicilian Templates",
                    linesForTemplate = listOf(TestTemplateLine),
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
                    linesForTemplate = listOf(TestTemplateLine),
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditTrainingListTestTag)
            .performScrollToNode(hasTestTag(EditTrainingMoveLegendSectionTestTag))
        waitForNodeDisplayed(EditTrainingMoveLegendSectionTestTag)
        composeRule.onNodeWithTag(moveChipTestTag("e4")).performScrollTo()
        waitForNodeDisplayed(moveChipTestTag("e4"))
        composeRule.onNodeWithTag(moveChipTestTag("e4"))
            .performSemanticsAction(SemanticsActions.OnClick)

        assertBoardFenEventually(AfterE4Fen)
    }

    @Test
    fun editTrainingTemplateScreen_moveChipOnUnselectedLineSelectsLineAndKeepsClickedPly() {
        composeRule.setContent {
            ChessBoardTheme {
                EditTrainingTemplateScreen(
                    initialTemplateName = "Sicilian Templates",
                    linesForTemplate = listOf(
                        TestTemplateLine,
                        SecondTemplateLine,
                    ),
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditTrainingListTestTag)
            .performScrollToNode(hasText("French Defense"))
        waitForTextDisplayed("French Defense")
        composeRule.onNodeWithTag(moveChipTestTag("d4")).performScrollTo()
        waitForNodeDisplayed(moveChipTestTag("d4"))
        composeRule.onNodeWithTag(moveChipTestTag("d4"))
            .performSemanticsAction(SemanticsActions.OnClick)

        assertBoardFenEventually(AfterD4Fen)
    }

    @Test
    fun editTrainingTemplateScreen_backShowsUnsavedChangesDialog_afterTemplateNameChange() {
        composeRule.setContent {
            ChessBoardTheme {
                EditTrainingTemplateScreen(
                    initialTemplateName = "Sicilian Templates",
                    linesForTemplate = listOf(TestTemplateLine),
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
                    linesForTemplate = listOf(TestTemplateLine),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Increase").performClick()
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.onNodeWithText("Unsaved Changes").assertIsDisplayed()
    }

    @Test
    fun editTrainingTemplateScreen_removeSelectedLineRemovesItFromTemplate() {
        composeRule.setContent {
            ChessBoardTheme {
                EditTrainingTemplateScreen(
                    initialTemplateName = "Sicilian Templates",
                    linesForTemplate = listOf(
                        TestTemplateLine,
                        SecondTemplateLine,
                    ),
                )
            }
        }

        waitForTextDisplayed("Delete")
        composeRule.onNodeWithText("Delete").performClick()
        waitForTextDisplayed("Remove Line")
        composeRule.onNode(hasText("Remove") and hasClickAction()).performClick()

        waitForTextInTree("Lines in template: 1")
        composeRule.onNodeWithTag(EditTrainingListTestTag)
            .performScrollToNode(hasText("Lines in template: 1"))
        waitForTextDisplayed("Lines in template: 1")
    }

    @Test
    fun editTrainingTemplateScreen_singleLineEditAndDeleteActionsWork() {
        val openedLineId = AtomicLong(-1L)

        composeRule.setContent {
            ChessBoardTheme {
                EditTrainingTemplateScreen(
                    initialTemplateName = "Sicilian Templates",
                    linesForTemplate = listOf(TestTemplateLine),
                    onOpenLineEditorClick = { lineId -> openedLineId.set(lineId) },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Edit line").performClick()
        composeRule.runOnIdle {
            assertEquals(TestTemplateLine.lineId, openedLineId.get())
        }

        composeRule.onNodeWithContentDescription("Remove line from template").performClick()
        waitForTextDisplayed("Remove Line")
        composeRule.onNode(hasText("Remove") and hasClickAction()).performClick()

        waitForTextInTree("Lines in template: 0")
        composeRule.onNodeWithText(TestTemplateLine.title).assertDoesNotExist()
    }

    @Test
    fun editTrainingTemplateScreen_remainingLineSupportsEditAndDeleteAfterDeletingOneOfTwo() {
        val openedLineId = AtomicLong(-1L)

        composeRule.setContent {
            ChessBoardTheme {
                EditTrainingTemplateScreen(
                    initialTemplateName = "Sicilian Templates",
                    linesForTemplate = listOf(
                        TestTemplateLine,
                        SecondTemplateLine,
                    ),
                    onOpenLineEditorClick = { lineId -> openedLineId.set(lineId) },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Remove line from template").performClick()
        waitForTextDisplayed("Remove Line")
        composeRule.onNode(hasText("Remove") and hasClickAction()).performClick()

        waitForTextInTree("Lines in template: 1")
        composeRule.onNodeWithText(TestTemplateLine.title).assertDoesNotExist()
        composeRule.onNodeWithText(SecondTemplateLine.title).assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Next move").performClick()
        assertBoardFenEventually(AfterD4Fen)

        composeRule.onNode(hasText("Edit") and hasClickAction()).performClick()
        waitForTextDisplayed("Unsaved Changes")
        composeRule.onNode(hasText("Discard") and hasClickAction()).performClick()
        composeRule.runOnIdle {
            assertEquals(SecondTemplateLine.lineId, openedLineId.get())
        }

        composeRule.onNodeWithContentDescription("Remove line from template").performClick()
        waitForTextDisplayed("Remove Line")
        composeRule.onNode(hasText("Remove") and hasClickAction()).performClick()

        waitForTextInTree("Lines in template: 0")
        composeRule.onNodeWithText(SecondTemplateLine.title).assertDoesNotExist()
    }

    @Test
    fun editTrainingTemplateScreen_selectingAnotherLineUpdatesEditAndDeleteActions() {
        val openedLineId = AtomicLong(-1L)

        composeRule.setContent {
            ChessBoardTheme {
                EditTrainingTemplateScreen(
                    initialTemplateName = "Sicilian Templates",
                    linesForTemplate = listOf(
                        TestTemplateLine,
                        SecondTemplateLine,
                        ThirdTemplateLine,
                    ),
                    onOpenLineEditorClick = { lineId -> openedLineId.set(lineId) },
                )
            }
        }

        composeRule.onNodeWithTag(EditTrainingListTestTag)
            .performScrollToNode(hasText(ThirdTemplateLine.title))
        waitForTextDisplayed(ThirdTemplateLine.title)
        composeRule.onNodeWithText(ThirdTemplateLine.title).performClick()

        composeRule.onNodeWithTag(EditTrainingListTestTag)
            .performScrollToNode(hasTestTag(moveChipTestTag("c6")))
        waitForNodeDisplayed(moveChipTestTag("c6"))
        composeRule.onNodeWithTag(moveChipTestTag("c6"))
            .performSemanticsAction(SemanticsActions.OnClick)

        assertBoardFenEventually(AfterE4C6Fen)

        composeRule.onNodeWithContentDescription("Edit line").performClick()
        composeRule.runOnIdle {
            assertEquals(ThirdTemplateLine.lineId, openedLineId.get())
        }

        composeRule.onNodeWithContentDescription("Remove line from template").performClick()
        waitForTextDisplayed("Remove Line")
        composeRule.onNode(hasText("Remove") and hasClickAction()).performClick()

        waitForTextInTree("Lines in template: 2")
        composeRule.onNodeWithText(ThirdTemplateLine.title).assertDoesNotExist()
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

    private fun waitForTextDisplayed(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText(text).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun waitForTextInTree(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText(text).fetchSemanticsNode()
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
        val TestTemplateLine = TrainingLineEditorItem(
            lineId = 1L,
            title = "Sicilian Defense",
            weight = 2,
            pgn = "1. e2e4 c7c5 *",
            sideMask = SideMask.WHITE,
        )
        val SecondTemplateLine = TrainingLineEditorItem(
            lineId = 2L,
            title = "French Defense",
            weight = 1,
            pgn = "1. d2d4 d7d5 *",
            sideMask = SideMask.WHITE,
        )
        val ThirdTemplateLine = TrainingLineEditorItem(
            lineId = 3L,
            title = "Caro-Kann Defense",
            weight = 3,
            pgn = "1. e2e4 c7c6 *",
            sideMask = SideMask.WHITE,
        )
        const val AfterE4Fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
        const val AfterE4C6Fen = "rnbqkbnr/pp1ppppp/2p5/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
        const val AfterD4Fen = "rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq - 0 1"
    }
}
