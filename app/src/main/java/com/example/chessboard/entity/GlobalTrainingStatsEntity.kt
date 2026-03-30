package com.example.chessboard.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "global_training_stats")
data class GlobalTrainingStatsEntity(
    @PrimaryKey
    val id: Long = SINGLE_ROW_ID,
    val totalTrainingsCount: Int = 0,
    val currentPerfectStreak: Int = 0,
    val bestPerfectStreak: Int = 0,
    val perfectTrainingsCount: Int = 0
) {
    companion object {
        const val SINGLE_ROW_ID = 1L
    }
}
