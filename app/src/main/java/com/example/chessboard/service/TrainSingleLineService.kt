package com.example.chessboard.service

import androidx.room.withTransaction
import com.example.chessboard.entity.GlobalTrainingStatsEntity
import com.example.chessboard.entity.LineEntity
import com.example.chessboard.entity.TrainingEntity
import com.example.chessboard.repository.AppDatabase

data class TrainingLineLaunchData(
    val trainingId: Long,
    val lineId: Long
)

data class TrainingStatsRecordResult(
    val totalTrainingsCount: Int,
    val newLevel: Int?,
)

// Describes all possible outcomes of resolving a selected training launch.
sealed interface TrainingLineLaunchResult

// The selected valid training/line pair was found and can be opened by the UI.
data class TrainingLineLaunchReady(
    val launchData: TrainingLineLaunchData
) : TrainingLineLaunchResult

// No trainings exist, so the training launch cannot open any line.
data object TrainingLineLaunchNoTrainings : TrainingLineLaunchResult

// The selected training was invalid, got cleaned up, and ended up being deleted.
data class TrainingLineLaunchBrokenTrainingDeleted(
    val trainingId: Long
) : TrainingLineLaunchResult

data class TrainingLineLaunchTrainingNotFound(
    val trainingId: Long
) : TrainingLineLaunchResult

data class TrainingLineLaunchLineNotFound(
    val trainingId: Long,
    val lineId: Long,
) : TrainingLineLaunchResult

data class TrainingLineLaunchLineRemovedFromTraining(
    val trainingId: Long,
    val lineId: Long,
) : TrainingLineLaunchResult

// Encapsulates the database operations needed by the single-line training flow.
class TrainSingleLineService(
    private val database: AppDatabase
) {

    // Loads the line that will be used by the single-line training session.
    suspend fun loadLine(lineId: Long): LineEntity? {
        return database.lineDao().getById(lineId)
    }

    // Records per-line and global stats for a completed training line.
    // Call this as soon as the line is finished, before the user presses Finish.
    suspend fun recordTrainingStats(lineId: Long, mistakesCount: Int): GlobalTrainingStatsEntity {
        val trainingResultService = TrainingResultService(database)
        val globalTrainingStatsService = GlobalTrainingStatsService(database)
        return database.withTransaction {
            trainingResultService.addTrainingResult(lineId = lineId, mistakesCount = mistakesCount)
            globalTrainingStatsService.recordTrainingResult(mistakesCount = mistakesCount)
        }
    }

    // Same as recordTrainingStats but also returns the new total count for completion-based prompts.
    suspend fun recordTrainingStatsCheckingLevelUp(
        lineId: Long,
        mistakesCount: Int,
    ): TrainingStatsRecordResult {
        val globalStatsService = GlobalTrainingStatsService(database)
        val levelBefore = resolveLevel(globalStatsService.getStats().totalTrainingsCount)
        val updatedStats = recordTrainingStats(lineId = lineId, mistakesCount = mistakesCount)
        val levelAfter = resolveLevel(updatedStats.totalTrainingsCount)
        val newLevel = if (levelAfter > levelBefore) levelAfter else null
        return TrainingStatsRecordResult(
            totalTrainingsCount = updatedStats.totalTrainingsCount,
            newLevel = newLevel,
        )
    }

    private fun resolveLevel(totalTrainings: Int): Int {
        var level = 1
        var threshold = 0
        while (true) {
            val required = if (level == 1) 3 else threshold + 10 + level
            if (totalTrainings < required) return level
            threshold = required
            level++
        }
    }

    // Decreases the weight of the trained line. Call this when the user confirms finish.
    suspend fun finishTraining(
        trainingId: Long,
        lineId: Long,
        mistakesCount: Int,
        keepLineIfZero: Boolean = false
    ): Boolean {
        val trainingService = TrainingService(
            database = database,
            lineDao = database.lineDao(),
            dao = database.trainingDao(),
            templateDao = database.trainingTemplateDao()
        )

        return trainingService.decreaseLineWeight(
            trainingId = trainingId,
            lineId = lineId,
            keepIfZero = keepLineIfZero
        )
    }

    suspend fun getTrainingLineLaunchData(trainingId: Long): TrainingLineLaunchResult {
        val training = database.trainingDao().getById(trainingId)
            ?: return TrainingLineLaunchTrainingNotFound(trainingId)

        return resolveTrainingLaunchData(training)
    }

    suspend fun getTrainingLineLaunchData(
        trainingId: Long,
        lineId: Long,
    ): TrainingLineLaunchResult {
        val training = database.trainingDao().getById(trainingId)
            ?: return TrainingLineLaunchTrainingNotFound(trainingId)

        val validatedTraining = validateTraining(training)
            ?: return TrainingLineLaunchBrokenTrainingDeleted(training.id)

        val trainingLines = OneLineTrainingData.fromJson(validatedTraining.linesJson)
        if (trainingLines.none { it.lineId == lineId }) {
            return TrainingLineLaunchLineRemovedFromTraining(
                trainingId = validatedTraining.id,
                lineId = lineId,
            )
        }

        val line = loadLine(lineId)
        if (line == null) {
            return TrainingLineLaunchLineNotFound(
                trainingId = validatedTraining.id,
                lineId = lineId,
            )
        }

        return TrainingLineLaunchReady(
            launchData = TrainingLineLaunchData(
                trainingId = validatedTraining.id,
                lineId = line.id,
            )
        )
    }

    private suspend fun resolveTrainingLaunchData(training: TrainingEntity): TrainingLineLaunchResult {
        val validatedTraining = validateTraining(training)
            ?: return TrainingLineLaunchBrokenTrainingDeleted(training.id)

        val firstLine = OneLineTrainingData.fromJson(validatedTraining.linesJson).firstOrNull()
            ?: return TrainingLineLaunchBrokenTrainingDeleted(validatedTraining.id)

        return TrainingLineLaunchReady(
            launchData = TrainingLineLaunchData(
                trainingId = validatedTraining.id,
                lineId = firstLine.lineId
            )
        )
    }

    private suspend fun validateTraining(training: TrainingEntity): TrainingEntity? {
        val trainingService = TrainingService(
            database = database,
            lineDao = database.lineDao(),
            dao = database.trainingDao(),
            templateDao = database.trainingTemplateDao()
        )

        return trainingService.validateTraining(training.id)
    }
}
