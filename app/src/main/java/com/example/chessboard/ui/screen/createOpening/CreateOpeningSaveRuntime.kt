package com.example.chessboard.ui.screen.createOpening

/**
 * File role: groups create-opening save runtime state and small save-runtime helpers.
 * Allowed here:
 * - runtime-only state used by the create-opening container while saving
 * - small helpers for formatting save runtime messages
 * - test seam types for replacing the save runner
 * Not allowed here:
 * - Compose UI rendering, launcher setup, or database save implementation
 */
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.LineSaver
import com.example.chessboard.service.TrainingService
import kotlinx.coroutines.Job

internal typealias CreateOpeningSaveRunner = suspend (
    snapshot: CreateOpeningSaveSnapshot,
    dbProvider: DatabaseProvider,
    lineSaver: LineSaver,
    trainingService: TrainingService,
    strings: CreateOpeningSaveStrings,
    onProgress: suspend (CreateOpeningSaveProgress) -> Unit,
) -> CreateOpeningSaveResult

internal data class CreateOpeningSaveRuntimeStrings(
    val saveCanceled: String,
    val processedLines: String,
    val savedLines: String,
    val skippedLines: String,
)

internal data class CreateOpeningSaveRuntimeState(
    val message: String? = null,
    val progress: CreateOpeningSaveProgress? = null,
    val job: Job? = null,
)

internal fun resolveCreateOpeningSaveCanceledMessage(
    progress: CreateOpeningSaveProgress?,
    strings: CreateOpeningSaveRuntimeStrings,
): String {
    val currentProgress = progress ?: return strings.saveCanceled

    return buildString {
        appendLine(strings.saveCanceled)
        appendLine(strings.processedLines.format(currentProgress.processedLinesCount, currentProgress.totalLines))
        appendLine(strings.savedLines.format(currentProgress.savedLinesCount))
        append(strings.skippedLines.format(currentProgress.skippedLinesCount))
    }
}
