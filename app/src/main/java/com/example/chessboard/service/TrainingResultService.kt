package com.example.chessboard.service

import com.example.chessboard.entity.TrainingResultEntity
import com.example.chessboard.repository.AppDatabase

class TrainingResultService(
    private val database: AppDatabase
) {

    suspend fun addTrainingResult(
        gameId: Long,
        mistakesCount: Int,
        trainedAt: Long = System.currentTimeMillis()
    ): Long {
        return database.trainingResultDao().insertAndTrim(
            TrainingResultEntity(
                gameId = gameId,
                mistakesCount = mistakesCount,
                trainedAt = trainedAt
            )
        )
    }
}
