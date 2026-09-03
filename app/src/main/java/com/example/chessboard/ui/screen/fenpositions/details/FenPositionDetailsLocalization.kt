package com.example.chessboard.ui.screen.fenpositions.details

/*
 * File role: groups localized text used by the FEN position details screen.
 * Allowed here:
 * - resource reads and small formatting helpers for details UI text
 * Not allowed here:
 * - layout, persistence, expansion state, or navigation
 * Validation date: 2026-09-03
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R

internal data class FenPositionDetailsStrings(
    val screenTitle: String,
    val previousPositionContentDescription: String,
    val nextPositionContentDescription: String,
    val editPositionContentDescription: String,
    val deletePositionContentDescription: String,
    val addContinuationContentDescription: String,
    val copyFenContentDescription: String,
    val analysisBoardContentDescription: String,
    val copyFenProgressTitle: String,
    val copyFenProgressMessage: String,
    val copyFenSuccessTitle: String,
    val copyFenSuccessMessage: String,
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
    val editDialog: FenPositionEditDialogStrings,
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

internal data class FenPositionEditDialogStrings(
    val title: String,
    val nameLabel: String,
    val namePlaceholder: String,
    val themeLabel: String,
    val themePlaceholder: String,
    val descriptionLabel: String,
    val descriptionPlaceholder: String,
    val themeRequired: String,
    val savingTitle: String,
    val savingMessage: String,
    val saveFailedTitle: String,
    val saveFailed: String,
    val positionNotFound: String,
    val cancel: String,
    val save: String,
)

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
        editPositionContentDescription = stringResource(
            R.string.fen_position_details_edit_content_description,
        ),
        deletePositionContentDescription = stringResource(
            R.string.fen_position_details_delete_content_description,
        ),
        addContinuationContentDescription = stringResource(
            R.string.fen_position_details_add_continuation_content_description,
        ),
        copyFenContentDescription = stringResource(
            R.string.fen_position_details_copy_fen_content_description,
        ),
        analysisBoardContentDescription = stringResource(
            R.string.fen_position_analysis_board_content_description,
        ),
        copyFenProgressTitle = stringResource(R.string.fen_position_copy_fen_progress_title),
        copyFenProgressMessage = stringResource(R.string.fen_position_copy_fen_progress_message),
        copyFenSuccessTitle = stringResource(R.string.fen_position_copy_fen_success_title),
        copyFenSuccessMessage = stringResource(R.string.fen_position_copy_fen_success_message),
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
        editDialog = FenPositionEditDialogStrings(
            title = stringResource(R.string.fen_position_edit_title),
            nameLabel = stringResource(R.string.fen_position_create_name_label),
            namePlaceholder = stringResource(R.string.fen_position_create_name_placeholder),
            themeLabel = stringResource(R.string.fen_position_create_theme_label),
            themePlaceholder = stringResource(R.string.fen_position_create_theme_placeholder),
            descriptionLabel = stringResource(R.string.fen_position_create_description_label),
            descriptionPlaceholder = stringResource(
                R.string.fen_position_create_description_placeholder,
            ),
            themeRequired = stringResource(R.string.fen_position_create_theme_required),
            savingTitle = stringResource(R.string.fen_position_edit_saving_title),
            savingMessage = stringResource(R.string.fen_position_edit_saving_message),
            saveFailedTitle = stringResource(R.string.fen_position_edit_save_failed_title),
            saveFailed = stringResource(R.string.fen_position_edit_save_failed),
            positionNotFound = stringResource(R.string.fen_position_edit_position_not_found),
            cancel = stringResource(R.string.common_cancel),
            save = stringResource(R.string.common_save),
        ),
    )
}
