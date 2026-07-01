package com.example.chessboard.service

/**
 * File role: records database-backed training-result signals created from game-opening analysis.
 * Allowed here:
 * - persistence helpers that translate analysis-derived line ids into training result rows
 * - database transactions for analysis-to-training statistics writes
 * Not allowed here:
 * - Compose UI, runtime screen state mutation, analyzer execution, or global training stats updates
 * Validation date: 2026-07-01
 */

import androidx.room.withTransaction
import com.example.chessboard.repository.AppDatabase

class GameOpeningAnalysisMistakeService(
    private val database: AppDatabase,
) {
    suspend fun recordDeviationMistake(
        lineIds: List<Long>,
        mistakesCount: Int,
    ): Int {
        require(mistakesCount > 0) {
            "mistakesCount must be positive"
        }

        val uniqueLineIds = lineIds.distinct()
        if (uniqueLineIds.isEmpty()) {
            return 0
        }

        val trainedAt = System.currentTimeMillis()
        val trainingResultService = TrainingResultService(database)
        database.withTransaction {
            uniqueLineIds.forEach { lineId ->
                trainingResultService.addTrainingResult(
                    lineId = lineId,
                    mistakesCount = mistakesCount,
                    trainedAt = trainedAt,
                )
            }
        }

        return uniqueLineIds.size
    }
}
