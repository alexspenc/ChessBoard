package com.example.chessboard.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Long = SINGLE_ROW_ID,
    val rankTier: String = "",
    val rankTitle: String = "",
    val simpleViewEnabled: Boolean = true,
    val removeLineIfRepIsZero: Boolean = false,
    val hideLinesWithWeightZero: Boolean = false,
    val hideSmartTrainingInfoCard: Boolean = false,
    val smartMaxLines: Int = 10,
    val smartOnlyWithMistakes: Boolean = false,
    val autoNextLine: Boolean = false,
    val languageTag: String = "en",
    val disableSimpleViewUpgradePrompt: Boolean = false,
    val simpleViewUpgradePromptInterval: Int = 20,
) {
    companion object {
        const val SINGLE_ROW_ID = 1L
    }
}
