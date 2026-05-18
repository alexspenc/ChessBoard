package com.example.chessboard.ui.screen.training.create

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.chessboard.service.OneLineTrainingData
import com.example.chessboard.service.StatisticsTrainingRecommendationSettings
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.HomeIconButton
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.RepeatStepIconButton
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.screen.training.DEFAULT_MAX_WEIGHT
import com.example.chessboard.ui.screen.training.DEFAULT_STATISTICS_TRAINING_NAME
import com.example.chessboard.ui.screen.training.MAX_STATISTICS_GAMES
import com.example.chessboard.ui.screen.training.common.CreateTrainingEditorState
import com.example.chessboard.ui.screen.training.common.TrainingLineEditorItem
import com.example.chessboard.ui.screen.training.common.toTrainingLineEditorItem
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TrainingAccentTeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


private data class StatisticsTrainingLoadState(
    val isLoading: Boolean = true,
    val trainingName: String = DEFAULT_STATISTICS_TRAINING_NAME,
    val linesForTraining: List<TrainingLineEditorItem> = emptyList(),
)

private data class StatisticsTrainingSaveSuccess(
    val trainingId: Long,
    val trainingName: String,
    val linesCount: Int,
)

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
                contentDescription = "Decrease $label",
                onStep = onDecreaseClick,
            )
            RepeatStepIconButton(
                icon = Icons.Default.Add,
                contentDescription = "Increase $label",
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
    onDecreaseMaxLinesClick: () -> Unit,
    onIncreaseMaxLinesClick: () -> Unit,
    onDecreaseMinDaysClick: () -> Unit,
    onIncreaseMinDaysClick: () -> Unit,
    onDecreaseMaxWeightClick: () -> Unit,
    onIncreaseMaxWeightClick: () -> Unit,
    onApplyClick: () -> Unit,
) {
    ScreenSection {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
        ) {
            StatisticsSettingStepper(
                label = "Max lines",
                value = maxLines,
                onDecreaseClick = onDecreaseMaxLinesClick,
                onIncreaseClick = onIncreaseMaxLinesClick,
            )
            StatisticsSettingStepper(
                label = "Min days since last training",
                value = minDaysSinceLastTraining,
                onDecreaseClick = onDecreaseMinDaysClick,
                onIncreaseClick = onIncreaseMinDaysClick,
            )
            StatisticsSettingStepper(
                label = "Max weight",
                value = maxWeight,
                onDecreaseClick = onDecreaseMaxWeightClick,
                onIncreaseClick = onIncreaseMaxWeightClick,
            )
            PrimaryButton(
                text = "Apply selection",
                onClick = onApplyClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun CreateTrainingByStatisticsScreenContainer(
    screenContext: ScreenContainerContext,
    modifier: Modifier = Modifier,
) {
    var loadState by remember { mutableStateOf(StatisticsTrainingLoadState()) }
    var trainingSaveSuccess by remember { mutableStateOf<StatisticsTrainingSaveSuccess?>(null) }
    var maxLines by remember { mutableIntStateOf(MAX_STATISTICS_GAMES) }
    var minDaysSinceLastTraining by remember { mutableIntStateOf(0) }
    var maxWeight by remember { mutableIntStateOf(DEFAULT_MAX_WEIGHT) }
    var appliedMaxLines by remember { mutableIntStateOf(MAX_STATISTICS_GAMES) }
    var appliedMinDays by remember { mutableIntStateOf(0) }
    var appliedMaxWeight by remember { mutableIntStateOf(DEFAULT_MAX_WEIGHT) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(appliedMaxLines, appliedMinDays, appliedMaxWeight) {
        loadState = loadState.copy(isLoading = true)

        val recommendations = withContext(Dispatchers.IO) {
            screenContext.inDbProvider
                .createStatisticsTrainingService()
                .getRecommendation(
                    recommendationSettings = StatisticsTrainingRecommendationSettings(
                        limit = appliedMaxLines,
                        minDaysSinceLastTraining = appliedMinDays,
                        maxWeight = appliedMaxWeight,
                    ),
                )
        }

        loadState = StatisticsTrainingLoadState(
            isLoading = false,
            trainingName = DEFAULT_STATISTICS_TRAINING_NAME,
            linesForTraining = recommendations.map { recommendation ->
                recommendation.line.toTrainingLineEditorItem(weight = recommendation.weight)
            }
        )
    }

    trainingSaveSuccess?.let { success ->
        AppMessageDialog(
            title = "Training Created",
            message = buildString {
                appendLine("ID: ${success.trainingId}")
                appendLine("Name: ${success.trainingName}")
                append("Lines added: ")
                append(success.linesCount)
            },
            onDismiss = {
                trainingSaveSuccess = null
                screenContext.onNavigate(ScreenType.Home)
            }
        )
    }

    if (loadState.isLoading) {
        AppScreenScaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                AppTopBar(
                    title = "Create Training by Statistics",
                    onBackClick = screenContext.onBackClick,
                    actions = {
                        HomeIconButton(onClick = { screenContext.onNavigate(ScreenType.Home) })
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
        editorState = CreateTrainingEditorState(
            trainingName = loadState.trainingName,
            editableLinesForTraining = loadState.linesForTraining,
        ),
        screenTitle = "Create Training by Statistics",
        linesCountLabel = "Lines selected by statistics",
        headerContent = {
            StatisticsTrainingSettingsSection(
                maxLines = maxLines,
                minDaysSinceLastTraining = minDaysSinceLastTraining,
                maxWeight = maxWeight,
                onDecreaseMaxLinesClick = {
                    maxLines = (maxLines - 1).coerceAtLeast(1)
                },
                onIncreaseMaxLinesClick = {
                    maxLines = (maxLines + 1).coerceAtMost(MAX_STATISTICS_GAMES)
                },
                onDecreaseMinDaysClick = {
                    minDaysSinceLastTraining = (minDaysSinceLastTraining - 1).coerceAtLeast(0)
                },
                onIncreaseMinDaysClick = {
                    minDaysSinceLastTraining += 1
                },
                onDecreaseMaxWeightClick = {
                    maxWeight = (maxWeight - 1).coerceAtLeast(1)
                },
                onIncreaseMaxWeightClick = {
                    maxWeight = (maxWeight + 1).coerceAtMost(DEFAULT_MAX_WEIGHT)
                },
                onApplyClick = {
                    appliedMaxLines = maxLines
                    appliedMinDays = minDaysSinceLastTraining
                    appliedMaxWeight = maxWeight
                },
            )
        },
        onBackClick = screenContext.onBackClick,
        onNavigate = screenContext.onNavigate,
        onSaveTraining = { trainingName, editableLines ->
            scope.launch {
                val normalizedName = trainingName.ifBlank { DEFAULT_STATISTICS_TRAINING_NAME }
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
            }
        },
        modifier = modifier,
    )
}
