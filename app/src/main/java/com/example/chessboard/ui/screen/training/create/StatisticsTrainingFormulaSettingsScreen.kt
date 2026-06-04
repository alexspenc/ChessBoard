package com.example.chessboard.ui.screen.training.create

/*
 * Screen for editing persisted statistics-training formula settings.
 *
 * Keep formula-settings UI, save/reset confirmation, and unsaved-change prompts
 * here. Do not add recommendation calculation or training creation save logic.
 *
 * Validation date: 2026-05-18
 */

import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.chessboard.R
import com.example.chessboard.entity.StatisticsTrainingFormulaSettingsEntity
import com.example.chessboard.runtimecontext.StatisticsTrainingRuntimeContext
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppMessageDialogAction
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.RepeatStepIconButton
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TrainingAccentTeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val FormulaDoubleStep = 0.1

private data class FormulaSettingsMessage(
    @StringRes val titleRes: Int,
    @StringRes val messageRes: Int,
    val afterDismiss: (() -> Unit)? = null,
)

@Composable
fun StatisticsTrainingFormulaSettingsScreenContainer(
    screenContext: ScreenContainerContext,
    statisticsTrainingRuntimeContext: StatisticsTrainingRuntimeContext,
    modifier: Modifier = Modifier,
) {
    var savedSettings by remember { mutableStateOf<StatisticsTrainingFormulaSettingsEntity?>(null) }
    var draftSettings by remember { mutableStateOf<StatisticsTrainingFormulaSettingsEntity?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var messageDialog by remember { mutableStateOf<FormulaSettingsMessage?>(null) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var pendingLeaveAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val scope = rememberCoroutineScope()

    fun hasUnsavedChanges(): Boolean {
        return savedSettings != null && draftSettings != null && savedSettings != draftSettings
    }

    fun requestLeave(action: () -> Unit) {
        if (!hasUnsavedChanges()) {
            action()
            return
        }

        pendingLeaveAction = action
    }

    fun saveSettings(onSaved: (() -> Unit)? = null) {
        val settingsToSave = draftSettings ?: return
        val currentSavedSettings = savedSettings
        if (currentSavedSettings == settingsToSave) {
            messageDialog = FormulaSettingsMessage(
                titleRes = R.string.statistics_formula_no_changes_title,
                messageRes = R.string.statistics_formula_no_changes_message,
                afterDismiss = onSaved,
            )
            return
        }

        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                screenContext.inDbProvider
                    .createStatisticsTrainingFormulaSettingsService()
                    .updateSettings(settingsToSave)
            }
            savedSettings = saved
            draftSettings = saved
            statisticsTrainingRuntimeContext.markFormulaChanged()
            messageDialog = FormulaSettingsMessage(
                titleRes = R.string.statistics_formula_settings_saved_title,
                messageRes = R.string.statistics_formula_settings_saved_message,
                afterDismiss = onSaved,
            )
        }
    }

    fun resetSettingsToDefaults() {
        val defaultSettings = StatisticsTrainingFormulaSettingsEntity()
        val currentSavedSettings = savedSettings
        val currentDraftSettings = draftSettings
        if (currentSavedSettings == defaultSettings && currentDraftSettings == defaultSettings) {
            messageDialog = FormulaSettingsMessage(
                titleRes = R.string.statistics_formula_already_defaults_title,
                messageRes = R.string.statistics_formula_already_defaults_message,
            )
            return
        }

        showResetConfirmation = true
    }

    fun confirmResetSettings() {
        val defaultSettings = StatisticsTrainingFormulaSettingsEntity()
        val currentSavedSettings = savedSettings
        showResetConfirmation = false
        scope.launch {
            val resetSettings = if (currentSavedSettings == defaultSettings) {
                defaultSettings
            } else {
                withContext(Dispatchers.IO) {
                    screenContext.inDbProvider
                        .createStatisticsTrainingFormulaSettingsService()
                        .resetSettings()
                }
            }

            if (currentSavedSettings != defaultSettings) {
                statisticsTrainingRuntimeContext.markFormulaChanged()
            }
            savedSettings = resetSettings
            draftSettings = resetSettings
            messageDialog = FormulaSettingsMessage(
                titleRes = R.string.statistics_formula_settings_reset_title,
                messageRes = R.string.statistics_formula_settings_reset_message,
            )
        }
    }

    LaunchedEffect(Unit) {
        val settings = withContext(Dispatchers.IO) {
            screenContext.inDbProvider
                .createStatisticsTrainingFormulaSettingsService()
                .getSettings()
        }
        savedSettings = settings
        draftSettings = settings
        isLoading = false
    }

    messageDialog?.let { message ->
        AppMessageDialog(
            title = stringResource(message.titleRes),
            message = stringResource(message.messageRes),
            onDismiss = {
                val afterDismiss = message.afterDismiss
                messageDialog = null
                afterDismiss?.invoke()
            },
        )
    }

    if (showResetConfirmation) {
        AppMessageDialog(
            title = stringResource(R.string.statistics_formula_reset_title),
            message = stringResource(R.string.statistics_formula_reset_message),
            onDismiss = { showResetConfirmation = false },
            actions = listOf(
                AppMessageDialogAction(
                    text = stringResource(R.string.common_reset),
                    onClick = ::confirmResetSettings,
                ),
                AppMessageDialogAction(
                    text = stringResource(R.string.common_cancel),
                    onClick = { showResetConfirmation = false },
                ),
            ),
        )
    }

    pendingLeaveAction?.let { leaveAction ->
        AppMessageDialog(
            title = stringResource(R.string.training_unsaved_changes_title),
            message = stringResource(R.string.statistics_formula_unsaved_changes_message),
            onDismiss = { pendingLeaveAction = null },
            actions = listOf(
                AppMessageDialogAction(
                    text = stringResource(R.string.common_save),
                    onClick = {
                        pendingLeaveAction = null
                        saveSettings(onSaved = leaveAction)
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

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.statistics_formula_title),
                onBackClick = { requestLeave(screenContext.onBackClick) },
                actions = {
                    IconButton(onClick = { saveSettings() }) {
                        IconMd(
                            imageVector = Icons.Default.Save,
                            contentDescription = stringResource(R.string.common_save),
                            tint = TrainingAccentTeal,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        val settings = draftSettings
        if (isLoading || settings == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = TrainingAccentTeal)
            }
            return@AppScreenScaffold
        }

        StatisticsTrainingFormulaSettingsScreen(
            settings = settings,
            onSettingsChange = { draftSettings = it },
            onResetClick = ::resetSettingsToDefaults,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        )
    }
}

@Composable
private fun StatisticsTrainingFormulaSettingsScreen(
    settings: StatisticsTrainingFormulaSettingsEntity,
    onSettingsChange: (StatisticsTrainingFormulaSettingsEntity) -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(AppDimens.spaceLg),
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
    ) {
        FormulaDescriptionSection()
        FormulaSettingsGroup(title = stringResource(R.string.statistics_formula_recent_results_title)) {
            FormulaExplanationText(
                text = stringResource(R.string.statistics_formula_recent_results_description)
            )
            IntFormulaStepper(
                label = stringResource(R.string.statistics_formula_recent_results_per_line_label),
                value = settings.recentResultsPerLine,
                onValueChange = { onSettingsChange(settings.copy(recentResultsPerLine = it)) },
            )
        }
        FormulaSettingsGroup(title = stringResource(R.string.statistics_formula_mistakes_title)) {
            FormulaExplanationText(
                text = stringResource(R.string.statistics_formula_last_mistakes_description)
            )
            DoubleFormulaStepper(
                label = stringResource(R.string.statistics_formula_last_mistake_weight_label),
                value = settings.lastMistakeWeight,
                onValueChange = { onSettingsChange(settings.copy(lastMistakeWeight = it)) },
            )
            IntFormulaStepper(
                label = stringResource(R.string.statistics_formula_max_last_mistakes_label),
                value = settings.maxMistakesLast,
                onValueChange = { onSettingsChange(settings.copy(maxMistakesLast = it)) },
            )
            FormulaExplanationText(
                text = stringResource(R.string.statistics_formula_average_mistakes_description)
            )
            DoubleFormulaStepper(
                label = stringResource(R.string.statistics_formula_average_mistakes_weight_label),
                value = settings.avgMistakesWeight,
                onValueChange = { onSettingsChange(settings.copy(avgMistakesWeight = it)) },
            )
            DoubleFormulaStepper(
                label = stringResource(R.string.statistics_formula_max_average_mistakes_label),
                value = settings.maxAvgMistakesRecent,
                onValueChange = { onSettingsChange(settings.copy(maxAvgMistakesRecent = it)) },
            )
        }
        FormulaSettingsGroup(title = stringResource(R.string.statistics_formula_recency_title)) {
            FormulaExplanationText(
                text = stringResource(R.string.statistics_formula_recency_description)
            )
            DoubleFormulaStepper(
                label = stringResource(R.string.statistics_formula_recency_weight_label),
                value = settings.recencyWeight,
                onValueChange = { onSettingsChange(settings.copy(recencyWeight = it)) },
            )
            IntFormulaStepper(
                label = stringResource(R.string.statistics_formula_recency_days_cap_label),
                value = settings.recencyDaysCap,
                onValueChange = { onSettingsChange(settings.copy(recencyDaysCap = it)) },
            )
        }
        FormulaSettingsGroup(title = stringResource(R.string.statistics_formula_perfect_training_title)) {
            FormulaExplanationText(
                text = stringResource(R.string.statistics_formula_perfect_training_description)
            )
            DoubleFormulaStepper(
                label = stringResource(R.string.statistics_formula_perfect_rate_penalty_label),
                value = settings.perfectRatePenaltyWeight,
                onValueChange = { onSettingsChange(settings.copy(perfectRatePenaltyWeight = it)) },
            )
        }
        FormulaSettingsGroup(title = stringResource(R.string.statistics_formula_attempt_boosts_title)) {
            FormulaExplanationText(
                text = stringResource(R.string.statistics_formula_attempt_boosts_description)
            )
            DoubleFormulaStepper(
                label = stringResource(R.string.statistics_formula_no_attempts_boost_label),
                value = settings.noAttemptsBoost,
                onValueChange = { onSettingsChange(settings.copy(noAttemptsBoost = it)) },
            )
            DoubleFormulaStepper(
                label = stringResource(R.string.statistics_formula_one_attempt_boost_label),
                value = settings.oneAttemptBoost,
                onValueChange = { onSettingsChange(settings.copy(oneAttemptBoost = it)) },
            )
            DoubleFormulaStepper(
                label = stringResource(R.string.statistics_formula_two_attempts_boost_label),
                value = settings.twoAttemptsBoost,
                onValueChange = { onSettingsChange(settings.copy(twoAttemptsBoost = it)) },
            )
        }
        FormulaSettingsGroup(title = stringResource(R.string.statistics_formula_weight_thresholds_title)) {
            FormulaExplanationText(
                text = stringResource(R.string.statistics_formula_weight_thresholds_description)
            )
            DoubleFormulaStepper(
                label = stringResource(R.string.statistics_formula_weight_5_threshold_label),
                value = settings.weight5ScoreThreshold,
                onValueChange = {
                    onSettingsChange(updateWeight5ScoreThreshold(settings = settings, value = it))
                },
            )
            DoubleFormulaStepper(
                label = stringResource(R.string.statistics_formula_weight_4_threshold_label),
                value = settings.weight4ScoreThreshold,
                onValueChange = {
                    onSettingsChange(updateWeight4ScoreThreshold(settings = settings, value = it))
                },
            )
            DoubleFormulaStepper(
                label = stringResource(R.string.statistics_formula_weight_3_threshold_label),
                value = settings.weight3ScoreThreshold,
                onValueChange = {
                    onSettingsChange(updateWeight3ScoreThreshold(settings = settings, value = it))
                },
            )
            DoubleFormulaStepper(
                label = stringResource(R.string.statistics_formula_weight_2_threshold_label),
                value = settings.weight2ScoreThreshold,
                onValueChange = {
                    onSettingsChange(updateWeight2ScoreThreshold(settings = settings, value = it))
                },
            )
        }
        PrimaryButton(
            text = stringResource(R.string.statistics_formula_reset_to_defaults),
            onClick = onResetClick,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(AppDimens.spaceMd))
    }
}

@Composable
private fun FormulaDescriptionSection() {
    ScreenSection {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
            SectionTitleText(text = stringResource(R.string.statistics_formula_description_title))
            BodySecondaryText(
                text = stringResource(R.string.statistics_formula_score_description)
            )
            BodySecondaryText(
                text = stringResource(R.string.statistics_formula_last_mistake_part_description)
            )
            BodySecondaryText(
                text = stringResource(R.string.statistics_formula_average_mistake_part_description)
            )
            BodySecondaryText(
                text = stringResource(R.string.statistics_formula_recency_part_description)
            )
            BodySecondaryText(
                text = stringResource(R.string.statistics_formula_perfect_penalty_description)
            )
            BodySecondaryText(
                text = stringResource(R.string.statistics_formula_selection_description)
            )
        }
    }
}

@Composable
private fun FormulaExplanationText(text: String) {
    BodySecondaryText(text = text)
}

@Composable
private fun FormulaSettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    ScreenSection {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = TrainingAccentTeal.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(AppDimens.radiusMd),
                )
                .padding(AppDimens.spaceMd),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
        ) {
            SectionTitleText(text = title)
            content()
        }
    }
}

