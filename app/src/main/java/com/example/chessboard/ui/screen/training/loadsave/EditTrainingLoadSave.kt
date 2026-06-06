package com.example.chessboard.ui.screen.training.loadsave

/*
 * Load/save helpers for the edit-training screen.
 *
 * Keep screen-specific loading state, save result models, and persistence-backed
 * load/save helpers here so EditTrainingScreen can stay focused on UI and local
 * screen orchestration. Do not add dialogs or broader screen layout code here.
 */

import com.example.chessboard.ui.screen.training.common.TrainingLineEditorItem

import com.example.chessboard.entity.LineEntity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.OneLineTrainingData
import com.example.chessboard.service.TrainingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class TrainingLoadState(
    val trainingName: String = "",
    val linesForTraining: List<TrainingLineEditorItem> = emptyList(),
    val allLinesById: Map<Long, LineEntity> = emptyMap(),
    val trainingLoadFailed: Boolean = false,
)

internal suspend fun loadEditTrainingState(
    inDbProvider: DatabaseProvider,
    trainingService: TrainingService,
    trainingId: Long,
    defaultTrainingName: String,
    unnamedOpeningName: String,
): TrainingLoadState {
    val allLines = withContext(Dispatchers.IO) {
        inDbProvider.getAllLines()
    }

    val training = withContext(Dispatchers.IO) {
        trainingService.getTrainingById(trainingId)
    } ?: return TrainingLoadState(
        trainingName = defaultTrainingName,
        linesForTraining = emptyList(),
        trainingLoadFailed = true,
    )

    return TrainingLoadState(
        trainingName = training.name.ifBlank { defaultTrainingName },
        linesForTraining = buildTrainingEditorItems(
            allLines = allLines,
            trainingLines = OneLineTrainingData.fromJson(training.linesJson),
            unnamedOpeningName = unnamedOpeningName,
        ),
        allLinesById = allLines.associateBy { line -> line.id },
    )
}

internal suspend fun saveEditedTraining(
    trainingService: TrainingService,
    trainingId: Long,
    trainingName: String,
    editableLines: List<TrainingLineEditorItem>,
    defaultTrainingName: String,
): TrainingSaveSuccess? {
    val normalizedName = normalizeTrainingEditorName(
        trainingName = trainingName,
        defaultName = defaultTrainingName,
    )
    val trainingLines = editableLines.map { line ->
        OneLineTrainingData(
            lineId = line.lineId,
            weight = line.weight,
        )
    }

    val wasUpdated = withContext(Dispatchers.IO) {
        trainingService.updateTrainingFromLines(
            trainingId = trainingId,
            name = normalizedName,
            lines = trainingLines,
        )
    }

    if (!wasUpdated) {
        return null
    }

    return TrainingSaveSuccess(
        trainingId = trainingId,
        trainingName = normalizedName,
        linesCount = editableLines.size,
    )
}
