package com.example.chessboard.ui.screen.training

/*
 * Shared unsaved-changes helpers for training-like editor screens.
 *
 * Keep name normalization, dirty-state checks, and the confirmation dialog here
 * so editor screens can reuse the same leave-with-unsaved-changes behavior.
 * Do not add screen loading, save flows, or unrelated UI to this file.
 */

import androidx.compose.runtime.Composable
import com.example.chessboard.ui.components.AppMessageDialog

internal fun normalizeTrainingEditorName(
    trainingName: String,
    defaultName: String = DEFAULT_TRAINING_NAME,
): String {
    if (trainingName.isBlank()) {
        return defaultName
    }

    return trainingName
}

internal fun hasUnsavedTrainingEditorChanges(
    editorState: CreateTrainingEditorState,
    initialTrainingName: String,
    initialGamesForTraining: List<TrainingGameEditorItem>,
    defaultName: String = DEFAULT_TRAINING_NAME,
): Boolean {
    val newName = normalizeTrainingEditorName(editorState.trainingName, defaultName)
    val oldName = normalizeTrainingEditorName(initialTrainingName, defaultName)
    if (newName != oldName) { return true }

    return editorState.editableGamesForTraining != initialGamesForTraining
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
        title = "Unsaved Changes",
        message = "Save training changes before leaving this screen?",
        onDismiss = onDismiss,
        confirmText = "Save",
        onConfirm = onSaveClick,
        dismissText = "Discard",
        onDismissClick = onDiscardClick,
    )
}
