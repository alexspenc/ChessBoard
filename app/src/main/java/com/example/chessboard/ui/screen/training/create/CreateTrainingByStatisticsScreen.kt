package com.example.chessboard.ui.screen.training.create

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.runtimecontext.StatisticsTrainingRuntimeContext
import com.example.chessboard.service.OneLineTrainingData
import com.example.chessboard.service.StatisticsTrainingRecommendationSettings
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppMessageDialogAction
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.HomeIconButton
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.RepeatStepIconButton
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.screen.training.MAX_STATISTICS_TRAINING_LINES
import com.example.chessboard.ui.screen.training.MAX_STATISTICS_TRAINING_WEIGHT
import com.example.chessboard.ui.screen.training.common.CreateTrainingEditorState
import com.example.chessboard.ui.screen.training.common.TrainingLineEditorItem
import com.example.chessboard.ui.screen.training.common.toTrainingLineEditorItem
import com.example.chessboard.ui.screen.training.loadsave.hasUnsavedTrainingEditorChanges
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


private data class StatisticsTrainingSaveSuccess(
    val trainingId: Long,
    val trainingName: String,
    val linesCount: Int,
)

private data class StatisticsTrainingMessage(
    @StringRes val titleRes: Int,
    @StringRes val messageRes: Int,
)

internal enum class StatisticsTrainingSaveAction {
    ShowEmptyTrainingMessage,
    SaveTraining,
}

internal fun resolveStatisticsTrainingSaveAction(
    hasLines: Boolean,
): StatisticsTrainingSaveAction {
    if (!hasLines) {
        return StatisticsTrainingSaveAction.ShowEmptyTrainingMessage
    }

    return StatisticsTrainingSaveAction.SaveTraining
}

@Composable
private fun StatisticsSettingStepper(
    label: String,
    value: Int,
    onDecreaseClick: () -> Unit,
    onIncreaseClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SectionTitleText(text = label)
            BodySecondaryText(text = value.toString())
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RepeatStepIconButton(
                icon = Icons.Default.Remove,
                contentDescription = stringResource(R.string.common_decrease_value_content_description, label),
                onStep = onDecreaseClick,
            )
            RepeatStepIconButton(
                icon = Icons.Default.Add,
                contentDescription = stringResource(R.string.common_increase_value_content_description, label),
                onStep = onIncreaseClick,
            )
        }
    }
}

@Composable
private fun StatisticsTrainingSettingsSection(
    maxLines: Int,
    minDaysSinceLastTraining: Int,
    maxWeight: Int,
    onChangeLimitsClick: () -> Unit,
) {
    ScreenSection {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
            ) {
                SectionTitleText(text = stringResource(R.string.statistics_training_limits_title))
                BodySecondaryText(
                    text = stringResource(
                        R.string.statistics_training_limits_summary,
                        maxLines,
                        minDaysSinceLastTraining,
                        maxWeight,
                    )
                )
            }
            IconButton(onClick = onChangeLimitsClick) {
                IconMd(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.statistics_training_change_limits_content_description),
                    tint = TrainingAccentTeal,
                )
            }
        }
    }
}

