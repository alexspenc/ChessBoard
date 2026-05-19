package com.example.chessboard.ui.screen.training.create

/*
 * Unit tests for statistics-training formula threshold editing helpers.
 *
 * Keep ordering and clamping checks for formula-settings UI helpers here.
 * Do not add recommendation calculation, Room persistence, or Compose rendering tests.
 *
 * Validation date: 2026-05-19
 */

import com.example.chessboard.entity.StatisticsTrainingFormulaSettingsEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsTrainingFormulaThresholdTest {

    @Test
    fun `weight 5 threshold stays above weight 4 threshold`() {
        val settings = StatisticsTrainingFormulaSettingsEntity(
            weight5ScoreThreshold = 10.0,
            weight4ScoreThreshold = 7.0,
        )

        val updated = updateWeight5ScoreThreshold(
            settings = settings,
            value = 6.9,
        )

        assertEquals(7.1, updated.weight5ScoreThreshold, 0.0001)
    }

    @Test
    fun `weight 4 threshold stays between weight 5 and weight 3 thresholds`() {
        val settings = StatisticsTrainingFormulaSettingsEntity(
            weight5ScoreThreshold = 10.0,
            weight4ScoreThreshold = 7.0,
            weight3ScoreThreshold = 4.0,
        )

        val tooHigh = updateWeight4ScoreThreshold(
            settings = settings,
            value = 10.0,
        )
        val tooLow = updateWeight4ScoreThreshold(
            settings = settings,
            value = 4.0,
        )

        assertEquals(9.9, tooHigh.weight4ScoreThreshold, 0.0001)
        assertEquals(4.1, tooLow.weight4ScoreThreshold, 0.0001)
    }

    @Test
    fun `weight 3 threshold stays between weight 4 and weight 2 thresholds`() {
        val settings = StatisticsTrainingFormulaSettingsEntity(
            weight4ScoreThreshold = 7.0,
            weight3ScoreThreshold = 4.0,
            weight2ScoreThreshold = 2.0,
        )

        val tooHigh = updateWeight3ScoreThreshold(
            settings = settings,
            value = 7.0,
        )
        val tooLow = updateWeight3ScoreThreshold(
            settings = settings,
            value = 2.0,
        )

        assertEquals(6.9, tooHigh.weight3ScoreThreshold, 0.0001)
        assertEquals(2.1, tooLow.weight3ScoreThreshold, 0.0001)
    }

    @Test
    fun `weight 2 threshold stays below weight 3 threshold and nonnegative`() {
        val settings = StatisticsTrainingFormulaSettingsEntity(
            weight3ScoreThreshold = 4.0,
            weight2ScoreThreshold = 2.0,
        )

        val tooHigh = updateWeight2ScoreThreshold(
            settings = settings,
            value = 4.0,
        )
        val tooLow = updateWeight2ScoreThreshold(
            settings = settings,
            value = -1.0,
        )

        assertEquals(3.9, tooHigh.weight2ScoreThreshold, 0.0001)
        assertEquals(0.0, tooLow.weight2ScoreThreshold, 0.0001)
    }
}
