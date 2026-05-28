package com.example.chessboard.ui.screen.home

/**
 * File role: provides small dialogs shared by Home-screen navigation guards.
 * Allowed here:
 * - generic Home pre-navigation progress dialogs
 * - shared missing-lines explanation dialogs
 * Not allowed here:
 * - Home layout branches
 * - database checks or navigation decision logic
 * - SmartTraining-specific or regular-training-specific state machines
 * Validation date: 2026-05-14
 */
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.ui.HomeNoLinesCreateOpeningTestTag
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppMessageDialogAction
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TrainingAccentTeal

@Composable
internal fun HomeNavigationPreparationDialog(
    title: String,
    message: String,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = Background.ScreenDark,
        title = {
            SectionTitleText(text = title)
        },
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            ) {
                CircularProgressIndicator(color = TrainingAccentTeal)
                BodySecondaryText(
                    text = message,
                    modifier = Modifier.padding(top = AppDimens.spaceXs),
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancel) {
                CardMetaText(text = stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
internal fun HomeNoLinesDialog(
    message: String,
    onCreateOpeningClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppMessageDialog(
        title = stringResource(R.string.home_no_openings_title),
        message = message,
        onDismiss = onDismiss,
        actions = listOf(
            AppMessageDialogAction(
                text = stringResource(R.string.home_create_opening_title),
                onClick = onCreateOpeningClick,
                testTag = HomeNoLinesCreateOpeningTestTag,
            ),
            AppMessageDialogAction(
                text = stringResource(R.string.common_cancel),
                onClick = onDismiss,
            ),
        ),
    )
}
