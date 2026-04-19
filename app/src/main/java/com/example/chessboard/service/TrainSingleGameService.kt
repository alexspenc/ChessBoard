package com.example.chessboard.service

import androidx.room.withTransaction
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.entity.TrainingEntity
import com.example.chessboard.repository.AppDatabase

data class TrainingGameLaunchData(
    val trainingId: Long,
    val gameId: Long
)

// Describes all possible outcomes of resolving a selected training launch.
sealed interface TrainingGameLaunchResult

// The selected valid training/game pair was found and can be opened by the UI.
data class TrainingGameLaunchReady(
    val launchData: TrainingGameLaunchData
) : TrainingGameLaunchResult

// No trainings exist, so the training launch cannot open any game.
data object TrainingGameLaunchNoTrainings : TrainingGameLaunchResult

// The selected training was invalid, got cleaned up, and ended up being deleted.
data class TrainingGameLaunchBrokenTrainingDeleted(
    val trainingId: Long
) : TrainingGameLaunchResult

data class TrainingGameLaunchTrainingNotFound(
    val trainingId: Long
) : TrainingGameLaunchResult

// Encapsulates the database operations needed by the single-game training flow.
class TrainSingleGameService(
    private val database: AppDatabase
) {

    // Loads the game that will be used by the single-game training session.
    suspend fun loadGame(gameId: Long): GameEntity? {
        return database.gameDao().getById(gameId)
    }

    // Records per-game and global stats for a completed training line.
    // Call this as soon as the line is finished, before the user presses Finish.
    suspend fun recordTrainingStats(gameId: Long, mistakesCount: Int) {
        val trainingResultService = TrainingResultService(database)
        val globalTrainingStatsService = GlobalTrainingStatsService(database)
        database.withTransaction {
            trainingResultService.addTrainingResult(gameId = gameId, mistakesCount = mistakesCount)
            globalTrainingStatsService.recordTrainingResult(mistakesCount = mistakesCount)
        }
    }

    // Decreases the weight of the trained line. Call this when the user confirms finish.
    suspend fun finishTraining(
        trainingId: Long,
        gameId: Long,
        mistakesCount: Int,
        keepLineIfZero: Boolean = false
    ): Boolean {
        val trainingService = TrainingService(
            database = database,
            gameDao = database.gameDao(),
            dao = database.trainingDao(),
            templateDao = database.trainingTemplateDao()
        )

        return trainingService.decreaseLineWeight(
            trainingId = trainingId,
            gameId = gameId,
            keepIfZero = keepLineIfZero
        )
    }

    suspend fun getTrainingGameLaunchData(trainingId: Long): TrainingGameLaunchResult {
        val training = database.trainingDao().getById(trainingId)
            ?: return TrainingGameLaunchTrainingNotFound(trainingId)

        return resolveTrainingLaunchData(training)
    }

    private suspend fun resolveTrainingLaunchData(training: TrainingEntity): TrainingGameLaunchResult {
        val validatedTraining = validateTraining(training)
            ?: return TrainingGameLaunchBrokenTrainingDeleted(training.id)

        val firstGame = OneGameTrainingData.fromJson(validatedTraining.gamesJson).firstOrNull()
            ?: return TrainingGameLaunchBrokenTrainingDeleted(validatedTraining.id)

        return TrainingGameLaunchReady(
            launchData = TrainingGameLaunchData(
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
