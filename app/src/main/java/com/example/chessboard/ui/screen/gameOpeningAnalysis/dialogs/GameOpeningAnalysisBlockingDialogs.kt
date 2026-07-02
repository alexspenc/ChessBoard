@file:Suppress("FunctionName")

package com.example.chessboard.ui.screen.gameOpeningAnalysis.dialogs

/*
 * File role: renders blocking dialogs for active game-opening analysis operations.
 * Allowed here:
 * - modal progress/loading dialogs that prevent interaction while work is running
 * - cancel callbacks for active blocking operations supplied by the screen
 * Not allowed here:
 * - status/result message dialogs, editor dialogs, persistence work, or operation launch logic
 * Validation date: 2026-07-01
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.runtimecontext.GameOpeningAnalysisProgress
import com.example.chessboard.ui.GameOpeningAnalysisExportProgressDialogTestTag
import com.example.chessboard.ui.GameOpeningAnalysisRecordDeviationMistakeProgressTestTag
import com.example.chessboard.ui.components.AppLoadingDialog
import com.example.chessboard.ui.screen.gameOpeningAnalysis.state.GameOpeningAnalysisDeviationMistakeState
import com.example.chessboard.ui.screen.gameOpeningAnalysis.state.GameOpeningAnalysisExportState
import com.example.chessboard.ui.screen.gameOpeningAnalysis.state.GameOpeningAnalysisImportState

@Composable
internal fun GameOpeningAnalysisBlockingDialogs(
    importState: GameOpeningAnalysisImportState,
    exportState: GameOpeningAnalysisExportState,
    deviationMistakeState: GameOpeningAnalysisDeviationMistakeState,
    analysisProgress: GameOpeningAnalysisProgress?,
    onCancelAnalysis: () -> Unit,
) {
    GameOpeningAnalysisProgressDialog(
        progress = analysisProgress,
        onCancel = onCancelAnalysis,
    )

    GameOpeningAnalysisImportProgressDialog(
        progress = importState.progress,
        onCancel = { importState.job?.cancel() },
    )

    if (exportState.inProgress) {
        AppLoadingDialog(
            title = stringResource(R.string.game_opening_analysis_export_progress_title),
            message =
                stringResource(
                    R.string.game_opening_analysis_export_progress_message,
                    exportState.pendingGames.size,
                ),
            modifier = Modifier.testTag(GameOpeningAnalysisExportProgressDialogTestTag),
        )
    }

    if (deviationMistakeState.inProgress) {
        AppLoadingDialog(
            title = stringResource(R.string.game_opening_analysis_record_deviation_mistake_progress_title),
            message = stringResource(R.string.game_opening_analysis_record_deviation_mistake_progress_message),
            modifier = Modifier.testTag(GameOpeningAnalysisRecordDeviationMistakeProgressTestTag),
        )
    }
}
