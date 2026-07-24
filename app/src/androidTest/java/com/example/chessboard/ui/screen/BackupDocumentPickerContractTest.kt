package com.example.chessboard.ui.screen

/*
 * File role: verifies backup picker intents and result parsing without opening the system picker.
 * Keep ACTION_CREATE_DOCUMENT, ACTION_OPEN_DOCUMENT, initial URI, MIME, and result assertions here.
 * Do not add Compose UI, backup file I/O, or real document-provider tests.
 * Validation date: 2026-07-24
 */

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupDocumentPickerContractTest {
    @Test
    fun createContractUsesSuggestedNameMimeTypeAndInitialDirectory() {
        val contract = BackupDocumentCreationContract("application/x-chess-pgn")
        val request =
            BackupDocumentCreationRequest(
                suggestedFileName = "lines-backup.pgn",
                initialDirectoryUri = LineBackupsUri,
            )

        val intent = contract.createIntent(context(), request)

        assertEquals(Intent.ACTION_CREATE_DOCUMENT, intent.action)
        assertTrue(intent.hasCategory(Intent.CATEGORY_OPENABLE))
        assertEquals("application/x-chess-pgn", intent.type)
        assertEquals("lines-backup.pgn", intent.getStringExtra(Intent.EXTRA_TITLE))
        assertEquals(LineBackupsUri, getInitialDirectoryUri(intent))
    }

    @Test
    fun openContractUsesMimeTypesAndInitialDirectoryWhenAvailable() {
        val contract = OpenBackupDocumentContract()
        val mimeTypes = arrayOf(FullDatabaseBackupMimeType)
        val request =
            BackupDocumentRequest(
                mimeTypes = mimeTypes,
                initialDirectoryUri = DatabaseBackupsUri,
            )

        val intent = contract.createIntent(context(), request)

        assertEquals(Intent.ACTION_OPEN_DOCUMENT, intent.action)
        assertTrue(intent.hasCategory(Intent.CATEGORY_OPENABLE))
        assertArrayEquals(mimeTypes, intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES))
        assertEquals(DatabaseBackupsUri, getInitialDirectoryUri(intent))
    }

    @Test
    fun openContractOmitsInitialDirectoryWhenStorageIsUnavailable() {
        val contract = OpenBackupDocumentContract()
        val request =
            BackupDocumentRequest(
                mimeTypes = arrayOf("*/*"),
                initialDirectoryUri = null,
            )

        val intent = contract.createIntent(context(), request)

        assertFalse(intent.hasExtra(DocumentsContract.EXTRA_INITIAL_URI))
    }

    @Test
    fun contractsReturnSelectedUriOnlyForSuccessfulResult() {
        val selectedUri = Uri.parse("content://test/document/backup")
        val resultIntent = Intent().setData(selectedUri)
        val createContract = BackupDocumentCreationContract("application/x-chess-pgn")
        val openContract = OpenBackupDocumentContract()

        assertEquals(selectedUri, createContract.parseResult(Activity.RESULT_OK, resultIntent))
        assertEquals(selectedUri, openContract.parseResult(Activity.RESULT_OK, resultIntent))
        assertNull(createContract.parseResult(Activity.RESULT_CANCELED, resultIntent))
        assertNull(openContract.parseResult(Activity.RESULT_OK, null))
    }

    private fun context(): Context {
        return InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Suppress("DEPRECATION")
    private fun getInitialDirectoryUri(intent: Intent): Uri? {
        return intent.getParcelableExtra(DocumentsContract.EXTRA_INITIAL_URI)
    }

    private companion object {
        val LineBackupsUri: Uri = Uri.parse("content://test/tree/chessboard/line-backups")
        val DatabaseBackupsUri: Uri = Uri.parse("content://test/tree/chessboard/database-backups")
    }
}
