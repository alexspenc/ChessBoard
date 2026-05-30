package com.example.chessboard.ui.screen.openingDeviation

/*
 * Localization holder for opening-deviation selection and display screens.
 * Keep grouped resource reads and formatting helpers for opening-deviation UI text here.
 * Do not add navigation, persistence, or board rendering behavior to this file.
 * Validation date: 2026-05-30
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R

internal data class OpeningDeviationStrings(
    val selectionTitle: String,
    private val positionsFormat: String,
    val startDisplayContentDescription: String,
    val selectedDeviationPositionTitle: String,
    val selectedLabel: String,
    private val branchesFormat: String,
    private val fenFormat: String,
    val selectionEmptyState: String,
    private val deviationPositionFormat: String,
    val displayTitle: String,
    val sourceDeviationPositionTitle: String,
    private val branchFormat: String,
    private val moveFormat: String,
    private val linesFormat: String,
    val displayEmptyState: String,
    val openLinesContentDescription: String,
    val blackToMove: String,
    val whiteToMove: String,
) {
    fun positions(count: Int): String {
        return positionsFormat.format(count)
    }

    fun branches(count: Int): String {
        return branchesFormat.format(count)
    }

    fun fen(fen: String): String {
        return fenFormat.format(fen)
    }

    fun deviationPosition(index: Int): String {
        return deviationPositionFormat.format(index + 1)
    }

    fun branch(index: Int): String {
        return branchFormat.format(index + 1)
    }

    fun move(moveUci: String): String {
        return moveFormat.format(moveUci)
    }

    fun lines(count: Int): String {
        return linesFormat.format(count)
    }
}

@Composable
internal fun openingDeviationStrings(): OpeningDeviationStrings {
    return OpeningDeviationStrings(
        selectionTitle = stringResource(R.string.opening_deviation_selection_title),
        positionsFormat = stringResource(R.string.opening_deviation_positions_count),
        startDisplayContentDescription = stringResource(
            R.string.opening_deviation_start_display_content_description
        ),
        selectedDeviationPositionTitle = stringResource(
            R.string.opening_deviation_selected_position_title
        ),
        selectedLabel = stringResource(R.string.opening_deviation_selected),
        branchesFormat = stringResource(R.string.opening_deviation_branches_count),
        fenFormat = stringResource(R.string.opening_deviation_fen),
        selectionEmptyState = stringResource(R.string.opening_deviation_selection_empty),
        deviationPositionFormat = stringResource(R.string.opening_deviation_position_title),
        displayTitle = stringResource(R.string.opening_deviation_display_title),
        sourceDeviationPositionTitle = stringResource(
            R.string.opening_deviation_source_position_title
        ),
        branchFormat = stringResource(R.string.opening_deviation_branch_title),
        moveFormat = stringResource(R.string.opening_deviation_move),
        linesFormat = stringResource(R.string.opening_deviation_lines_count),
        displayEmptyState = stringResource(R.string.opening_deviation_display_empty),
        openLinesContentDescription = stringResource(
            R.string.opening_deviation_open_lines_content_description
        ),
        blackToMove = stringResource(R.string.opening_deviation_black_to_move),
        whiteToMove = stringResource(R.string.opening_deviation_white_to_move),
    )
}