@Composable
private fun IntFormulaStepper(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    FormulaStepperRow(
        label = label,
        valueText = value.toString(),
        onDecreaseClick = { onValueChange((value - 1).coerceAtLeast(0)) },
        onIncreaseClick = { onValueChange(value + 1) },
    )
}

@Composable
private fun DoubleFormulaStepper(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
) {
    FormulaStepperRow(
        label = label,
        valueText = formatFormulaDouble(value),
        onDecreaseClick = { onValueChange(stepFormulaDouble(value, -FormulaDoubleStep)) },
        onIncreaseClick = { onValueChange(stepFormulaDouble(value, FormulaDoubleStep)) },
    )
}

@Composable
private fun FormulaStepperRow(
    label: String,
    valueText: String,
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
            BodySecondaryText(text = valueText)
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

private fun stepFormulaDouble(value: Double, delta: Double): Double {
    return (((value + delta).coerceAtLeast(0.0)) * 10.0).roundToInt() / 10.0
}

internal fun updateWeight5ScoreThreshold(
    settings: StatisticsTrainingFormulaSettingsEntity,
    value: Double,
): StatisticsTrainingFormulaSettingsEntity {
    val minValue = settings.weight4ScoreThreshold + FormulaDoubleStep
    return settings.copy(weight5ScoreThreshold = max(value, minValue).roundFormulaDouble())
}

internal fun updateWeight4ScoreThreshold(
    settings: StatisticsTrainingFormulaSettingsEntity,
    value: Double,
): StatisticsTrainingFormulaSettingsEntity {
    return settings.copy(
        weight4ScoreThreshold = value.coerceFormulaThreshold(
            minValue = settings.weight3ScoreThreshold + FormulaDoubleStep,
            maxValue = settings.weight5ScoreThreshold - FormulaDoubleStep,
        )
    )
}

internal fun updateWeight3ScoreThreshold(
    settings: StatisticsTrainingFormulaSettingsEntity,
    value: Double,
): StatisticsTrainingFormulaSettingsEntity {
    return settings.copy(
        weight3ScoreThreshold = value.coerceFormulaThreshold(
            minValue = settings.weight2ScoreThreshold + FormulaDoubleStep,
            maxValue = settings.weight4ScoreThreshold - FormulaDoubleStep,
        )
    )
}

internal fun updateWeight2ScoreThreshold(
    settings: StatisticsTrainingFormulaSettingsEntity,
    value: Double,
): StatisticsTrainingFormulaSettingsEntity {
    return settings.copy(
        weight2ScoreThreshold = min(
            value.coerceAtLeast(0.0),
            settings.weight3ScoreThreshold - FormulaDoubleStep,
        ).roundFormulaDouble()
    )
}

private fun Double.coerceFormulaThreshold(
    minValue: Double,
    maxValue: Double,
): Double {
    return min(max(this, minValue), maxValue).roundFormulaDouble()
}

private fun Double.roundFormulaDouble(): Double {
    return (this * 10.0).roundToInt() / 10.0
}

private fun formatFormulaDouble(value: Double): String {
    return "%.1f".format(value)
}
