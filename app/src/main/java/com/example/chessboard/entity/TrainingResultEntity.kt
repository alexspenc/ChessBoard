package com.example.chessboard.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "training_results",
    indices = [
        Index(value = ["gameId"]),
        Index(value = ["trainedAt"])
    ]
)
data class TrainingResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: Long,
    val mistakesCount: Int,
    val trainedAt: Long
)
