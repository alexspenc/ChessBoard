package com.example.chessboard.runtimecontext

/*
 * Unit tests for statistics-training runtime-only selection state.
 *
 * Keep snapshot, formula-revision, and temporary recommendation-settings tests here.
 * Do not add Room persistence, recommendation calculation, or Compose UI behavior here.
 *
 * Validation date: 2026-05-18
 */

import com.example.chessboard.service.StatisticsTrainingRecommendationSettings
import com.example.chessboard.ui.screen.training.common.CreateTrainingEditorState
import com.example.chessboard.ui.screen.training.common.TrainingLineEditorItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsTrainingRuntimeContextTest {

    @Test
    fun `rememberLoadedSelection stores loaded editor state settings and formula revision`() {
        val runtimeContext = StatisticsTrainingRuntimeContext()
        val editorState = CreateTrainingEditorState(
            trainingName = "Statistics Training",
            currentPage = 2,
            editableLinesForTraining = listOf(
                TrainingLineEditorItem(lineId = 10L, title = "Line 10", weight = 3),
            ),
        )
        val settings = StatisticsTrainingRecommendationSettings(
            limit = 12,
            minDaysSinceLastTraining = 3,
            maxWeight = 4,
        )
        runtimeContext.updateRecommendationSettings(settings)
        runtimeContext.markFormulaChanged()

        runtimeContext.rememberLoadedSelection(
            newEditorState = editorState,
            settings = settings,
        )

        assertEquals(editorState, runtimeContext.editorState)
        assertEquals(editorState, runtimeContext.loadedEditorState)
        assertEquals(settings, runtimeContext.recommendationSettings)
        assertEquals(settings, runtimeContext.loadedRecommendationSettings)
        assertEquals(1, runtimeContext.loadedFormulaRevision)
        assertFalse(runtimeContext.isSelectionOutOfDate())
        assertTrue(runtimeContext.hasLoadedSelection)
    }

    @Test
    fun `markFormulaChanged makes loaded selection formula revision outdated`() {
        val runtimeContext = StatisticsTrainingRuntimeContext()
        runtimeContext.rememberLoadedSelection(
            newEditorState = CreateTrainingEditorState(),
            settings = StatisticsTrainingRecommendationSettings(),
        )

        runtimeContext.markFormulaChanged()

        assertEquals(1, runtimeContext.formulaRevision)
        assertEquals(0, runtimeContext.loadedFormulaRevision)
        assertTrue(runtimeContext.isSelectionOutOfDate())
    }

    @Test
    fun `isLoadedFormulaOutOfDate is false before selection is loaded`() {
        val runtimeContext = StatisticsTrainingRuntimeContext()

        runtimeContext.markFormulaChanged()

        assertFalse(runtimeContext.isLoadedFormulaOutOfDate())
    }

    @Test
    fun `isLoadedFormulaOutOfDate detects loaded selection from older formula revision`() {
        val runtimeContext = StatisticsTrainingRuntimeContext()
        runtimeContext.rememberLoadedSelection(
            newEditorState = CreateTrainingEditorState(),
            settings = StatisticsTrainingRecommendationSettings(),
        )

        runtimeContext.markFormulaChanged()

        assertTrue(runtimeContext.isLoadedFormulaOutOfDate())
    }

    @Test
    fun `rememberLoadedSelection updates loaded formula revision after refresh`() {
        val runtimeContext = StatisticsTrainingRuntimeContext()
        runtimeContext.rememberLoadedSelection(
            newEditorState = CreateTrainingEditorState(),
            settings = StatisticsTrainingRecommendationSettings(),
        )
        runtimeContext.markFormulaChanged()

        runtimeContext.rememberLoadedSelection(
            newEditorState = CreateTrainingEditorState(),
            settings = StatisticsTrainingRecommendationSettings(),
        )

        assertFalse(runtimeContext.isLoadedFormulaOutOfDate())
    }

    @Test
    fun `updateRecommendationSettings keeps loaded recommendation settings snapshot unchanged`() {
        val runtimeContext = StatisticsTrainingRuntimeContext()
        val loadedSettings = StatisticsTrainingRecommendationSettings()
        val updatedSettings = StatisticsTrainingRecommendationSettings(limit = 10)
        runtimeContext.rememberLoadedSelection(
            newEditorState = CreateTrainingEditorState(),
            settings = loadedSettings,
        )

        runtimeContext.updateRecommendationSettings(updatedSettings)

        assertEquals(updatedSettings, runtimeContext.recommendationSettings)
        assertEquals(loadedSettings, runtimeContext.loadedRecommendationSettings)
        assertTrue(runtimeContext.isSelectionOutOfDate())
    }
}
