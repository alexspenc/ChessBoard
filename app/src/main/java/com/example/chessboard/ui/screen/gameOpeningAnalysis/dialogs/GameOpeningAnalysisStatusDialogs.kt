@file:Suppress("FunctionName")

package com.example.chessboard.ui.screen.gameOpeningAnalysis.dialogs

/*
 * File role: renders non-blocking status dialogs for the game-opening analysis screen.
 * Allowed here:
 * - message dialogs for import, export, and analysis status
 * - small presentation helpers used only by those status dialogs
 * Not allowed here:
 * - blocking progress dialogs, editor/action dialogs, file picker launchers, runtime-context mutation beyond supplied callbacks, or main screen content
 * Validation date: 2026-07-01
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.runtimecontext.GameOpeningAnalysisProgress
import com.example.chessboard.runtimecontext.ImportGamesSummary
import com.example.chessboard.ui.GameOpeningAnalysisImportSummaryDialogTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultPgnCopiedDialogTestTag
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.screen.gameOpeningAnalysis.state.GameOpeningAnalysisCopyPgnState
import com.example.chessboard.ui.screen.gameOpeningAnalysis.state.GameOpeningAnalysisDeviationMistakeState
import com.example.chessboard.ui.screen.gameOpeningAnalysis.state.GameOpeningAnalysisExportState
import com.example.chessboard.ui.screen.gameOpeningAnalysis.state.GameOpeningAnalysisImportState
import com.example.chessboard.ui.screen.gameOpeningAnalysis.state.GameOpeningAnalysisRunMessage

@Composable
internal fun GameOpeningAnalysisStatusDialogs(
    importState: GameOpeningAnalysisImportState,
    exportState: GameOpeningAnalysisExportState,
    copyPgnState: GameOpeningAnalysisCopyPgnState,
    deviationMistakeState: GameOpeningAnalysisDeviationMistakeState,
    analysisProgress: GameOpeningAnalysisProgress?,
    analysisRunMessage: GameOpeningAnalysisRunMessage?,
    onDismissAnalysisRunMessage: () -> Unit,
    onCancelAnalysis: () -> Unit,
) {
    val currentImportFileErrorMessage = importState.fileErrorMessage
    if (currentImportFileErrorMessage != null) {
        AppMessageDialog(
            title = stringResource(R.string.game_opening_analysis_import_failed_title),
            message = currentImportFileErrorMessage,
            onDismiss = { importState.fileErrorMessage = null },
        )
    }

    val currentExportErrorMessage = exportState.errorMessage
    if (currentExportErrorMessage != null) {
        AppMessageDialog(
            title = stringResource(R.string.game_opening_analysis_export_failed_title),
            message = currentExportErrorMessage,
            onDismiss = { exportState.errorMessage = null },
        )
    }

    val currentExportMessage = exportState.message
    if (currentExportMessage != null) {
        AppMessageDialog(
            title = stringResource(R.string.game_opening_analysis_export_saved_title),
            message = currentExportMessage,
            onDismiss = { exportState.message = null },
        )
    }

    val currentCopyPgnErrorMessage = copyPgnState.errorMessage
    if (currentCopyPgnErrorMessage != null) {
        AppMessageDialog(
            title = stringResource(R.string.game_opening_analysis_copy_game_pgn_failed_title),
            message = currentCopyPgnErrorMessage,
            onDismiss = { copyPgnState.errorMessage = null },
        )
    }

    if (copyPgnState.copied) {
        AppMessageDialog(
            title = stringResource(R.string.game_opening_analysis_copy_game_pgn_copied_title),
            message = stringResource(R.string.game_opening_analysis_copy_game_pgn_copied_message),
            onDismiss = { copyPgnState.copied = false },
            modifier = Modifier.testTag(GameOpeningAnalysisResultPgnCopiedDialogTestTag),
        )
    }

    val currentImportSummary = importState.summary
    if (currentImportSummary != null) {
        AppMessageDialog(
            title = stringResource(R.string.game_opening_analysis_import_summary_title),
            message = gameOpeningAnalysisImportSummaryMessage(currentImportSummary),
            onDismiss = { importState.summary = null },
            modifier = Modifier.testTag(GameOpeningAnalysisImportSummaryDialogTestTag),
        )
    }

    val currentDeviationMistakeErrorMessage = deviationMistakeState.errorMessage
    if (currentDeviationMistakeErrorMessage != null) {
        AppMessageDialog(
            title = stringResource(R.string.game_opening_analysis_record_deviation_mistake_failed_title),
            message = currentDeviationMistakeErrorMessage,
            onDismiss = { deviationMistakeState.errorMessage = null },
        )
    }

    val currentRecordedLinesCount = deviationMistakeState.recordedLinesCount
    if (currentRecordedLinesCount != null) {
        AppMessageDialog(
            title = stringResource(R.string.game_opening_analysis_record_deviation_mistake_saved_title),
            message =
                stringResource(
                    R.string.game_opening_analysis_record_deviation_mistake_saved_message,
                    currentRecordedLinesCount,
                ),
            onDismiss = { deviationMistakeState.recordedLinesCount = null },
        )
    }

    GameOpeningAnalysisBlockingDialogs(
        importState = importState,
        exportState = exportState,
        deviationMistakeState = deviationMistakeState,
        analysisProgress = analysisProgress,
        onCancelAnalysis = onCancelAnalysis,
    )

    val currentAnalysisRunMessage = analysisRunMessage
    if (currentAnalysisRunMessage != null) {
        AppMessageDialog(
            title = analysisRunMessageTitle(currentAnalysisRunMessage),
            message = analysisRunMessageBody(currentAnalysisRunMessage),
            onDismiss = onDismissAnalysisRunMessage,
        )
    }
}

@Composable
private fun gameOpeningAnalysisImportSummaryMessage(summary: ImportGamesSummary): String {
    return listOf(
        stringResource(R.string.game_opening_analysis_import_summary_scanned, summary.scannedCount),
        stringResource(R.string.game_opening_analysis_import_summary_added, summary.addedCount),
        stringResource(
            R.string.game_opening_analysis_import_summary_skipped_duplicates,
            summary.skippedDuplicateCount,
        ),
        stringResource(
            R.string.game_opening_analysis_import_summary_skipped_parse_errors,
            summary.skippedParseErrorCount,
        ),
    ).joinToString(separator = "\n")
}

@Composable
private fun analysisRunMessageTitle(message: GameOpeningAnalysisRunMessage): String =
    when (message) {
        GameOpeningAnalysisRunMessage.FilterRequired -> {
            stringResource(R.string.game_opening_analysis_filter_required_title)
        }

        GameOpeningAnalysisRunMessage.NoFilteredGames -> {
            stringResource(R.string.game_opening_analysis_no_filtered_games_title)
        }

        GameOpeningAnalysisRunMessage.NoResults -> {
            stringResource(R.string.game_opening_analysis_no_results_title)
        }
    }

@Composable
private fun analysisRunMessageBody(message: GameOpeningAnalysisRunMessage): String =
    when (message) {
        GameOpeningAnalysisRunMessage.FilterRequired -> {
            stringResource(R.string.game_opening_analysis_filter_required_message)
        }

        GameOpeningAnalysisRunMessage.NoFilteredGames -> {
            stringResource(R.string.game_opening_analysis_no_filtered_games_message)
        }

        GameOpeningAnalysisRunMessage.NoResults -> {
            stringResource(R.string.game_opening_analysis_no_results_message)
        }
    }
