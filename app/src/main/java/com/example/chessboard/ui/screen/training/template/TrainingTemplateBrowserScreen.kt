package com.example.chessboard.ui.screen.training.template

/**
 * Training template browser screen.
 *
 * Responsibilities:
 * - load and render existing templates
 * - delete templates
 * - forward the selected template to template editing
 */

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.chessboard.R
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppConfirmDialog
import com.example.chessboard.ui.components.AppLoadingDialog
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.HomeIconButton
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class TrainingTemplateBrowserState(
    val isLoading: Boolean = true,
    val templates: List<TrainingTemplateCardItem> = emptyList(),
    val templateToDelete: TrainingTemplateCardItem? = null,
    val infoDialog: TrainingTemplateInfoDialog? = null,
    val isBuildingTemplatePgn: Boolean = false,
)

@Composable
fun TrainingTemplateBrowserScreenContainer(
    screenContext: ScreenContainerContext,
    onOpenTemplate: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inDbProvider = screenContext.inDbProvider
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val unnamedTemplate = stringResource(R.string.training_template_unnamed)
    val pgnStrings = trainingTemplatePgnStrings()
    var state by remember { mutableStateOf(TrainingTemplateBrowserState()) }

    LaunchedEffect(unnamedTemplate) {
        val templates = withContext(Dispatchers.IO) {
            inDbProvider.createTrainingTemplateService().getAllTemplates().map { template ->
                template.toTrainingTemplateCardItem(unnamedTemplate)
            }
        }
        state = state.copy(
            isLoading = false,
            templates = templates,
        )
    }

    TrainingTemplateBrowserScreen(
        state = state,
        modifier = modifier,
        onBackClick = screenContext.onBackClick,
        onNavigate = screenContext.onNavigate,
        onOpenTemplate = onOpenTemplate,
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
                    val infoDialog = withContext(Dispatchers.IO) {
                        copyTemplatePgnToClipboard(
                            lineListService = inDbProvider.createLineListService(),
                            clipboard = clipboard,
                            lineIds = template.lineIds,
                            strings = pgnStrings,
                        )
                    }
                    state = state.copy(
                        infoDialog = infoDialog,
                    )
                } finally {
                    state = state.copy(isBuildingTemplatePgn = false)
                }
            }
        },
    )
}

@Composable
private fun TrainingTemplateBrowserScreen(
    state: TrainingTemplateBrowserState,
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onOpenTemplate: (Long) -> Unit = {},
    onDeleteTemplate: (Long) -> Unit = {},
    onTemplateToDeleteChange: (TrainingTemplateCardItem?) -> Unit = {},
    onCopyTemplatePgnClick: (TrainingTemplateCardItem) -> Unit = {},
    onInfoDialogDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (state.templateToDelete != null) {
        AppConfirmDialog(
            title = stringResource(R.string.training_template_delete_title),
            message = stringResource(
                R.string.training_template_delete_message,
                state.templateToDelete.name,
                state.templateToDelete.templateId,
            ),
            onDismiss = { onTemplateToDeleteChange(null) },
            onConfirm = {
                onDeleteTemplate(state.templateToDelete.templateId)
            },
            confirmText = stringResource(R.string.common_delete),
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
            title = stringResource(R.string.training_template_building_pgn_title),
            message = stringResource(R.string.training_template_building_pgn_message),
        )
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.training_template_title),
                onBackClick = onBackClick,
                handleSystemBack = true,
                filledBackButton = true,
                actions = {
                    HomeIconButton(onClick = { onNavigate(ScreenType.Home) })
                },
            )
        },
        bottomBar = {
            AppBottomNavigation(
                items = defaultAppBottomNavigationItems(),
                selectedItem = ScreenType.Training,
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
                                text = stringResource(R.string.training_template_empty),
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
