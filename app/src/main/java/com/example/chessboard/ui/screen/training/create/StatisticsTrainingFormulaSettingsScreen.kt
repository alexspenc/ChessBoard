package com.example.chessboard.ui.screen.training.create

/*
 * Screen for editing persisted statistics-training formula settings.
 *
 * Keep formula-settings UI, save/reset confirmation, and unsaved-change prompts
 * here. Do not add recommendation calculation or training creation save logic.
 *
 * Validation date: 2026-05-18
 */

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
import kotlin.math.roundToInt

private data class FormulaSettingsMessage(
    val title: String,
    val message: String,
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
                title = "No Changes",
                message = "Formula settings are already saved.",
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
                title = "Settings Saved",
                message = "Training formula settings were saved.",
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
                title = "Already Defaults",
                message = "Settings are already defaults.",
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
                title = "Settings Reset",
                message = "Training formula settings were reset to defaults.",
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
            title = message.title,
            message = message.message,
            onDismiss = {
                val afterDismiss = message.afterDismiss
                messageDialog = null
                afterDismiss?.invoke()
            },
        )
    }

    if (showResetConfirmation) {
        AppMessageDialog(
            title = "Reset Formula Settings",
            message = "Reset training formula settings to defaults?",
            onDismiss = { showResetConfirmation = false },
            actions = listOf(
                AppMessageDialogAction(
                    text = "Reset",
                    onClick = ::confirmResetSettings,
                ),
                AppMessageDialogAction(
                    text = "Cancel",
                    onClick = { showResetConfirmation = false },
                ),
            ),
        )
    }

    pendingLeaveAction?.let { leaveAction ->
        AppMessageDialog(
            title = "Unsaved Changes",
            message = "Save formula settings before leaving this screen?",
            onDismiss = { pendingLeaveAction = null },
            actions = listOf(
                AppMessageDialogAction(
                    text = "Save",
                    onClick = {
                        pendingLeaveAction = null
                        saveSettings(onSaved = leaveAction)
                    },
                ),
                AppMessageDialogAction(
                    text = "Discard",
                    onClick = {
                        pendingLeaveAction = null
                        leaveAction()
                    },
                ),
                AppMessageDialogAction(
                    text = "Cancel",
                    onClick = { pendingLeaveAction = null },
                ),
            ),
        )
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Training Formula Settings",
                onBackClick = { requestLeave(screenContext.onBackClick) },
                actions = {
                    IconButton(onClick = { saveSettings() }) {
                        IconMd(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save",
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
        FormulaSettingsGroup(title = "Recent Results") {
            IntFormulaStepper(
                label = "Recent results per line",
                value = settings.recentResultsPerLine,
                onValueChange = { onSettingsChange(settings.copy(recentResultsPerLine = it)) },
            )
        }
        FormulaSettingsGroup(title = "Mistakes") {
            DoubleFormulaStepper(
                label = "Last mistake weight",
                value = settings.lastMistakeWeight,
                onValueChange = { onSettingsChange(settings.copy(lastMistakeWeight = it)) },
            )
            IntFormulaStepper(
                label = "Max last mistakes",
                value = settings.maxMistakesLast,
                onValueChange = { onSettingsChange(settings.copy(maxMistakesLast = it)) },
            )
            DoubleFormulaStepper(
                label = "Average mistakes weight",
                value = settings.avgMistakesWeight,
                onValueChange = { onSettingsChange(settings.copy(avgMistakesWeight = it)) },
            )
            DoubleFormulaStepper(
                label = "Max average mistakes",
                value = settings.maxAvgMistakesRecent,
                onValueChange = { onSettingsChange(settings.copy(maxAvgMistakesRecent = it)) },
            )
        }
        FormulaSettingsGroup(title = "Recency") {
            DoubleFormulaStepper(
                label = "Recency weight",
                value = settings.recencyWeight,
                onValueChange = { onSettingsChange(settings.copy(recencyWeight = it)) },
            )
            IntFormulaStepper(
                label = "Recency days cap",
                value = settings.recencyDaysCap,
                onValueChange = { onSettingsChange(settings.copy(recencyDaysCap = it)) },
            )
        }
        FormulaSettingsGroup(title = "Perfect Training") {
            DoubleFormulaStepper(
                label = "Perfect rate penalty",
                value = settings.perfectRatePenaltyWeight,
                onValueChange = { onSettingsChange(settings.copy(perfectRatePenaltyWeight = it)) },
            )
        }
        FormulaSettingsGroup(title = "Attempt Boosts") {
            DoubleFormulaStepper(
                label = "No attempts boost",
                value = settings.noAttemptsBoost,
                onValueChange = { onSettingsChange(settings.copy(noAttemptsBoost = it)) },
            )
            DoubleFormulaStepper(
                label = "One attempt boost",
                value = settings.oneAttemptBoost,
                onValueChange = { onSettingsChange(settings.copy(oneAttemptBoost = it)) },
            )
            DoubleFormulaStepper(
                label = "Two attempts boost",
                value = settings.twoAttemptsBoost,
                onValueChange = { onSettingsChange(settings.copy(twoAttemptsBoost = it)) },
            )
        }
        PrimaryButton(
            text = "Reset to defaults",
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
            SectionTitleText(text = "Formula")
            BodySecondaryText(
                text = "Score = last mistakes + average mistakes + recency + attempt boost - perfect training penalty."
            )
            BodySecondaryText(
                text = "Higher scores select lines earlier and usually assign them a higher training weight."
            )
        }
    }
}

@Composable
private fun FormulaSettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    ScreenSection {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
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
        onDecreaseClick = { onValueChange(stepFormulaDouble(value, -0.1)) },
        onIncreaseClick = { onValueChange(stepFormulaDouble(value, 0.1)) },
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

private fun stepFormulaDouble(value: Double, delta: Double): Double {
    return (((value + delta).coerceAtLeast(0.0)) * 10.0).roundToInt() / 10.0
}

private fun formatFormulaDouble(value: Double): String {
    return "%.1f".format(value)
}
