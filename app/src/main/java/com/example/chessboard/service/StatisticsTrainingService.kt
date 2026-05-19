package com.example.chessboard.service

/*
 * Builds statistics-based training recommendations from persisted lines and
 * training results.
 *
 * Keep database-backed loading and formula application here. Do not add
 * composable UI state, navigation flow, or screen-specific editor behavior.
 *
 * Validation date: 2026-05-18
 */

import com.example.chessboard.entity.LineEntity
import com.example.chessboard.entity.StatisticsTrainingFormulaSettingsEntity
import com.example.chessboard.entity.TrainingResultEntity
import com.example.chessboard.repository.AppDatabase
import com.example.chessboard.repository.TrainingResultDao
import kotlin.math.min

private const val MillisPerDay = 24L * 60L * 60L * 1000L
private val DefaultStatisticsTrainingFormulaSettings = StatisticsTrainingFormulaSettingsEntity()
private val DefaultStatisticsTrainingRecommendationSettings = StatisticsTrainingRecommendationSettings()

data class LineTrainingStats(
    val attemptsCount: Int,
    val lastTrainedAt: Long?,
    val mistakesLast: Int,
    val avgMistakesRecent: Double,
    val perfectRateRecent: Double,
    val daysSinceLastTraining: Int,
)

data class StatisticsTrainingRecommendationItem(
    val line: LineEntity,
    val weight: Int,
    val score: Double,
    val stats: LineTrainingStats,
)

data class StatisticsTrainingRecommendationSettings(
    /** Number of recommended lines requested by the current selection run. */
    val limit: Int = 50,
    /** Minimum days since last training required for a trained line to be included. */
    val minDaysSinceLastTraining: Int = 0,
    /** Highest weight that can be assigned to a recommended line for this run. */
    val maxWeight: Int = 2,
)

