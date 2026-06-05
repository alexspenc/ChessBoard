package com.example.chessboard.ui.screen.positions

/**
 * Shared position-search result flow for screens that search lines by board position.
 *
 * Keep reusable found-lines dialogs and create-template helpers here. Do not add screen layout,
 * card rendering, or position-search board editing logic to this file.
 */
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.ui.PositionTemplateNameConfirmTestTag
import com.example.chessboard.service.OneLineTrainingData
import com.example.chessboard.ui.PositionSearchResultShowLinesActionTestTag
import com.example.chessboard.ui.PositionSearchResultMessageTestTag
import com.example.chessboard.ui.PositionSearchResultTemplateActionTestTag
import com.example.chessboard.ui.PositionSearchResultTrainingActionTestTag
import com.example.chessboard.ui.components.AppTextField
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppMessageDialogAction
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background

internal data class PositionTemplateNameDialogState(
    val lineIds: List<Long>,
    val templateName: String,
)

internal data class PositionSearchResultDialogActions(
    val onDismiss: () -> Unit,
    val onCreateTrainingClick: () -> Unit,
    val onCreateTemplateClick: () -> Unit,
    val onShowLinesClick: (() -> Unit)? = null,
    val showTemplateAction: Boolean = true,
    val templateNameDialogState: PositionTemplateNameDialogState? = null,
    val onTemplateNameChange: (String) -> Unit = {},
    val onTemplateNameDismiss: () -> Unit = {},
    val onConfirmTemplateName: () -> Unit = {},
)

@Composable
internal fun RenderPositionSearchResultDialog(
    foundLineIds: List<Long>?,
    actions: PositionSearchResultDialogActions,
) {
    val strings = positionSearchResultStrings()

    RenderPositionTemplateNameDialog(
        dialogState = actions.templateNameDialogState,
        onTemplateNameChange = actions.onTemplateNameChange,
        onDismiss = actions.onTemplateNameDismiss,
        onConfirm = actions.onConfirmTemplateName,
        strings = strings,
    )

    if (foundLineIds == null) {
        return
    }

    if (foundLineIds.isEmpty()) {
        AppMessageDialog(
            title = resolveFoundLineIdsTitle(foundLineIds, strings),
            message = resolveFoundLineIdsMessage(foundLineIds, strings),
            onDismiss = actions.onDismiss,
            messageModifier = Modifier.testTag(PositionSearchResultMessageTestTag),
        )
        return
    }

    val resultActions = buildList {
        actions.onShowLinesClick?.let { onShowLinesClick ->
            add(
                AppMessageDialogAction(
                    text = strings.showLinesAction,
                    onClick = onShowLinesClick,
                    testTag = PositionSearchResultShowLinesActionTestTag,
                )
            )
        }
        if (actions.showTemplateAction) {
            add(
                AppMessageDialogAction(
                    text = strings.templateAction,
                    onClick = actions.onCreateTemplateClick,
                    testTag = PositionSearchResultTemplateActionTestTag,
                )
            )
        }
        add(
            AppMessageDialogAction(
                text = strings.trainingAction,
                onClick = actions.onCreateTrainingClick,
                testTag = PositionSearchResultTrainingActionTestTag,
            )
        )
    }

    AppMessageDialog(
        title = resolveFoundLineIdsTitle(foundLineIds, strings),
        message = resolveFoundLineIdsMessage(foundLineIds, strings),
        onDismiss = actions.onDismiss,
        actions = resultActions,
        messageModifier = Modifier.testTag(PositionSearchResultMessageTestTag),
    )
}

internal suspend fun createPositionTemplateFromLineIds(
    dbProvider: DatabaseProvider,
    lineIds: List<Long>,
    templateName: String,
    defaultTemplateName: String,
): Long? {
    val templateService = dbProvider.createTrainingTemplateService()
    val templateId = templateService.createTemplate(
        resolvePositionTemplateName(templateName, defaultTemplateName)
    )
    if (templateId <= 0L) {
        return null
    }

    val allLinesAdded = lineIds.all { lineId ->
        templateService.addLine(
            templateId = templateId,
            lineId = lineId,
            weight = OneLineTrainingData(lineId = lineId, weight = 1).weight,
        )
    }

    return templateId.takeIf { allLinesAdded }
}

@Composable
private fun RenderPositionTemplateNameDialog(
    dialogState: PositionTemplateNameDialogState?,
    onTemplateNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    strings: PositionSearchResultStrings,
) {
    val currentState = dialogState ?: return

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Background.ScreenDark,
        title = {
            ScreenTitleText(text = strings.newTemplateTitle)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                BodySecondaryText(
                    text = strings.templateNamePrompt
                )
                AppTextField(
                    value = currentState.templateName,
                    onValueChange = onTemplateNameChange,
                    label = strings.templateNameLabel,
                    placeholder = strings.templateDefaultName,
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = strings.templateAction,
                onClick = onConfirm,
                modifier = Modifier.testTag(PositionTemplateNameConfirmTestTag),
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                CardMetaText(text = strings.cancelAction)
            }
        },
    )
}

private fun resolveFoundLineIdsTitle(
    foundLineIds: List<Long>,
    strings: PositionSearchResultStrings,
): String {
    if (foundLineIds.isEmpty()) {
        return strings.linesNotFoundTitle
    }

    return strings.linesFoundTitle
}

private fun resolveFoundLineIdsMessage(
    foundLineIds: List<Long>,
    strings: PositionSearchResultStrings,
): String {
    if (foundLineIds.isEmpty()) {
        return strings.noLinesFoundMessage
    }

    return strings.foundLinesMessage(foundLineIds.size)
}

private fun resolvePositionTemplateName(
    templateName: String,
    defaultTemplateName: String,
): String {
    val normalizedName = templateName.trim()
    if (normalizedName.isBlank()) {
        return defaultTemplateName
    }

    return normalizedName
}
