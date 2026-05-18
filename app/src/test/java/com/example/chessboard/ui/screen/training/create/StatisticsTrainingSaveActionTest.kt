package com.example.chessboard.ui.screen.training.create

/*
 * Unit tests for statistics-training save decision logic.
 *
 * Keep pure save-action routing tests here so refresh-before-save behavior can be
 * verified without Compose, Room, or navigation.
 * Do not add UI rendering assertions or recommendation formula calculations here.
 *
 * Validation date: 2026-05-18
 */

import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsTrainingSaveActionTest {

    @Test
    fun `outdated selection with lines refreshes before save`() {
        val action = resolveStatisticsTrainingSaveAction(
            isSelectionOutOfDate = true,
            hasLines = true,
        )

        assertEquals(StatisticsTrainingSaveAction.RefreshSelection, action)
    }

    @Test
    fun `outdated selection without lines refreshes before empty message`() {
        val action = resolveStatisticsTrainingSaveAction(
            isSelectionOutOfDate = true,
            hasLines = false,
        )

        assertEquals(StatisticsTrainingSaveAction.RefreshSelection, action)
    }

    @Test
    fun `fresh selection without lines shows empty training message`() {
        val action = resolveStatisticsTrainingSaveAction(
            isSelectionOutOfDate = false,
            hasLines = false,
        )

        assertEquals(StatisticsTrainingSaveAction.ShowEmptyTrainingMessage, action)
    }

    @Test
    fun `fresh selection with lines saves training`() {
        val action = resolveStatisticsTrainingSaveAction(
            isSelectionOutOfDate = false,
            hasLines = true,
        )

        assertEquals(StatisticsTrainingSaveAction.SaveTraining, action)
    }
}
