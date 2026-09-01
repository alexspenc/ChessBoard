package com.example.chessboard.ui.screen

/*
 * File role: defines MIME selection policy for backup documents opened through the system picker.
 * Keep backup-specific MIME constants and compatible full-backup restore filtering here.
 * Do not add generic picker contracts, backup file I/O, Compose launchers, or restore workflows.
 * Validation date: 2026-08-31
 */

internal const val FullDatabaseBackupMimeType = "application/vnd.sqlite3"
private const val GenericBinaryMimeType = "application/octet-stream"
private const val AnyFileMimeType = "*/*"

internal fun resolveCompatibleFullDatabaseRestoreMimeTypes(): Array<String> {
    return arrayOf(
        FullDatabaseBackupMimeType,
        GenericBinaryMimeType,
        AnyFileMimeType,
    )
}
