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
        assertTrue(runtimeContext.hasLoadedSelection)
    }
}
