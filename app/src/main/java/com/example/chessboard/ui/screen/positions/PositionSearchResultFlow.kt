package com.example.chessboard.ui.screen.positions

/**
 * Shared position-search result flow for screens that search games by board position.
 *
 * Keep reusable found-games dialogs and create-template helpers here. Do not add screen layout,
 * card rendering, or position-editor board editing logic to this file.
 */
import androidx.compose.runtime.Composable
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.OneGameTrainingData
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppMessageDialogAction

private const val PositionTemplateDefaultName = "Template From Position"

internal data class PositionSearchResultDialogActions(
    val onDismiss: () -> Unit,
    val onCreateTrainingClick: () -> Unit,
    val onCreateTemplateClick: () -> Unit,
)

@Composable
internal fun RenderPositionSearchResultDialog(
    foundGameIds: List<Long>?,
    actions: PositionSearchResultDialogActions,
) {
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
                text = "Create Template",
                onClick = actions.onCreateTemplateClick,
            ),
            AppMessageDialogAction(
                text = "Create Training",
                onClick = actions.onCreateTrainingClick,
            ),
        ),
    )
}

internal suspend fun createPositionTemplateFromGameIds(
    dbProvider: DatabaseProvider,
    gameIds: List<Long>,
): Long? {
    val templateService = dbProvider.createTrainingTemplateService()
    val templateId = templateService.createTemplate(PositionTemplateDefaultName)
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

    return "Found games: ${foundGameIds.size}"
}
