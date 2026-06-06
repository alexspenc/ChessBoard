package com.example.chessboard.ui.screen.training.loadsave

/*
 * Shared mapping helpers for training-like editor load flows.
 *
 * Keep pure mapping from stored line ids and weights to editor items here so
 * training and template editors can reuse one persistence-to-UI conversion.
 * Do not add screen state, dialog code, or save orchestration to this file.
 */

import com.example.chessboard.ui.screen.training.common.TrainingLineEditorItem
import com.example.chessboard.ui.screen.training.common.toTrainingLineEditorItem

import com.example.chessboard.entity.LineEntity
import com.example.chessboard.service.OneLineTrainingData

internal fun buildTrainingEditorItems(
    allLines: List<LineEntity>,
    trainingLines: List<OneLineTrainingData>,
    unnamedOpeningName: String,
): List<TrainingLineEditorItem> {
    if (trainingLines.isEmpty()) {
        return emptyList()
    }

    val weightsByLineId = trainingLines.associate { trainingLine ->
        trainingLine.lineId to trainingLine.weight
    }

    return allLines.mapNotNull { line ->
        val weight = weightsByLineId[line.id] ?: return@mapNotNull null
        line.toTrainingLineEditorItem(weight = weight, unnamedOpeningName = unnamedOpeningName)
    }
}
