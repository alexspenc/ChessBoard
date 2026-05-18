package com.example.chessboard.service

/*
 * Unit tests for statistics-based training recommendation calculations.
 *
 * Keep pure formula and ordering coverage here using in-memory line/result
 * fixtures. Do not add Room integration tests or screen behavior tests here.
 *
 * Validation date: 2026-05-18
 */

import com.example.chessboard.entity.LineEntity
import com.example.chessboard.entity.TrainingResultEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsTrainingServiceTest {

    private val nowMillis = 10 * MillisPerDayForTest

    @Test
    fun `recommendations are ordered by score and include calculated weights`() {
        val recommendations = buildStatisticsTrainingRecommendation(
            allLines = listOf(line(1L), line(2L), line(3L)),
            recentResults = listOf(
                result(id = 1L, lineId = 1L, mistakesCount = 0, daysAgo = 1),
                result(id = 2L, lineId = 2L, mistakesCount = 2, daysAgo = 1),
                result(id = 3L, lineId = 3L, mistakesCount = 1, daysAgo = 5),
            ),
            nowMillis = nowMillis,
        )

        assertEquals(listOf(3L, 2L, 1L), recommendations.map { recommendation -> recommendation.line.id })
        assertEquals(listOf(5, 5, 1), recommendations.map { recommendation -> recommendation.weight })
        assertEquals(14.0, recommendations[0].score, 0.0001)
        assertEquals(10.0, recommendations[1].score, 0.0001)
        assertEquals(2.0, recommendations[2].score, 0.0001)
    }

    @Test
    fun `recommendations use only configured number of recent results for formula stats`() {
        val recommendations = buildStatisticsTrainingRecommendation(
            allLines = listOf(line(1L)),
            recentResults = listOf(
                result(id = 1L, lineId = 1L, mistakesCount = 0, daysAgo = 1),
                result(id = 2L, lineId = 1L, mistakesCount = 3, daysAgo = 2),
            ),
            nowMillis = nowMillis,
            formulaSettings = StatisticsTrainingFormulaSettings(recentResultsPerLine = 1),
        )

        val stats = recommendations.single().stats
        assertEquals(2, stats.attemptsCount)
        assertEquals(0, stats.mistakesLast)
        assertEquals(0.0, stats.avgMistakesRecent, 0.0001)
        assertEquals(1.0, stats.perfectRateRecent, 0.0001)
        assertEquals(0.0, recommendations.single().score, 0.0001)
    }

    @Test
    fun `min days filter keeps never trained lines and old enough trained lines`() {
        val recommendations = buildStatisticsTrainingRecommendation(
            allLines = listOf(line(1L), line(2L), line(3L)),
            recentResults = listOf(
                result(id = 1L, lineId = 1L, mistakesCount = 0, daysAgo = 1),
                result(id = 2L, lineId = 2L, mistakesCount = 0, daysAgo = 3),
            ),
            nowMillis = nowMillis,
            recommendationSettings = StatisticsTrainingRecommendationSettings(
                minDaysSinceLastTraining = 2,
            ),
        )

        assertEquals(listOf(2L, 3L), recommendations.map { recommendation -> recommendation.line.id })
    }

    @Test
    fun `custom formula settings change score and max weight`() {
        val recommendations = buildStatisticsTrainingRecommendation(
            allLines = listOf(line(1L)),
            recentResults = listOf(
                result(id = 1L, lineId = 1L, mistakesCount = 3, daysAgo = 4),
            ),
            nowMillis = nowMillis,
            recommendationSettings = StatisticsTrainingRecommendationSettings(
                maxWeight = 4,
            ),
            formulaSettings = StatisticsTrainingFormulaSettings(
                lastMistakeWeight = 1.0,
                avgMistakesWeight = 0.0,
                recencyWeight = 0.0,
                perfectRatePenaltyWeight = 0.0,
                oneAttemptBoost = 0.0,
                weight5ScoreThreshold = 5.0,
                weight4ScoreThreshold = 3.0,
                weight3ScoreThreshold = 2.0,
                weight2ScoreThreshold = 1.0,
            ),
        )

        assertEquals(3.0, recommendations.single().score, 0.0001)
        assertEquals(4, recommendations.single().weight)
    }

    @Test
    fun `limit is read from recommendation settings`() {
        val recommendations = buildStatisticsTrainingRecommendation(
            allLines = listOf(line(1L), line(2L), line(3L)),
            recentResults = emptyList(),
            nowMillis = nowMillis,
            recommendationSettings = StatisticsTrainingRecommendationSettings(
                limit = 2,
            ),
        )

        assertEquals(listOf(1L, 2L), recommendations.map { recommendation -> recommendation.line.id })
    }

    private fun line(id: Long): LineEntity {
        return LineEntity(
            id = id,
            pgn = "",
            initialFen = "",
        )
    }

    private fun result(
        id: Long,
        lineId: Long,
        mistakesCount: Int,
        daysAgo: Int,
    ): TrainingResultEntity {
        return TrainingResultEntity(
            id = id,
            lineId = lineId,
            mistakesCount = mistakesCount,
            trainedAt = nowMillis - daysAgo * MillisPerDayForTest,
        )
    }

    private companion object {
        const val MillisPerDayForTest = 24L * 60L * 60L * 1000L
    }
}
