package com.example.chessboard.ui.screen.createOpening

/**
 * File role: groups create-opening import helpers for parsing PGN text and applying imported metadata.
 * Allowed here:
 * - screen-specific PGN import parsing models and helpers
 * - draft autofill logic derived from imported opening chapters
 * - file-reading helpers used only by the create-opening screen import flow
 * Not allowed here:
 * - compose UI layout or dialog rendering
 * - save orchestration and post-save navigation policy
 * Validation date: 2026-05-05
 */

import com.example.chessboard.boardmodel.LineDraft
import com.example.chessboard.service.PgnParseErrorStrings
import com.example.chessboard.service.extractPgnHeaders
import com.example.chessboard.service.parsePgnToUciLines
import com.example.chessboard.service.splitPgnChapters

/** One parsed chapter: its PGN headers and the expanded UCI lines. */
internal data class ImportedChapter(
    val headers: Map<String, String>,
    val uciLines: List<List<String>>,
)

internal fun ImportedChapter.resolveChapterName(): String? {
    val chapterName = headerValue("ChapterName")
    if (chapterName != null) {
        return chapterName
    }

    val event = headerValue("Event")
    if (event != null) {
        return event
    }

    return headerValue("StudyName")
}

internal fun ImportedChapter.headerValue(key: String): String? {
    val value = headers[key] ?: return null
    if (value.isBlank() || value == "?") {
        return null
    }

    return value
}

internal fun parseImportedChapters(
    pgnText: String,
    errorStrings: PgnParseErrorStrings,
): List<ImportedChapter> {
    return splitPgnChapters(pgnText).mapNotNull { chapterPgn ->
        val headers = extractPgnHeaders(chapterPgn)
        val uciLines = parsePgnToUciLines(
            pgnText = chapterPgn,
            errorStrings = errorStrings,
        )
        if (uciLines.isEmpty()) {
            return@mapNotNull null
        }

        ImportedChapter(
            headers = headers,
            uciLines = uciLines,
        )
    }
}

internal fun applyImportedChapterToDraft(
    lineDraft: LineDraft,
    importedChapter: ImportedChapter,
): LineDraft {
    var updatedLine = lineDraft.line

    val importedEvent = importedChapter.headerValue("Event")
    if (importedEvent != null) {
        updatedLine = updatedLine.copy(event = importedEvent)
    }

    val importedEco = importedChapter.headerValue("ECO")
    if (importedEco != null) {
        updatedLine = updatedLine.copy(eco = importedEco)
    }

    return lineDraft.copy(line = updatedLine)
}
