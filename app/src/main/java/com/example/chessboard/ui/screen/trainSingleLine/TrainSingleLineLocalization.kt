package com.example.chessboard.ui.screen.trainSingleLine

/*
 * Localization holder for single-line training UI text that needs small formatting helpers.
 * Keep grouped resource reads and message formatting for train-single-line UI here.
 * Do not add training progression logic, persistence, navigation, or board behavior to this file.
 * Validation date: 2026-06-04
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.ui.BoardOrientation

internal data class TrainSingleLineCompletionStrings(
    val title: String,
    val completedMessage: String,
    private val sideCompletedMessageFormat: String,
    private val whiteSideLabel: String,
    private val blackSideLabel: String,
    val repeatAction: String,
    val nextAction: String,
    val doubtNextAction: String,
    val finishAction: String,
) {
    fun completionMessage(dialogState: TrainSingleLineCompletionState): String {
        if (!dialogState.hasNextSide) {
            return completedMessage
        }

        return sideCompletedMessageFormat.format(sideLabel(dialogState.completedOrientation))
    }

    private fun sideLabel(orientation: BoardOrientation): String {
        if (orientation == BoardOrientation.WHITE) {
            return whiteSideLabel
        }

        return blackSideLabel
    }
}

@Composable
internal fun trainSingleLineCompletionStrings(): TrainSingleLineCompletionStrings {
    return TrainSingleLineCompletionStrings(
        title = stringResource(R.string.train_single_line_variation_completed_title),
        completedMessage = stringResource(R.string.train_single_line_completed_message),
        sideCompletedMessageFormat = stringResource(R.string.train_single_line_side_completed_message),
        whiteSideLabel = stringResource(R.string.train_single_line_white_side_label),
        blackSideLabel = stringResource(R.string.train_single_line_black_side_label),
        repeatAction = stringResource(R.string.train_single_line_repeat_action),
        nextAction = stringResource(R.string.train_single_line_next_action),
        doubtNextAction = stringResource(R.string.train_single_line_doubt_next_action),
        finishAction = stringResource(R.string.train_single_line_finish_action),
    )
}
