package com.example.chessboard.ui.screen.home

/**
 * File role: owns the Home-screen pre-navigation workflow for opening regular trainings.
 * Allowed here:
 * - temporary UI state for preparing navigation from Home to the trainings area
 * - blocking preparation dialogs and missing-lines explanation dialogs
 * - screen-level checks needed before leaving Home for regular trainings
 * Not allowed here:
 * - SimpleView or regular Home layout
 * - SmartTraining-specific validation
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
import com.example.chessboard.ui.error.AppErrorReporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class TrainingNavigationState(
    val job: Job? = null,
    val requestId: Int = 0,
    val showNoLinesDialog: Boolean = false,
)

@Composable
internal fun HomeTrainingNavigationHost(
    lineListService: LineListService,
    errorReporter: AppErrorReporter,
    onTrainingClick: () -> Unit,
    onCreateOpeningClick: () -> Unit,
    content: @Composable (onTrainingClick: () -> Unit) -> Unit,
) {
    var state by remember { mutableStateOf(TrainingNavigationState()) }
    val scope = rememberCoroutineScope()
    val failedPrepareTrainings = stringResource(R.string.home_failed_prepare_trainings)
    val preparingTrainings = stringResource(R.string.home_preparing_trainings_title)
    val checkingOpenings = stringResource(R.string.home_checking_openings)
    val noLinesForTrainings = stringResource(R.string.home_no_lines_for_trainings)

    fun cancelTrainingPreparation() {
        val currentJob = state.job
        state = state.copy(
            job = null,
            requestId = state.requestId + 1,
        )
        currentJob?.cancel()
    }

    fun prepareTrainingNavigation() {
        if (state.job != null) {
            return
        }

        val requestId = state.requestId + 1
        state = state.copy(
            requestId = requestId,
            showNoLinesDialog = false,
        )
        val job = scope.launch {
            try {
                val hasLines = withContext(Dispatchers.IO) {
                    lineListService.getLinesCount() > 0
                }
                if (state.requestId != requestId) {
                    return@launch
                }

                state = state.copy(job = null)
                if (hasLines) {
                    onTrainingClick()
                    return@launch
                }

                state = state.copy(showNoLinesDialog = true)
            } catch (_: CancellationException) {
                if (state.requestId == requestId) {
                    state = state.copy(job = null)
                }
            } catch (error: Throwable) {
                if (state.requestId == requestId) {
                    state = state.copy(job = null)
                    errorReporter.report(
                        error = error,
                        message = failedPrepareTrainings,
                    )
                }
            }
        }
        state = state.copy(job = job)
    }

    content(::prepareTrainingNavigation)

    if (state.job != null) {
        HomeNavigationPreparationDialog(
            title = preparingTrainings,
            message = checkingOpenings,
            onCancel = ::cancelTrainingPreparation,
        )
    }

    if (state.showNoLinesDialog) {
        HomeNoLinesDialog(
            message = noLinesForTrainings,
            onCreateOpeningClick = {
                state = state.copy(showNoLinesDialog = false)
                onCreateOpeningClick()
            },
            onDismiss = {
                state = state.copy(showNoLinesDialog = false)
            },
        )
    }
}
