package com.example.chessboard.ui.testtags.fenpositions

/*
 * File role: defines stable Compose test tags for the FEN position catalog feature.
 * Allowed here:
 * - catalog screen, state, pagination, actions, card, create-dialog, and board tags
 * Not allowed here:
 * - tags for other features, UI rendering, or test assertion helpers
 * Validation date: 2026-09-01
 */

const val FenPositionCatalogScreenTestTag = "fen-position-catalog-screen"
const val FenPositionCatalogHomeEntryTestTag = "fen-position-catalog-home-entry"
const val FenPositionCatalogContentTestTag = "fen-position-catalog-content"
const val FenPositionCatalogLoadingTestTag = "fen-position-catalog-loading"
const val FenPositionCatalogEmptyTestTag = "fen-position-catalog-empty"
const val FenPositionCatalogPreviousPageTestTag = "fen-position-catalog-previous-page"
const val FenPositionCatalogNextPageTestTag = "fen-position-catalog-next-page"
const val FenPositionCatalogAddTestTag = "fen-position-catalog-add"
const val FenPositionCatalogOpenTestTag = "fen-position-catalog-open"
const val FenPositionCatalogDeleteTestTag = "fen-position-catalog-delete"
const val FenPositionCreateDialogTestTag = "fen-position-create-dialog"
const val FenPositionCreateFenInputTestTag = "fen-position-create-fen-input"
const val FenPositionCreateNameInputTestTag = "fen-position-create-name-input"
const val FenPositionCreateThemeInputTestTag = "fen-position-create-theme-input"
const val FenPositionCreateDescriptionInputTestTag = "fen-position-create-description-input"
const val FenPositionCreatePreviewBoardTestTag = "fen-position-create-preview-board"
const val FenPositionCreateConfirmTestTag = "fen-position-create-confirm"

fun fenPositionCatalogCardTestTag(positionId: Long): String {
    return "fen-position-catalog-card-$positionId"
}

fun fenPositionCatalogBoardTestTag(positionId: Long): String {
    return "fen-position-catalog-board-$positionId"
}
