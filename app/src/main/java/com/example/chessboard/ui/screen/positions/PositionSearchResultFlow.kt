package com.example.chessboard.ui.screen.positions

/**
 * Shared position-search result flow for screens that search games by board position.
 *
 * Keep reusable found-games dialogs and create-template helpers here. Do not add screen layout,
 * card rendering, or position-editor board editing logic to this file.
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
import com.example.chessboard.service.OneGameTrainingData
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

internal const val PositionTemplateDefaultName = "Template From Position"

internal data class PositionTemplateNameDialogState(
    val gameIds: List<Long>,
    val templateName: String = PositionTemplateDefaultName,
)

internal data class PositionSearchResultDialogActions(
    val onDismiss: () -> Unit,
    val onCreateTrainingClick: () -> Unit,
    val onCreateTemplateClick: () -> Unit,
    val templateNameDialogState: PositionTemplateNameDialogState? = null,
    val onTemplateNameChange: (String) -> Unit = {},
    val onTemplateNameDismiss: () -> Unit = {},
    val onConfirmTemplateName: () -> Unit = {},
)

@Composable
internal fun RenderPositionSearchResultDialog(
    foundGameIds: List<Long>?,
    actions: PositionSearchResultDialogActions,
) {
    RenderPositionTemplateNameDialog(
        dialogState = actions.templateNameDialogState,
        onTemplateNameChange = actions.onTemplateNameChange,
        onDismiss = actions.onTemplateNameDismiss,
        onConfirm = actions.onConfirmTemplateName,
    )

    if (foundGameIds == null) {
        return
    }

    if (foundGameIds.isEmpty()) {
        AppMessageDialog(
            title = resolveFoundGameIdsTitle(foundGameIds),
            message = resolveFoundGameIdsMessage(foundGameIds),
            onDismiss = actions.onDismiss,
        )
        return
    }

    AppMessageDialog(
        title = resolveFoundGameIdsTitle(foundGameIds),
        message = resolveFoundGameIdsMessage(foundGameIds),
        onDismiss = actions.onDismiss,
        actions = listOf(
            AppMessageDialogAction(
                text = "Close",
                onClick = actions.onDismiss,
            ),
            AppMessageDialogAction(
                text = "Template",
                onClick = actions.onCreateTemplateClick,
                testTag = PositionSearchResultTemplateActionTestTag,
            ),
            AppMessageDialogAction(
                text = "Training",
                onClick = actions.onCreateTrainingClick,
                testTag = PositionSearchResultTrainingActionTestTag,
            ),
        ),
    )

}

internal suspend fun createPositionTemplateFromGameIds(
    dbProvider: DatabaseProvider,
    gameIds: List<Long>,
    templateName: String = PositionTemplateDefaultName,
): Long? {
    val templateService = dbProvider.createTrainingTemplateService()
    val templateId = templateService.createTemplate(
        resolvePositionTemplateName(templateName)
    )
    if (templateId <= 0L) {
        return null
    }

    val allGamesAdded = gameIds.all { gameId ->
        templateService.addGame(
            templateId = templateId,
            gameId = gameId,
            weight = OneGameTrainingData(gameId = gameId, weight = 1).weight,
        )
    }

    return templateId.takeIf { allGamesAdded }
}

@Composable
private fun RenderPositionTemplateNameDialog(
    dialogState: PositionTemplateNameDialogState?,
    onTemplateNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val currentState = dialogState ?: return

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Background.ScreenDark,
        title = {
            ScreenTitleText(text = "New Template")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                BodySecondaryText(
                    text = "Name the template created from the matched games."
                )
                AppTextField(
                    value = currentState.templateName,
                    onValueChange = onTemplateNameChange,
                    label = "Template Name",
                    placeholder = PositionTemplateDefaultName,
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Template",
                onClick = onConfirm,
                modifier = Modifier.testTag(PositionTemplateNameConfirmTestTag),
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                CardMetaText(text = "Cancel")
            }
        },
    )
}

private fun resolveFoundGameIdsTitle(foundGameIds: List<Long>): String {
    if (foundGameIds.isEmpty()) {
        return "Games Not Found"
    }

    return "Games Found"
}

private fun resolveFoundGameIdsMessage(foundGameIds: List<Long>): String {
    if (foundGameIds.isEmpty()) {
        return "No saved games contain this position."
    }

    return "${foundGameIds.size} saved games match this position. Choose what to create from them."
}

private fun resolvePositionTemplateName(templateName: String): String {
    val normalizedName = templateName.trim()
    if (normalizedName.isBlank()) {
        return PositionTemplateDefaultName
    }

    return normalizedName
}
