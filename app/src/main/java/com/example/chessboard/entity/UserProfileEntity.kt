package com.example.chessboard.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Long = SINGLE_ROW_ID,
    val rankTier: String = "",
    val rankTitle: String = "",
    val simpleViewEnabled: Boolean = false,
    val dontRemoveLineIfRepIsZero: Boolean = false,
    val hideLinesWithWeightZero: Boolean = false,
) {
    companion object {
        const val SINGLE_ROW_ID = 1L
    }
}
