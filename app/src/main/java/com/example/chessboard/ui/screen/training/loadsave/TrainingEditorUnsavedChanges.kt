package com.example.chessboard.ui.screen.training.loadsave

/*
 * Shared unsaved-changes helpers for training-like editor screens.
 *
 * Keep name normalization, dirty-state checks, and the confirmation dialog here
 * so editor screens can reuse the same leave-with-unsaved-changes behavior.
 * Do not add screen loading, save flows, or unrelated UI to this file.
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.screen.training.common.CreateTrainingEditorState
import com.example.chessboard.ui.screen.training.common.TrainingLineEditorItem

internal fun normalizeTrainingEditorName(
    trainingName: String,
    defaultName: String,
): String {
    if (trainingName.isBlank()) {
        return defaultName
    }

    return trainingName
}

internal fun hasUnsavedTrainingEditorChanges(
    editorState: CreateTrainingEditorState,
    initialTrainingName: String,
    initialLinesForTraining: List<TrainingLineEditorItem>,
    defaultName: String,
): Boolean {
    val newName = normalizeTrainingEditorName(editorState.trainingName, defaultName)
    val oldName = normalizeTrainingEditorName(initialTrainingName, defaultName)
    if (newName != oldName) { return true }

    return editorState.editableLinesForTraining != initialLinesForTraining
}

@Composable
internal fun RenderUnsavedTrainingChangesDialog(
    pendingLeaveAction: (() -> Unit)?,
    onDismiss: () -> Unit,
    onSaveClick: () -> Unit,
    onDiscardClick: () -> Unit,
) {
    if (pendingLeaveAction == null) { return }

    AppMessageDialog(
        title = stringResource(R.string.training_unsaved_changes_title),
        message = stringResource(R.string.training_unsaved_changes_message),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.common_save),
        onConfirm = onSaveClick,
        dismissText = stringResource(R.string.common_discard),
        onDismissClick = onDiscardClick,
    )
}
