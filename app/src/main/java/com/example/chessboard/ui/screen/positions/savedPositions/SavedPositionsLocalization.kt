package com.example.chessboard.ui.screen.positions.savedPositions

/*
 * Localization holder for SavedPositions screen and its local components.
 * Keep grouped resource reads and formatting helpers for saved-position UI text here.
 * Do not add persistence, navigation, or layout behavior to this file.
 * Validation date: 2026-05-29
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R

internal data class SavedPositionsStrings(
    val screenTitle: String,
    private val subtitleFormat: String,
    val searchContentDescription: String,
    val previousPageContentDescription: String,
    val nextPageContentDescription: String,
    val emptyState: String,
    val emptyFilterState: String,
    val unnamedPosition: String,
    val selectedLabel: String,
    private val positionIdFormat: String,
    private val fenFormat: String,
    val openPositionContentDescription: String,
    val createFromPositionContentDescription: String,
    val findDeviationsContentDescription: String,
    val deletePositionContentDescription: String,
    val searchDialogTitle: String,
    val searchNameLabel: String,
    val searchNamePlaceholder: String,
    val searchCaseSensitiveTitle: String,
    val searchCaseSensitiveSubtitle: String,
    val applyAction: String,
    val cancelAction: String,
    val deletePositionTitle: String,
    val deleteAction: String,
    private val deletePositionMessageFormat: String,
    val createTrainingFromPositionTitle: String,
    val linesFoundForPositionLabel: String,
    val templateDefaultName: String,
    val templateErrorTitle: String,
    val templateErrorMessage: String,
    val templateCreatedTitle: String,
    private val templateCreatedMessageFormat: String,
    val noDeviationsTitle: String,
    val noDeviationsMessage: String,
    val deviationSearchTitle: String,
    private val deviationSearchMessageFormat: String,
    val deviationSearchHelp: String,
    val openingDeviationsTitle: String,
    private val openingDeviationsMessageFormat: String,
    val deviationsAction: String,
    val closeAction: String,
    val previewTitle: String,
    val blackToMove: String,
    val whiteToMove: String,
) {
    fun topBarSubtitle(
        totalPositionsCount: Int,
        currentPage: Int,
        totalPages: Int,
    ): String {
        return subtitleFormat.format(totalPositionsCount, currentPage, totalPages)
    }

    fun positionId(positionId: Long): String {
        return positionIdFormat.format(positionId)
    }

    fun fen(fen: String): String {
        return fenFormat.format(fen)
    }

    fun deletePositionMessage(position: SavedPositionListItem): String {
        return deletePositionMessageFormat.format(position.name, position.id)
    }

    fun templateCreatedMessage(
        templateId: Long,
        lineCount: Int,
    ): String {
        return templateCreatedMessageFormat.format(templateId, lineCount)
    }

    fun deviationSearchMessage(positionName: String): String {
        return deviationSearchMessageFormat.format(positionName)
    }

    fun openingDeviationsMessage(deviationCount: Int): String {
        return openingDeviationsMessageFormat.format(deviationCount)
    }
}

@Composable
internal fun savedPositionsStrings(): SavedPositionsStrings {
    return SavedPositionsStrings(
        screenTitle = stringResource(R.string.saved_positions_title),
        subtitleFormat = stringResource(R.string.saved_positions_subtitle),
        searchContentDescription = stringResource(R.string.saved_positions_search_content_description),
        previousPageContentDescription = stringResource(R.string.saved_positions_previous_page_content_description),
        nextPageContentDescription = stringResource(R.string.saved_positions_next_page_content_description),
        emptyState = stringResource(R.string.saved_positions_empty),
        emptyFilterState = stringResource(R.string.saved_positions_empty_filter),
        unnamedPosition = stringResource(R.string.saved_positions_unnamed_position),
        selectedLabel = stringResource(R.string.saved_positions_selected),
        positionIdFormat = stringResource(R.string.saved_positions_position_id),
        fenFormat = stringResource(R.string.saved_positions_fen),
        openPositionContentDescription = stringResource(R.string.saved_positions_open_content_description),
        createFromPositionContentDescription = stringResource(R.string.saved_positions_create_content_description),
        findDeviationsContentDescription = stringResource(R.string.saved_positions_find_deviations_content_description),
        deletePositionContentDescription = stringResource(R.string.saved_positions_delete_content_description),
        searchDialogTitle = stringResource(R.string.saved_positions_search_dialog_title),
        searchNameLabel = stringResource(R.string.saved_positions_search_name_label),
        searchNamePlaceholder = stringResource(R.string.saved_positions_search_name_placeholder),
        searchCaseSensitiveTitle = stringResource(R.string.saved_positions_search_case_sensitive_title),
        searchCaseSensitiveSubtitle = stringResource(R.string.saved_positions_search_case_sensitive_subtitle),
        applyAction = stringResource(R.string.common_apply),
        cancelAction = stringResource(R.string.common_cancel),
        deletePositionTitle = stringResource(R.string.saved_positions_delete_title),
        deleteAction = stringResource(R.string.common_delete),
        deletePositionMessageFormat = stringResource(R.string.saved_positions_delete_message),
        createTrainingFromPositionTitle = stringResource(R.string.saved_positions_create_training_from_position_title),
        linesFoundForPositionLabel = stringResource(R.string.saved_positions_lines_found_for_position_label),
        templateDefaultName = stringResource(R.string.position_search_template_default_name),
        templateErrorTitle = stringResource(R.string.saved_positions_template_error_title),
        templateErrorMessage = stringResource(R.string.saved_positions_template_error_message),
        templateCreatedTitle = stringResource(R.string.saved_positions_template_created_title),
        templateCreatedMessageFormat = stringResource(R.string.saved_positions_template_created_message),
        noDeviationsTitle = stringResource(R.string.saved_positions_no_deviations_title),
        noDeviationsMessage = stringResource(R.string.saved_positions_no_deviations_message),
        deviationSearchTitle = stringResource(R.string.saved_positions_deviation_search_title),
        deviationSearchMessageFormat = stringResource(R.string.saved_positions_deviation_search_message),
        deviationSearchHelp = stringResource(R.string.saved_positions_deviation_search_help),
        openingDeviationsTitle = stringResource(R.string.saved_positions_opening_deviations_title),
        openingDeviationsMessageFormat = stringResource(R.string.saved_positions_opening_deviations_message),
        deviationsAction = stringResource(R.string.saved_positions_deviations_action),
        closeAction = stringResource(R.string.saved_positions_close_action),
        previewTitle = stringResource(R.string.saved_positions_preview_title),
        blackToMove = stringResource(R.string.saved_positions_black_to_move),
        whiteToMove = stringResource(R.string.saved_positions_white_to_move),
    )
}
