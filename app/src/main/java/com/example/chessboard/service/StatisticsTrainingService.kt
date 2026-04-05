package com.example.chessboard.service

import com.example.chessboard.entity.GameEntity
import com.example.chessboard.entity.TrainingResultEntity
import com.example.chessboard.repository.AppDatabase
import com.example.chessboard.repository.TrainingResultDao
import kotlin.math.min

private const val RecentResultsPerGame = 5
private const val MaxStatisticsTrainingGames = 50
private const val RecencyDaysCap = 7
private const val MillisPerDay = 24L * 60L * 60L * 1000L

data class GameTrainingStats(
    val attemptsCount: Int,
    val lastTrainedAt: Long?,
    val mistakesLast: Int,
    val avgMistakesRecent: Double,
    val perfectRateRecent: Double,
    val daysSinceLastTraining: Int,
)

data class StatisticsTrainingRecommendationItem(
    val game: GameEntity,
    val weight: Int,
    val score: Double,
    val stats: GameTrainingStats,
)

class StatisticsTrainingService(
    private val database: AppDatabase,
) {

    suspend fun getRecommendation(
        limit: Int = MaxStatisticsTrainingGames,
        minDaysSinceLastTraining: Int = 0,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<StatisticsTrainingRecommendationItem> {
        return buildRecommendation(
            allGames = database.gameDao().getAllGames(),
            recentResults = database.trainingResultDao().getRecentResults(TrainingResultDao.MAX_TRAINING_RESULTS),
            nowMillis = nowMillis,
            limit = limit,
            minDaysSinceLastTraining = minDaysSinceLastTraining,
        )
    }

    private fun buildRecommendation(
        allGames: List<GameEntity>,
        recentResults: List<TrainingResultEntity>,
        nowMillis: Long = System.currentTimeMillis(),
        limit: Int = MaxStatisticsTrainingGames,
        minDaysSinceLastTraining: Int = 0,
    ): List<StatisticsTrainingRecommendationItem> {
        val resultsByGameId = recentResults
            .groupBy { result -> result.gameId }
            .mapValues { (_, results) ->
                results.sortedWith(compareByDescending<TrainingResultEntity> { it.trainedAt }.thenByDescending { it.id })
            }

        return allGames
            .map { game ->
                val allGameResults = resultsByGameId[game.id].orEmpty()
                val recentGameResults = allGameResults.take(RecentResultsPerGame)
                val stats = buildGameStats(
                    results = recentGameResults,
                    attemptsCount = allGameResults.size,
                    nowMillis = nowMillis,
                )
                val score = computeNeedScore(stats)
                StatisticsTrainingRecommendationItem(
                    game = game,
                    weight = mapScoreToWeight(score),
                    score = score,
                    stats = stats,
                )
            }
            .filter { recommendation ->
                shouldIncludeRecommendation(
                    stats = recommendation.stats,
                    minDaysSinceLastTraining = minDaysSinceLastTraining,
                )
            }
            .sortedWith(
                compareByDescending<StatisticsTrainingRecommendationItem> { it.score }
                    .thenBy { it.stats.attemptsCount }
                    .thenByDescending { it.stats.daysSinceLastTraining }
                    .thenBy { it.game.id }
            )
            .take(limit.coerceIn(1, MaxStatisticsTrainingGames))
    }

    private fun buildGameStats(
        results: List<TrainingResultEntity>,
        attemptsCount: Int,
        nowMillis: Long,
    ): GameTrainingStats {
        val lastResult = results.firstOrNull()
        val lastTrainedAt = lastResult?.trainedAt
        val avgMistakesRecent = if (results.isEmpty()) {
            0.0
        } else {
            results.map { result -> result.mistakesCount }.average()
        }
        val perfectRateRecent = if (results.isEmpty()) {
            0.0
        } else {
            results.count { result -> result.mistakesCount == 0 }.toDouble() / results.size.toDouble()
        }

        return GameTrainingStats(
            attemptsCount = attemptsCount,
            lastTrainedAt = lastTrainedAt,
            mistakesLast = lastResult?.mistakesCount ?: 0,
            avgMistakesRecent = avgMistakesRecent,
            perfectRateRecent = perfectRateRecent,
            daysSinceLastTraining = resolveDaysSinceLastTraining(lastTrainedAt, nowMillis),
        )
    }

    private fun shouldIncludeRecommendation(
        stats: GameTrainingStats,
        minDaysSinceLastTraining: Int,
    ): Boolean {
        if (minDaysSinceLastTraining <= 0) {
            return true
        }

        if (stats.lastTrainedAt == null) {
            return true
        }

        return stats.daysSinceLastTraining >= minDaysSinceLastTraining
    }

    private fun computeNeedScore(stats: GameTrainingStats): Double {
        val recencyDays = min(stats.daysSinceLastTraining, RecencyDaysCap)
        return 4.0 * min(stats.mistakesLast, 3) +
            2.0 * min(stats.avgMistakesRecent, 3.0) +
            2.0 * recencyDays +
            resolveLowAttemptsBoost(stats.attemptsCount) -
            2.0 * stats.perfectRateRecent
    }

    private fun resolveLowAttemptsBoost(attemptsCount: Int): Double {
        if (attemptsCount <= 0) {
            return 3.0
        }
        if (attemptsCount == 1) {
            return 2.0
        }
        if (attemptsCount == 2) {
            return 1.0
        }
        return 0.0
    }

    private fun resolveDaysSinceLastTraining(lastTrainedAt: Long?, nowMillis: Long): Int {
        if (lastTrainedAt == null) {
            return 0
        }

        val deltaMillis = (nowMillis - lastTrainedAt).coerceAtLeast(0L)
        return (deltaMillis / MillisPerDay).toInt()
    }

    private fun mapScoreToWeight(score: Double): Int {
        if (score >= 10.0) {
            return 5
        }
        if (score >= 7.0) {
            return 4
        }
        if (score >= 4.0) {
            return 3
        }
        if (score >= 2.0) {
            return 2
        }
        return 1
    }
}
