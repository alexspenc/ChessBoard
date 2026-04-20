package com.example.chessboard.ui.screen.positions

/*
 * Screen-specific save-position dialog for PositionEditor.
 *
 * Keep in this file:
 * - UI-only dialog rendering for naming a saved position
 * - small dialog-local layout details tied to the PositionEditor save flow
 *
 * Do not add here:
 * - database calls or save orchestration
 * - navigation logic
 * - reusable generic dialog components that belong in ui/components
 */

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.chessboard.ui.PositionEditorSaveNameFieldTestTag
import com.example.chessboard.ui.components.AppTextField
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TrainingErrorRed

@Composable
internal fun RenderPositionEditorSaveDialog(
    saveDialogState: PositionEditorSaveDialogState?,
    actions: PositionEditorScreenActions.SaveDialog
) {
    val currentState = saveDialogState ?: return

    AlertDialog(
        onDismissRequest = actions.onDismiss,
        title = {
            ScreenTitleText(text = "Save Position")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                BodySecondaryText(
                    text = "Enter a name for this position."
                )
                AppTextField(
                    value = currentState.positionName,
                    onValueChange = actions.onPositionNameChange,
                    label = "Position Name",
                    placeholder = "e.g., Carlsbad Structure",
                    isError = currentState.errorMessage != null,
                    inputTestTag = PositionEditorSaveNameFieldTestTag
                )
                if (currentState.errorMessage != null) {
                    BodySecondaryText(
                        text = currentState.errorMessage,
                        color = TrainingErrorRed
                    )
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Save",
                onClick = actions.onConfirm
            )
        },
        dismissButton = {
            TextButton(onClick = actions.onDismiss) {
                CardMetaText(text = "Cancel")
            }
        },
        containerColor = Background.ScreenDark,
    )
}
