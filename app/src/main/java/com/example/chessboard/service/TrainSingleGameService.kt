package com.example.chessboard.service

import com.example.chessboard.entity.GameEntity
import com.example.chessboard.entity.TrainingEntity
import com.example.chessboard.repository.AppDatabase

data class FirstTrainingGameLaunchData(
    val trainingId: Long,
    val gameId: Long
)

// Describes all possible outcomes of resolving the temporary "start first training" flow.
sealed interface FirstTrainingGameLaunchResult

// The first valid training/game pair was found and can be opened by the UI.
data class FirstTrainingGameLaunchReady(
    val launchData: FirstTrainingGameLaunchData
) : FirstTrainingGameLaunchResult

// No trainings exist, so the temporary start flow cannot open any game.
data object FirstTrainingGameLaunchNoTrainings : FirstTrainingGameLaunchResult

// The first training was invalid, got cleaned up, and ended up being deleted.
data class FirstTrainingGameLaunchBrokenTrainingDeleted(
    val trainingId: Long
) : FirstTrainingGameLaunchResult

// Encapsulates the database operations needed by the single-game training flow.
class TrainSingleGameService(
    private val database: AppDatabase
) {

    // Loads the game that will be used by the single-game training session.
    suspend fun loadGame(gameId: Long): GameEntity? {
        return database.gameDao().getById(gameId)
    }

    // Applies the training result by decreasing the weight of the trained line.
    suspend fun finishTraining(trainingId: Long, gameId: Long): Boolean {
        val trainingService = TrainingService(
            database = database,
            gameDao = database.gameDao(),
            dao = database.trainingDao(),
            templateDao = database.trainingTemplateDao()
        )

        return trainingService.decreaseLineWeight(
            trainingId = trainingId,
            gameId = gameId
        )
    }

    // Resolves the first valid training/game pair after validating the first training.
    suspend fun getFirstTrainingGameLaunchData(): FirstTrainingGameLaunchResult {
        val firstTraining = database.trainingDao().getFirst() ?: return FirstTrainingGameLaunchNoTrainings
        val validatedTraining = validateTraining(firstTraining)
            ?: return FirstTrainingGameLaunchBrokenTrainingDeleted(firstTraining.id)

        val firstGame = OneGameTrainingData.fromJson(validatedTraining.gamesJson).firstOrNull()
            ?: return FirstTrainingGameLaunchBrokenTrainingDeleted(validatedTraining.id)

        return FirstTrainingGameLaunchReady(
            launchData = FirstTrainingGameLaunchData(
                trainingId = validatedTraining.id,
                gameId = firstGame.gameId
            )
        )
    }

    private suspend fun validateTraining(training: TrainingEntity): TrainingEntity? {
        val trainingService = TrainingService(
            database = database,
            gameDao = database.gameDao(),
            dao = database.trainingDao(),
            templateDao = database.trainingTemplateDao()
        )

        return trainingService.validateTraining(training.id)
    }
}
