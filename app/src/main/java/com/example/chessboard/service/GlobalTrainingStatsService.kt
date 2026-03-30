package com.example.chessboard.service

import kotlin.math.max
import com.example.chessboard.entity.GlobalTrainingStatsEntity
import com.example.chessboard.repository.AppDatabase

class GlobalTrainingStatsService(
    private val database: AppDatabase
) {

    suspend fun getStats(): GlobalTrainingStatsEntity {
        val existingStats = database.globalTrainingStatsDao().getById()
        if (existingStats != null) {
            return existingStats
        }

        val defaultStats = GlobalTrainingStatsEntity()
        database.globalTrainingStatsDao().upsert(defaultStats)
        return defaultStats
    }

    suspend fun recordTrainingResult(mistakesCount: Int): GlobalTrainingStatsEntity {
        val currentStats = getStats()
        val totalTrainingsCount = currentStats.totalTrainingsCount + 1

        if (mistakesCount != 0) {
            val updatedStats = currentStats.copy(
                totalTrainingsCount = totalTrainingsCount,
                currentPerfectStreak = 0
            )
            database.globalTrainingStatsDao().upsert(updatedStats)
            return updatedStats
        }

        val currentPerfectStreak = currentStats.currentPerfectStreak + 1
        val updatedStats = currentStats.copy(
            totalTrainingsCount = totalTrainingsCount,
            currentPerfectStreak = currentPerfectStreak,
            bestPerfectStreak = max(currentStats.bestPerfectStreak, currentPerfectStreak),
            perfectTrainingsCount = currentStats.perfectTrainingsCount + 1
        )
        database.globalTrainingStatsDao().upsert(updatedStats)
        return updatedStats
    }
}
