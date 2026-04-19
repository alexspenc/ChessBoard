package com.example.chessboard.ui.screen.training.common

/*
 * Shared editor models and list operations for training-like collections.
 *
 * Keep reusable item models and pure list helpers here so create/edit screens can
 * share them. Do not add screen-specific Compose UI, loading, or save flows here.
 */

import com.example.chessboard.entity.GameEntity
import com.example.chessboard.entity.SideMask

data class TrainingGameEditorItem(
    val gameId: Long,
    val title: String,
    val weight: Int = 1,
    val eco: String? = null,
    val pgn: String = "",
    val sideMask: Int = SideMask.BOTH
)

internal fun decreaseTrainingGameWeight(
    games: List<TrainingGameEditorItem>,
    gameId: Long
): List<TrainingGameEditorItem> {
    if (games.none { it.gameId == gameId }) {
        return games
    }

    return games.map { game ->
        if (game.gameId != gameId) {
            return@map game
        }

        return@map game.copy(weight = (game.weight - 1).coerceAtLeast(1))
    }
}

internal fun increaseTrainingGameWeight(
    games: List<TrainingGameEditorItem>,
    gameId: Long
): List<TrainingGameEditorItem> {
    if (games.none { it.gameId == gameId }) {
        return games
    }

    return games.map { game ->
        if (game.gameId != gameId) {
            return@map game
        }

        return@map game.copy(weight = game.weight + 1)
    }
}

internal fun removeTrainingGame(
    games: List<TrainingGameEditorItem>,
    gameId: Long
): List<TrainingGameEditorItem> {
    if (games.none { it.gameId == gameId }) {
        return games
    }

    return games.filterNot { game ->
        game.gameId == gameId
    }
}

internal fun GameEntity.toTrainingGameEditorItem(weight: Int = 1): TrainingGameEditorItem {
    return TrainingGameEditorItem(
        gameId = id,
        title = event ?: "Unnamed Opening",
        weight = weight,
        eco = eco,
        pgn = pgn,
        sideMask = sideMask
    )
}
