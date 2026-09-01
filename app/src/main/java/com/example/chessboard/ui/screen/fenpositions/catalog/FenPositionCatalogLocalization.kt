package com.example.chessboard.ui.screen.fenpositions.catalog

/*
 * File role: groups localized text used by the FEN position catalog screen.
 * Allowed here:
 * - resource reads and small formatting helpers for catalog UI text
 * Not allowed here:
 * - layout, pagination behavior, navigation, or persistence operations
 * Validation date: 2026-09-01
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R

internal data class FenPositionCatalogStrings(
    val topBar: FenPositionCatalogTopBarStrings,
    private val themeFormat: String,
    val emptyState: String,
    val unnamedPosition: String,
    val addPositionContentDescription: String,
    val openPositionContentDescription: String,
    val deletePositionContentDescription: String,
    val createDialog: FenPositionCreateDialogStrings,
    val deleteDialog: FenPositionDeleteDialogStrings,
) {
    fun theme(theme: String): String {
        return themeFormat.format(theme)
    }

    fun name(name: String): String {
        if (name.isBlank()) {
            return unnamedPosition
        }

        return name
    }
}

internal data class FenPositionCreateDialogStrings(
    val title: String,
    val fenLabel: String,
    val fenPlaceholder: String,
    val nameLabel: String,
    val namePlaceholder: String,
    val themeLabel: String,
    val themePlaceholder: String,
    val descriptionLabel: String,
    val descriptionPlaceholder: String,
    val checkingFen: String,
    val previewPrompt: String,
    val invalidFen: String,
    val themeRequired: String,
    val duplicateFen: String,
    val saveFailedTitle: String,
    val saveFailed: String,
    val savingTitle: String,
    val savingMessage: String,
    val cancel: String,
    val add: String,
)

internal data class FenPositionDeleteDialogStrings(
    val title: String,
    val message: String,
    val confirm: String,
    val deletingTitle: String,
    val deletingMessage: String,
    val failedTitle: String,
    val failedMessage: String,
)

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
        unnamedPosition = stringResource(R.string.fen_position_catalog_unnamed),
        addPositionContentDescription = stringResource(
            R.string.fen_position_catalog_add_content_description,
        ),
        openPositionContentDescription = stringResource(
            R.string.fen_position_catalog_open_content_description,
        ),
        deletePositionContentDescription = stringResource(
            R.string.fen_position_catalog_delete_content_description,
        ),
        createDialog = FenPositionCreateDialogStrings(
            title = stringResource(R.string.fen_position_create_title),
            fenLabel = stringResource(R.string.fen_position_create_fen_label),
            fenPlaceholder = stringResource(R.string.fen_position_create_fen_placeholder),
            nameLabel = stringResource(R.string.fen_position_create_name_label),
            namePlaceholder = stringResource(R.string.fen_position_create_name_placeholder),
            themeLabel = stringResource(R.string.fen_position_create_theme_label),
            themePlaceholder = stringResource(R.string.fen_position_create_theme_placeholder),
            descriptionLabel = stringResource(R.string.fen_position_create_description_label),
            descriptionPlaceholder = stringResource(
                R.string.fen_position_create_description_placeholder,
            ),
            checkingFen = stringResource(R.string.fen_position_create_checking_fen),
            previewPrompt = stringResource(R.string.fen_position_create_preview_prompt),
            invalidFen = stringResource(R.string.fen_position_create_invalid_fen),
            themeRequired = stringResource(R.string.fen_position_create_theme_required),
            duplicateFen = stringResource(R.string.fen_position_create_duplicate_fen),
            saveFailedTitle = stringResource(R.string.fen_position_create_save_failed_title),
            saveFailed = stringResource(R.string.fen_position_create_save_failed),
            savingTitle = stringResource(R.string.fen_position_create_saving_title),
            savingMessage = stringResource(R.string.fen_position_create_saving_message),
            cancel = stringResource(R.string.common_cancel),
            add = stringResource(R.string.fen_position_create_add),
        ),
        deleteDialog = FenPositionDeleteDialogStrings(
            title = stringResource(R.string.fen_position_delete_title),
            message = stringResource(R.string.fen_position_delete_message),
            confirm = stringResource(R.string.fen_position_delete_confirm),
            deletingTitle = stringResource(R.string.fen_position_delete_deleting_title),
            deletingMessage = stringResource(R.string.fen_position_delete_deleting_message),
            failedTitle = stringResource(R.string.fen_position_delete_failed_title),
            failedMessage = stringResource(R.string.fen_position_delete_failed_message),
        ),
    )
}
