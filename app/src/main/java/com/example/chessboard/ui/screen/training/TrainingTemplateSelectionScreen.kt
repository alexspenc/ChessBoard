package com.example.chessboard.ui.screen.training

/**
 * Training template selection screen.
 *
 * Responsibilities:
 * - load and render existing templates
 * - delete templates
 * - forward the selected template to training creation
 */

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.chessboard.entity.TrainingTemplateEntity
import com.example.chessboard.service.OneGameTrainingData
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppConfirmDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.DeleteIconButton
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
)

private data class TrainingTemplateSelectionState(
    val isLoading: Boolean = true,
    val templates: List<TrainingTemplateListItem> = emptyList(),
    val templateToDelete: TrainingTemplateListItem? = null,
)

@Composable
fun TrainingTemplateSelectionScreenContainer(
    screenContext: ScreenContainerContext,
    onSelectTemplate: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inDbProvider = screenContext.inDbProvider
    val scope = rememberCoroutineScope()
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
            DeleteIconButton(onClick = onDeleteClick, contentDescription = "Delete template")
        }
    }
}

private fun TrainingTemplateEntity.toTrainingTemplateListItem(): TrainingTemplateListItem {
    return TrainingTemplateListItem(
        templateId = id,
        name = name.ifBlank { "Unnamed Template" },
        gamesCount = OneGameTrainingData.fromJson(gamesJson).size,
    )
}

private fun resolveDeleteTemplateMessage(template: TrainingTemplateListItem): String {
    return "Delete \"${template.name}\"?\nTemplate ID: ${template.templateId}"
}
