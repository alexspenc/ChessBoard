package com.example.chessboard.service

/*
 * File role: verifies Room-backed deviation mistake recording for game-opening analysis.
 * Allowed here:
 * - integration tests for GameOpeningAnalysisMistakeService persistence effects
 * - checks that analysis mistake recording does not update global training stats
 * Not allowed here:
 * - Compose UI behavior, runtime-context navigation, or chess-analysis algorithms
 * Validation date: 2026-07-01
 */

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.chessboard.entity.GlobalTrainingStatsEntity
import com.example.chessboard.repository.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GameOpeningAnalysisMistakeServiceTest {

    private lateinit var database: AppDatabase
    private lateinit var service: GameOpeningAnalysisMistakeService

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        service = GameOpeningAnalysisMistakeService(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // Checks that each affected opening line receives one training result with the provided mistake weight.
    @Test
    fun recordDeviationMistakeCreatesTrainingResultsForEachLine() = runBlocking {
        val recordedCount = service.recordDeviationMistake(
            lineIds = listOf(10L, 20L, 30L),
            mistakesCount = 3,
        )

        val resultsByLineId =
            database.trainingResultDao()
                .getRecentResults(limit = 10)
                .associateBy { result -> result.lineId }

        assertEquals(3, recordedCount)
        assertEquals(setOf(10L, 20L, 30L), resultsByLineId.keys)
        assertEquals(listOf(3, 3, 3), resultsByLineId.values.map { result -> result.mistakesCount })
    }

    // Checks that repeated line ids are recorded once so one position shared by many refs does not duplicate a line row.
    @Test
    fun recordDeviationMistakeDeduplicatesLineIds() = runBlocking {
        val recordedCount = service.recordDeviationMistake(
            lineIds = listOf(10L, 10L, 20L, 10L),
            mistakesCount = 2,
        )

        val results = database.trainingResultDao().getRecentResults(limit = 10)

        assertEquals(2, recordedCount)
        assertEquals(2, results.size)
        assertEquals(setOf(10L, 20L), results.map { result -> result.lineId }.toSet())
        assertEquals(listOf(2, 2), results.map { result -> result.mistakesCount })
    }

    // Checks that analysis mistake recording is not treated as completed training and leaves global stats unchanged.
    @Test
    fun recordDeviationMistakeDoesNotUpdateGlobalTrainingStats() = runBlocking {
        val initialStats = GlobalTrainingStatsEntity(
            totalTrainingsCount = 5,
            currentPerfectStreak = 2,
            bestPerfectStreak = 3,
            perfectTrainingsCount = 4,
        )
        database.globalTrainingStatsDao().upsert(initialStats)

        service.recordDeviationMistake(
            lineIds = listOf(10L, 20L),
            mistakesCount = 2,
        )

        assertEquals(initialStats, database.globalTrainingStatsDao().getById())
    }
}
