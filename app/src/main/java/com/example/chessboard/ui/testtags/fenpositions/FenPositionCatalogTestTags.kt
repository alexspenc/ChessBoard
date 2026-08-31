package com.example.chessboard.ui.testtags.fenpositions

/*
 * File role: defines stable Compose test tags for the FEN position catalog feature.
 * Allowed here:
 * - catalog screen, state, pagination, card, and board tag constants or builders
 * Not allowed here:
 * - tags for other features, UI rendering, or test assertion helpers
 * Validation date: 2026-08-31
 */

const val FenPositionCatalogScreenTestTag = "fen-position-catalog-screen"
const val FenPositionCatalogHomeEntryTestTag = "fen-position-catalog-home-entry"
const val FenPositionCatalogContentTestTag = "fen-position-catalog-content"
const val FenPositionCatalogLoadingTestTag = "fen-position-catalog-loading"
const val FenPositionCatalogEmptyTestTag = "fen-position-catalog-empty"
const val FenPositionCatalogPreviousPageTestTag = "fen-position-catalog-previous-page"
const val FenPositionCatalogNextPageTestTag = "fen-position-catalog-next-page"

fun fenPositionCatalogCardTestTag(positionId: Long): String {
    return "fen-position-catalog-card-$positionId"
}

fun fenPositionCatalogBoardTestTag(positionId: Long): String {
    return "fen-position-catalog-board-$positionId"
}
