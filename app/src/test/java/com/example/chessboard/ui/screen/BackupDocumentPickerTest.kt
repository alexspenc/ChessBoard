package com.example.chessboard.ui.screen

/*
 * File role: verifies MIME selection for full database backup restore.
 * Keep pure document-picker policy assertions here.
 * Do not add Compose UI checks or database backup behavior tests.
 * Validation date: 2026-07-24
 */

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class BackupDocumentPickerTest {
    @Test
    fun `strict full database restore accepts only SQLite files`() {
        assertArrayEquals(
            arrayOf(FullDatabaseBackupMimeType),
            resolveFullDatabaseRestoreMimeTypes(strictFileSelection = true),
        )
    }

    @Test
    fun `non-strict full database restore keeps broad legacy selection`() {
        assertArrayEquals(
            arrayOf(
                FullDatabaseBackupMimeType,
                "application/octet-stream",
                "*/*",
            ),
            resolveFullDatabaseRestoreMimeTypes(strictFileSelection = false),
        )
    }
}
