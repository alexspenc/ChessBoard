package com.example.chessboard.ui.screen.fenpositions.continuations

/*
 * File role: renders dialogs owned by the add-FEN-position-continuations screen.
 * Allowed here:
 * - blocking progress, processing-result, error, and discard-confirmation presentation
 * Not allowed here:
 * - parsing, persistence, navigation decisions, or mutation of screen state
 * Validation date: 2026-09-02
 */

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.example.chessboard.R
import com.example.chessboard.ui.components.AppConfirmDialog
import com.example.chessboard.ui.components.AppLoadingDialog
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddDiscardDialogTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddDiscardExitTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddErrorDialogTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddProcessingDialogTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddResultConfirmTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddResultDialogTestTag
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TextColor

internal enum class AddFenPositionContinuationsProcessingStage {
    LoadingPosition,
    ParsingContinuations,
    CheckingLines,
    CheckingStoredContinuations,
    SavingContinuations,
}

internal data class AddFenPositionContinuationsProcessingResult(
    val recognizedLinesCount: Int,
    val exactDuplicateLinesCount: Int,
    val coveredPrefixLinesCount: Int,
    val coveredByStoredLinesCount: Int,
    val newLinesCount: Int,
)

internal sealed interface AddFenPositionContinuationsDialogState {
    data class Processing(
        val stage: AddFenPositionContinuationsProcessingStage,
    ) : AddFenPositionContinuationsDialogState

    data class Result(
        val result: AddFenPositionContinuationsProcessingResult,
    ) : AddFenPositionContinuationsDialogState

    data class Error(
        val title: String,
        val message: String,
    ) : AddFenPositionContinuationsDialogState

    data object ConfirmDiscard : AddFenPositionContinuationsDialogState
}

internal data class AddFenPositionContinuationsDialogActions(
    val onResultDismiss: () -> Unit,
    val onErrorDismiss: () -> Unit,
    val onDiscardStay: () -> Unit,
    val onDiscardExit: () -> Unit,
)

@Composable
internal fun AddFenPositionContinuationsDialogs(
    dialogState: AddFenPositionContinuationsDialogState?,
    strings: AddFenPositionContinuationsStrings,
    actions: AddFenPositionContinuationsDialogActions,
) {
    when (dialogState) {
        null -> Unit

        is AddFenPositionContinuationsDialogState.Processing -> AppLoadingDialog(
            title = strings.processingTitle(dialogState.stage),
            message = strings.processingMessage(dialogState.stage),
            modifier = Modifier.testTag(FenPositionContinuationAddProcessingDialogTestTag),
        )

        is AddFenPositionContinuationsDialogState.Result -> ProcessingResultDialog(
            message = strings.resultMessage(dialogState.result),
            title = strings.resultTitle,
            onDismiss = actions.onResultDismiss,
        )

        is AddFenPositionContinuationsDialogState.Error -> AppMessageDialog(
            title = dialogState.title,
            message = dialogState.message,
            onDismiss = actions.onErrorDismiss,
            modifier = Modifier.testTag(FenPositionContinuationAddErrorDialogTestTag),
        )

        AddFenPositionContinuationsDialogState.ConfirmDiscard -> AppConfirmDialog(
            title = strings.discardTitle,
            message = strings.discardMessage,
            onDismiss = actions.onDiscardStay,
            onConfirm = actions.onDiscardExit,
            confirmText = strings.exit,
            confirmButtonModifier = Modifier.testTag(FenPositionContinuationAddDiscardExitTestTag),
            dismissText = strings.stay,
            modifier = Modifier.testTag(FenPositionContinuationAddDiscardDialogTestTag),
            isDestructive = true,
        )
    }
}

@Composable
private fun ProcessingResultDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.testTag(FenPositionContinuationAddResultDialogTestTag),
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
        containerColor = Background.ScreenDark,
        title = {
            SectionTitleText(text = title)
        },
        text = {
            BodySecondaryText(text = message)
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(FenPositionContinuationAddResultConfirmTestTag),
            ) {
                BodySecondaryText(
                    text = stringResource(R.string.common_ok),
                    color = TextColor.Primary,
                )
            }
        },
    )
}
