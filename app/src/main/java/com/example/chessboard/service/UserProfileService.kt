package com.example.chessboard.service

import com.example.chessboard.entity.UserProfileEntity
import com.example.chessboard.repository.UserProfileDao

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
        dontRemoveLineIfRepIsZero: Boolean,
        hideLinesWithWeightZero: Boolean,
    ) {
        val current = getProfile()
        dao.upsertProfile(
            current.copy(
                simpleViewEnabled = simpleViewEnabled,
                dontRemoveLineIfRepIsZero = dontRemoveLineIfRepIsZero,
                hideLinesWithWeightZero = hideLinesWithWeightZero,
            )
        )
    }
}
