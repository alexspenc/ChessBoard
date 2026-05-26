package com.example.chessboard.ui.screen.trainSingleLine

/*
 * UI tests for the single-line training completion dialog.
 *
 * Keep only dialog-level regression coverage here. Do not add broader screen
 * navigation or full training-session flow tests to this file.
 */

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Rule
import org.junit.Test

class TrainSingleLineCompletionDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun completionDialog_hidesNextTrainingButton_whenCallbackIsMissing() {
        composeRule.setContent {
            ChessBoardTheme {
                TrainSingleLineCompletionDialog(
                    dialogState = completedVariationDialogState(),
                    onRepeatClick = {},
                    onFinishClick = {},
                )
            }
        }

        composeRule.onAllNodesWithText("Next").assertCountEquals(0)
    }

    @Test
    fun completionDialog_showsNextTrainingButton_whenCallbackIsProvided() {
        composeRule.setContent {
            ChessBoardTheme {
                TrainSingleLineCompletionDialog(
                    dialogState = completedVariationDialogState(),
                    onRepeatClick = {},
                    onFinishClick = {},
                    onNextTrainingClick = {},
                )
            }
        }

        composeRule.onAllNodesWithText("Next").assertCountEquals(1)
    }

    @Test
    fun completionDialog_showsDoubtNextButton_whenCallbackIsProvided() {
        composeRule.setContent {
            ChessBoardTheme {
                TrainSingleLineCompletionDialog(
                    dialogState = completedVariationDialogState(),
                    onRepeatClick = {},
                    onFinishClick = {},
                    onMarkDubiousAndNextTrainingClick = {},
                )
            }
        }

        composeRule.onAllNodesWithText("Doubt & Next").assertCountEquals(1)
    }

    private fun completedVariationDialogState(): TrainSingleLineCompletionState =
        TrainSingleLineCompletionState(
            title = "Variation completed",
            message = "You reached the end of the line.",
            finishLabel = "Finish variation",
            hasNextSide = false,
        )
}
