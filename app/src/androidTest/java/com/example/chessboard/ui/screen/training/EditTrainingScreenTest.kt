package com.example.chessboard.ui.screen.training

/**
 * File role: tests the regular edit-training screen UI behavior.
 * Allowed here:
 * - Compose UI checks for selecting, previewing, starting, and editing lines in a training
 * - regression tests for screen-level edit-training interactions
 * Not allowed here:
 * - Room persistence tests or training-service save/load behavior
 * - unrelated template, smart-training, or single-line training screen coverage
 * Validation date: 2026-06-15
 */

import com.example.chessboard.ui.screen.training.train.EditTrainingScreen
import com.example.chessboard.ui.screen.training.common.TrainingLineEditorItem

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import com.example.chessboard.runtimecontext.RuntimeContext
import com.example.chessboard.runtimecontext.TrainingRuntimeContext
import com.example.chessboard.boardmodel.InitialBoardFen
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

class EditTrainingScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun editTrainingScreen_moveChipUpdatesVisibleBoardPosition() {
        setEditTrainingScreenContent(linesForTraining = listOf(TestTrainingLine))

        composeRule.waitForIdle()
        // This screen auto-scrolls to the selected line with animateScrollToItem(...).
        // On slower emulators that animation can overlap with performScrollTo(), so we wait
        // until the target node is actually displayed before clicking it.
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
    fun editTrainingScreen_moveChipOnUnselectedLineSelectsLineAndKeepsClickedPly() {
        setEditTrainingScreenContent(
            linesForTraining = listOf(
                TestTrainingLine,
                SecondTrainingLine,
            )
        )

        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditTrainingListTestTag)
            .performScrollToNode(hasText("Second Opening"))
        waitForTextDisplayed("Second Opening")
        composeRule.onNodeWithTag(moveChipTestTag("d4")).performScrollTo()
        waitForNodeDisplayed(moveChipTestTag("d4"))
        composeRule.onNodeWithTag(moveChipTestTag("d4"))
            .performSemanticsAction(SemanticsActions.OnClick)

