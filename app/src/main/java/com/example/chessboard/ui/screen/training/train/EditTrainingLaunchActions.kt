package com.example.chessboard.ui.screen.training.train

/*
 * Training-only launch actions for EditTrainingScreen.
 *
 * Keep per-game start-training action wiring and the analyze action here so
 * the shared editor shell does not depend on training launch behavior. Do not
 * add generic save UI, load/save helpers, or shared editor scaffolding.
 */

import com.example.chessboard.ui.screen.training.common.TrainingEditorPrimaryAction

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.rounded.PlayArrow

internal fun createEditTrainingPrimaryAction(
    gameId: Long,
    orderedGameIds: List<Long>,
    requestLeave: (() -> Unit) -> Unit,
    onStartGameTrainingClick: (Long, List<Long>) -> Unit,
): TrainingEditorPrimaryAction {
    return TrainingEditorPrimaryAction(
        onClick = {
            requestLeave {
                onStartGameTrainingClick(gameId, orderedGameIds)
            }
        },
        icon = Icons.Rounded.PlayArrow,
        contentDescription = "Start training"
    )
}

internal fun createEditTrainingAnalyzeAction(
    uciMoves: List<String>,
    currentPly: Int,
    requestLeave: (() -> Unit) -> Unit,
    onAnalyzeGameClick: (List<String>, Int) -> Unit,
): TrainingEditorPrimaryAction {
    return TrainingEditorPrimaryAction(
        onClick = {
            requestLeave {
                onAnalyzeGameClick(uciMoves, currentPly.coerceIn(0, uciMoves.size))
            }
        },
        icon = Icons.Default.Analytics,
        contentDescription = "Analyze game",
    )
}
