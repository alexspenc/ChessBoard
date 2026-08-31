package com.example.chessboard.ui.screen.fenpositions.catalog

/*
 * File role: verifies input gating and debounced FEN handling in the create-position dialog.
 * Allowed here:
 * - dialog field interaction, pending validation, and emitted create requests
 * Not allowed here:
 * - Room persistence, catalog paging, or app navigation
 * Validation date: 2026-08-31
 */

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCreateConfirmTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCreateFenInputTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCreateThemeInputTestTag
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CreateFenPositionDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun validFenIsDebouncedAndSubmittedAsFourFieldFen() {
        val requests = mutableListOf<CreateFenPositionRequest>()
        setDialog(onCreate = requests::add)
        composeRule.mainClock.autoAdvance = false

        try {
            composeRule.onNodeWithTag(FenPositionCreateFenInputTestTag)
                .performTextInput("$InitialPositionFen 17 42")

            // Keep this frame advance before the debounce wait: it commits the entered text and
            // starts LaunchedEffect(fen). Without it, virtual time can advance before validation starts.
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.onNodeWithText("Checking FEN…").assertIsDisplayed()
            composeRule.onNodeWithTag(FenPositionCreateConfirmTestTag).assertIsNotEnabled()

            // Production debounce is 600 ms. Virtual time has a 100 ms margin here at no wall-clock cost.
            composeRule.mainClock.advanceTimeBy(FenValidationDebounceWaitMs)
            composeRule.mainClock.autoAdvance = true
            waitForNodeEnabled(FenPositionCreateConfirmTestTag)

            composeRule.onNodeWithTag(FenPositionCreateThemeInputTestTag)
                .performScrollTo()
                .performTextInput("Basics")
            composeRule.onNodeWithTag(FenPositionCreateConfirmTestTag)
                .assertIsEnabled()
                .performClick()

            composeRule.runOnIdle {
                assertEquals(
                    listOf(
                        CreateFenPositionRequest(
                            fen = InitialPositionFen,
                            name = "",
                            theme = "Basics",
                            description = "",
                        ),
                    ),
                    requests,
                )
            }
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun invalidFenDoesNotSubmitCreateRequest() {
        val requests = mutableListOf<CreateFenPositionRequest>()
        setDialog(onCreate = requests::add)
        composeRule.mainClock.autoAdvance = false

        try {
            composeRule.onNodeWithTag(FenPositionCreateFenInputTestTag)
                .performTextInput("not a fen")

            // Keep this frame advance before the debounce wait: it commits the entered text and
            // starts LaunchedEffect(fen). Without it, virtual time can advance before validation starts.
            composeRule.mainClock.advanceTimeByFrame()
            // Production debounce is 600 ms. Virtual time has a 100 ms margin here at no wall-clock cost.
            composeRule.mainClock.advanceTimeBy(FenValidationDebounceWaitMs)
            composeRule.mainClock.autoAdvance = true
            waitForTextExists("Invalid FEN")

            composeRule.onNodeWithText("Invalid FEN")
                .assertExists()
                .assertIsDisplayed()
            composeRule.onNodeWithTag(FenPositionCreateConfirmTestTag)
                .assertIsEnabled()
                .performClick()
            composeRule.runOnIdle {
                assertEquals(emptyList<CreateFenPositionRequest>(), requests)
            }
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    private fun waitForNodeEnabled(testTag: String) {
        // FEN parsing runs on Dispatchers.Default, outside the Compose test clock. This polling wait
        // must remain after the debounce; waiting only for Compose idleness does not await that work.
        composeRule.waitUntil(timeoutMillis = BackgroundValidationTimeoutMs) {
            runCatching {
                composeRule.onNodeWithTag(testTag).assertIsEnabled()
                true
            }.getOrDefault(false)
        }
    }

    private fun waitForTextExists(text: String) {
        // FEN parsing runs on Dispatchers.Default, outside the Compose test clock. This polling wait
        // must remain after the debounce; waiting only for Compose idleness does not await that work.
        composeRule.waitUntil(timeoutMillis = BackgroundValidationTimeoutMs) {
            runCatching {
                composeRule.onNodeWithText(text).assertExists()
                true
            }.getOrDefault(false)
        }
    }

    private fun setDialog(onCreate: (CreateFenPositionRequest) -> Unit) {
        composeRule.setContent {
            ChessBoardTheme {
                CreateFenPositionDialog(
                    strings = dialogStrings(),
                    isSaving = false,
                    saveErrorMessage = null,
                    onInputChanged = {},
                    onDismiss = {},
                    onCreate = onCreate,
                )
            }
        }
    }

    private fun dialogStrings(): FenPositionCreateDialogStrings {
        return FenPositionCreateDialogStrings(
            title = "New position",
            fenLabel = "FEN *",
            fenPlaceholder = "Paste FEN",
            nameLabel = "Name",
            namePlaceholder = "Position name",
            themeLabel = "Theme *",
            themePlaceholder = "Strategy",
            descriptionLabel = "Description",
            descriptionPlaceholder = "Notes",
            checkingFen = "Checking FEN…",
            previewPrompt = "Enter FEN",
            invalidFen = "Invalid FEN",
            themeRequired = "Theme is required",
            duplicateFen = "Duplicate FEN",
            saveFailedTitle = "Save failed",
            saveFailed = "Save failed",
            savingTitle = "Saving position",
            savingMessage = "Saving position and description…",
            cancel = "Cancel",
            add = "Add",
        )
    }

    private companion object {
        const val FenValidationDebounceWaitMs = 700L
        const val BackgroundValidationTimeoutMs = 5_000L
        const val InitialPositionFen =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -"
    }
}
