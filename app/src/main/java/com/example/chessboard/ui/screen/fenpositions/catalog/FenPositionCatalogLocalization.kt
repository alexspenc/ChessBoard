package com.example.chessboard.ui.screen.fenpositions.catalog

/*
 * File role: groups localized text used by the FEN position catalog screen.
 * Allowed here:
 * - resource reads and small formatting helpers for catalog UI text
 * Not allowed here:
 * - layout, pagination behavior, navigation, or persistence operations
 * Validation date: 2026-08-30
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R

// TODO: Move this feature's string resources from shared strings.xml files to dedicated files.
internal data class FenPositionCatalogStrings(
    val topBar: FenPositionCatalogTopBarStrings,
    private val themeFormat: String,
    val emptyState: String,
) {
    fun theme(theme: String): String {
        return themeFormat.format(theme)
    }
}

internal data class FenPositionCatalogTopBarStrings(
    val screenTitle: String,
    private val subtitleFormat: String,
    val previousPageContentDescription: String,
    val nextPageContentDescription: String,
) {
    fun subtitle(
        totalPositionsCount: Int,
        currentPage: Int,
        totalPages: Int,
    ): String {
        return subtitleFormat.format(totalPositionsCount, currentPage, totalPages)
    }
}

@Composable
internal fun fenPositionCatalogStrings(): FenPositionCatalogStrings {
    return FenPositionCatalogStrings(
        topBar = FenPositionCatalogTopBarStrings(
            screenTitle = stringResource(R.string.fen_position_catalog_title),
            subtitleFormat = stringResource(R.string.fen_position_catalog_subtitle),
            previousPageContentDescription = stringResource(
                R.string.fen_position_catalog_previous_page_content_description,
            ),
            nextPageContentDescription = stringResource(
                R.string.fen_position_catalog_next_page_content_description,
            ),
        ),
        themeFormat = stringResource(R.string.fen_position_catalog_theme),
        emptyState = stringResource(R.string.fen_position_catalog_empty),
    )
}