class StatisticsTrainingService(
    private val database: AppDatabase,
) {

    suspend fun getRecommendation(
        recommendationSettings: StatisticsTrainingRecommendationSettings = DefaultStatisticsTrainingRecommendationSettings,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<StatisticsTrainingRecommendationItem> {
        val formulaSettings = StatisticsTrainingFormulaSettingsService(
            dao = database.statisticsTrainingFormulaSettingsDao(),
        ).getSettings()

        return buildStatisticsTrainingRecommendation(
            allLines = database.lineDao().getAllLines(),
            recentResults = database.trainingResultDao().getRecentResults(TrainingResultDao.MAX_TRAINING_RESULTS),
            nowMillis = nowMillis,
            recommendationSettings = recommendationSettings,
            formulaSettings = formulaSettings,
        )
    }
}

internal fun buildStatisticsTrainingRecommendation(
    allLines: List<LineEntity>,
    recentResults: List<TrainingResultEntity>,
    nowMillis: Long = System.currentTimeMillis(),
    recommendationSettings: StatisticsTrainingRecommendationSettings = DefaultStatisticsTrainingRecommendationSettings,
    formulaSettings: StatisticsTrainingFormulaSettingsEntity = DefaultStatisticsTrainingFormulaSettings,
): List<StatisticsTrainingRecommendationItem> {
    val resultsByLineId = recentResults
        .groupBy { result -> result.lineId }
        .mapValues { (_, results) ->
            results.sortedWith(compareByDescending<TrainingResultEntity> { it.trainedAt }.thenByDescending { it.id })
        }

    return allLines
        .map { line ->
            val allLineResults = resultsByLineId[line.id].orEmpty()
            val recentLineResults = allLineResults.take(formulaSettings.recentResultsPerLine.coerceAtLeast(1))
            val stats = buildLineStats(
                results = recentLineResults,
                attemptsCount = allLineResults.size,
                nowMillis = nowMillis,
            )
            val score = computeNeedScore(
                stats = stats,
                formulaSettings = formulaSettings,
            )
            StatisticsTrainingRecommendationItem(
                line = line,
                weight = mapScoreToWeight(
                    score = score,
                    recommendationSettings = recommendationSettings,
                    formulaSettings = formulaSettings,
                ),
                score = score,
                stats = stats,
            )
        }
        .filter { recommendation ->
            shouldIncludeRecommendation(
                stats = recommendation.stats,
                minDaysSinceLastTraining = recommendationSettings.minDaysSinceLastTraining,
            )
        }
        .sortedWith(
            compareByDescending<StatisticsTrainingRecommendationItem> { it.score }
                .thenBy { it.stats.attemptsCount }
                .thenByDescending { it.stats.daysSinceLastTraining }
                .thenBy { it.line.id }
        )
        .take(recommendationSettings.limit.coerceAtLeast(1))
}

private fun buildLineStats(
    results: List<TrainingResultEntity>,
    attemptsCount: Int,
    nowMillis: Long,
): LineTrainingStats {
    val lastResult = results.firstOrNull()
    val lastTrainedAt = lastResult?.trainedAt
    val avgMistakesRecent = resolveAvgMistakesRecent(results)
    val perfectRateRecent = resolvePerfectRateRecent(results)

    return LineTrainingStats(
        attemptsCount = attemptsCount,
        lastTrainedAt = lastTrainedAt,
        mistakesLast = lastResult?.mistakesCount ?: 0,
        avgMistakesRecent = avgMistakesRecent,
        perfectRateRecent = perfectRateRecent,
        daysSinceLastTraining = resolveDaysSinceLastTraining(lastTrainedAt, nowMillis),
    )
}

private fun resolveAvgMistakesRecent(results: List<TrainingResultEntity>): Double {
    if (results.isEmpty()) {
        return 0.0
    }

    return results.map { result -> result.mistakesCount }.average()
}

private fun resolvePerfectRateRecent(results: List<TrainingResultEntity>): Double {
    if (results.isEmpty()) {
        return 0.0
    }

    return results.count { result -> result.mistakesCount == 0 }.toDouble() / results.size.toDouble()
}

private fun shouldIncludeRecommendation(
    stats: LineTrainingStats,
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

private fun computeNeedScore(
    stats: LineTrainingStats,
    formulaSettings: StatisticsTrainingFormulaSettingsEntity,
): Double {
    val recencyDays = min(stats.daysSinceLastTraining, formulaSettings.recencyDaysCap.coerceAtLeast(0))
    return formulaSettings.lastMistakeWeight * min(stats.mistakesLast, formulaSettings.maxMistakesLast.coerceAtLeast(0)) +
        formulaSettings.avgMistakesWeight * min(stats.avgMistakesRecent, formulaSettings.maxAvgMistakesRecent.coerceAtLeast(0.0)) +
        formulaSettings.recencyWeight * recencyDays +
        resolveLowAttemptsBoost(
            attemptsCount = stats.attemptsCount,
            formulaSettings = formulaSettings,
        ) -
        formulaSettings.perfectRatePenaltyWeight * stats.perfectRateRecent
}

private fun resolveLowAttemptsBoost(
    attemptsCount: Int,
    formulaSettings: StatisticsTrainingFormulaSettingsEntity,
): Double {
    if (attemptsCount <= 0) {
        return formulaSettings.noAttemptsBoost
    }
    if (attemptsCount == 1) {
        return formulaSettings.oneAttemptBoost
    }
    if (attemptsCount == 2) {
        return formulaSettings.twoAttemptsBoost
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

private fun mapScoreToWeight(
    score: Double,
    recommendationSettings: StatisticsTrainingRecommendationSettings,
    formulaSettings: StatisticsTrainingFormulaSettingsEntity,
): Int {
    val safeMaxWeight = recommendationSettings.maxWeight.coerceAtLeast(1)
    val rawWeight = when {
        score >= formulaSettings.weight5ScoreThreshold -> 5
        score >= formulaSettings.weight4ScoreThreshold -> 4
        score >= formulaSettings.weight3ScoreThreshold -> 3
        score >= formulaSettings.weight2ScoreThreshold -> 2
        else -> 1
    }
    return rawWeight.coerceAtMost(safeMaxWeight)
}
