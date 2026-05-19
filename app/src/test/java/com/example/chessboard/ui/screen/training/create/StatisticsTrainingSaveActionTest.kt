package com.example.chessboard.ui.screen.training.create

/*
 * Unit tests for statistics-training save decision logic.
 *
 * Keep pure save-action routing tests here so save-time validation can be verified
 * without Compose, Room, or navigation.
 * Do not add UI rendering assertions or recommendation formula calculations here.
 *
 * Validation date: 2026-05-18
 */

import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsTrainingSaveActionTest {

    @Test
    fun `selection with lines saves training`() {
        val action = resolveStatisticsTrainingSaveAction(
            hasLines = true,
        )

        assertEquals(StatisticsTrainingSaveAction.SaveTraining, action)
    }

    @Test
    fun `selection without lines shows empty training message`() {
        val action = resolveStatisticsTrainingSaveAction(
            hasLines = false,
        )

        assertEquals(StatisticsTrainingSaveAction.ShowEmptyTrainingMessage, action)
    }
}
