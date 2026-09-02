package com.example.chessboard.ui.screen.fenpositions.continuations

/*
 * File role: verifies container wiring for debounced pasted continuation text.
 * Allowed here:
 * - virtual debounce timing, processing/result/error dialogs, SAN output, and ready-line callbacks
 * Not allowed here:
 * - Room persistence, app navigation routing, or pure screen layout coverage
 * Validation date: 2026-09-02
 */

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddErrorDialogTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddProcessingDialogTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddResultConfirmTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddResultDialogTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddSaveTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddTextInputTestTag
import com.example.chessboard.ui.theme.ChessBoardTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class AddFenPositionContinuationsContainerTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /*
     * This test covers the complete successful container path:
     * - no processing begins before the 600 ms debounce expires;
     * - a blocking stage is visible while saved continuations are being loaded;
     * - equal parsed branches are counted and reduced to one new line;
     * - dismissing the one-time result keeps the full SAN preview and enables Save;
     * - Save forwards only the filtered UCI line.
     */
    @Test
    fun debouncedTextProcessingShowsStatisticsAndForwardsNewLine() {
        val storedLinesGate = CompletableDeferred<List<List<String>>>()
        val loaderCalls = AtomicInteger(0)
        var savedLines = emptyList<List<String>>()
        setContainer(
            loadStoredUciLines = {
                loaderCalls.incrementAndGet()
                storedLinesGate.await()
            },
            onSaveClick = { lines -> savedLines = lines },
        )
        composeRule.mainClock.autoAdvance = false

        try {
            composeRule.onNodeWithTag(FenPositionContinuationAddTextInputTestTag)
                .performTextInput(DuplicateBranchText)

            // Keep this frame advance: it commits BasicTextField state and starts the new
            // LaunchedEffect before virtual debounce time is advanced.
            composeRule.mainClock.advanceTimeByFrame()
            assertEquals(0, loaderCalls.get())
            composeRule.onNodeWithTag(FenPositionContinuationAddProcessingDialogTestTag)
                .assertDoesNotExist()

            // Production debounce is 600 ms. The extra 100 ms prevents a boundary race without
            // adding wall-clock time because this delay runs on the Compose test clock.
            composeRule.mainClock.advanceTimeBy(DebounceWaitMillis)
            composeRule.mainClock.autoAdvance = true

            // Parsing and filtering run on Dispatchers.Default, outside the virtual test clock.
            // Keep this polling wait so the assertion cannot race the background transition.
            waitForText("Checking saved continuations")
            composeRule.onNodeWithTag(FenPositionContinuationAddProcessingDialogTestTag)
                .assertIsDisplayed()
            composeRule.onNodeWithText("Checking saved continuations").assertIsDisplayed()
            assertEquals(1, loaderCalls.get())

            storedLinesGate.complete(emptyList())

            // Stored-line loading and SAN construction also run outside the Compose clock. Keep
            // this wait with a generous emulator margin before reading the result dialog.
            waitForTag(FenPositionContinuationAddResultDialogTestTag)
            composeRule.onNodeWithText("Recognized lines: 2", substring = true).assertExists()
            composeRule.onNodeWithText("Exact duplicates removed: 1", substring = true)
                .assertExists()
            composeRule.onNodeWithText("New lines: 1", substring = true).assertExists()
            composeRule.onNodeWithTag(FenPositionContinuationAddResultConfirmTestTag)
                .performClick()

            composeRule.onNodeWithTag(FenPositionContinuationAddResultDialogTestTag)
                .assertDoesNotExist()
            composeRule.onNodeWithText("1. e4 e5")
                .performScrollTo()
                .assertIsDisplayed()
            composeRule.onNodeWithTag(FenPositionContinuationAddSaveTestTag)
                .assertIsEnabled()
                .performClick()

            composeRule.runOnIdle {
                assertEquals(listOf(listOf("e2e4", "e7e5")), savedLines)
            }
        } finally {
            storedLinesGate.complete(emptyList())
            composeRule.mainClock.autoAdvance = true
        }
    }

    /*
     * A line already covered by an equal saved continuation is still a successful processing
     * result, but it produces no screen preview and cannot be submitted again.
     */
    @Test
    fun storedContinuationProducesNoNewLinesAndKeepsSaveDisabled() {
        setContainer(
            loadStoredUciLines = { listOf(listOf("e2e4", "e7e5")) },
            onSaveClick = ::ignoreLines,
        )

        enterTextAndFinishDebounce("1. e4 e5")
        waitForTag(FenPositionContinuationAddResultDialogTestTag)

        composeRule.onNodeWithText("Already covered by saved continuations: 1", substring = true)
            .assertExists()
        composeRule.onNodeWithText("No new continuations", substring = true).assertExists()
        composeRule.onNodeWithTag(FenPositionContinuationAddResultConfirmTestTag).performClick()
        composeRule.onNodeWithText("Continuation 1").assertDoesNotExist()
        composeRule.onNodeWithTag(FenPositionContinuationAddSaveTestTag)
            .assertIsNotEnabled()
    }

    /*
     * One invalid variation rejects the whole pasted tree before the database-read stage. Closing
     * the error dialog must leave the original text available for correction.
     */
    @Test
    fun invalidVariationShowsErrorAndRetainsText() {
        val loaderCalls = AtomicInteger(0)
        val invalidText = "1. e4 e5 (1... Qa5) 2. Nf3"
        setContainer(
            loadStoredUciLines = {
                loaderCalls.incrementAndGet()
                emptyList()
            },
            onSaveClick = ::ignoreLines,
        )

        enterTextAndFinishDebounce(invalidText)

        // Parsing happens on Dispatchers.Default, so Compose idleness alone does not guarantee
        // that the error state has already returned to the main composition.
        waitForTag(FenPositionContinuationAddErrorDialogTestTag)
        composeRule.onNodeWithText("Could not process continuations").assertIsDisplayed()
        composeRule.onNodeWithText("Cannot play Qa5", substring = true).assertExists()
        assertEquals(0, loaderCalls.get())

        composeRule.onNodeWithText("OK").performClick()
        composeRule.onNodeWithTag(FenPositionContinuationAddErrorDialogTestTag)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(FenPositionContinuationAddTextInputTestTag)
            .assertTextContains(invalidText)
        composeRule.onNodeWithTag(FenPositionContinuationAddSaveTestTag)
            .assertIsNotEnabled()
    }

    private fun setContainer(
        loadStoredUciLines: suspend () -> List<List<String>>,
        onSaveClick: (List<List<String>>) -> Unit,
    ) {
        composeRule.setContent {
            ChessBoardTheme {
                AddFenPositionContinuationsContainer(
                    startFen = InitialPositionFen,
                    positionName = "Position",
                    positionTheme = "Strategy",
                    loadStoredUciLines = loadStoredUciLines,
                    onBackClick = ::ignoreAction,
                    onSaveClick = onSaveClick,
                )
            }
        }
    }

    private fun enterTextAndFinishDebounce(text: String) {
        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.onNodeWithTag(FenPositionContinuationAddTextInputTestTag)
                .performTextInput(text)
            // Keep the initial frame and 100 ms timing margin for the same reasons documented in
            // debouncedTextProcessingShowsStatisticsAndForwardsNewLine.
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.mainClock.advanceTimeBy(DebounceWaitMillis)
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    private fun waitForTag(tag: String) {
        // Keep this polling wait after advancing debounce time: parsing, stored-line loading, and
        // SAN construction use background dispatchers that are not controlled by mainClock.
        composeRule.waitUntil(timeoutMillis = BackgroundProcessingTimeoutMillis) {
            runCatching {
                composeRule.onNodeWithTag(tag).fetchSemanticsNode()
                true
            }.getOrDefault(false)
        }
    }

    private fun waitForText(text: String) {
        // Stage changes surround work on background dispatchers, so a single immediate semantics
        // lookup can observe the previous processing stage even after virtual time has advanced.
        composeRule.waitUntil(timeoutMillis = BackgroundProcessingTimeoutMillis) {
            runCatching {
                composeRule.onNodeWithText(text).fetchSemanticsNode()
                true
            }.getOrDefault(false)
        }
    }

    private fun ignoreAction() = Unit

    private fun ignoreLines(lines: List<List<String>>) {
        lines.size
    }

    private companion object {
        const val InitialPositionFen =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -"
        const val DuplicateBranchText = "1. e4 (1. e4 e5) e5"
        const val DebounceWaitMillis = 700L
        const val BackgroundProcessingTimeoutMillis = 10_000L
    }
}
