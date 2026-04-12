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

enum class PlayerTier(val label: String, val symbol: String, val titles: List<String>) {
    Pawn(
        label = "Pawn",
        symbol = "♟",
        titles = listOf(
            "Walking Blunder",
            "Free Material",
            "\"Oops, hung it\"",
            "Pawn With Dreams",
            "Future Rage Quitter",
            "Center Feeder",
            "One-Move Genius",
            "Hope Chess Beginner",
            "Sacrifice Without Reason",
            "Still Learning Rules",
        )
    ),
    Knight(
        label = "Knight",
        symbol = "♞",
        titles = listOf(
            "Fork Victim",
            "L-Move Confusion",
            "Random Jumper",
            "\"That was calculated\"",
            "Hope Chess Player",
            "Missed the Fork",
            "Tactical Tourist",
            "Blunder Enthusiast",
            "Chaos Enjoyer",
            "Still Not Seeing It",
        )
    ),
    Bishop(
        label = "Bishop",
        symbol = "♝",
        titles = listOf(
            "Bad Bishop Owner",
            "Diagonal Pretender",
            "Long-Range Miss",
            "\"Didn't see that\"",
            "Still Hanging Pieces",
            "Color Complex Victim",
            "Passive Observer",
            "Fake Strategist",
            "Trapped Bishop Club",
            "Almost Improving",
        )
    ),
    Rook(
        label = "Rook",
        symbol = "♜",
        titles = listOf(
            "Open File Tourist",
            "Rook Hanger",
            "Endgame Thrower",
            "\"I Had This Won\"",
            "Almost Competent",
            "Back Rank Victim",
            "Late Game Blunderer",
            "Missed Mate Threat",
            "Panic Defender",
            "Somehow Winning",
        )
    ),
    Queen(
        label = "Queen",
        symbol = "♛",
        titles = listOf(
            "Queen Blunder Specialist",
            "\"It Was a Sacrifice\"",
            "Tilt Manager",
            "Calculation Optional",
            "One Move Genius",
            "Overconfident Player",
            "Attack Without Plan",
            "Fake Tactician",
            "Blunder Recovery Expert",
            "Chaos Creator",
        )
    ),
    King(
        label = "King",
        symbol = "♚",
        titles = listOf(
            "Blunders Mate in 1",
            "Self-Check Artist",
            "\"I Didn't See That\"",
            "Certified Thrower",
            "Still Not GM",
            "King in Danger",
            "Panic Mode Activated",
            "Last Hope Defender",
            "Barely Surviving",
            "Lucky Survivor",
        )
    ),
}

fun resolvePlayerTier(level: Int): PlayerTier = when {
    level <= 5  -> PlayerTier.Pawn
    level <= 15 -> PlayerTier.Knight
    level <= 25 -> PlayerTier.Bishop
    level <= 40 -> PlayerTier.Rook
    level <= 60 -> PlayerTier.Queen
    else        -> PlayerTier.King
}

// Returns the saved title if the tier matches, otherwise picks a new random one.
private fun resolveTitle(profile: UserProfileEntity, tier: PlayerTier): String {
    if (profile.rankTier == tier.name && profile.rankTitle.isNotEmpty()) {
        return profile.rankTitle
    }
    return tier.titles.random()
}

data class ProfileState(
    val level: Int = 1,
    val tier: PlayerTier = PlayerTier.Pawn,
    val rankTitle: String = PlayerTier.Pawn.titles.first(),
    val totalTrainings: Int = 0,
    val levelTrainingThreshold: Int = 10,
    val accuracy: Int = 0,
    val bestStreak: Int = 0,
    val achievements: List<AchievementItem> = defaultAchievements()
)

data class AchievementItem(
    val title: String,
    val description: String,
    val isUnlocked: Boolean = false
)

private data class ProfileLevelProgress(
    val level: Int,
    val nextLevelThreshold: Int
)

private const val BaseLevelThresholdStep = 10

private fun defaultAchievements() = listOf(
    AchievementItem("First Steps", "Complete your first training session"),
    AchievementItem("Dedication", "Complete 100 trainings"),
    AchievementItem("Opening Master", "Learn 10 different openings"),
    AchievementItem("Streak King", "Achieve a 5-move perfect streak"),
)

private fun resolveProfileLevelProgress(totalTrainingsCount: Int): ProfileLevelProgress {
    var level = 1
    var nextLevelThreshold = 0

    while (true) {
        val requiredTrainings = nextLevelThreshold + BaseLevelThresholdStep + level
        if (totalTrainingsCount < requiredTrainings) {
            return ProfileLevelProgress(
                level = level,
                nextLevelThreshold = requiredTrainings
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
        rankTitle = resolveTitle(profile, tier),
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
            title = "First Steps",
            description = "Complete your first training session",
            isUnlocked = stats.totalTrainingsCount >= 1,
        ),
        AchievementItem(
            title = "Dedication",
            description = "Complete 100 trainings",
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
        if (isLoaded) return

        isLoaded = true
        viewModelScope.launch {
            val (stats, profile) = withContext(Dispatchers.IO) {
                inDbProvider.getGlobalTrainingStats() to inDbProvider.getUserProfile()
            }
            val newState = buildProfileState(stats, profile)
            // Persist rank title to DB if tier changed
            if (profile.rankTier != newState.tier.name || profile.rankTitle != newState.rankTitle) {
                withContext(Dispatchers.IO) {
                    inDbProvider.updateUserProfileRankTitle(newState.tier.name, newState.rankTitle)
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
