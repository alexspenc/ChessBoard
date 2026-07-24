package com.example.chessboard.service

/*
 * File role: defines and implements coordinated access to the configured ChessBoard document tree.
 * Keep the public storage contract, serialized setup workflow, and dependency assembly here.
 * Do not add Compose state, folder-picker launchers, backup serialization, or screen-specific behavior.
 * Validation date: 2026-07-24
 */

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * App-scoped access to the configured ChessBoard document tree.
 *
 * Create one instance at the app composition root and pass this interface to each consumer. A
 * shared instance is required so all document-tree setup operations use the same serialization
 * boundary.
 */
interface AppDocumentStorage {
    suspend fun loadState(): State

    suspend fun configureRoot(rootUri: Uri): State.Ready

    suspend fun disconnectRoot()

    sealed interface State {
        data object NotConfigured : State

        data class Ready(
            val structure: AppDocumentStructure,
        ) : State

        data class PermissionLost(
            val rootUri: Uri,
        ) : State
    }
}

internal class AndroidAppDocumentStorage(
    private val structureService: AppDocumentStructureService,
    private val rootStore: AppDocumentRootStore,
    private val permissionGateway: AppDocumentPermissionGateway,
) : AppDocumentStorage {
    private val operationMutex = Mutex()

    override suspend fun loadState(): AppDocumentStorage.State {
        return operationMutex.withLock {
            loadStateLocked()
        }
    }

    override suspend fun configureRoot(rootUri: Uri): AppDocumentStorage.State.Ready {
        return operationMutex.withLock {
            configureRootLocked(rootUri)
        }
    }

    override suspend fun disconnectRoot() {
        operationMutex.withLock {
            disconnectRootLocked()
        }
    }

    private suspend fun loadStateLocked(): AppDocumentStorage.State {
        val rootUriValue = rootStore.getRootUri()
        if (rootUriValue == null) {
            return AppDocumentStorage.State.NotConfigured
        }

        val rootUri = Uri.parse(rootUriValue)
        if (!permissionGateway.hasReadWritePermission(rootUri)) {
            return AppDocumentStorage.State.PermissionLost(rootUri)
        }

        val structure = structureService.ensureAppStructure(rootUri)
        return AppDocumentStorage.State.Ready(structure)
    }

    private suspend fun configureRootLocked(rootUri: Uri): AppDocumentStorage.State.Ready {
        val previousRootUri = getStoredRootUri()
        val permissionAlreadyPersisted = permissionGateway.hasReadWritePermission(rootUri)
        if (!permissionAlreadyPersisted) {
            permissionGateway.takeReadWritePermission(rootUri)
        }

        val structure: AppDocumentStructure
        try {
            structure = structureService.ensureAppStructure(rootUri)
            rootStore.saveRootUri(rootUri.toString())
        } catch (error: Exception) {
            rollbackNewPermission(
                rootUri = rootUri,
                permissionAlreadyPersisted = permissionAlreadyPersisted,
                originalError = error,
            )
            throw error
        }

        releasePreviousRootPermission(
            previousRootUri = previousRootUri,
            currentRootUri = rootUri,
        )
        return AppDocumentStorage.State.Ready(structure)
    }

    private suspend fun disconnectRootLocked() {
        val rootUri = getStoredRootUri()
        if (rootUri == null) {
            return
        }

        permissionGateway.releasePersistedPermission(rootUri)
        rootStore.clearRootUri()
    }

    private suspend fun getStoredRootUri(): Uri? {
        val rootUriValue = rootStore.getRootUri()
        if (rootUriValue == null) {
            return null
        }

        return Uri.parse(rootUriValue)
    }

    private fun rollbackNewPermission(
        rootUri: Uri,
        permissionAlreadyPersisted: Boolean,
        originalError: Exception,
    ) {
        if (permissionAlreadyPersisted) {
            return
        }

        try {
            permissionGateway.releasePersistedPermission(rootUri)
        } catch (releaseError: Exception) {
            originalError.addSuppressed(releaseError)
        }
    }

    private fun releasePreviousRootPermission(
        previousRootUri: Uri?,
        currentRootUri: Uri,
    ) {
        if (previousRootUri == null) {
            return
        }
        if (previousRootUri == currentRootUri) {
            return
        }

        permissionGateway.releasePersistedPermission(previousRootUri)
    }
}

/** Creates the app-scoped implementation for the composition root. */
internal fun createAppDocumentStorage(context: Context): AppDocumentStorage {
    val applicationContext = context.applicationContext
    val contentResolver = applicationContext.contentResolver
    return AndroidAppDocumentStorage(
        structureService = AppDocumentDirectoryService(contentResolver),
        rootStore = SharedPreferencesAppDocumentRootStore(applicationContext),
        permissionGateway = ContentResolverAppDocumentPermissionGateway(contentResolver),
    )
}
