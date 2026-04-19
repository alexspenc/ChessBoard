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

import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import com.example.chessboard.ui.components.PrimaryButton

private fun resolveRandomTrainingGameId(
    games: List<TrainingGameEditorItem>
): Long? {
    if (games.isEmpty()) {
        return null
    }

    return games.random().gameId
}

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

@Composable
internal fun RenderEditTrainingRandomAction(
    games: List<TrainingGameEditorItem>,
    moveRange: TrainingMoveRange,
    requestLeave: (() -> Unit) -> Unit,
    onStartGameTrainingClick: (Long, Int, Int, List<Long>) -> Unit,
) {
    PrimaryButton(
        text = "Random",
        onClick = {
            val randomGameId = resolveRandomTrainingGameId(games) ?: return@PrimaryButton
            requestTrainingLaunch(
                gameId = randomGameId,
                orderedGameIds = games.map { it.gameId },
                moveRange = moveRange,
                requestLeave = requestLeave,
                onStartGameTrainingClick = onStartGameTrainingClick
            )
        }
    )
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
