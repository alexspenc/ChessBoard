package com.example.chessboard.ui.screen.training.loadsave

/*
 * Shared mapping helpers for training-like editor load flows.
 *
 * Keep pure mapping from stored game ids and weights to editor items here so
 * training and template editors can reuse one persistence-to-UI conversion.
 * Do not add screen state, dialog code, or save orchestration to this file.
 */

import com.example.chessboard.entity.GameEntity
import com.example.chessboard.service.OneGameTrainingData
import com.example.chessboard.ui.screen.training.TrainingGameEditorItem
import com.example.chessboard.ui.screen.training.toTrainingGameEditorItem

internal fun buildTrainingEditorItems(
    allGames: List<GameEntity>,
    trainingGames: List<OneGameTrainingData>,
): List<TrainingGameEditorItem> {
    if (trainingGames.isEmpty()) {
        return emptyList()
    }

    val weightsByGameId = trainingGames.associate { trainingGame ->
        trainingGame.gameId to trainingGame.weight
    }

    return allGames.mapNotNull { game ->
        val weight = weightsByGameId[game.id] ?: return@mapNotNull null
        game.toTrainingGameEditorItem(weight = weight)
    }
}
