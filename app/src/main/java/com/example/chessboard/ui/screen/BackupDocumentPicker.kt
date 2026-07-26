package com.example.chessboard.ui.screen

/*
 * File role: defines MIME selection policy for backup documents.
 * Keep backup-specific MIME constants and strict-versus-compatible restore filtering here.
 * Do not add generic picker contracts, backup file I/O, Compose launchers, or restore workflows.
 * Validation date: 2026-07-24
 */

internal const val FullDatabaseBackupMimeType = "application/vnd.sqlite3"
private const val GenericBinaryMimeType = "application/octet-stream"
private const val AnyFileMimeType = "*/*"

internal fun resolveFullDatabaseRestoreMimeTypes(
    strictFileSelection: Boolean,
): Array<String> {
    if (strictFileSelection) {
        return arrayOf(FullDatabaseBackupMimeType)
    }

    return arrayOf(
        FullDatabaseBackupMimeType,
        GenericBinaryMimeType,
        AnyFileMimeType,
    )
}
