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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.chessboard.ui.PositionEditorSaveNameFieldTestTag
import com.example.chessboard.ui.components.AppTextField
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background

@Composable
internal fun RenderPositionEditorSaveDialog(
    saveDialogState: PositionEditorSaveDialogState?,
    onDismiss: () -> Unit,
    onPositionNameChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    val currentState = saveDialogState ?: return

    AlertDialog(
        onDismissRequest = onDismiss,
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
                    onValueChange = onPositionNameChange,
                    label = "Position Name",
                    placeholder = "e.g., Carlsbad Structure",
                    modifier = Modifier.testTag(PositionEditorSaveNameFieldTestTag)
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Save",
                onClick = onConfirm
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                CardMetaText(text = "Cancel")
            }
        },
        containerColor = Background.ScreenDark,
    )
}
