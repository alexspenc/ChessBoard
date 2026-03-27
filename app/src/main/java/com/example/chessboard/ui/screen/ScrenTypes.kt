package com.example.chessboard.ui.screen

sealed class ScreenType(val title: String) {

    object Home : ScreenType("Home")
    object Training : ScreenType("Training")
    object GamesExplorer : ScreenType("Games")
    data class TrainSingleGame(val trainingId: Long, val gameId: Long) : ScreenType("TrainSingleGame")
    data class CreateTraining(val trainingId: Long?) : ScreenType("CreateTraining")
    object CreateOpening : ScreenType("CreateOpening")
    object GameEditor : ScreenType("GameEditor")
    object Stats : ScreenType("Stats")
    object Profile : ScreenType("Profile")

    override fun toString(): String = title
}
