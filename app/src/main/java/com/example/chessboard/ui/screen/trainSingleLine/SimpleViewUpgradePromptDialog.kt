package com.example.chessboard.ui.screen.trainSingleLine

/**
 * File role: renders the simple-to-full-view suggestion dialog for completed trainings.
 * Allowed here:
 * - localized dialog UI for the full-view suggestion
 * - action wiring owned by callers
 * Not allowed here:
 * - prompt eligibility decisions, navigation state, or database access
 * Validation date: 2026-06-06
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.ui.SimpleViewUpgradePromptCancelTestTag
import com.example.chessboard.ui.SimpleViewUpgradePromptOpenSettingsTestTag
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppMessageDialogAction

@Composable
fun SimpleViewUpgradePromptDialog(
    trainingsCount: Int,
    onOpenSettingsClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppMessageDialog(
        title = stringResource(R.string.train_single_line_full_view_prompt_title),
        message = stringResource(
            R.string.train_single_line_full_view_prompt_message,
            trainingsCount,
        ),
        onDismiss = onDismiss,
        actions = listOf(
            AppMessageDialogAction(
                text = stringResource(R.string.train_single_line_full_view_prompt_action),
                onClick = onOpenSettingsClick,
                testTag = SimpleViewUpgradePromptOpenSettingsTestTag,
            ),
            AppMessageDialogAction(
                text = stringResource(R.string.common_cancel),
                onClick = onDismiss,
                testTag = SimpleViewUpgradePromptCancelTestTag,
            ),
        ),
    )
}
