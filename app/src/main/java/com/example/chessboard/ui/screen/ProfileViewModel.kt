package com.example.chessboard.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chessboard.entity.GlobalTrainingStatsEntity
import com.example.chessboard.repository.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProfileState(
    val userName: String = "Chess Enthusiast",
    val level: Int = 1,
    val totalMoves: Int = 0,
    val levelMoveThreshold: Int = 10,
    val accuracy: Int = 0,
    val bestStreak: Int = 0,
    val achievements: List<AchievementItem> = defaultAchievements()
)

data class AchievementItem(
    val title: String,
    val description: String,
    val isUnlocked: Boolean = false
)

private fun defaultAchievements() = listOf(
    AchievementItem("First Steps", "Complete your first training session"),
    AchievementItem("Dedication", "Practice 100 moves"),
    AchievementItem("Opening Master", "Learn 10 different openings"),
    AchievementItem("Streak King", "Achieve a 5-move perfect streak"),
)

private fun buildProfileState(stats: GlobalTrainingStatsEntity): ProfileState {
    val totalTrainingsCount = stats.totalTrainingsCount
    val level = totalTrainingsCount / 10 + 1
    val levelMoveThreshold = level * 10
    val accuracy = resolveAccuracy(stats)

    return ProfileState(
        level = level,
        totalMoves = totalTrainingsCount,
        levelMoveThreshold = levelMoveThreshold,
        accuracy = accuracy,
        bestStreak = stats.bestPerfectStreak,
        achievements = buildAchievements(stats),
    )
}

private fun resolveAccuracy(stats: GlobalTrainingStatsEntity): Int {
    if (stats.totalTrainingsCount == 0) {
        return 0
    }

    return stats.perfectTrainingsCount * 100 / stats.totalTrainingsCount
}

private fun buildAchievements(stats: GlobalTrainingStatsEntity): List<AchievementItem> {
    return listOf(
        AchievementItem(
            title = "First Steps",
            description = "Complete your first training session",
            isUnlocked = stats.totalTrainingsCount >= 1,
        ),
        AchievementItem(
            title = "Dedication",
            description = "Practice 100 moves",
            isUnlocked = stats.totalTrainingsCount >= 100,
        ),
        AchievementItem(
            title = "Opening Master",
            description = "Learn 10 different openings",
            isUnlocked = stats.perfectTrainingsCount >= 10,
        ),
        AchievementItem(
            title = "Streak King",
            description = "Achieve a 5-move perfect streak",
            isUnlocked = stats.bestPerfectStreak >= 5,
        ),
    )
}

class ProfileViewModel : ViewModel() {
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private var isLoaded = false

    fun loadStats(inDbProvider: DatabaseProvider) {
        if (isLoaded) {
            return
        }

        isLoaded = true
        viewModelScope.launch {
            val stats = inDbProvider.getGlobalTrainingStats()
            _state.value = buildProfileState(stats)
        }
    }

    fun clearAllData(inDbProvider: DatabaseProvider) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                inDbProvider.clearAllData()
            }
            isLoaded = true
            _state.value = buildProfileState(GlobalTrainingStatsEntity())
        }
    }
}
