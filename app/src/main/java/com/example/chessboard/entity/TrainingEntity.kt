package com.example.chessboard.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trainings")
data class TrainingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val gamesJson: String = "[]"
)
