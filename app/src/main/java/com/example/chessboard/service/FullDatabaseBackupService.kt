package com.example.chessboard.service

/*
 * File role: owns full SQLite database backup and restore file operations.
 * Allowed here:
 * - database-file snapshot export and replacement restore logic
 * - WAL checkpoint and sidecar-file cleanup needed to keep full backups consistent
 * Not allowed here:
 * - Compose UI, file-picker wiring, PGN/line-only backup behavior, or screen navigation
 * Validation date: 2026-06-28
 */

import android.content.Context
import com.example.chessboard.repository.AppDatabase
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class FullDatabaseBackupService(
    private val context: Context,
    private val databaseName: String,
    private val databaseProvider: () -> AppDatabase,
    private val closeDatabase: () -> Unit,
    private val reopenDatabase: () -> Unit,
) {
    fun writeBackup(outputStream: OutputStream) {
        checkpointDatabase()
        databaseFile().inputStream().use { input ->
            input.copyTo(outputStream)
        }
    }

    fun restoreBackup(inputStream: InputStream) {
        val restoreFile = databaseFile()
        val incomingFile = restoreFile.resolveSibling("$databaseName-incoming-restore")
        val rollbackFile = restoreFile.resolveSibling("$databaseName-rollback")

        inputStream.use { source ->
            incomingFile.outputStream().use { target ->
                source.copyTo(target)
            }
        }

        closeDatabase()
        try {
            prepareRollbackFile(restoreFile, rollbackFile)
            deleteDatabaseSidecarFiles()
            incomingFile.copyTo(restoreFile, overwrite = true)
            reopenDatabase()
        } catch (error: Throwable) {
            restoreRollbackFile(restoreFile, rollbackFile)
            runCatching { reopenDatabase() }
            throw error
        } finally {
            incomingFile.delete()
            rollbackFile.delete()
        }
    }

    private fun checkpointDatabase() {
        databaseProvider().openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { cursor ->
            cursor.moveToFirst()
        }
    }

    private fun prepareRollbackFile(
        restoreFile: File,
        rollbackFile: File,
    ) {
        rollbackFile.delete()
        if (!restoreFile.exists()) {
            return
        }

        restoreFile.copyTo(rollbackFile, overwrite = true)
    }

    private fun restoreRollbackFile(
        restoreFile: File,
        rollbackFile: File,
    ) {
        deleteDatabaseSidecarFiles()
        if (!rollbackFile.exists()) {
            restoreFile.delete()
            return
        }

        rollbackFile.copyTo(restoreFile, overwrite = true)
    }

    private fun deleteDatabaseSidecarFiles() {
        databaseFile("-wal").delete()
        databaseFile("-shm").delete()
        databaseFile("-journal").delete()
    }

    private fun databaseFile(suffix: String = "") = context.getDatabasePath("$databaseName$suffix")
}
