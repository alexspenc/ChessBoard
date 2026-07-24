package com.example.chessboard.ui.screen

/*
 * File role: defines reusable document-picker requests and contracts with optional initial folders.
 * Keep ACTION_CREATE_DOCUMENT and ACTION_OPEN_DOCUMENT intent construction here.
 * Do not add feature-specific MIME policies, file I/O, Compose launchers, or screen workflows.
 * Validation date: 2026-07-24
 */

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContract

private const val AnyFileMimeType = "*/*"

internal data class AppDocumentCreationRequest(
    val suggestedFileName: String,
    val initialDirectoryUri: Uri,
)

internal data class AppDocumentSelectionRequest(
    val mimeTypes: Array<String>,
    val initialDirectoryUri: Uri?,
)

internal class AppDocumentCreationContract(
    private val mimeType: String,
    private val includeOpenableCategory: Boolean,
) : ActivityResultContract<AppDocumentCreationRequest, Uri?>() {
    override fun createIntent(
        context: Context,
        input: AppDocumentCreationRequest,
    ): Intent {
        val intent =
            Intent(Intent.ACTION_CREATE_DOCUMENT)
                .setType(mimeType)
                .putExtra(Intent.EXTRA_TITLE, input.suggestedFileName)
                .putExtra(DocumentsContract.EXTRA_INITIAL_URI, input.initialDirectoryUri)
        if (includeOpenableCategory) {
            intent.addCategory(Intent.CATEGORY_OPENABLE)
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

internal class AppDocumentSelectionContract :
    ActivityResultContract<AppDocumentSelectionRequest, Uri?>() {
    override fun createIntent(
        context: Context,
        input: AppDocumentSelectionRequest,
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
