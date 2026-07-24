package com.example.chessboard.service

/*
 * File role: manages Android persisted SAF permissions for the selected app document tree.
 * Keep ContentResolver permission inspection, acquisition, and release here.
 * Do not add URI preference storage, directory creation, UI launchers, or screen state.
 * Validation date: 2026-07-24
 */

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri

internal interface AppDocumentPermissionGateway {
    fun hasReadWritePermission(rootUri: Uri): Boolean

    fun takeReadWritePermission(rootUri: Uri)

    fun releasePersistedPermission(rootUri: Uri)
}

internal class ContentResolverAppDocumentPermissionGateway(
    private val contentResolver: ContentResolver,
) : AppDocumentPermissionGateway {
    override fun hasReadWritePermission(rootUri: Uri): Boolean {
        for (permission in contentResolver.persistedUriPermissions) {
            if (permission.uri != rootUri) {
                continue
            }
            if (!permission.isReadPermission) {
                continue
            }
            if (!permission.isWritePermission) {
                continue
            }

            return true
        }

        return false
    }

    override fun takeReadWritePermission(rootUri: Uri) {
        contentResolver.takePersistableUriPermission(
            rootUri,
            ReadWritePermissionFlags,
        )
    }

    override fun releasePersistedPermission(rootUri: Uri) {
        val permissionFlags = resolvePersistedPermissionFlags(rootUri)
        if (permissionFlags == 0) {
            return
        }

        contentResolver.releasePersistableUriPermission(
            rootUri,
            permissionFlags,
        )
    }

    private fun resolvePersistedPermissionFlags(rootUri: Uri): Int {
        var permissionFlags = 0
        for (permission in contentResolver.persistedUriPermissions) {
            if (permission.uri != rootUri) {
                continue
            }
            if (permission.isReadPermission) {
                permissionFlags = permissionFlags or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            if (permission.isWritePermission) {
                permissionFlags = permissionFlags or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            }
        }

        return permissionFlags
    }

    private companion object {
        val ReadWritePermissionFlags =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }
}
