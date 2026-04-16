package com.example.chessboard.ui.screen.training

/*
 * Training-only launch actions for EditTrainingScreen.
 *
 * Keep random-start UI and per-game start-training action wiring here so a
 * future shared editor shell does not depend on training launch behavior. Do
 * not add generic save UI, load/save helpers, or shared editor scaffolding.
 */

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
    moveRange: TrainingMoveRange,
    requestLeave: (() -> Unit) -> Unit,
    onStartGameTrainingClick: (Long, Int, Int) -> Unit,
) {
    requestLeave {
        onStartGameTrainingClick(gameId, moveRange.from, moveRange.to)
    }
}

@Composable
internal fun RenderEditTrainingRandomAction(
    games: List<TrainingGameEditorItem>,
    moveRange: TrainingMoveRange,
    requestLeave: (() -> Unit) -> Unit,
    onStartGameTrainingClick: (Long, Int, Int) -> Unit,
) {
    PrimaryButton(
        text = "Random",
        onClick = {
            val randomGameId = resolveRandomTrainingGameId(games) ?: return@PrimaryButton
            requestTrainingLaunch(
                gameId = randomGameId,
                moveRange = moveRange,
                requestLeave = requestLeave,
                onStartGameTrainingClick = onStartGameTrainingClick
            )
        }
    )
}

internal fun createEditTrainingPrimaryAction(
    gameId: Long,
    moveRange: TrainingMoveRange,
    requestLeave: (() -> Unit) -> Unit,
    onStartGameTrainingClick: (Long, Int, Int) -> Unit,
): TrainingEditorPrimaryAction {
    return TrainingEditorPrimaryAction(
        onClick = {
            requestTrainingLaunch(
                gameId = gameId,
                moveRange = moveRange,
                requestLeave = requestLeave,
                onStartGameTrainingClick = onStartGameTrainingClick
            )
        },
        icon = Icons.Rounded.PlayArrow,
        contentDescription = "Start training"
    )
}
