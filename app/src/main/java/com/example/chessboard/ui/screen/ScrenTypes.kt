package com.example.chessboard.ui.screen

/**
 * File role: defines app-level screen destinations used by top-level navigation.
 * Allowed here:
 * - destination objects and the parameters required to open a screen
 * Not allowed here:
 * - composable rendering, navigation execution, or screen runtime state
 * Validation date: 2026-09-03
 */

sealed class ScreenType(val title: String) {

    object Home : ScreenType("Home")
    object Training : ScreenType("Training")
    object LinesExplorer : ScreenType("Lines")
    data class TrainSingleLine(val trainingId: Long, val lineId: Long) : ScreenType("TrainSingleLine")
    data class AnalyzeLine(
        val uciMoves: List<String>,
        val initialPly: Int,
        val backTarget: ScreenType,
    ) : ScreenType("AnalyzeLine")
    data class AnalyzeFenPosition(
        val fen: String,
        val variationLines: List<List<String>>,
        val selectedLine: List<String>,
        val initialPly: Int,
        val backTarget: ScreenType,
    ) : ScreenType("AnalyzeFenPosition")
    object CreateTrainingChoice : ScreenType("CreateTrainingChoice")
    object CreateTrainingByStatistics : ScreenType("CreateTrainingByStatistics")
    object TrainingFormulaSettings : ScreenType("TrainingFormulaSettings")
    object CreateTraining : ScreenType("CreateTraining")
    object TrainingTemplateSelection : ScreenType("TrainingTemplateSelection")
    object TrainingTemplates : ScreenType("TrainingTemplates")
    data class CreateTrainingFromTemplate(val templateId: Long) : ScreenType("CreateTrainingFromTemplate")
    data class CreateTrainingFromLineIds(
        val lineIds: List<Long>,
        val backTarget: ScreenType,
        val initialTrainingName: String?,
        val screenTitle: String,
        val linesCountLabel: String,
    ) : ScreenType("CreateTrainingFromLineIds")
    data class EditTrainingTemplate(val templateId: Long) : ScreenType("EditTrainingTemplate")
    data class EditTraining(val trainingId: Long) : ScreenType("EditTraining")
    data class TrainingSettings(val trainingId: Long) : ScreenType("TrainingSettings")
    object CreateOpening : ScreenType("CreateOpening")
    object PositionSearch : ScreenType("PositionSearch")
    object PositionSearchSettings : ScreenType("PositionSearchSettings")
    object ImportPositionFromImage : ScreenType("ImportPositionFromImage")
    object SavedPositions : ScreenType("Saved Positions")
    object FenPositionCatalog : ScreenType("FEN Positions")
    data class FenPositionDetails(val positionId: Long) : ScreenType("FEN Position Details")
    data class FenPositionContinuationDetails(
        val positionId: Long,
        val continuationId: Long,
    ) : ScreenType("FEN Continuation Details")
    data class AddFenPositionContinuations(val positionId: Long) : ScreenType("Add FEN Continuations")
    object GameOpeningAnalysis : ScreenType("Compare")
    object SelectOpeningDeviationPosition : ScreenType("SelectOpeningDeviationPosition")
    object ShowOpeningDeviation : ScreenType("ShowOpeningDeviation")
    object Backup : ScreenType("Backup")
    object LineEditor : ScreenType("LineEditor")
    object Stats : ScreenType("Stats")
    object Profile : ScreenType("Profile")
    object Settings : ScreenType("Settings")
    object SmartTraining : ScreenType("SmartTraining")
    object SmartSettings : ScreenType("SmartSettings")
    data class SmartTrainLine(val trainingId: Long, val lineId: Long) : ScreenType("SmartTrainLine")

    override fun toString(): String = title
}
