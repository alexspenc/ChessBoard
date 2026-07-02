package com.example.chessboard.ui.screen.gameOpeningAnalysis

/*
 * File role: coordinates screen-level actions for recording deviation mistakes from analysis results.
 * Allowed here:
 * - coroutine launch and local screen-state transitions for deviation-mistake recording
 * - invoking injected recording callbacks supplied by the screen container
 * Not allowed here:
 * - Compose UI rendering, direct database/service creation, analysis algorithms, or result-card layout
 * Validation date: 2026-07-01
 */

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal typealias GameOpeningAnalysisDeviationMistakeRecorder = suspend (
    lineIds: List<Long>,
    mistakesCount: Int,
) -> Int

internal fun startGameOpeningAnalysisDeviationMistakeRecording(
    coroutineScope: CoroutineScope,
    state: GameOpeningAnalysisDeviationMistakeState,
    lineIds: List<Long>,
    recordDeviationMistake: GameOpeningAnalysisDeviationMistakeRecorder,
    onRecorded: () -> Unit,
    fallbackErrorMessage: String,
) {
    if (state.inProgress) {
        return
    }

    val affectedLineIds = lineIds.distinct()
    if (affectedLineIds.isEmpty()) {
        return
    }

    coroutineScope.launch {
        state.inProgress = true
        try {
            val mistakesCount = affectedLineIds.size
            val recordedLinesCount =
                withContext(Dispatchers.IO) {
                    recordDeviationMistake(affectedLineIds, mistakesCount)
                }
            state.recordedLinesCount = recordedLinesCount
            onRecorded()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            state.errorMessage = error.message ?: fallbackErrorMessage
        } finally {
            state.inProgress = false
        }
    }
}
