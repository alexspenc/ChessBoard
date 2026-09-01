package com.example.chessboard.service

/*
 * File role: lists full database backup documents from the configured SAF directory.
 * Keep document-provider queries and SQLite backup filename filtering here.
 * Do not add Compose UI, picker launchers, database restore operations, or folder setup logic.
 * Validation date: 2026-08-31
 */

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class FullDatabaseBackupDocument(
    val uri: Uri,
    val displayName: String,
)

internal class FullDatabaseBackupDocumentService(
    private val contentResolver: ContentResolver,
) {
    suspend fun listBackups(directoryUri: Uri): List<FullDatabaseBackupDocument> {
        return withContext(Dispatchers.IO) {
            queryBackups(directoryUri)
        }
    }

    private fun queryBackups(directoryUri: Uri): List<FullDatabaseBackupDocument> {
        val directoryDocumentId = resolveDocumentId(directoryUri)
        val childrenUri =
            DocumentsContract.buildChildDocumentsUriUsingTree(
                directoryUri,
                directoryDocumentId,
            )
        val projection =
            arrayOf(
                Document.COLUMN_DOCUMENT_ID,
                Document.COLUMN_DISPLAY_NAME,
                Document.COLUMN_MIME_TYPE,
            )
        val cursor =
            contentResolver.query(
                childrenUri,
                projection,
                null,
                null,
                null,
            )
        if (cursor == null) {
            throw IllegalStateException("Document provider did not return database backups.")
        }

        cursor.use {
            val documentIdColumn = cursor.getColumnIndexOrThrow(Document.COLUMN_DOCUMENT_ID)
            val displayNameColumn = cursor.getColumnIndexOrThrow(Document.COLUMN_DISPLAY_NAME)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(Document.COLUMN_MIME_TYPE)
            val backups = mutableListOf<FullDatabaseBackupDocument>()
            while (cursor.moveToNext()) {
                val mimeType = cursor.getString(mimeTypeColumn)
                if (mimeType == Document.MIME_TYPE_DIR) {
                    continue
                }

                val displayName = cursor.getString(displayNameColumn)
                if (!isFullDatabaseBackupFileName(displayName)) {
                    continue
                }

                val documentId = cursor.getString(documentIdColumn)
                backups.add(
                    FullDatabaseBackupDocument(
                        uri =
                            DocumentsContract.buildDocumentUriUsingTree(
                                directoryUri,
                                documentId,
                            ),
                        displayName = displayName,
                    ),
                )
            }

            return backups.sortedByDescending { backup -> backup.displayName }
        }
    }

    private fun resolveDocumentId(documentUri: Uri): String {
        try {
            return DocumentsContract.getDocumentId(documentUri)
        } catch (_: IllegalArgumentException) {
            return DocumentsContract.getTreeDocumentId(documentUri)
        }
    }
}

internal fun isFullDatabaseBackupFileName(displayName: String): Boolean {
    return displayName.endsWith(".sqlite3", ignoreCase = true)
}
