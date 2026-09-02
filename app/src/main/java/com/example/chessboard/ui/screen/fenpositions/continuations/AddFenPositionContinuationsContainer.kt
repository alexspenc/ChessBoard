package com.example.chessboard.ui.screen.fenpositions.continuations

/*
 * File role: connects manual and pasted-text continuation drafts to the add screen.
 * Allowed here:
 * - screen state mapping, processing/result/error dialogs, discard confirmation, and callbacks
 * Not allowed here:
 * - Room access, continuation insertion, app-wide routing, or presentational layout details
 * Validation date: 2026-09-02
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
internal fun AddFenPositionContinuationsContainer(
    startFen: String,
    positionName: String,
    positionTheme: String,
    loadStoredUciLines: suspend () -> List<List<String>>,
    onBackClick: () -> Unit,
    onSaveClick: (List<List<String>>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = addFenPositionContinuationsStrings()
    val pgnParseErrorStrings = addFenPositionContinuationsPgnParseErrorStrings()
    val manualInput = remember(startFen) {
        FenPositionContinuationManualInput(startFen)
    }
    val textState = rememberAddFenPositionContinuationsTextState(
        startFen = startFen,
        errorStrings = pgnParseErrorStrings,
        noValidLinesMessage = strings.noValidLinesMessage,
        failedProcessingMessage = strings.failedProcessingMessage,
        loadStoredUciLines = loadStoredUciLines,
    )
    var discardConfirmationVisible by remember(startFen) { mutableStateOf(false) }
    val manualSanLine = remember(manualInput.lineController.boardState) {
        manualInput.sanLine
    }
    val textResult = textState.latestResult

    fun newUciLines(): List<List<String>> {
        val manualUciMoves = manualInput.uciMoves
        if (manualUciMoves.isNotEmpty()) {
            return listOf(manualUciMoves)
        }

        return textResult?.newUciLines.orEmpty()
    }

    fun requestBack() {
        if (manualInput.uciMoves.isEmpty() && textState.text.isBlank()) {
            onBackClick()
            return
        }

        discardConfirmationVisible = true
    }

    fun dialogState(): AddFenPositionContinuationsDialogState? {
        if (discardConfirmationVisible) {
            return AddFenPositionContinuationsDialogState.ConfirmDiscard
        }

        val processingStage = textState.processingStage
        if (processingStage != null) {
            return AddFenPositionContinuationsDialogState.Processing(processingStage)
        }

        val errorMessage = textState.errorMessage
        if (errorMessage != null) {
            return AddFenPositionContinuationsDialogState.Error(
                title = strings.processingErrorTitle,
                message = errorMessage,
            )
        }

        val pendingResult = textState.pendingResult
        if (pendingResult != null) {
            return AddFenPositionContinuationsDialogState.Result(pendingResult.toDialogResult())
        }

        return null
    }

    AddFenPositionContinuationsScreen(
        lineController = manualInput.lineController,
        state = AddFenPositionContinuationsScreenState(
            positionName = positionName,
            theme = positionTheme,
            manualSanLine = manualSanLine,
            text = textState.text,
            newContinuationSanLines = textResult?.newSanLines.orEmpty(),
            canUndoManualLine = manualInput.canUndo,
            canClearManualLine = manualInput.canClear,
            canSave = newUciLines().isNotEmpty(),
            dialogState = dialogState(),
        ),
        actions = AddFenPositionContinuationsScreenActions(
            onBackClick = ::requestBack,
            onManualBackClick = {
                manualInput.undo()
            },
            onManualClearClick = manualInput::clear,
            onTextChange = textState::updateText,
            onTextClearClick = textState::clearText,
            onSaveClick = {
                onSaveClick(newUciLines())
            },
            dialogActions = AddFenPositionContinuationsDialogActions(
                onResultDismiss = textState::dismissResult,
                onErrorDismiss = textState::dismissError,
                onDiscardStay = {
                    discardConfirmationVisible = false
                },
                onDiscardExit = {
                    discardConfirmationVisible = false
                    onBackClick()
                },
            ),
        ),
        modifier = modifier,
    )
}

private fun FenPositionContinuationTextProcessingResult.toDialogResult():
    AddFenPositionContinuationsProcessingResult {
    return AddFenPositionContinuationsProcessingResult(
        recognizedLinesCount = recognizedLinesCount,
        exactDuplicateLinesCount = exactDuplicateLinesCount,
        coveredPrefixLinesCount = coveredPrefixLinesCount,
        coveredByStoredLinesCount = coveredByStoredLinesCount,
        newLinesCount = newUciLines.size,
    )
}
