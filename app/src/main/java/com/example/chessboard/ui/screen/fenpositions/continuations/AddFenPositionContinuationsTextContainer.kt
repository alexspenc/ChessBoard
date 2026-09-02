package com.example.chessboard.ui.screen.fenpositions.continuations

/*
 * File role: owns debounced processing state for pasted continuation text on the add screen.
 * Allowed here:
 * - text state, stale-work cancellation, processing stages, and read-only stored-line loading
 * Not allowed here:
 * - screen layout, dialog rendering, navigation, continuation insertion, or manual board input
 * Validation date: 2026-09-02
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.example.chessboard.service.PgnParseErrorStrings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val FenPositionContinuationTextDebounceMillis = 600L

internal class AddFenPositionContinuationsTextState {
    var text by mutableStateOf("")
        private set

    var processingStage by mutableStateOf<AddFenPositionContinuationsProcessingStage?>(null)
        private set

    var latestResult by mutableStateOf<FenPositionContinuationTextProcessingResult?>(null)
        private set

    var pendingResult by mutableStateOf<FenPositionContinuationTextProcessingResult?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun updateText(value: String) {
        if (text == value) {
            return
        }

        text = value
        processingStage = null
        latestResult = null
        pendingResult = null
        errorMessage = null
    }

    fun clearText() {
        updateText("")
    }

    fun dismissResult() {
        pendingResult = null
    }

    fun dismissError() {
        errorMessage = null
    }

    internal fun updateProcessingStage(
        sourceText: String,
        stage: AddFenPositionContinuationsProcessingStage,
    ) {
        if (text != sourceText) {
            return
        }

        processingStage = stage
    }

    internal fun completeProcessing(
        sourceText: String,
        result: FenPositionContinuationTextProcessingResult,
    ) {
        if (text != sourceText) {
            return
        }

        processingStage = null
        latestResult = result
        pendingResult = result
        errorMessage = null
    }

    internal fun failProcessing(
        sourceText: String,
        message: String,
    ) {
        if (text != sourceText) {
            return
        }

        processingStage = null
        latestResult = null
        pendingResult = null
        errorMessage = message
    }

    internal fun cancelProcessing(sourceText: String) {
        if (text != sourceText) {
            return
        }

        processingStage = null
    }
}

@Composable
internal fun rememberAddFenPositionContinuationsTextState(
    startFen: String,
    errorStrings: PgnParseErrorStrings,
    noValidLinesMessage: String,
    failedProcessingMessage: String,
    loadStoredUciLines: suspend () -> List<List<String>>,
): AddFenPositionContinuationsTextState {
    val state = remember(startFen) { AddFenPositionContinuationsTextState() }
    val currentStoredLinesLoader by rememberUpdatedState(loadStoredUciLines)

    LaunchedEffect(
        state.text,
        startFen,
        errorStrings,
        noValidLinesMessage,
        failedProcessingMessage,
    ) {
        val sourceText = state.text
        if (sourceText.isBlank()) {
            return@LaunchedEffect
        }

        // This pause is intentional: pasted text may arrive through several rapid edit events.
        // LaunchedEffect cancels the previous delay and processing when a newer value arrives.
        delay(FenPositionContinuationTextDebounceMillis)

        try {
            state.updateProcessingStage(
                sourceText = sourceText,
                stage = AddFenPositionContinuationsProcessingStage.ParsingContinuations,
            )
            val parsedLines = withContext(Dispatchers.Default) {
                parseFenPositionContinuationText(
                    text = sourceText,
                    startFen = startFen,
                    errorStrings = errorStrings,
                    noValidLinesMessage = noValidLinesMessage,
                )
            }

            state.updateProcessingStage(
                sourceText = sourceText,
                stage = AddFenPositionContinuationsProcessingStage.CheckingLines,
            )
            val preparation = withContext(Dispatchers.Default) {
                prepareFenPositionContinuationText(parsedLines)
            }

            state.updateProcessingStage(
                sourceText = sourceText,
                stage = AddFenPositionContinuationsProcessingStage.CheckingStoredContinuations,
            )
            val storedUciLines = withContext(Dispatchers.IO) {
                currentStoredLinesLoader()
            }
            val result = withContext(Dispatchers.Default) {
                buildFenPositionContinuationTextProcessingResult(
                    preparation = preparation,
                    storedUciLines = storedUciLines,
                    startFen = startFen,
                )
            }
            state.completeProcessing(
                sourceText = sourceText,
                result = result,
            )
        } catch (error: CancellationException) {
            state.cancelProcessing(sourceText)
            throw error
        } catch (error: Exception) {
            state.failProcessing(
                sourceText = sourceText,
                message = error.message ?: failedProcessingMessage,
            )
        }
    }

    return state
}
