package com.example.chessboard.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Long = SINGLE_ROW_ID,
    val rankTier: String = "",
    val rankTitle: String = "",
    val simpleViewEnabled: Boolean = false,
    val removeLineIfRepIsZero: Boolean = true,
    val hideLinesWithWeightZero: Boolean = false,
    val hideSmartTrainingInfoCard: Boolean = false,
) {
    companion object {
        const val SINGLE_ROW_ID = 1L
    }
}
