package com.example.chessboard.ui.screen.gameOpeningAnalysis

/*
 * File role: groups screen-level Android export helpers for game-opening analysis imported games.
 * Allowed here:
 * - default export file naming for the comparison screen
 * - writing normalized imported-game PGN text to a selected Android document Uri
 * Not allowed here:
 * - Compose UI rendering, document picker launcher state, PGN serialization rules, or runtime state mutation
 * Validation date: 2026-07-02
 */

import android.content.Context
import android.net.Uri
import com.example.chessboard.runtimecontext.ImportedGameItem
import com.example.chessboard.runtimecontext.buildGameOpeningAnalysisGamesPgn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun resolveGameOpeningAnalysisExportFileName(prefix: String = "filtered-games"): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm", Locale.US)
    val timestamp = formatter.format(Date())
    return "$prefix-$timestamp.pgn"
}

internal fun writeGameOpeningAnalysisGamesPgnExport(
    context: Context,
    uri: Uri,
    games: List<ImportedGameItem>,
    failedOpenDestinationMessage: String,
) {
    val pgnText = buildGameOpeningAnalysisGamesPgn(games)
    val outputStream =
        context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException(failedOpenDestinationMessage)

    outputStream.writer(Charsets.UTF_8).use { writer ->
        writer.write(pgnText)
    }
}
