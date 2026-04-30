package com.example.chessboard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.ButtonColor
import com.example.chessboard.ui.theme.TextColor

data class AppMessageDialogAction(
    val text: String,
    val onClick: () -> Unit,
    val testTag: String? = null,
)

/** Displays a standard app dialog for informational messages and simple actions. */
@Composable
fun AppMessageDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "OK",
    onConfirm: (() -> Unit)? = null,
    dismissText: String? = null,
    onDismissClick: (() -> Unit)? = null,
    actions: List<AppMessageDialogAction>? = null
) {
    @Composable
    fun RenderMessageDialogConfirmButton() {

        @Composable
        fun RenderMessageDialogActionButtons(
            actions: List<AppMessageDialogAction>
        ) {
            fun resolveMessageDialogActionModifier(
                testTag: String?
            ): Modifier {
                if (testTag == null) {
                    return Modifier
                }

                return Modifier.testTag(testTag)
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                actions.forEach { action ->
                    TextButton(
                        onClick = action.onClick,
                        modifier = resolveMessageDialogActionModifier(action.testTag),
                    ) {
                        BodySecondaryText(
                            text = action.text,
                            color = TextColor.Primary
                        )
                    }
                }
            }
        }

        if (!actions.isNullOrEmpty()) {
            RenderMessageDialogActionButtons(actions)
            return
        }

        TextButton(
            onClick = resolveMessageDialogConfirmAction(
                onConfirm = onConfirm,
                onDismiss = onDismiss
            )
        ) {
            BodySecondaryText(
                text = confirmText,
                color = TextColor.Primary
            )
        }
    }

    @Composable
    fun RenderMessageDialogDismissArea() {
        if (!actions.isNullOrEmpty()) {
            return
        }

        RenderMessageDialogDismissButton(
            dismissText = dismissText,
            onDismiss = onDismiss,
            onDismissClick = onDismissClick
        )
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        containerColor = Background.ScreenDark,
        title = {
            SectionTitleText(text = title)
        },
        text = {
            BodySecondaryText(text = message)
        },
        confirmButton = {
            RenderMessageDialogConfirmButton()
        },
        dismissButton = {
            RenderMessageDialogDismissArea()
        }
    )
}

/** Displays a standard app confirmation dialog for irreversible or important actions. */
@Composable
fun AppConfirmDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    isDestructive: Boolean = false
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        containerColor = Background.ScreenDark,
        title = {
            ScreenTitleText(text = title)
        },
        text = {
            BodySecondaryText(text = message)
        },
        confirmButton = {
            PrimaryButton(
                text = confirmText,
                onClick = onConfirm,
                containerColor = resolveConfirmDialogContainerColor(
                    isDestructive = isDestructive
                )
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                CardMetaText(text = dismissText)
            }
        }
    )
}

@Composable
private fun RenderMessageDialogDismissButton(
    dismissText: String?,
    onDismiss: () -> Unit,
    onDismissClick: (() -> Unit)?
) {
    if (dismissText == null) {
        return
    }

    TextButton(
        onClick = resolveMessageDialogDismissAction(
            onDismiss = onDismiss,
            onDismissClick = onDismissClick
        )
    ) {
        BodySecondaryText(
            text = dismissText,
            color = TextColor.Primary
        )
    }
}

private fun resolveMessageDialogConfirmAction(
    onConfirm: (() -> Unit)?,
    onDismiss: () -> Unit
): () -> Unit {
    if (onConfirm != null) {
        return onConfirm
    }

    return onDismiss
}

private fun resolveMessageDialogDismissAction(
    onDismiss: () -> Unit,
    onDismissClick: (() -> Unit)?
): () -> Unit {
    if (onDismissClick != null) {
        return onDismissClick
    }

    return onDismiss
}

private fun resolveConfirmDialogContainerColor(
    isDestructive: Boolean
): androidx.compose.ui.graphics.Color {
    if (isDestructive) {
        return ButtonColor.DestructiveContainer
    }

    return ButtonColor.PrimaryContainer
}
