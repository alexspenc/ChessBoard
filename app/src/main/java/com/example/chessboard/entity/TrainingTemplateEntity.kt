package com.example.chessboard.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Training template.
 *
 * gamesJson format:
 * [
 *   {"gameId": 1, "weight": 3},
 *   {"gameId": 5, "weight": 1}
 * ]
 */
@Entity(tableName = "training_templates")
data class TrainingTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val gamesJson: String = "[]"
)
