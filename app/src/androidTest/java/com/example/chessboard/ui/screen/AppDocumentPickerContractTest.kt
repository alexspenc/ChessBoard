package com.example.chessboard.ui.screen

/*
 * File role: verifies reusable app document-picker intents and result parsing without opening the system picker.
 * Keep ACTION_CREATE_DOCUMENT, ACTION_OPEN_DOCUMENT, initial URI, MIME, and result assertions here.
 * Do not add Compose UI, feature-specific file I/O, or real document-provider tests.
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

class AppDocumentPickerContractTest {
    @Test
    fun createContractUsesSuggestedNameMimeTypeAndInitialDirectory() {
        val contract =
            AppDocumentCreationContract(
                mimeType = "application/x-chess-pgn",
                includeOpenableCategory = true,
            )
        val request =
            AppDocumentCreationRequest(
                suggestedFileName = "analysis-games.pgn",
                initialDirectoryUri = GameAnalysisUri,
            )

        val intent = contract.createIntent(context(), request)

        assertEquals(Intent.ACTION_CREATE_DOCUMENT, intent.action)
        assertTrue(intent.hasCategory(Intent.CATEGORY_OPENABLE))
        assertEquals("application/x-chess-pgn", intent.type)
        assertEquals("analysis-games.pgn", intent.getStringExtra(Intent.EXTRA_TITLE))
        assertEquals(GameAnalysisUri, getInitialDirectoryUri(intent))
    }

    @Test
    fun createContractCanPreserveAndroidXIntentWithoutOpenableCategory() {
        val contract =
            AppDocumentCreationContract(
                mimeType = "application/x-chess-pgn",
                includeOpenableCategory = false,
            )
        val request =
            AppDocumentCreationRequest(
                suggestedFileName = "analysis-games.pgn",
                initialDirectoryUri = GameAnalysisUri,
            )

        val intent = contract.createIntent(context(), request)

        assertFalse(intent.hasCategory(Intent.CATEGORY_OPENABLE))
    }

    @Test
    fun openContractUsesMimeTypesAndInitialDirectoryWhenAvailable() {
        val contract = AppDocumentSelectionContract()
        val mimeTypes = arrayOf("application/x-chess-pgn", "text/plain")
        val request =
            AppDocumentSelectionRequest(
                mimeTypes = mimeTypes,
                initialDirectoryUri = GameAnalysisUri,
            )

        val intent = contract.createIntent(context(), request)

        assertEquals(Intent.ACTION_OPEN_DOCUMENT, intent.action)
        assertTrue(intent.hasCategory(Intent.CATEGORY_OPENABLE))
        assertEquals("*/*", intent.type)
        assertArrayEquals(mimeTypes, intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES))
        assertEquals(GameAnalysisUri, getInitialDirectoryUri(intent))
    }

    @Test
    fun openContractOmitsInitialDirectoryWhenStorageIsUnavailable() {
        val contract = AppDocumentSelectionContract()
        val request =
            AppDocumentSelectionRequest(
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
        val createContract =
            AppDocumentCreationContract(
                mimeType = "application/x-chess-pgn",
                includeOpenableCategory = false,
            )
        val openContract = AppDocumentSelectionContract()

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
        val GameAnalysisUri: Uri = Uri.parse("content://test/tree/chessboard/game-analysis")
    }
}
