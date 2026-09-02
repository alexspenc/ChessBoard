package com.example.chessboard.ui.testtags.fenpositions

/*
 * File role: defines stable Compose test tags for adding FEN position continuations.
 * Allowed here:
 * - add-continuation screen, input, action, preview, and dialog tags
 * Not allowed here:
 * - tags for other screens, UI rendering, or test assertion helpers
 * Validation date: 2026-09-02
 */

const val FenPositionContinuationAddBoardTestTag = "fen-position-continuation-add-board"
const val FenPositionContinuationAddManualBackTestTag = "fen-position-continuation-add-manual-back"
const val FenPositionContinuationAddManualClearTestTag = "fen-position-continuation-add-manual-clear"
const val FenPositionContinuationAddTextInputTestTag = "fen-position-continuation-add-text-input"
const val FenPositionContinuationAddTextClearTestTag = "fen-position-continuation-add-text-clear"
const val FenPositionContinuationAddSaveTestTag = "fen-position-continuation-add-save"
const val FenPositionContinuationAddProcessingDialogTestTag =
    "fen-position-continuation-add-processing-dialog"
const val FenPositionContinuationAddResultDialogTestTag =
    "fen-position-continuation-add-result-dialog"
const val FenPositionContinuationAddResultConfirmTestTag =
    "fen-position-continuation-add-result-confirm"
const val FenPositionContinuationAddErrorDialogTestTag =
    "fen-position-continuation-add-error-dialog"
const val FenPositionContinuationAddDiscardDialogTestTag =
    "fen-position-continuation-add-discard-dialog"
const val FenPositionContinuationAddDiscardExitTestTag =
    "fen-position-continuation-add-discard-exit"
