package com.example.chessboard.service

import com.example.chessboard.entity.TrainingResultEntity
import com.example.chessboard.repository.AppDatabase
import com.example.chessboard.repository.LineMistakesTotal

class TrainingResultService(
    private val database: AppDatabase
) {

    suspend fun addTrainingResult(
        lineId: Long,
        mistakesCount: Int,
        trainedAt: Long = System.currentTimeMillis()
    ): Long {
        return database.trainingResultDao().insertAndTrim(
            TrainingResultEntity(
                lineId = lineId,
                mistakesCount = mistakesCount,
                trainedAt = trainedAt
            )
        )
    }
    suspend fun getRecentResults(limit: Int): List<TrainingResultEntity> {
        return database.trainingResultDao().getRecentResults(limit)
    }

    suspend fun getResultsForLine(lineId: Long, limit: Int): List<TrainingResultEntity> {
        return database.trainingResultDao().getResultsForLine(
            lineId = lineId,
            limit = limit
        )
    }

    suspend fun getLineMistakeTotals(): List<LineMistakesTotal> {
        return database.trainingResultDao().getLineMistakeTotals()
    }
}
