package com.example.chessboard.ui.screen.openingDeviation

/**
 * Defines UI-facing models for opening deviation screens.
 *
 * Keep lightweight presentation data for deviation selection and display here.
 * Do not add Compose rendering, navigation wiring, or database access to this file.
 */
data class OpeningDeviationItem(
    val positionFen: String,
    val branches: List<OpeningDeviationBranch>,
)

data class OpeningDeviationBranch(
    val moveUci: String,
    val resultFen: String,
    val gamesCount: Int,
)
