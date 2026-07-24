package com.example.chessboard.ui.screen

/*
 * File role: defines backup document-picker requests, contracts, and MIME selection policy.
 * Keep ACTION_CREATE_DOCUMENT and ACTION_OPEN_DOCUMENT intent construction here.
 * Do not add backup file I/O, Compose launchers, folder setup UI, or restore workflows.
 * Validation date: 2026-07-24
 */

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContract

internal const val FullDatabaseBackupMimeType = "application/vnd.sqlite3"
private const val GenericBinaryMimeType = "application/octet-stream"
private const val AnyFileMimeType = "*/*"

internal data class BackupDocumentCreationRequest(
    val suggestedFileName: String,
    val initialDirectoryUri: Uri,
)

internal data class BackupDocumentRequest(
    val mimeTypes: Array<String>,
    val initialDirectoryUri: Uri?,
)

// TODO: Let the user select an existing backup file and overwrite it while saving a backup.
// Revisit matching the AndroidX CreateDocument intent without CATEGORY_OPENABLE and use
// truncating "wt" output mode before treating an existing document URI as a save destination.
internal class BackupDocumentCreationContract(
    private val mimeType: String,
) : ActivityResultContract<BackupDocumentCreationRequest, Uri?>() {
    override fun createIntent(
        context: Context,
        input: BackupDocumentCreationRequest,
    ): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(mimeType)
            .putExtra(Intent.EXTRA_TITLE, input.suggestedFileName)
            .putExtra(DocumentsContract.EXTRA_INITIAL_URI, input.initialDirectoryUri)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): Uri? {
        if (resultCode != Activity.RESULT_OK) {
            return null
        }
        if (intent == null) {
            return null
        }

        return intent.data
    }
}

internal class OpenBackupDocumentContract :
    ActivityResultContract<BackupDocumentRequest, Uri?>() {
    override fun createIntent(
        context: Context,
        input: BackupDocumentRequest,
    ): Intent {
        val intent =
            Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(AnyFileMimeType)
                .putExtra(Intent.EXTRA_MIME_TYPES, input.mimeTypes)
        val initialDirectoryUri = input.initialDirectoryUri
        if (initialDirectoryUri != null) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialDirectoryUri)
        }

        return intent
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): Uri? {
        if (resultCode != Activity.RESULT_OK) {
            return null
        }
        if (intent == null) {
            return null
        }

        return intent.data
    }
}

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
