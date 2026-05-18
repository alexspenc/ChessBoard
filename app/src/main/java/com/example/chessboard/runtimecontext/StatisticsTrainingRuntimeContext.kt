package com.example.chessboard.runtimecontext

/**
 * File role: keeps runtime-only state for statistics-based training creation.
 * Allowed here:
 * - temporary selection settings, selected-line snapshots, and formula revision markers
 * - in-memory state that should survive navigating to formula settings and back
 * Not allowed here:
 * - Room access, persisted settings, composable UI, or recommendation calculation
 * Validation date: 2026-05-18
 */

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.chessboard.service.StatisticsTrainingRecommendationSettings
import com.example.chessboard.ui.screen.training.common.CreateTrainingEditorState

class StatisticsTrainingRuntimeContext {
    var formulaRevision by mutableIntStateOf(0)
        private set
    var editorState by mutableStateOf(CreateTrainingEditorState())
        private set
    var loadedEditorState by mutableStateOf(CreateTrainingEditorState())
        private set
    var recommendationSettings by mutableStateOf(StatisticsTrainingRecommendationSettings())
        private set
    var loadedRecommendationSettings by mutableStateOf<StatisticsTrainingRecommendationSettings?>(null)
        private set
    var loadedFormulaRevision by mutableIntStateOf(0)
        private set
    var hasLoadedSelection by mutableStateOf(false)
        private set

    fun markFormulaChanged() {
        formulaRevision += 1
    }

    fun updateRecommendationSettings(settings: StatisticsTrainingRecommendationSettings) {
        recommendationSettings = settings
    }

    fun updateEditorState(newEditorState: CreateTrainingEditorState) {
        editorState = newEditorState
    }

    fun rememberLoadedSelection(
        newEditorState: CreateTrainingEditorState,
        settings: StatisticsTrainingRecommendationSettings,
    ) {
        editorState = newEditorState
        loadedEditorState = newEditorState
        loadedRecommendationSettings = settings
        loadedFormulaRevision = formulaRevision
        hasLoadedSelection = true
    }
}
