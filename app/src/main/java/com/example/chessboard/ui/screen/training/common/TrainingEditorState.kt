package com.example.chessboard.ui.screen.training.common

/*
 * Shared editor state for training-like collection screens.
 *
 * Keep only state that is reused by create, edit-training, and edit-template
 * flows here. Do not add screen-specific load/save orchestration or Compose UI.
 */

data class CreateTrainingEditorState(
    val trainingName: String = "",
    val currentPage: Int = 0,
    val editableLinesForTraining: List<TrainingLineEditorItem> = emptyList()
)
