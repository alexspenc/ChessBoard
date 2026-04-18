package com.example.chessboard.ui.screen.trainSingleGame

/*
 * UI tests for the single-game training completion dialog.
 *
 * Keep only dialog-level regression coverage here. Do not add broader screen
 * navigation or full training-session flow tests to this file.
 */

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Rule
import org.junit.Test

class TrainSingleGameCompletionDialogTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun completionDialog_hidesNextTrainingButton_whenCallbackIsMissing() {
        composeRule.setContent {
            ChessBoardTheme {
                TrainSingleGameCompletionDialog(
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
                TrainSingleGameCompletionDialog(
                    dialogState = completedVariationDialogState(),
                    onRepeatClick = {},
                    onFinishClick = {},
                    onNextTrainingClick = {},
                )
            }
        }

        composeRule.onAllNodesWithText("Next").assertCountEquals(1)
    }

    private fun completedVariationDialogState(): TrainSingleGameCompletionState {
        return TrainSingleGameCompletionState(
            title = "Variation completed",
            message = "You reached the end of the game.",
            finishLabel = "Finish variation",
            hasNextSide = false,
        )
    }
}
