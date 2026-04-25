package com.example.chessboard.ui.screen.training.train

/*
 * Training-only launch actions for EditTrainingScreen.
 *
 * Keep random-start UI and per-game start-training action wiring here so a
 * future shared editor shell does not depend on training launch behavior. Do
 * not add generic save UI, load/save helpers, or shared editor scaffolding.
 */

import com.example.chessboard.ui.screen.training.common.TrainingEditorPrimaryAction
import com.example.chessboard.ui.screen.training.common.TrainingGameEditorItem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.rounded.PlayArrow

private fun requestTrainingLaunch(
    gameId: Long,
    orderedGameIds: List<Long>,
    moveRange: TrainingMoveRange,
    requestLeave: (() -> Unit) -> Unit,
    onStartGameTrainingClick: (Long, Int, Int, List<Long>) -> Unit,
) {
    requestLeave {
        onStartGameTrainingClick(gameId, moveRange.from, moveRange.to, orderedGameIds)
    }
}

internal fun createEditTrainingPrimaryAction(
    gameId: Long,
    orderedGameIds: List<Long>,
    moveRange: TrainingMoveRange,
    requestLeave: (() -> Unit) -> Unit,
    onStartGameTrainingClick: (Long, Int, Int, List<Long>) -> Unit,
): TrainingEditorPrimaryAction {
    return TrainingEditorPrimaryAction(
        onClick = {
            requestTrainingLaunch(
                gameId = gameId,
                orderedGameIds = orderedGameIds,
                moveRange = moveRange,
                requestLeave = requestLeave,
                onStartGameTrainingClick = onStartGameTrainingClick
            )
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
