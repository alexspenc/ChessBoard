package com.example.chessboard.ui.screen.training.loadsave

/*
 * Load/save helpers for the edit-training screen.
 *
 * Keep screen-specific loading state, save result models, and persistence-backed
 * load/save helpers here so EditTrainingScreen can stay focused on UI and local
 * screen orchestration. Do not add dialogs or broader screen layout code here.
 */

import com.example.chessboard.entity.GameEntity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.OneGameTrainingData
import com.example.chessboard.service.TrainingService
import com.example.chessboard.ui.screen.training.DEFAULT_TRAINING_NAME
import com.example.chessboard.ui.screen.training.TrainingGameEditorItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class TrainingLoadState(
    val trainingName: String = DEFAULT_TRAINING_NAME,
    val gamesForTraining: List<TrainingGameEditorItem> = emptyList(),
    val allGamesById: Map<Long, GameEntity> = emptyMap(),
    val trainingLoadFailed: Boolean = false,
)

internal suspend fun loadEditTrainingState(
    inDbProvider: DatabaseProvider,
    trainingService: TrainingService,
    trainingId: Long,
): TrainingLoadState {
    val allGames = withContext(Dispatchers.IO) {
        inDbProvider.getAllGames()
    }

    val training = withContext(Dispatchers.IO) {
        trainingService.getTrainingById(trainingId)
    } ?: return TrainingLoadState(
        trainingName = DEFAULT_TRAINING_NAME,
        gamesForTraining = emptyList(),
        trainingLoadFailed = true,
    )

    return TrainingLoadState(
        trainingName = training.name.ifBlank { DEFAULT_TRAINING_NAME },
        gamesForTraining = buildTrainingEditorItems(
            allGames = allGames,
            trainingGames = OneGameTrainingData.fromJson(training.gamesJson),
        ),
        allGamesById = allGames.associateBy { game -> game.id },
    )
}

internal suspend fun saveEditedTraining(
    trainingService: TrainingService,
    trainingId: Long,
    trainingName: String,
    editableGames: List<TrainingGameEditorItem>,
): TrainingSaveSuccess? {
    val normalizedName = normalizeTrainingEditorName(trainingName)
    val trainingGames = editableGames.map { game ->
        OneGameTrainingData(
            gameId = game.gameId,
            weight = game.weight,
        )
    }

    val wasUpdated = withContext(Dispatchers.IO) {
        trainingService.updateTrainingFromGames(
            trainingId = trainingId,
            name = normalizedName,
            games = trainingGames,
        )
    }

    if (!wasUpdated) {
        return null
    }

    return TrainingSaveSuccess(
        trainingId = trainingId,
        trainingName = normalizedName,
        gamesCount = editableGames.size,
    )
}
