package com.example.chessboard.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chessboard.entity.GlobalTrainingStatsEntity
import com.example.chessboard.entity.UserProfileEntity
import com.example.chessboard.repository.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun resolvePlayerTier(level: Int): PlayerTier = when {
    level <= 1  -> PlayerTier.Pawn
    level <= 3  -> PlayerTier.Knight
    level <= 6  -> PlayerTier.Bishop
    level <= 10 -> PlayerTier.Rook
    level <= 15 -> PlayerTier.Queen
    else        -> PlayerTier.King
}

// Returns the saved title id if the tier matches, otherwise picks a new random one.
private fun resolveTitleId(profile: UserProfileEntity, tier: PlayerTier): ProfileRankTitleId {
    if (profile.rankTier == tier.name) {
        val savedTitleId = ProfileLocalization.rankTitleIdFromStorage(profile.rankTitle)
        if (savedTitleId != null && savedTitleId in tier.titleIds) {
            return savedTitleId
        }
    }

    return tier.titleIds.random()
}

data class ProfileState(
    val level: Int = 1,
    val tier: PlayerTier = PlayerTier.Pawn,
    val rankTitleId: ProfileRankTitleId = PlayerTier.Pawn.titleIds.first(),
    val totalTrainings: Int = 0,
    val levelTrainingThreshold: Int = 10,
    val accuracy: Int = 0,
    val bestStreak: Int = 0,
    val achievements: List<AchievementItem> = defaultAchievements(),
)

data class AchievementItem(
    val id: ProfileAchievementId,
    val isUnlocked: Boolean = false,
)

private data class ProfileLevelProgress(
    val level: Int,
    val nextLevelThreshold: Int,
)

private const val BaseLevelThresholdStep = 10

private fun defaultAchievements() = listOf(
    AchievementItem(ProfileAchievementId.FirstSteps),
    AchievementItem(ProfileAchievementId.Dedication),
    AchievementItem(ProfileAchievementId.OpeningMaster),
    AchievementItem(ProfileAchievementId.StreakKing),
)

private const val FirstLevelThreshold = 3

private fun resolveProfileLevelProgress(totalTrainingsCount: Int): ProfileLevelProgress {
    var level = 1
    var nextLevelThreshold = 0

    while (true) {
        var requiredTrainings = nextLevelThreshold + BaseLevelThresholdStep + level
        if (level == 1) {
            requiredTrainings = FirstLevelThreshold
        }
        if (totalTrainingsCount < requiredTrainings) {
            return ProfileLevelProgress(
                level = level,
                nextLevelThreshold = requiredTrainings,
            )
        }

        nextLevelThreshold = requiredTrainings
        level += 1
    }
}

private fun buildProfileState(
    stats: GlobalTrainingStatsEntity,
    profile: UserProfileEntity,
): ProfileState {
    val totalTrainingsCount = stats.totalTrainingsCount
    val levelProgress = resolveProfileLevelProgress(totalTrainingsCount)
    val tier = resolvePlayerTier(levelProgress.level)

    return ProfileState(
        level = levelProgress.level,
        tier = tier,
        rankTitleId = resolveTitleId(profile, tier),
        totalTrainings = totalTrainingsCount,
        levelTrainingThreshold = levelProgress.nextLevelThreshold,
        accuracy = resolveAccuracy(stats),
        bestStreak = stats.bestPerfectStreak,
        achievements = buildAchievements(stats),
    )
}

private fun resolveAccuracy(stats: GlobalTrainingStatsEntity): Int {
    if (stats.totalTrainingsCount == 0) return 0
    return stats.perfectTrainingsCount * 100 / stats.totalTrainingsCount
}

private fun buildAchievements(stats: GlobalTrainingStatsEntity): List<AchievementItem> {
    return listOf(
        AchievementItem(
            id = ProfileAchievementId.FirstSteps,
            isUnlocked = stats.totalTrainingsCount >= 1,
        ),
        AchievementItem(
            id = ProfileAchievementId.Dedication,
            isUnlocked = stats.totalTrainingsCount >= 100,
        ),
        AchievementItem(
            id = ProfileAchievementId.OpeningMaster,
            isUnlocked = stats.perfectTrainingsCount >= 10,
        ),
        AchievementItem(
            id = ProfileAchievementId.StreakKing,
            isUnlocked = stats.bestPerfectStreak >= 5,
        ),
    )
}

class ProfileViewModel : ViewModel() {
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private var isLoaded = false

    fun loadStats(inDbProvider: DatabaseProvider) {
        if (isLoaded) return

        isLoaded = true
        viewModelScope.launch {
            val userProfileService = inDbProvider.createUserProfileService()
            val (stats, profile) = withContext(Dispatchers.IO) {
                inDbProvider.getGlobalTrainingStats() to userProfileService.getProfile()
            }
            val newState = buildProfileState(stats, profile)
            // Persist rank title id to DB if tier changed or a legacy title was found.
            if (profile.rankTier != newState.tier.name || profile.rankTitle != newState.rankTitleId.storageKey) {
                withContext(Dispatchers.IO) {
                    userProfileService.updateRankTitle(newState.tier.name, newState.rankTitleId.storageKey)
                }
            }
            _state.value = newState
        }
    }

    fun clearAllData(inDbProvider: DatabaseProvider) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { inDbProvider.clearAllData() }
            isLoaded = true
            _state.value = buildProfileState(GlobalTrainingStatsEntity(), UserProfileEntity())
        }
    }
}
