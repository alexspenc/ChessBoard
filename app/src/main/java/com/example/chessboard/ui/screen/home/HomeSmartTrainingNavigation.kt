package com.example.chessboard.ui.screen.home

/**
 * File role: owns the Home-screen Smart Training pre-navigation workflow.
 * Allowed here:
 * - temporary UI state for preparing Smart Training navigation from Home
 * - blocking preparation dialogs and missing-data explanation dialogs
 * - screen-level checks needed before leaving Home for Smart Training
 * Not allowed here:
 * - SimpleView or regular Home layout
 * - SmartTraining screen internals
 * - persistence rules beyond calling an injected screen service
 * Validation date: 2026-05-14
 */
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.service.LineListService
import com.example.chessboard.service.TrainingService
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.error.AppErrorReporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class SmartTrainingNavigationState(
    val job: Job? = null,
    val requestId: Int = 0,
    val dialog: SmartTrainingNavigationDialog? = null,
)

@Composable
internal fun HomeSmartTrainingNavigationHost(
    lineListService: LineListService,
    trainingService: TrainingService,
    errorReporter: AppErrorReporter,
    onSmartTrainingClick: () -> Unit,
    onCreateOpeningClick: () -> Unit,
    onCreateTrainingClick: () -> Unit,
    content: @Composable (onSmartTrainingClick: () -> Unit) -> Unit,
) {
    var state by remember { mutableStateOf(SmartTrainingNavigationState()) }
    val scope = rememberCoroutineScope()
    val failedPrepareSmartTraining = stringResource(R.string.home_failed_prepare_smart_training)
    val preparingSmartTraining = stringResource(R.string.home_preparing_smart_training_title)
    val checkingOpeningsAndTrainings = stringResource(R.string.home_checking_openings_and_trainings)
    val noLinesForSmartTraining = stringResource(R.string.home_no_lines_for_smart_training)
    val noTrainingTitle = stringResource(R.string.home_no_training_title)
    val noTrainingForSmartTraining = stringResource(R.string.home_no_training_for_smart_training)
    val createTraining = stringResource(R.string.home_create_training_title)
    val cancel = stringResource(R.string.common_cancel)

    fun cancelSmartTrainingPreparation() {
        val currentJob = state.job
        state = state.copy(
            job = null,
            requestId = state.requestId + 1,
        )
        currentJob?.cancel()
    }

    fun prepareSmartTrainingNavigation() {
        if (state.job != null) {
            return
        }

        val requestId = state.requestId + 1
        state = state.copy(
            requestId = requestId,
            dialog = null,
        )
        val job = scope.launch {
            try {
                val target = withContext(Dispatchers.IO) {
                    resolveSmartTrainingNavigationTarget(
                        lineListService = lineListService,
                        trainingService = trainingService,
                    )
                }
                if (state.requestId != requestId) {
                    return@launch
                }

                state = state.copy(job = null)
                when (target) {
                    SmartTrainingNavigationTarget.SmartTraining -> onSmartTrainingClick()
                    SmartTrainingNavigationTarget.CreateOpening -> {
                        state = state.copy(dialog = SmartTrainingNavigationDialog.NoLines)
                    }
                    SmartTrainingNavigationTarget.CreateTraining -> {
                        state = state.copy(dialog = SmartTrainingNavigationDialog.NoTrainings)
                    }
                }
            } catch (_: CancellationException) {
                if (state.requestId == requestId) {
                    state = state.copy(job = null)
                }
            } catch (error: Throwable) {
                if (state.requestId == requestId) {
                    state = state.copy(job = null)
                    errorReporter.report(
                        error = error,
                        message = failedPrepareSmartTraining,
                    )
                }
            }
        }
        state = state.copy(job = job)
    }

    content(::prepareSmartTrainingNavigation)

    if (state.job != null) {
        HomeNavigationPreparationDialog(
            title = preparingSmartTraining,
            message = checkingOpeningsAndTrainings,
            onCancel = ::cancelSmartTrainingPreparation,
        )
    }

    if (state.dialog == SmartTrainingNavigationDialog.NoLines) {
        HomeNoLinesDialog(
            message = noLinesForSmartTraining,
            onCreateOpeningClick = {
                state = state.copy(dialog = null)
                onCreateOpeningClick()
            },
            onDismiss = {
                state = state.copy(dialog = null)
            },
        )
    }

    if (state.dialog == SmartTrainingNavigationDialog.NoTrainings) {
        AppMessageDialog(
            title = noTrainingTitle,
            message = noTrainingForSmartTraining,
            confirmText = createTraining,
            onConfirm = {
                state = state.copy(dialog = null)
                onCreateTrainingClick()
            },
            dismissText = cancel,
            onDismiss = {
                state = state.copy(dialog = null)
            },
        )
    }
}

private enum class SmartTrainingNavigationDialog {
    NoLines,
    NoTrainings,
}

private enum class SmartTrainingNavigationTarget {
    SmartTraining,
    CreateOpening,
    CreateTraining,
}

private suspend fun resolveSmartTrainingNavigationTarget(
    lineListService: LineListService,
    trainingService: TrainingService,
): SmartTrainingNavigationTarget {
    val linesCount = lineListService.getLinesCount()
    if (linesCount <= 0) {
        return SmartTrainingNavigationTarget.CreateOpening
    }

    if (!trainingService.hasAnyTraining()) {
        return SmartTrainingNavigationTarget.CreateTraining
    }

    return SmartTrainingNavigationTarget.SmartTraining
}
