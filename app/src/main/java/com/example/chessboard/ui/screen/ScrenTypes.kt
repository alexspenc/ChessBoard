package com.example.chessboard.ui.screen

sealed class ScreenType(val title: String) {

    object Home : ScreenType("Home")
    object Training : ScreenType("Training")
    object GamesExplorer : ScreenType("Games")
    data class TrainSingleGame(val trainingId: Long, val gameId: Long) : ScreenType("TrainSingleGame")
    object CreateTraining : ScreenType("CreateTraining")
    data class EditTraining(val trainingId: Long) : ScreenType("EditTraining")
    object CreateOpening : ScreenType("CreateOpening")
    object PositionEditor : ScreenType("PositionEditor")
    object GameEditor : ScreenType("GameEditor")
    object Stats : ScreenType("Stats")
    object Profile : ScreenType("Profile")
    object Settings : ScreenType("Settings")

    override fun toString(): String = title
}