@Composable
private fun StatisticsTrainingLimitsDialog(
    settings: StatisticsTrainingRecommendationSettings,
    onConfirm: (StatisticsTrainingRecommendationSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    var draftSettings by remember(settings) { mutableStateOf(settings) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Background.ScreenDark,
        title = {
            SectionTitleText(text = stringResource(R.string.statistics_training_limits_dialog_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            ) {
                StatisticsSettingStepper(
                    label = stringResource(R.string.statistics_training_max_lines_label),
                    value = draftSettings.limit,
                    onDecreaseClick = {
                        draftSettings = draftSettings.copy(
                            limit = (draftSettings.limit - 1).coerceAtLeast(1)
                        )
                    },
                    onIncreaseClick = {
                        draftSettings = draftSettings.copy(
                            limit = (draftSettings.limit + 1)
                                .coerceAtMost(MAX_STATISTICS_TRAINING_LINES)
                        )
                    },
                )
                StatisticsSettingStepper(
                    label = stringResource(R.string.statistics_training_min_days_since_last_training_label),
                    value = draftSettings.minDaysSinceLastTraining,
                    onDecreaseClick = {
                        draftSettings = draftSettings.copy(
                            minDaysSinceLastTraining = (draftSettings.minDaysSinceLastTraining - 1)
                                .coerceAtLeast(0)
                        )
                    },
                    onIncreaseClick = {
                        draftSettings = draftSettings.copy(
                            minDaysSinceLastTraining = draftSettings.minDaysSinceLastTraining + 1
                        )
                    },
                )
                StatisticsSettingStepper(
                    label = stringResource(R.string.statistics_training_max_weight_label),
                    value = draftSettings.maxWeight,
                    onDecreaseClick = {
                        draftSettings = draftSettings.copy(
                            maxWeight = (draftSettings.maxWeight - 1).coerceAtLeast(1)
                        )
                    },
                    onIncreaseClick = {
                        draftSettings = draftSettings.copy(
                            maxWeight = (draftSettings.maxWeight + 1)
                                .coerceAtMost(MAX_STATISTICS_TRAINING_WEIGHT)
                        )
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(draftSettings) }) {
                BodySecondaryText(
                    text = stringResource(R.string.common_ok),
                    color = TextColor.Primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                BodySecondaryText(
                    text = stringResource(R.string.common_cancel),
                    color = TextColor.Primary,
                )
            }
        },
    )
}

@Composable
fun CreateTrainingByStatisticsScreenContainer(
    screenContext: ScreenContainerContext,
    statisticsTrainingRuntimeContext: StatisticsTrainingRuntimeContext,
    onOpenFormulaSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isLoading by remember { mutableStateOf(!statisticsTrainingRuntimeContext.hasLoadedSelection) }
    var trainingSaveSuccess by remember { mutableStateOf<StatisticsTrainingSaveSuccess?>(null) }
    var messageDialog by remember { mutableStateOf<StatisticsTrainingMessage?>(null) }
    var pendingLeaveAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var afterSaveAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showLimitsDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val recommendationSettings = statisticsTrainingRuntimeContext.recommendationSettings
    val isSelectionOutOfDate = statisticsTrainingRuntimeContext.isSelectionOutOfDate()
    val unnamedOpeningName = stringResource(R.string.training_line_unnamed_opening)
    val defaultStatisticsTrainingName = stringResource(R.string.statistics_training_default_name)

    fun resolveTrainingNameForRefreshedSelection(currentEditorState: CreateTrainingEditorState): String {
        if (statisticsTrainingRuntimeContext.hasLoadedSelection) {
            return currentEditorState.trainingName
        }

        return defaultStatisticsTrainingName
    }

    suspend fun refreshSelection(
        settings: StatisticsTrainingRecommendationSettings = statisticsTrainingRuntimeContext.recommendationSettings,
    ): Boolean {
        val recommendations = withContext(Dispatchers.IO) {
            screenContext.inDbProvider
                .createStatisticsTrainingService()
                .getRecommendation(recommendationSettings = settings)
        }

        val linesForTraining = recommendations.map { recommendation ->
            recommendation.line.toTrainingLineEditorItem(
                weight = recommendation.weight,
                unnamedOpeningName = unnamedOpeningName,
            )
        }
        val currentEditorState = statisticsTrainingRuntimeContext.editorState
        val trainingName = resolveTrainingNameForRefreshedSelection(currentEditorState)
        statisticsTrainingRuntimeContext.rememberLoadedSelection(
            newEditorState = currentEditorState.copy(
                trainingName = trainingName,
                currentPage = 0,
                editableLinesForTraining = linesForTraining,
            ),
            settings = settings,
        )
        return linesForTraining.isNotEmpty()
    }

    fun saveTraining(
        trainingName: String,
        editableLines: List<TrainingLineEditorItem>,
        onSaved: (() -> Unit)? = null,
    ) {
        scope.launch {
            val normalizedName = trainingName.ifBlank { defaultStatisticsTrainingName }
            val trainingLines = editableLines.map { line ->
                OneLineTrainingData(
                    lineId = line.lineId,
                    weight = line.weight,
                )
            }

            val savedTrainingId = withContext(Dispatchers.IO) {
                val trainingService = screenContext.inDbProvider.createTrainingService()
                trainingService.createTrainingFromLines(
                    name = normalizedName,
                    lines = trainingLines,
                )
            }

            trainingSaveSuccess = StatisticsTrainingSaveSuccess(
                trainingId = savedTrainingId ?: return@launch,
                trainingName = normalizedName,
                linesCount = editableLines.size,
            )
            afterSaveAction = onSaved
        }
    }

    fun requestSave(onSaved: (() -> Unit)? = null) {
        val editorState = statisticsTrainingRuntimeContext.editorState
        when (
            resolveStatisticsTrainingSaveAction(
                hasLines = editorState.editableLinesForTraining.isNotEmpty(),
            )
        ) {
            StatisticsTrainingSaveAction.ShowEmptyTrainingMessage -> {
                messageDialog = StatisticsTrainingMessage(
                    titleRes = R.string.statistics_training_not_saved_title,
                    messageRes = R.string.statistics_training_empty_message,
                )
                return
            }
            StatisticsTrainingSaveAction.SaveTraining -> Unit
        }

        saveTraining(
            trainingName = editorState.trainingName,
            editableLines = editorState.editableLinesForTraining,
            onSaved = onSaved,
        )
    }

    fun hasUnsavedChanges(): Boolean {
        if (isSelectionOutOfDate) {
            return true
        }

        return hasUnsavedTrainingEditorChanges(
            editorState = statisticsTrainingRuntimeContext.editorState,
            initialTrainingName = statisticsTrainingRuntimeContext.loadedEditorState.trainingName,
            initialLinesForTraining = statisticsTrainingRuntimeContext.loadedEditorState.editableLinesForTraining,
            defaultName = defaultStatisticsTrainingName,
        )
    }

    fun requestLeave(action: () -> Unit) {
        if (!statisticsTrainingRuntimeContext.hasLoadedSelection || !hasUnsavedChanges()) {
            action()
            return
        }

        pendingLeaveAction = action
    }

    fun updateRecommendationSettings(settings: StatisticsTrainingRecommendationSettings) {
        statisticsTrainingRuntimeContext.updateRecommendationSettings(settings)
    }

    fun confirmRecommendationSettings(settings: StatisticsTrainingRecommendationSettings) {
        showLimitsDialog = false
        if (settings == recommendationSettings) {
            return
        }

        updateRecommendationSettings(settings)
        scope.launch {
            isLoading = true
            refreshSelection(settings = settings)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        if (statisticsTrainingRuntimeContext.hasLoadedSelection) {
            return@LaunchedEffect
        }

        isLoading = true
        refreshSelection()
        isLoading = false
    }

    LaunchedEffect(statisticsTrainingRuntimeContext.formulaRevision) {
        if (!statisticsTrainingRuntimeContext.isLoadedFormulaOutOfDate()) {
            return@LaunchedEffect
        }

        isLoading = true
        refreshSelection()
        isLoading = false
    }

    messageDialog?.let { message ->
        AppMessageDialog(
            title = stringResource(message.titleRes),
            message = stringResource(message.messageRes),
            onDismiss = { messageDialog = null },
        )
    }

    pendingLeaveAction?.let { leaveAction ->
        AppMessageDialog(
            title = stringResource(R.string.training_unsaved_changes_title),
            message = stringResource(R.string.statistics_training_unsaved_changes_message),
            onDismiss = { pendingLeaveAction = null },
            actions = listOf(
                AppMessageDialogAction(
                    text = stringResource(R.string.common_save),
                    onClick = {
                        pendingLeaveAction = null
                        requestSave(onSaved = leaveAction)
                    },
                ),
                AppMessageDialogAction(
                    text = stringResource(R.string.common_discard),
                    onClick = {
                        pendingLeaveAction = null
                        leaveAction()
                    },
                ),
                AppMessageDialogAction(
                    text = stringResource(R.string.common_cancel),
                    onClick = { pendingLeaveAction = null },
                ),
            ),
        )
    }

    if (showLimitsDialog && !isLoading) {
        StatisticsTrainingLimitsDialog(
            settings = recommendationSettings,
            onConfirm = ::confirmRecommendationSettings,
            onDismiss = { showLimitsDialog = false },
        )
    }

    trainingSaveSuccess?.let { success ->
        AppMessageDialog(
            title = stringResource(R.string.create_training_created_title),
            message = stringResource(
                R.string.create_training_created_message,
                success.trainingId,
                success.trainingName,
                success.linesCount,
            ),
            onDismiss = {
                val nextAction = afterSaveAction
                trainingSaveSuccess = null
                afterSaveAction = null
                if (nextAction != null) {
                    nextAction()
                    return@AppMessageDialog
                }

                screenContext.onNavigate(ScreenType.Home)
            }
        )
    }

    if (isLoading) {
        AppScreenScaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                AppTopBar(
                    title = stringResource(R.string.statistics_training_title),
                    onBackClick = { requestLeave(screenContext.onBackClick) },
                    actions = {
                        HomeIconButton(onClick = { requestLeave { screenContext.onNavigate(ScreenType.Home) } })
                    },
                )
            },
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = TrainingAccentTeal)
            }
        }
        return
    }

    CreateTrainingScreen(
        editorState = statisticsTrainingRuntimeContext.editorState,
        screenTitle = stringResource(R.string.statistics_training_title),
        linesCountLabel = stringResource(R.string.statistics_training_lines_count_label),
        defaultTrainingName = defaultStatisticsTrainingName,
        headerContent = {
            StatisticsTrainingSettingsSection(
                maxLines = recommendationSettings.limit,
                minDaysSinceLastTraining = recommendationSettings.minDaysSinceLastTraining,
                maxWeight = recommendationSettings.maxWeight,
                onChangeLimitsClick = { showLimitsDialog = true },
            )
        },
        topBarActions = {
            IconButton(onClick = onOpenFormulaSettings) {
                IconMd(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.common_settings),
                    tint = TrainingAccentTeal,
                )
            }
        },
        onBackClick = { requestLeave(screenContext.onBackClick) },
        onNavigate = { screenType -> requestLeave { screenContext.onNavigate(screenType) } },
        onEditorStateChange = statisticsTrainingRuntimeContext::updateEditorState,
        onSaveTraining = { _, _ -> requestSave() },
        modifier = modifier,
    )
}
