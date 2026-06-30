package com.example.chessboard.ui.screen.gameOpeningAnalysis

/*
 * File role: stores local Compose state holders for the game-opening analysis screen.
 * Allowed here:
 * - small state holder classes remembered by GameOpeningAnalysisScreen
 * - screen-local state that does not belong in runtime context or persistence
 * Not allowed here:
 * - UI rendering, runtime-context mutation, import/export execution, or analysis algorithms
 * Validation date: 2026-06-30
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.chessboard.runtimecontext.ImportedGameItem

internal class GameOpeningAnalysisDialogState {
    var showImportDialog by mutableStateOf(false)
    var showFilterDialog by mutableStateOf(false)
    var showAnalysisOptionsDialog by mutableStateOf(false)
    var showDeleteGameDialog by mutableStateOf(false)
    var showGameActionsDialog by mutableStateOf(false)
    var showDeleteFilteredGamesDialog by mutableStateOf(false)
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
