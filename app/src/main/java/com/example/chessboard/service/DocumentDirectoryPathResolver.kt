package com.example.chessboard.service

/*
 * File role: resolves directory paths over an abstract document tree.
 * Keep provider-independent directory lookup, creation, validation, and conflict handling here.
 * Do not add Android ContentResolver calls, UI folder selection, or persisted URI settings.
 * Validation date: 2026-07-24
 */

internal data class DocumentTreeEntry<T>(
    val reference: T,
    val displayName: String,
    val isDirectory: Boolean,
)

internal interface DocumentTreeGateway<T> {
    fun listChildren(parentReference: T): List<DocumentTreeEntry<T>>

    fun createDirectory(
        parentReference: T,
        displayName: String,
    ): T
}

internal class DocumentDirectoryPathResolver<T>(
    private val gateway: DocumentTreeGateway<T>,
) {
    fun resolveOrCreateDirectory(
        parentReference: T,
        directoryName: String,
    ): T {
        validateDirectoryName(directoryName)

        val matchingEntries =
            gateway
                .listChildren(parentReference)
                .filter { entry -> entry.displayName == directoryName }
        val conflictingFile = matchingEntries.firstOrNull { entry -> !entry.isDirectory }
        if (conflictingFile != null) {
            throw IllegalStateException(
                "A document named '$directoryName' already exists and is not a directory.",
            )
        }

        val existingDirectory = matchingEntries.firstOrNull()
        if (existingDirectory != null) {
            return existingDirectory.reference
        }

        return gateway.createDirectory(
            parentReference = parentReference,
            displayName = directoryName,
        )
    }

    fun resolveOrCreatePath(
        rootReference: T,
        directoryNames: List<String>,
    ): T {
        directoryNames.forEach { directoryName ->
            validateDirectoryName(directoryName)
        }

        var currentReference = rootReference
        directoryNames.forEach { directoryName ->
            currentReference =
                resolveOrCreateDirectory(
                    parentReference = currentReference,
                    directoryName = directoryName,
                )
        }

        return currentReference
    }

    private fun validateDirectoryName(directoryName: String) {
        if (directoryName.isBlank()) {
            throw IllegalArgumentException("Directory name must not be blank.")
        }
        if (directoryName == "." || directoryName == "..") {
            throw IllegalArgumentException("Directory name must not be '$directoryName'.")
        }
        if (directoryName.contains('/')) {
            throw IllegalArgumentException("Directory name must not contain '/'.")
        }
    }
}
