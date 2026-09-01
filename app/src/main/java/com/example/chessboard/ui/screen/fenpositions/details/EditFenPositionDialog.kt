package com.example.chessboard.ui.screen.fenpositions.details

/*
 * File role: renders and validates the dialog for editing FEN position metadata.
 * Allowed here:
 * - dialog-local name, theme, and description state
 * - required-theme validation and emitting one edit request
 * Not allowed here:
 * - FEN editing, service calls, persistence results, or app navigation
 * Validation date: 2026-09-01
 */

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.chessboard.ui.components.AppTextField
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.testtags.fenpositions.FenPositionEditConfirmTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionEditDescriptionInputTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionEditDialogTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionEditNameInputTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionEditThemeInputTestTag
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background

private const val DialogScreenHeightFraction = 0.82f

internal data class EditFenPositionRequest(
    val name: String,
    val theme: String,
    val description: String,
)

@Composable
internal fun EditFenPositionDialog(
    initialName: String,
    initialTheme: String,
    initialDescription: String,
    strings: FenPositionEditDialogStrings,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (EditFenPositionRequest) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var theme by remember(initialTheme) { mutableStateOf(initialTheme) }
    var description by remember(initialDescription) { mutableStateOf(initialDescription) }
    var hasAttemptedSave by remember { mutableStateOf(false) }
    val showThemeError = hasAttemptedSave && theme.isBlank()

    fun save() {
        hasAttemptedSave = true
        if (theme.isBlank()) {
            return
        }

        onSave(
            EditFenPositionRequest(
                name = name,
                theme = theme,
                description = description,
            ),
        )
    }

    val configuration = LocalConfiguration.current
    val dialogMaxHeight = (configuration.screenHeightDp * DialogScreenHeightFraction).dp

    Dialog(
        onDismissRequest = {
            if (!isSaving) {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = dialogMaxHeight)
                .testTag(FenPositionEditDialogTestTag),
            shape = RoundedCornerShape(AppDimens.radiusXl),
            color = Background.ScreenDark,
        ) {
            Column(
                modifier = Modifier.padding(AppDimens.spaceLg),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            ) {
                ScreenTitleText(text = strings.title)

                Column(
                    modifier = Modifier
                        .weight(weight = 1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                ) {
                    AppTextField(
                        value = name,
                        onValueChange = { value -> name = value },
                        label = strings.nameLabel,
                        placeholder = strings.namePlaceholder,
                        inputTestTag = FenPositionEditNameInputTestTag,
                    )
                    AppTextField(
                        value = theme,
                        onValueChange = { value -> theme = value },
                        label = strings.themeLabel,
                        placeholder = strings.themePlaceholder,
                        isError = showThemeError,
                        inputTestTag = FenPositionEditThemeInputTestTag,
                    )
                    AppTextField(
                        value = description,
                        onValueChange = { value -> description = value },
                        label = strings.descriptionLabel,
                        placeholder = strings.descriptionPlaceholder,
                        minLines = 3,
                        inputTestTag = FenPositionEditDescriptionInputTestTag,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSaving,
                    ) {
                        CardMetaText(text = strings.cancel)
                    }
                    PrimaryButton(
                        text = strings.save,
                        onClick = ::save,
                        enabled = !isSaving,
                        modifier = Modifier.testTag(FenPositionEditConfirmTestTag),
                    )
                }
            }
        }
    }
}
