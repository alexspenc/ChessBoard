package com.example.chessboard.service

/*
 * File role: persists the SAF tree URI selected as the ChessBoard document root.
 * Keep private preference access and root-URI persistence here.
 * Do not add document-provider operations, permission handling, UI state, or file export logic.
 * Validation date: 2026-07-24
 */

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal interface AppDocumentRootStore {
    suspend fun getRootUri(): String?

    suspend fun saveRootUri(rootUri: String)

    suspend fun clearRootUri()
}

internal class SharedPreferencesAppDocumentRootStore(
    context: Context,
) : AppDocumentRootStore {
    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(
            PreferencesName,
            Context.MODE_PRIVATE,
        )

    override suspend fun getRootUri(): String? {
        return withContext(Dispatchers.IO) {
            preferences.getString(RootUriKey, null)
        }
    }

    override suspend fun saveRootUri(rootUri: String) {
        withContext(Dispatchers.IO) {
            val saved =
                preferences
                    .edit()
                    .putString(RootUriKey, rootUri)
                    .commit()
            requireSuccessfulPreferenceWrite(saved)
        }
    }

    override suspend fun clearRootUri() {
        withContext(Dispatchers.IO) {
            val saved =
                preferences
                    .edit()
                    .remove(RootUriKey)
                    .commit()
            requireSuccessfulPreferenceWrite(saved)
        }
    }

    private fun requireSuccessfulPreferenceWrite(saved: Boolean) {
        if (!saved) {
            throw IllegalStateException("Failed to persist the app document root setting.")
        }
    }

    private companion object {
        const val PreferencesName = "app_document_storage"
        const val RootUriKey = "root_tree_uri"
    }
}
