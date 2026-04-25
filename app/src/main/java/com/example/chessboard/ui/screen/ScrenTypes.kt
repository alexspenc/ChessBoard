package com.example.chessboard.ui.screen

sealed class ScreenType(val title: String) {

    object Home : ScreenType("Home")
    object Training : ScreenType("Training")
    object GamesExplorer : ScreenType("Games")
    data class TrainSingleGame(val trainingId: Long, val gameId: Long) : ScreenType("TrainSingleGame")
    data class AnalyzeGame(
        val uciMoves: List<String>,
        val initialPly: Int,
        val backTarget: ScreenType,
    ) : ScreenType("AnalyzeGame")
    object CreateTrainingChoice : ScreenType("CreateTrainingChoice")
    object CreateTrainingByStatistics : ScreenType("CreateTrainingByStatistics")
    object CreateTraining : ScreenType("CreateTraining")
    object TrainingTemplateSelection : ScreenType("TrainingTemplateSelection")
    object TrainingTemplates : ScreenType("TrainingTemplates")
    data class CreateTrainingFromTemplate(val templateId: Long) : ScreenType("CreateTrainingFromTemplate")
    data class CreateTrainingFromGameIds(
        val gameIds: List<Long>,
        val backTarget: ScreenType = PositionEditor,
    ) : ScreenType("CreateTrainingFromGameIds")
    data class EditTrainingTemplate(val templateId: Long) : ScreenType("EditTrainingTemplate")
    data class EditTraining(val trainingId: Long) : ScreenType("EditTraining")
    data class TrainingSettings(val trainingId: Long) : ScreenType("TrainingSettings")
    object CreateOpening : ScreenType("CreateOpening")
    object PositionEditor : ScreenType("PositionEditor")
    object SavedPositions : ScreenType("Saved Positions")
    object SelectOpeningDeviationPosition : ScreenType("SelectOpeningDeviationPosition")
    object ShowOpeningDeviation : ScreenType("ShowOpeningDeviation")
    object Backup : ScreenType("Backup")
    object GameEditor : ScreenType("GameEditor")
    object Stats : ScreenType("Stats")
    object Profile : ScreenType("Profile")
    object Settings : ScreenType("Settings")
    object SmartTraining : ScreenType("SmartTraining")
    data class SmartTrainGame(val trainingId: Long, val gameId: Long) : ScreenType("SmartTrainGame")

    override fun toString(): String = title
}
