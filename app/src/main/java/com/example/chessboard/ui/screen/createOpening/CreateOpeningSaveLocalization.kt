package com.example.chessboard.ui.screen.createOpening

/*
 * Localization holder for create-opening save and post-save flows.
 * Keep pre-resolved strings and small formatting helpers needed by save callbacks here.
 * Do not add persistence, navigation, or Compose layout code to this file.
 * Validation date: 2026-06-05
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R

internal data class CreateOpeningSaveStrings(
    val defaultOpeningName: String,
    private val defaultChapterNameFormat: String,
    private val lineEventNameFormat: String,
    val noImportedChaptersSaved: String,
    val noImportedLinesSaved: String,
    val failedSaveOpening: String,
    val runtime: CreateOpeningSaveRuntimeStrings,
) {
    fun defaultChapterName(chapterIndex: Int): String {
        return defaultChapterNameFormat.format(chapterIndex + 1)
    }

    fun importedLineEventName(
        baseName: String,
        index: Int,
        total: Int,
    ): String {
        val resolvedBaseName = baseName.ifBlank { defaultOpeningName }
        if (total <= 1 || index == 0) {
            return resolvedBaseName
        }

        return lineEventNameFormat.format(resolvedBaseName, index + 1)
    }
}

@Composable
internal fun createOpeningSaveStrings(): CreateOpeningSaveStrings {
    return CreateOpeningSaveStrings(
        defaultOpeningName = stringResource(R.string.create_opening_default_opening_name),
        defaultChapterNameFormat = stringResource(R.string.create_opening_default_chapter_name),
        lineEventNameFormat = stringResource(R.string.create_opening_line_event_name),
        noImportedChaptersSaved = stringResource(R.string.create_opening_no_imported_chapters_saved),
        noImportedLinesSaved = stringResource(R.string.create_opening_no_imported_lines_saved),
        failedSaveOpening = stringResource(R.string.create_opening_failed_save_opening),
        runtime = CreateOpeningSaveRuntimeStrings(
            saveCanceled = stringResource(R.string.create_opening_save_canceled),
            processedLines = stringResource(R.string.create_opening_processed_lines),
            savedLines = stringResource(R.string.create_opening_saved_lines),
            skippedLines = stringResource(R.string.create_opening_skipped_lines),
        ),
    )
}
