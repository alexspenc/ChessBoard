package com.example.chessboard.ui.screen.training

/**
 * Training template selection screen.
 *
 * Responsibilities:
 * - load and render existing templates
 * - delete templates
 * - forward the selected template to training creation
 */

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.chessboard.entity.TrainingTemplateEntity
import com.example.chessboard.service.buildAnalysisPgnFromGames
import com.example.chessboard.service.OneGameTrainingData
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppConfirmDialog
import com.example.chessboard.ui.components.AppLoadingDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.DeleteIconButton
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class TrainingTemplateListItem(
    val templateId: Long,
    val name: String,
    val gamesCount: Int,
    val gameIds: List<Long>,
)

private data class TrainingTemplateSelectionInfoDialog(
    val title: String,
    val message: String,
)

private data class TrainingTemplateSelectionState(
    val isLoading: Boolean = true,
    val templates: List<TrainingTemplateListItem> = emptyList(),
    val templateToDelete: TrainingTemplateListItem? = null,
    val infoDialog: TrainingTemplateSelectionInfoDialog? = null,
    val isBuildingTemplatePgn: Boolean = false,
)

@Composable
fun TrainingTemplateSelectionScreenContainer(
    screenContext: ScreenContainerContext,
    onSelectTemplate: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inDbProvider = screenContext.inDbProvider
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    var state by remember { mutableStateOf(TrainingTemplateSelectionState()) }

    LaunchedEffect(Unit) {
        val templates = withContext(Dispatchers.IO) {
            inDbProvider.createTrainingTemplateService().getAllTemplates().map { template ->
                template.toTrainingTemplateListItem()
            }
        }
        state = state.copy(
            isLoading = false,
            templates = templates,
        )
    }

    TrainingTemplateSelectionScreen(
        state = state,
        modifier = modifier,
        onBackClick = screenContext.onBackClick,
        onNavigate = screenContext.onNavigate,
        onOpenTemplate = onSelectTemplate,
        onTemplateToDeleteChange = { template ->
            state = state.copy(templateToDelete = template)
        },
        onInfoDialogDismiss = {
            state = state.copy(infoDialog = null)
        },
        onDeleteTemplate = { templateId ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    inDbProvider.createTrainingTemplateService().deleteTemplate(templateId)
                }
                state = state.copy(
                    templates = state.templates.filterNot { it.templateId == templateId },
                    templateToDelete = null,
                )
            }
        },
        onCopyTemplatePgnClick = { template ->
            scope.launch {
                state = state.copy(isBuildingTemplatePgn = true)
                try {
                    val games = withContext(Dispatchers.IO) {
                        inDbProvider.createGameListService().getGamesByIds(template.gameIds)
                    }
                    val templatePgn = withContext(Dispatchers.Default) {
                        buildAnalysisPgnFromGames(games)
                    }

                    if (templatePgn.isBlank()) {
                        state = state.copy(
                            infoDialog = TrainingTemplateSelectionInfoDialog(
                                title = "PGN unavailable",
                                message = "Template games could not be exported to PGN.",
                            )
                        )
                        return@launch
                    }

                    clipboard.setClipEntry(
                        ClipEntry(
                            ClipData.newPlainText(
                                "Template PGN",
                                templatePgn,
                            )
                        )
                    )
                    state = state.copy(
                        infoDialog = TrainingTemplateSelectionInfoDialog(
                            title = "PGN copied",
                            message = "Template PGN was copied to the clipboard.",
                        )
                    )
                } finally {
                    state = state.copy(isBuildingTemplatePgn = false)
                }
            }
        },
    )
}

@Composable
private fun TrainingTemplateSelectionScreen(
    state: TrainingTemplateSelectionState,
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onOpenTemplate: (Long) -> Unit = {},
    onDeleteTemplate: (Long) -> Unit = {},
    onTemplateToDeleteChange: (TrainingTemplateListItem?) -> Unit = {},
    onCopyTemplatePgnClick: (TrainingTemplateListItem) -> Unit = {},
    onInfoDialogDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (state.templateToDelete != null) {
        AppConfirmDialog(
            title = "Delete Template",
            message = resolveDeleteTemplateMessage(state.templateToDelete),
            onDismiss = { onTemplateToDeleteChange(null) },
            onConfirm = {
                onDeleteTemplate(state.templateToDelete.templateId)
            },
            confirmText = "Delete",
            isDestructive = true,
        )
    }

    if (state.infoDialog != null) {
        AppMessageDialog(
            title = state.infoDialog.title,
            message = state.infoDialog.message,
            onDismiss = onInfoDialogDismiss,
        )
    }

    if (state.isBuildingTemplatePgn) {
        AppLoadingDialog(
            title = "Building PGN",
            message = "Preparing template PGN...",
        )
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Training Templates",
                onBackClick = onBackClick,
                filledBackButton = true,
            )
        },
        bottomBar = {
            AppBottomNavigation(
                items = defaultAppBottomNavigationItems(),
                selectedItem = ScreenType.Home,
                onItemSelected = onNavigate,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                horizontal = AppDimens.spaceLg,
                vertical = AppDimens.spaceLg,
            ),
        ) {
            when {
                state.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = TrainingAccentTeal)
                        }
                    }
                }

                state.templates.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            BodySecondaryText(
                                text = "No templates available.",
                                color = TextColor.Secondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                else -> {
                    items(state.templates, key = { it.templateId }) { template ->
                        TrainingTemplateCard(
                            template = template,
                            onClick = { onOpenTemplate(template.templateId) },
                            onCopyPgnClick = { onCopyTemplatePgnClick(template) },
                            onDeleteClick = { onTemplateToDeleteChange(template) },
                        )
                        Spacer(modifier = Modifier.height(AppDimens.spaceMd))
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainingTemplateCard(
    template: TrainingTemplateListItem,
    onClick: () -> Unit,
    onCopyPgnClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CardSurface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                ScreenTitleText(text = template.name)
                Spacer(modifier = Modifier.height(AppDimens.spaceXs))
                CardMetaText(text = "Template ID: ${template.templateId}")
                CardMetaText(text = "Games: ${template.gamesCount}")
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onCopyPgnClick,
                ) {
                    IconMd(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy template PGN",
                    )
                }
                DeleteIconButton(onClick = onDeleteClick, contentDescription = "Delete template")
            }
        }
    }
}

private fun TrainingTemplateEntity.toTrainingTemplateListItem(): TrainingTemplateListItem {
    return TrainingTemplateListItem(
        templateId = id,
        name = name.ifBlank { "Unnamed Template" },
        gamesCount = OneGameTrainingData.fromJson(gamesJson).size,
        gameIds = OneGameTrainingData.fromJson(gamesJson).map { game -> game.gameId },
    )
}

private fun resolveDeleteTemplateMessage(template: TrainingTemplateListItem): String {
    return "Delete \"${template.name}\"?\nTemplate ID: ${template.templateId}"
}
