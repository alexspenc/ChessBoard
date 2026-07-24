package com.example.chessboard.service

/*
 * File role: creates and resolves the stable ChessBoard directory structure inside a granted SAF tree.
 * Keep Android document-provider access and app-owned directory definitions here.
 * Do not add Compose launchers, folder-picker dialogs, URI preference storage, or file export logic.
 * Validation date: 2026-07-24
 */

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class AppDocumentDirectory(
    val path: List<String>,
) {
    LINE_BACKUPS(listOf("line-backups")),
    DATABASE_BACKUPS(listOf("database-backups")),
    GAME_ANALYSIS(listOf("game-analysis")),
}

data class AppDocumentStructure(
    val rootUri: Uri,
    val lineBackupsUri: Uri,
    val databaseBackupsUri: Uri,
    val gameAnalysisUri: Uri,
)

/**
 * Resolves and creates app directories inside a granted document tree.
 *
 * Calls targeting the same tree must be serialized by an external coordinator. The lookup and
 * creation steps are separate document-provider operations and are not atomic.
 */
class AppDocumentDirectoryService(
    contentResolver: ContentResolver,
) {
    private val pathResolver =
        DocumentDirectoryPathResolver(
            ContentResolverDocumentTreeGateway(contentResolver),
        )

    suspend fun resolveOrCreateDirectory(
        parentUri: Uri,
        directoryName: String,
    ): Uri {
        return runDocumentTreeOperation {
            pathResolver.resolveOrCreateDirectory(
                parentReference = parentUri,
                directoryName = directoryName,
            )
        }
    }

    suspend fun resolveOrCreatePath(
        rootUri: Uri,
        directoryNames: List<String>,
    ): Uri {
        return runDocumentTreeOperation {
            pathResolver.resolveOrCreatePath(
                rootReference = rootUri,
                directoryNames = directoryNames,
            )
        }
    }

    suspend fun resolveDirectory(
        rootUri: Uri,
        directory: AppDocumentDirectory,
    ): Uri {
        return resolveOrCreatePath(
            rootUri = rootUri,
            directoryNames = directory.path,
        )
    }

    suspend fun ensureAppStructure(rootUri: Uri): AppDocumentStructure {
        return runDocumentTreeOperation {
            val lineBackupsUri =
                pathResolver.resolveOrCreatePath(
                    rootReference = rootUri,
                    directoryNames = AppDocumentDirectory.LINE_BACKUPS.path,
                )
            val databaseBackupsUri =
                pathResolver.resolveOrCreatePath(
                    rootReference = rootUri,
                    directoryNames = AppDocumentDirectory.DATABASE_BACKUPS.path,
                )
            val gameAnalysisUri =
                pathResolver.resolveOrCreatePath(
                    rootReference = rootUri,
                    directoryNames = AppDocumentDirectory.GAME_ANALYSIS.path,
                )

            AppDocumentStructure(
                rootUri = rootUri,
                lineBackupsUri = lineBackupsUri,
                databaseBackupsUri = databaseBackupsUri,
                gameAnalysisUri = gameAnalysisUri,
            )
        }
    }

    private suspend fun <T> runDocumentTreeOperation(operation: () -> T): T {
        return withContext(Dispatchers.IO) {
            operation()
        }
    }
}

private class ContentResolverDocumentTreeGateway(
    private val contentResolver: ContentResolver,
) : DocumentTreeGateway<Uri> {
    override fun listChildren(parentReference: Uri): List<DocumentTreeEntry<Uri>> {
        val parentDocumentId = resolveDocumentId(parentReference)
        val childrenUri =
            DocumentsContract.buildChildDocumentsUriUsingTree(
                parentReference,
                parentDocumentId,
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
            throw IllegalStateException("Document provider did not return directory contents.")
        }

        cursor.use {
            val documentIdColumn = cursor.getColumnIndexOrThrow(Document.COLUMN_DOCUMENT_ID)
            val displayNameColumn = cursor.getColumnIndexOrThrow(Document.COLUMN_DISPLAY_NAME)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(Document.COLUMN_MIME_TYPE)
            val entries = mutableListOf<DocumentTreeEntry<Uri>>()
            while (cursor.moveToNext()) {
                val documentId = cursor.getString(documentIdColumn)
                val displayName = cursor.getString(displayNameColumn)
                val mimeType = cursor.getString(mimeTypeColumn)
                entries.add(
                    DocumentTreeEntry(
                        reference =
                            DocumentsContract.buildDocumentUriUsingTree(
                                parentReference,
                                documentId,
                            ),
                        displayName = displayName,
                        isDirectory = mimeType == Document.MIME_TYPE_DIR,
                    ),
                )
            }

            return entries
        }
    }

    override fun createDirectory(
        parentReference: Uri,
        displayName: String,
    ): Uri {
        val parentDocumentUri =
            DocumentsContract.buildDocumentUriUsingTree(
                parentReference,
                resolveDocumentId(parentReference),
            )
        val directoryUri =
            DocumentsContract.createDocument(
                contentResolver,
                parentDocumentUri,
                Document.MIME_TYPE_DIR,
                displayName,
            )
        if (directoryUri == null) {
            throw IllegalStateException("Document provider failed to create directory '$displayName'.")
        }

        return directoryUri
    }

    private fun resolveDocumentId(documentUri: Uri): String {
        try {
            return DocumentsContract.getDocumentId(documentUri)
        } catch (_: IllegalArgumentException) {
            return DocumentsContract.getTreeDocumentId(documentUri)
        }
    }
}
