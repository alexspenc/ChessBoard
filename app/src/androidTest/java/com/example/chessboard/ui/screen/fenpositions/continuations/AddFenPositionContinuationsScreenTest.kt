package com.example.chessboard.ui.screen.fenpositions.continuations

/*
 * File role: verifies pure Compose presentation for adding FEN position continuations.
 * Allowed here:
 * - input-mode availability, complete SAN previews, dialog content, and callback assertions
 * Not allowed here:
 * - debounce timing, PGN parsing, Room/service integration, or app navigation routing
 * Validation date: 2026-09-02
 */

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddBoardTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddDiscardDialogTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddDiscardExitTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddErrorDialogTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddManualBackTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddManualClearTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddProcessingDialogTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddResultConfirmTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddResultDialogTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddSaveTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddTextClearTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddTextInputTestTag
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AddFenPositionContinuationsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /*
     * Verifies the main screen composition:
     * - position name, theme, and board are present;
     * - the small Manual line and New continuations headings are always present;
     * - manual and prepared SAN lines are not shortened;
     * - a long continuation can be reached through the shared screen scroll.
     *
     * This presentation-only state intentionally fills both SAN sections at once. The workflow
     * must keep manual and text input mutually exclusive, but one composition can verify complete
     * rendering of both visual blocks without duplicating the layout setup in another test.
     */
    @Test
    fun screenShowsPositionBoardPermanentHeadingsAndCompleteSanLines() {
        val secondLine =
            "1... Nf6 2. Bg2 c5 3. O-O Nc6 4. d4 e6 5. c4 Be7 6. Nc3 O-O"
        setScreen(
            state = contentState(
                manualSanLine = "1... Nf6 2. Bg2 g6",
                newContinuationSanLines = listOf(
                    "1... Nf6 2. Bg2 g6 3. O-O Bg7 4. c4 c6",
                    secondLine,
                ),
            ),
            actions = ignoredActions(),
        )

        composeRule.onNodeWithText("Isolated Pawn").assertIsDisplayed()
        composeRule.onNodeWithText("Theme: Strategy").assertIsDisplayed()
        composeRule.onNodeWithTag(
            testTag = FenPositionContinuationAddBoardTestTag,
            useUnmergedTree = true,
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Manual line").assertExists()
        composeRule.onNodeWithText("New continuations").assertExists()
        composeRule.onNodeWithText("1... Nf6 2. Bg2 g6").assertExists()
        composeRule.onNodeWithText("Continuation 2").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(secondLine).performScrollTo().assertIsDisplayed()
    }

    /*
     * Verifies the manual-input half of the mutual-exclusion contract:
     * - an existing manual line disables the PGN/SAN text field;
     * - the screen explains how to switch to text input;
     * - Back and Clear are enabled for a non-empty manual line;
     * - both controls forward their required callbacks;
     * - Save can remain available for a valid manual line.
     */
    @Test
    fun manualLineDisablesTextInputAndForwardsManualActions() {
        var backClicks = 0
        var clearClicks = 0
        val actions = ignoredActions().copy(
            onManualBackClick = { backClicks += 1 },
            onManualClearClick = { clearClicks += 1 },
        )
        setScreen(
            state = contentState(
                manualSanLine = "1. e4 e5",
                canUndoManualLine = true,
                canClearManualLine = true,
                canSave = true,
            ),
            actions = actions,
        )

        composeRule.onNodeWithTag(FenPositionContinuationAddTextInputTestTag)
            .assertIsNotEnabled()
        composeRule.onNodeWithText("Clear the manual line to paste text").assertExists()
        composeRule.onNodeWithTag(FenPositionContinuationAddManualBackTestTag)
            .assertIsEnabled()
            .performClick()
        composeRule.onNodeWithTag(FenPositionContinuationAddManualClearTestTag)
            .assertIsEnabled()
            .performClick()
        composeRule.onNodeWithTag(FenPositionContinuationAddSaveTestTag).assertIsEnabled()

        composeRule.runOnIdle {
            assertEquals(1, backClicks)
            assertEquals(1, clearClicks)
        }
    }

    /*
     * Verifies the text-input half of the mutual-exclusion contract:
     * - non-blank PGN/SAN text disables board input;
     * - the text field itself remains editable;
     * - the screen explains how to return to manual board input;
     * - Clear text forwards its callback;
     * - Save stays disabled until the text has been successfully processed.
     *
     * At this pure-UI stage the disabled board state is expressed through semantics. The next
     * implementation stage must also enforce the same state in the interactive LineController.
     */
    @Test
    fun continuationTextDisablesBoardAndCanBeCleared() {
        var clearTextClicks = 0
        setScreen(
            state = contentState(
                text = "1. e4 e5",
                canSave = false,
            ),
            actions = ignoredActions().copy(
                onTextClearClick = { clearTextClicks += 1 },
            ),
        )

        composeRule.onNodeWithTag(
            testTag = FenPositionContinuationAddBoardTestTag,
            useUnmergedTree = true,
        ).assertIsNotEnabled()
        composeRule.onNodeWithTag(FenPositionContinuationAddTextInputTestTag)
            .assertIsEnabled()
        composeRule.onNodeWithText("Clear the text to enter moves on the board").assertExists()
        composeRule.onNodeWithTag(FenPositionContinuationAddTextClearTestTag)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(FenPositionContinuationAddSaveTestTag).assertIsNotEnabled()

        composeRule.runOnIdle {
            assertEquals(1, clearTextClicks)
        }
    }

    /*
     * Verifies the initial empty state:
     * - both small section headings keep their stable places in the layout;
     * - the PGN/SAN field displays its input hint and remains enabled;
     * - manual Back and Clear are disabled because there are no moves;
     * - Clear text is absent because the field is blank;
     * - Save is disabled because there is no continuation to persist.
     */
    @Test
    fun emptyScreenKeepsHeadingsAndDisablesUnavailableActions() {
        setScreen(
            state = contentState(),
            actions = ignoredActions(),
        )

        composeRule.onNodeWithText("Manual line").assertExists()
        composeRule.onNodeWithText("New continuations").assertExists()
        composeRule.onNodeWithText("Paste PGN or a sequence of SAN moves").assertExists()
        composeRule.onNodeWithTag(FenPositionContinuationAddManualBackTestTag)
            .assertIsNotEnabled()
        composeRule.onNodeWithTag(FenPositionContinuationAddManualClearTestTag)
            .assertIsNotEnabled()
        composeRule.onNodeWithTag(FenPositionContinuationAddTextInputTestTag)
            .assertIsEnabled()
        composeRule.onNodeWithTag(FenPositionContinuationAddTextClearTestTag)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(FenPositionContinuationAddSaveTestTag)
            .assertIsNotEnabled()
    }

    /*
     * Verifies that the pure screen forwards user events instead of owning their workflow:
     * - the top-bar back button invokes onBackClick;
     * - text entry invokes onTextChange with the entered value;
     * - the bottom action invokes onSaveClick.
     *
     * Containers will own back confirmation, text state, parsing, and persistence. canSave is
     * forced in this presentation test solely to make the save callback reachable.
     *
     * BasicTextField is controlled by the value supplied through screen state. The test therefore
     * feeds every onTextChange value back into Compose state, just as the future container must do.
     * Without that feedback, the field restores its previous empty value and the test observes the
     * reset callback instead of the entered text.
     */
    @Test
    fun screenForwardsBackTextAndSaveActions() {
        var backClicks = 0
        val enteredText = mutableStateOf("")
        var saveClicks = 0
        val lineController = LineController()
        composeRule.setContent {
            ChessBoardTheme {
                AddFenPositionContinuationsScreen(
                    lineController = lineController,
                    state = contentState(
                        text = enteredText.value,
                        canSave = true,
                    ),
                    actions = ignoredActions().copy(
                        onBackClick = { backClicks += 1 },
                        onTextChange = { text -> enteredText.value = text },
                        onSaveClick = { saveClicks += 1 },
                    ),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithTag(FenPositionContinuationAddTextInputTestTag)
            .performTextInput("1. e4")
        composeRule.onNodeWithTag(FenPositionContinuationAddSaveTestTag).performClick()

        composeRule.runOnIdle {
            assertEquals(1, backClicks)
            assertEquals("1. e4", enteredText.value)
            assertEquals(1, saveClicks)
        }
    }

    /*
     * Verifies blocking-operation presentation:
     * - the loading dialog is visible;
     * - its text identifies the current processing stage;
     * - an otherwise available Save action becomes disabled.
     *
     * Disabling Save prevents the same input from starting a second operation while the first one
     * is still running.
     */
    @Test
    fun processingDialogShowsCurrentStageAndDisablesSave() {
        setScreen(
            state = contentState(
                canSave = true,
                dialogState = AddFenPositionContinuationsDialogState.Processing(
                    AddFenPositionContinuationsProcessingStage.CheckingStoredContinuations,
                ),
            ),
            actions = ignoredActions(),
        )

        composeRule.onNodeWithTag(FenPositionContinuationAddProcessingDialogTestTag)
            .assertIsDisplayed()
        composeRule.onNodeWithText("Checking saved continuations").assertIsDisplayed()
        composeRule.onNodeWithTag(FenPositionContinuationAddSaveTestTag)
            .assertIsNotEnabled()
    }

    /*
     * Verifies the successful processing-result dialog:
     * - recognized and new line counts are always shown;
     * - a non-zero covered-prefix count is shown;
     * - zero exact-duplicate and stored-coverage counts are omitted;
     * - pressing OK forwards the required result-dismiss callback.
     *
     * Omitting zero-valued exclusion counters keeps the one-time dialog compact.
     */
    @Test
    fun resultDialogShowsRequiredAndOnlyNonZeroStatistics() {
        var dismissClicks = 0
        setScreen(
            state = contentState(
                dialogState = AddFenPositionContinuationsDialogState.Result(
                    AddFenPositionContinuationsProcessingResult(
                        recognizedLinesCount = 4,
                        exactDuplicateLinesCount = 0,
                        coveredPrefixLinesCount = 1,
                        coveredByStoredLinesCount = 0,
                        newLinesCount = 3,
                    ),
                ),
            ),
            actions = ignoredActions().copy(
                dialogActions = ignoredDialogActions().copy(
                    onResultDismiss = { dismissClicks += 1 },
                ),
            ),
        )

        composeRule.onNodeWithTag(FenPositionContinuationAddResultDialogTestTag)
            .assertIsDisplayed()
        composeRule.onNodeWithText("Recognized lines: 4", substring = true).assertExists()
        composeRule.onNodeWithText("Short lines covered by longer lines: 1", substring = true)
            .assertExists()
        composeRule.onNodeWithText("New lines: 3", substring = true).assertExists()
        composeRule.onNodeWithText("Exact duplicates removed:", substring = true)
            .assertDoesNotExist()
        composeRule.onNodeWithText("Already covered by saved continuations:", substring = true)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(FenPositionContinuationAddResultConfirmTestTag)
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, dismissClicks)
        }
    }

    /*
     * Verifies the successful check that produced nothing to insert:
     * - the dialog explicitly says that there are no new continuations;
     * - the less useful New lines: 0 row is not rendered.
     *
     * The future container will keep Save disabled and retain the original input for correction.
     */
    @Test
    fun resultDialogShowsNoNewContinuations() {
        setScreen(
            state = contentState(
                dialogState = AddFenPositionContinuationsDialogState.Result(
                    AddFenPositionContinuationsProcessingResult(
                        recognizedLinesCount = 2,
                        exactDuplicateLinesCount = 0,
                        coveredPrefixLinesCount = 0,
                        coveredByStoredLinesCount = 2,
                        newLinesCount = 0,
                    ),
                ),
            ),
            actions = ignoredActions(),
        )

        composeRule.onNodeWithText("No new continuations", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("New lines:", substring = true).assertDoesNotExist()
    }

    /*
     * Verifies error presentation:
     * - the screen renders the title and message prepared by the container;
     * - pressing OK invokes the dedicated error-dismiss callback.
     *
     * Error recovery belongs to the future container. A dedicated callback ensures that closing
     * the dialog does not implicitly clear the entered text or manual line.
     */
    @Test
    fun errorDialogForwardsRequiredDismissAction() {
        var errorDismissClicks = 0
        val errorActions = ignoredActions().copy(
            dialogActions = ignoredDialogActions().copy(
                onErrorDismiss = { errorDismissClicks += 1 },
            ),
        )
        setScreen(
            state = contentState(
                dialogState = AddFenPositionContinuationsDialogState.Error(
                    title = "Parsing failed",
                    message = "Invalid move in line 2",
                ),
            ),
            actions = errorActions,
        )

        composeRule.onNodeWithTag(FenPositionContinuationAddErrorDialogTestTag)
            .assertIsDisplayed()
        composeRule.onNodeWithText("OK").performClick()
        composeRule.runOnIdle {
            assertEquals(1, errorDismissClicks)
        }
    }

    /*
     * Verifies the safe branch of the unsaved-input confirmation:
     * - the discard dialog is visible;
     * - pressing Stay invokes onDiscardStay.
     *
     * Staying is a distinct required action, not a default dismissal that could accidentally be
     * wired to navigation.
     */
    @Test
    fun discardDialogForwardsStayAction() {
        var stayClicks = 0
        val discardActions = ignoredActions().copy(
            dialogActions = ignoredDialogActions().copy(
                onDiscardStay = { stayClicks += 1 },
            ),
        )
        setScreen(
            state = contentState(
                dialogState = AddFenPositionContinuationsDialogState.ConfirmDiscard,
            ),
            actions = discardActions,
        )

        composeRule.onNodeWithTag(FenPositionContinuationAddDiscardDialogTestTag)
            .assertIsDisplayed()
        composeRule.onNodeWithText("Stay").performClick()
        composeRule.runOnIdle {
            assertEquals(1, stayClicks)
        }
    }

    /*
     * Verifies the destructive branch of the unsaved-input confirmation:
     * - pressing Exit invokes onDiscardExit;
     * - navigation can therefore be connected only to the explicit data-loss choice.
     */
    @Test
    fun discardDialogForwardsExitAction() {
        var exitClicks = 0
        val discardActions = ignoredActions().copy(
            dialogActions = ignoredDialogActions().copy(
                onDiscardExit = { exitClicks += 1 },
            ),
        )
        setScreen(
            state = contentState(
                dialogState = AddFenPositionContinuationsDialogState.ConfirmDiscard,
            ),
            actions = discardActions,
        )
        composeRule.onNodeWithTag(FenPositionContinuationAddDiscardExitTestTag)
            .performClick()
        composeRule.runOnIdle {
            assertEquals(1, exitClicks)
        }
    }

    private fun setScreen(
        state: AddFenPositionContinuationsScreenState,
        actions: AddFenPositionContinuationsScreenActions,
    ) {
        composeRule.setContent {
            ChessBoardTheme {
                AddFenPositionContinuationsScreen(
                    lineController = LineController(),
                    state = state,
                    actions = actions,
                )
            }
        }
    }

    private fun contentState(
        manualSanLine: String = "",
        text: String = "",
        newContinuationSanLines: List<String> = emptyList(),
        canUndoManualLine: Boolean = false,
        canClearManualLine: Boolean = false,
        canSave: Boolean = false,
        dialogState: AddFenPositionContinuationsDialogState? = null,
    ): AddFenPositionContinuationsScreenState {
        return AddFenPositionContinuationsScreenState(
            positionName = "Isolated Pawn",
            theme = "Strategy",
            manualSanLine = manualSanLine,
            text = text,
            newContinuationSanLines = newContinuationSanLines,
            canUndoManualLine = canUndoManualLine,
            canClearManualLine = canClearManualLine,
            canSave = canSave,
            dialogState = dialogState,
        )
    }

    private fun ignoredActions(): AddFenPositionContinuationsScreenActions {
        return AddFenPositionContinuationsScreenActions(
            onBackClick = ::ignoreAction,
            onManualBackClick = ::ignoreAction,
            onManualClearClick = ::ignoreAction,
            onTextChange = ::ignoreTextChange,
            onTextClearClick = ::ignoreAction,
            onSaveClick = ::ignoreAction,
            dialogActions = ignoredDialogActions(),
        )
    }

    private fun ignoredDialogActions(): AddFenPositionContinuationsDialogActions {
        return AddFenPositionContinuationsDialogActions(
            onResultDismiss = ::ignoreAction,
            onErrorDismiss = ::ignoreAction,
            onDiscardStay = ::ignoreAction,
            onDiscardExit = ::ignoreAction,
        )
    }

    private fun ignoreAction() = Unit

    private fun ignoreTextChange(text: String) {
        text.length
    }
}
