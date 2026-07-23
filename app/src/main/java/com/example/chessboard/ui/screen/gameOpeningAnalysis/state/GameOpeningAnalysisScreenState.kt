package com.example.chessboard.ui.screen.gameOpeningAnalysis.state

/*
 * File role: stores local Compose state holders for the game-opening analysis screen.
 * Allowed here:
 * - small state holder classes remembered by GameOpeningAnalysisScreen
 * - screen-local state that does not belong in runtime context or persistence
 * Not allowed here:
 * - UI rendering, runtime-context mutation, import/export execution, or analysis algorithms
 * Validation date: 2026-07-02
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.chessboard.runtimecontext.GameOpeningAnalysisFilter
import com.example.chessboard.runtimecontext.GameOpeningAnalysisOptions
import com.example.chessboard.runtimecontext.ImportGamesSummary
import com.example.chessboard.runtimecontext.ImportedGameItem
import com.example.chessboard.ui.screen.gameOpeningAnalysis.dialogs.GameOpeningAnalysisImportProgress
import kotlinx.coroutines.Job

internal sealed interface GameOpeningAnalysisRunMessage {
    data object FilterRequired : GameOpeningAnalysisRunMessage
    data object NoFilteredGames : GameOpeningAnalysisRunMessage
    data object NoResults : GameOpeningAnalysisRunMessage
}

internal class GameOpeningAnalysisDialogState {
    var showImportDialog by mutableStateOf(false)
    var showFilterDialog by mutableStateOf(false)
    var showAnalysisOptionsDialog by mutableStateOf(false)
    var showDeleteGameDialog by mutableStateOf(false)
    var showGameActionsDialog by mutableStateOf(false)
    var showDeleteFilteredGamesDialog by mutableStateOf(false)
    var showDeleteResultGamesDialog by mutableStateOf(false)
}

@Composable
internal fun rememberGameOpeningAnalysisDialogState(): GameOpeningAnalysisDialogState {
    return remember { GameOpeningAnalysisDialogState() }
}

internal class GameOpeningAnalysisExportState {
    var inProgress by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)
    var pendingGames by mutableStateOf<List<ImportedGameItem>>(emptyList())
    var pendingFileName by mutableStateOf("")
}

@Composable
internal fun rememberGameOpeningAnalysisExportState(): GameOpeningAnalysisExportState {
    return remember { GameOpeningAnalysisExportState() }
}

internal class GameOpeningAnalysisCopyPgnState {
    var inProgress by mutableStateOf(false)
    var copied by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
}

@Composable
internal fun rememberGameOpeningAnalysisCopyPgnState(): GameOpeningAnalysisCopyPgnState {
    return remember { GameOpeningAnalysisCopyPgnState() }
}

internal class GameOpeningAnalysisImportState {
    var job by mutableStateOf<Job?>(null)
    var progress by mutableStateOf<GameOpeningAnalysisImportProgress?>(null)
    var pgnText by mutableStateOf("")
    var summary by mutableStateOf<ImportGamesSummary?>(null)
    var fileErrorMessage by mutableStateOf<String?>(null)
}

@Composable
internal fun rememberGameOpeningAnalysisImportState(): GameOpeningAnalysisImportState {
    return remember { GameOpeningAnalysisImportState() }
}

internal class GameOpeningAnalysisDeviationMistakeState {
    var inProgress by mutableStateOf(false)
    var recordedLinesCount by mutableStateOf<Int?>(null)
    var errorMessage by mutableStateOf<String?>(null)
}

@Composable
internal fun rememberGameOpeningAnalysisDeviationMistakeState(): GameOpeningAnalysisDeviationMistakeState {
    return remember { GameOpeningAnalysisDeviationMistakeState() }
}

internal class GameOpeningAnalysisDraftState(
    initialFilter: GameOpeningAnalysisFilter,
    initialAnalysisOptions: GameOpeningAnalysisOptions,
) {
    var filter by mutableStateOf(initialFilter)
    var analysisOptions by mutableStateOf(initialAnalysisOptions)
}

@Composable
internal fun rememberGameOpeningAnalysisDraftState(
    initialFilter: GameOpeningAnalysisFilter,
    initialAnalysisOptions: GameOpeningAnalysisOptions,
): GameOpeningAnalysisDraftState {
    return remember {
        GameOpeningAnalysisDraftState(
            initialFilter = initialFilter,
            initialAnalysisOptions = initialAnalysisOptions,
        )
    }
}
