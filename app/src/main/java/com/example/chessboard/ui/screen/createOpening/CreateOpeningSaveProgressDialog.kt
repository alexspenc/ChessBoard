package com.example.chessboard.ui.screen.createOpening

/**
 * File role: renders create-opening save progress while a batch save is running.
 * Allowed here:
 * - small save-progress dialog UI for this screen
 * - UI-only labels for create-opening save progress
 * Not allowed here:
 * - save orchestration, database access, or cancellation policy
 */
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.ui.CreateOpeningSaveCancelTestTag
import com.example.chessboard.ui.CreateOpeningSaveProgressDialogTestTag
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background

@Composable
internal fun CreateOpeningSaveProgressDialog(
    progress: CreateOpeningSaveProgress,
    onCancel: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.testTag(CreateOpeningSaveProgressDialogTestTag),
        onDismissRequest = {},
        title = {
            ScreenTitleText(text = stringResource(R.string.create_opening_saving_lines_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                BodySecondaryText(text = stringResource(R.string.create_opening_total_lines, progress.totalLines))
                BodySecondaryText(
                    text = stringResource(
                        R.string.create_opening_processed_lines,
                        progress.processedLinesCount,
                        progress.totalLines,
                    )
                )
                BodySecondaryText(text = stringResource(R.string.create_opening_saved_lines, progress.savedLinesCount))
                BodySecondaryText(text = stringResource(R.string.create_opening_skipped_lines, progress.skippedLinesCount))
            }
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(R.string.create_opening_stop),
                onClick = onCancel,
                modifier = Modifier.testTag(CreateOpeningSaveCancelTestTag)
            )
        },
        containerColor = Background.ScreenDark,
    )
}
