package com.example.chessboard.service

import com.example.chessboard.entity.UserProfileEntity
import com.example.chessboard.repository.UserProfileDao

const val SimpleViewUpgradePromptIntervalMin = 20
const val SimpleViewUpgradePromptIntervalMax = 100
const val SimpleViewUpgradePromptIntervalDefault = 20

fun clampSimpleViewUpgradePromptInterval(interval: Int): Int {
    return interval.coerceIn(
        SimpleViewUpgradePromptIntervalMin,
        SimpleViewUpgradePromptIntervalMax,
    )
}

class UserProfileService(private val dao: UserProfileDao) {

    suspend fun getProfile(): UserProfileEntity {
        return dao.getProfile() ?: UserProfileEntity()
    }

    suspend fun updateRankTitle(tier: String, title: String) {
        val current = getProfile()
        dao.upsertProfile(current.copy(rankTier = tier, rankTitle = title))
    }

    suspend fun updateSettings(
        simpleViewEnabled: Boolean,
        removeLineIfRepIsZero: Boolean,
        hideLinesWithWeightZero: Boolean,
    ) {
        val current = getProfile()
        dao.upsertProfile(
            current.copy(
                simpleViewEnabled = simpleViewEnabled,
                removeLineIfRepIsZero = removeLineIfRepIsZero,
                hideLinesWithWeightZero = hideLinesWithWeightZero,
            )
        )
    }

    suspend fun setHideSmartTrainingInfoCard(hide: Boolean) {
        val current = getProfile()
        dao.upsertProfile(current.copy(hideSmartTrainingInfoCard = hide))
    }

    suspend fun updateSmartSettings(maxLines: Int, onlyWithMistakes: Boolean) {
        val current = getProfile()
        dao.upsertProfile(current.copy(smartMaxLines = maxLines, smartOnlyWithMistakes = onlyWithMistakes))
    }

    suspend fun updateAutoNextLine(enabled: Boolean) {
        val current = getProfile()
        dao.upsertProfile(current.copy(autoNextLine = enabled))
    }

    suspend fun updateLanguageTag(languageTag: String) {
        val current = getProfile()
        dao.upsertProfile(current.copy(languageTag = languageTag))
    }

    suspend fun updateSimpleViewUpgradePromptSettings(
        disabled: Boolean,
        interval: Int,
    ) {
        val current = getProfile()
        dao.upsertProfile(
            current.copy(
                disableSimpleViewUpgradePrompt = disabled,
                simpleViewUpgradePromptInterval = clampSimpleViewUpgradePromptInterval(interval),
            )
        )
    }
}
