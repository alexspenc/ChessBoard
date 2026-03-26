package com.example.chessboard.ui.screen

sealed class ScreenType(val title: String) {

    object Home : ScreenType("Home")
    object Training : ScreenType("Training")
    object CreateTraining : ScreenType("CreateTraining")
    object CreateOpening : ScreenType("CreateOpening")
    object GameEditor : ScreenType("GameEditor")
    object Stats : ScreenType("Stats")
    object Profile : ScreenType("Profile")

    override fun toString(): String = title
}