        assertBoardFenEventually(AfterD4Fen)
    }

    @Test
    fun editTrainingScreen_moveChipOnUnselectedLineSelectsLineWithoutStartedTrainingSession() {
        var launchedLineId: Long? = null

        setEditTrainingScreenContentWithoutStartedSession(
            linesForTraining = listOf(
                TestTrainingLine,
                SecondTrainingLine,
            ),
            onStartLineTrainingClick = { lineId, _ ->
                launchedLineId = lineId
            },
        )

        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditTrainingListTestTag)
            .performScrollToNode(hasText("Second Opening"))
        waitForTextDisplayed("Second Opening")
        composeRule.onNodeWithTag(moveChipTestTag("d4")).performScrollTo()
        waitForNodeDisplayed(moveChipTestTag("d4"))
        composeRule.onNodeWithTag(moveChipTestTag("d4"))
            .performSemanticsAction(SemanticsActions.OnClick)

        assertBoardFenEventually(AfterD4Fen)

        waitForNodeDisplayedByContentDescription("Start training")
        composeRule.onNodeWithContentDescription("Start training").performClick()

        composeRule.runOnIdle {
            assertEquals(SecondTrainingLine.lineId, launchedLineId)
        }
    }

    @Test
    fun editTrainingScreen_nextArrowUpdatesVisibleBoardPosition() {
        setEditTrainingScreenContent(linesForTraining = listOf(TestTrainingLine))

        composeRule.waitForIdle()
        // Keep these waits. The test is intentionally defensive because this screen has
        // multiple async pieces of UI work:
        // 1. LazyColumn.performScrollTo() moves the list to the legend controls.
        // 2. The screen itself also auto-scrolls to the selected line via animateScrollToItem(...).
        // 3. The selected line's moves are loaded in LaunchedEffect(...), and only after that
        //    canRedo becomes true and the next-arrow click reliably advances the board.
        // Removing these waits tends to make the test flaky on slower emulators.
        composeRule.onNodeWithTag(EditTrainingListTestTag)
            .performScrollToNode(hasTestTag(EditTrainingMoveLegendSectionTestTag))
        waitForNodeDisplayed(EditTrainingMoveLegendSectionTestTag)
        waitForNodeDisplayedByContentDescription("Next move")
        // Wait for the board to settle at the initial position before clicking next.
        // This makes the test assert the intended transition: start position -> first move.
        assertBoardFenEventually(InitialBoardFen)
        composeRule.onNodeWithContentDescription("Next move").performClick()

        assertBoardFenEventually(AfterE4Fen)
    }

    @Test
    fun editTrainingScreen_startTrainingActionLaunchesTraining() {
        var launchedLineId: Long? = null

        setEditTrainingScreenContent(
            linesForTraining = listOf(TestTrainingLine),
            onStartLineTrainingClick = { lineId, _ ->
                launchedLineId = lineId
            },
        )

        waitForNodeDisplayedByContentDescription("Start training")
        composeRule.onNodeWithContentDescription("Start training").performClick()

        composeRule.runOnIdle {
            assert(launchedLineId == TestTrainingLine.lineId)
        }
    }

    @Test
    fun editTrainingScreen_removeSelectedLineRemovesItFromTraining() {
        setEditTrainingScreenContent(
            linesForTraining = listOf(
                TestTrainingLine,
                SecondTrainingLine,
            ),
        )

        composeRule.onNodeWithContentDescription("Remove line from training").performClick()
        waitForTextDisplayed("Remove Line")
        composeRule.onNode(hasText("Remove") and hasClickAction()).performClick()

        waitForTextInTree("Lines in training: 1")
        composeRule.onNodeWithTag(EditTrainingListTestTag)
            .performScrollToNode(hasText("Lines in training: 1"))
        waitForTextDisplayed("Lines in training: 1")
    }

    private fun assertBoardFenEventually(expectedFen: String) {
        // Do not replace this with a single immediate assert unless the screen's async behavior
        // is removed. On this screen the visible board can lag slightly behind the click because
        // selection, scroll, and board loading happen through Compose effects.
        val normalizedExpectedFen = normalizeFenForAssertion(expectedFen)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            currentBoardFen()?.let(::normalizeFenForAssertion) == normalizedExpectedFen
        }
        composeRule.onNodeWithTag(InteractiveChessBoardTestTag).assert(
            fenStateDescriptionMatcher(expectedFen)
        )
    }

    private fun waitForNodeDisplayed(tag: String) {
        // The node can exist in the semantics tree before it is actually stable and visible after
        // list scrolling. Waiting here removes a common source of flaky click failures.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag(tag).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun waitForNodeDisplayedByContentDescription(contentDescription: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithContentDescription(contentDescription).assertIsDisplayed()
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

    private fun setEditTrainingScreenContent(
        linesForTraining: List<TrainingLineEditorItem>,
    ) {
        setEditTrainingScreenContent(
            linesForTraining = linesForTraining,
            onStartLineTrainingClick = ::ignoreStartLineTrainingClick,
        )
    }

    private fun setEditTrainingScreenContent(
        linesForTraining: List<TrainingLineEditorItem>,
        onStartLineTrainingClick: (Long, List<Long>) -> Unit,
    ) {
        val trainingRuntimeContext = TrainingRuntimeContext()
        val orderedLineIds = linesForTraining.map { it.lineId }
        orderedLineIds.firstOrNull()?.let { firstLineId ->
            trainingRuntimeContext.rememberLaunch(
                trainingId = TestTrainingId,
                lineId = firstLineId,
                orderedLineIds = orderedLineIds,
            )
        }
        composeRule.setContent {
            ChessBoardTheme {
                EditTrainingScreen(
                    trainingId = TestTrainingId,
                    linesForTraining = linesForTraining,
                    orderLinesInTraining = RuntimeContext.OrderLinesInTraining(),
                    trainingRuntimeContext = trainingRuntimeContext,
                    onBackClick = ::ignoreBackClick,
                    onNavigate = ::ignoreNavigate,
                    onStartLineTrainingClick = onStartLineTrainingClick,
                    onOpenLineEditorClick = ::ignoreOpenLineEditorClick,
                    onOpenSettingsClick = ::ignoreOpenSettingsClick,
                    onSaveTraining = ::ignoreSaveTraining,
                )
            }
        }
    }

    private fun setEditTrainingScreenContentWithoutStartedSession(
        linesForTraining: List<TrainingLineEditorItem>,
        onStartLineTrainingClick: (Long, List<Long>) -> Unit,
    ) {
        composeRule.setContent {
            ChessBoardTheme {
                EditTrainingScreen(
                    trainingId = TestTrainingId,
                    linesForTraining = linesForTraining,
                    orderLinesInTraining = RuntimeContext.OrderLinesInTraining(),
                    trainingRuntimeContext = TrainingRuntimeContext(),
                    onBackClick = ::ignoreBackClick,
                    onNavigate = ::ignoreNavigate,
                    onStartLineTrainingClick = onStartLineTrainingClick,
                    onOpenLineEditorClick = ::ignoreOpenLineEditorClick,
                    onOpenSettingsClick = ::ignoreOpenSettingsClick,
                    onSaveTraining = ::ignoreSaveTraining,
                )
            }
        }
    }

    private companion object {
        const val TestTrainingId = 1L

        fun ignoreBackClick() = Unit

        fun ignoreNavigate(screenType: com.example.chessboard.ui.screen.ScreenType) = Unit

        fun ignoreStartLineTrainingClick(lineId: Long, orderedLineIds: List<Long>) = Unit

        fun ignoreOpenLineEditorClick(lineId: Long) = Unit

        fun ignoreOpenSettingsClick() = Unit

        fun ignoreSaveTraining(
            trainingName: String,
            editableLines: List<TrainingLineEditorItem>,
            showSuccessMessage: Boolean,
            onSaved: (() -> Unit)?,
        ) {
            onSaved?.invoke()
        }

        val TestTrainingLine = TrainingLineEditorItem(
            lineId = 1L,
            title = "Test Opening",
            pgn = "1. e2e4 e7e5 *",
            sideMask = SideMask.WHITE
        )
        val SecondTrainingLine = TrainingLineEditorItem(
            lineId = 2L,
            title = "Second Opening",
            pgn = "1. d2d4 d7d5 *",
            sideMask = SideMask.WHITE
        )
        const val AfterE4Fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
        const val AfterD4Fen = "rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq - 0 1"
    }
}
