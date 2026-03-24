package com.example.chessboard.ui.screen.trainingTemplateScreen

data class GameUiModel(
    val id: Long,
    val name: String
)

data class TemplateGameUiModel(
    val id: Long,
    val name: String,
    val weight: Int
)