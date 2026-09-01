package com.example.chessboard.ui.screen

/*
 * File role: verifies MIME selection for compatible full database backup restore.
 * Keep pure system document-picker policy assertions here.
 * Do not add Compose UI checks or database backup behavior tests.
 * Validation date: 2026-08-31
 */

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class BackupDocumentPickerTest {
    @Test
    fun `compatible full database restore keeps broad legacy selection`() {
        assertArrayEquals(
            arrayOf(
                FullDatabaseBackupMimeType,
                "application/octet-stream",
                "*/*",
            ),
            resolveCompatibleFullDatabaseRestoreMimeTypes(),
        )
    }
}
