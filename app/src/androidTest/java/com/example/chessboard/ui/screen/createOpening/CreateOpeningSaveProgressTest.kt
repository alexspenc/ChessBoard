package com.example.chessboard.ui.screen.createOpening

/**
 * File role: verifies create-opening save progress UI, cancellation behavior, and localization wrapper startup.
 * Allowed here:
 * - deterministic Compose tests for save-progress dialog behavior
 * - smoke tests for create-opening launchers inside localized composition
 * - fake save runners that avoid real PGN files and long database operations
 * Not allowed here:
 * - broad navigation coverage or database-backed save integration scenarios
 * Validation date: 2026-05-28
 */
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.chessboard.boardmodel.LineDraft
import com.example.chessboard.localization.AppLanguage
import com.example.chessboard.localization.ProvideAppLanguage
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.ui.CreateOpeningContentTestTag
import com.example.chessboard.ui.CreateOpeningSaveCancelTestTag
import com.example.chessboard.ui.CreateOpeningSaveProgressDialogTestTag
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.theme.ChessBoardTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.junit.Rule
import org.junit.Test

class CreateOpeningSaveProgressTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun createOpening_canOpenInsideLocalizedComposition() {
        composeRule.setContent {
            ChessBoardTheme {
                ProvideAppLanguage(AppLanguage.RUSSIAN) {
                    CreateOpeningScreenContainer(
                        activity = composeRule.activity,
                        screenContext = ScreenContainerContext(
                            inDbProvider = DatabaseProvider.createInstance(composeRule.activity)
                        ),
                        initialDraft = LineDraft(),
                    )
                }
            }
        }

        composeRule.onNodeWithTag(CreateOpeningContentTestTag).assertIsDisplayed()
    }

    @Test
    fun createOpening_saveShowsProgressAndCanBeCanceledAtFifthLine() {
        val waitForCancelAtFifthLine = CompletableDeferred<Unit>()

        composeRule.setContent {
            ChessBoardTheme {
                CreateOpeningScreenContainer(
                    activity = composeRule.activity,
                    screenContext = ScreenContainerContext(
                        inDbProvider = DatabaseProvider.createInstance(composeRule.activity)
                    ),
                    initialDraft = LineDraft(
                        line = LineDraft().line.copy(event = "Cancelable Opening")
                    ),
                    saveOpeningRunner = { _, _, _, _, _, onProgress ->
                        val totalLines = 15
                        var processedLinesCount = 0
                        var savedLinesCount = 0
                        var skippedLinesCount = 0

                        onProgress(
                            CreateOpeningSaveProgress(
                                totalLines = totalLines,
                                processedLinesCount = processedLinesCount,
                                savedLinesCount = savedLinesCount,
                                skippedLinesCount = skippedLinesCount,
                            )
                        )

                        repeat(totalLines) {
                            currentCoroutineContext().ensureActive()
                            processedLinesCount += 1
                            savedLinesCount += 1
                            onProgress(
                                CreateOpeningSaveProgress(
                                    totalLines = totalLines,
                                    processedLinesCount = processedLinesCount,
                                    savedLinesCount = savedLinesCount,
                                    skippedLinesCount = skippedLinesCount,
                                )
                            )

                            if (processedLinesCount == 5) {
                                waitForCancelAtFifthLine.await()
                            }
                        }

                        CreateOpeningSaveResult.NavigateBack
                    }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Save").performClick()

        waitForNodeWithTag(CreateOpeningSaveProgressDialogTestTag)
        waitForText("Processed lines: 5/15")
        composeRule.onNodeWithTag(CreateOpeningSaveCancelTestTag).performClick()

        waitForText("Save canceled.")
        waitForText("Processed lines: 5/15")
        waitForNodeWithTagToDisappear(CreateOpeningSaveProgressDialogTestTag)
    }

    private fun waitForNodeWithTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithTag(tag).fetchSemanticsNode()
                true
            }.getOrDefault(false)
        }
    }

    private fun waitForNodeWithTagToDisappear(tag: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithTag(tag).fetchSemanticsNode()
                false
            }.getOrDefault(true)
        }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithText(text, substring = true).fetchSemanticsNode()
                true
            }.getOrDefault(false)
        }
    }
}
