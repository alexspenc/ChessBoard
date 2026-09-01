package com.example.chessboard.service

/*
 * File role: verifies filename filtering for full database backup documents.
 * Keep provider-independent SQLite backup filename policy checks here.
 * Do not add ContentResolver, Compose UI, or database restore behavior tests.
 * Validation date: 2026-08-31
 */

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FullDatabaseBackupDocumentServiceTest {
    @Test
    fun `SQLite backup extension is accepted case insensitively`() {
        assertTrue(isFullDatabaseBackupFileName("cb-backup-2026-08-31.sqlite3"))
        assertTrue(isFullDatabaseBackupFileName("renamed-backup.SQLITE3"))
    }

    @Test
    fun `non-SQLite filenames are rejected`() {
        assertFalse(isFullDatabaseBackupFileName("cb-backup-2026-08-31.db"))
        assertFalse(isFullDatabaseBackupFileName("cb-backup-2026-08-31.sqlite3.zip"))
    }
}
