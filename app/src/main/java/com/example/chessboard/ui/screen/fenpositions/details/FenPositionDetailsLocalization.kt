package com.example.chessboard.ui.screen.fenpositions.details

/*
 * File role: groups localized text used by the FEN position details screen.
 * Allowed here:
 * - resource reads and small formatting helpers for details UI text
 * Not allowed here:
 * - layout, persistence, expansion state, or navigation
 * Validation date: 2026-09-01
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R

internal data class FenPositionDetailsStrings(
    val screenTitle: String,
    val previousPositionContentDescription: String,
    val nextPositionContentDescription: String,
    val deletePositionContentDescription: String,
    private val themeFormat: String,
    val unnamedPosition: String,
    val descriptionTitle: String,
    val descriptionAbsent: String,
    val collapse: String,
    val expandDescriptionContentDescription: String,
    val collapseDescriptionContentDescription: String,
    private val continuationsFormat: String,
    val expandContinuationsContentDescription: String,
    val collapseContinuationsContentDescription: String,
    val notFound: String,
    val loadFailed: String,
) {
    fun name(name: String): String {
        if (name.isBlank()) {
            return unnamedPosition
        }

        return name
    }

    fun theme(theme: String): String {
        return themeFormat.format(theme)
    }

    fun continuations(count: Int): String {
        return continuationsFormat.format(count)
    }
}

@Composable
internal fun fenPositionDetailsStrings(): FenPositionDetailsStrings {
    return FenPositionDetailsStrings(
        screenTitle = stringResource(R.string.fen_position_details_title),
        previousPositionContentDescription = stringResource(
            R.string.fen_position_details_previous_position_content_description,
        ),
        nextPositionContentDescription = stringResource(
            R.string.fen_position_details_next_position_content_description,
        ),
        deletePositionContentDescription = stringResource(
            R.string.fen_position_details_delete_content_description,
        ),
        themeFormat = stringResource(R.string.fen_position_catalog_theme),
        unnamedPosition = stringResource(R.string.fen_position_catalog_unnamed),
        descriptionTitle = stringResource(R.string.fen_position_details_description_title),
        descriptionAbsent = stringResource(R.string.fen_position_details_description_absent),
        collapse = stringResource(R.string.fen_position_details_collapse),
        expandDescriptionContentDescription = stringResource(
            R.string.fen_position_details_expand_description_content_description,
        ),
        collapseDescriptionContentDescription = stringResource(
            R.string.fen_position_details_collapse_description_content_description,
        ),
        continuationsFormat = stringResource(R.string.fen_position_details_continuations),
        expandContinuationsContentDescription = stringResource(
            R.string.fen_position_details_expand_continuations_content_description,
        ),
        collapseContinuationsContentDescription = stringResource(
            R.string.fen_position_details_collapse_continuations_content_description,
        ),
        notFound = stringResource(R.string.fen_position_details_not_found),
        loadFailed = stringResource(R.string.fen_position_details_load_failed),
    )
}
