package com.example.chessboard.service

/*
 * File role: verifies provider-independent document directory path resolution.
 * Keep fake-tree tests for lookup, creation, validation, conflicts, and idempotency here.
 * Do not add Android URI, ContentResolver, Compose UI, or real filesystem tests.
 * Validation date: 2026-07-24
 */

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DocumentDirectoryPathResolverTest {
    @Test
    fun `existing directory is returned without creating a duplicate`() {
        val gateway = FakeDocumentTreeGateway()
        gateway.addDirectory(
            parentReference = RootReference,
            displayName = "database-backups",
            reference = "existing-database-backups",
        )
        val resolver = DocumentDirectoryPathResolver(gateway)

        val result =
            resolver.resolveOrCreateDirectory(
                parentReference = RootReference,
                directoryName = "database-backups",
            )

        assertEquals("existing-database-backups", result)
        assertEquals(0, gateway.createDirectoryCallsCount)
    }

    @Test
    fun `missing directory is created`() {
        val gateway = FakeDocumentTreeGateway()
        val resolver = DocumentDirectoryPathResolver(gateway)

        val result =
            resolver.resolveOrCreateDirectory(
                parentReference = RootReference,
                directoryName = "line-backups",
            )

        assertEquals("root/line-backups", result)
        assertEquals(1, gateway.createDirectoryCallsCount)
    }

    @Test
    fun `nested path is resolved and missing segments are created`() {
        val gateway = FakeDocumentTreeGateway()
        gateway.addDirectory(
            parentReference = RootReference,
            displayName = "backups",
            reference = "existing-backups",
        )
        val resolver = DocumentDirectoryPathResolver(gateway)

        val result =
            resolver.resolveOrCreatePath(
                rootReference = RootReference,
                directoryNames = listOf("backups", "database"),
            )

        assertEquals("existing-backups/database", result)
        assertEquals(1, gateway.createDirectoryCallsCount)
    }

    @Test
    fun `repeated path resolution is idempotent`() {
        val gateway = FakeDocumentTreeGateway()
        val resolver = DocumentDirectoryPathResolver(gateway)

        val firstResult =
            resolver.resolveOrCreatePath(
                rootReference = RootReference,
                directoryNames = listOf("game-analysis"),
            )
        val secondResult =
            resolver.resolveOrCreatePath(
                rootReference = RootReference,
                directoryNames = listOf("game-analysis"),
            )

        assertEquals(firstResult, secondResult)
        assertEquals(1, gateway.createDirectoryCallsCount)
    }

    @Test
    fun `file with requested directory name is reported as conflict`() {
        val gateway = FakeDocumentTreeGateway()
        gateway.addFile(
            parentReference = RootReference,
            displayName = "database-backups",
            reference = "conflicting-file",
        )
        val resolver = DocumentDirectoryPathResolver(gateway)

        val error =
            assertThrows(IllegalStateException::class.java) {
                resolver.resolveOrCreateDirectory(
                    parentReference = RootReference,
                    directoryName = "database-backups",
                )
            }

        assertEquals(
            "A document named 'database-backups' already exists and is not a directory.",
            error.message,
        )
        assertEquals(0, gateway.createDirectoryCallsCount)
    }

    @Test
    fun `invalid directory names are rejected before path creation starts`() {
        val gateway = FakeDocumentTreeGateway()
        val resolver = DocumentDirectoryPathResolver(gateway)

        assertThrows(IllegalArgumentException::class.java) {
            resolver.resolveOrCreateDirectory(
                parentReference = RootReference,
                directoryName = " ",
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            resolver.resolveOrCreateDirectory(
                parentReference = RootReference,
                directoryName = "..",
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            resolver.resolveOrCreatePath(
                rootReference = RootReference,
                directoryNames = listOf("backups", "database/legacy"),
            )
        }
        assertEquals(0, gateway.createDirectoryCallsCount)
    }

    @Test
    fun `provider creation failure is propagated`() {
        val gateway = FakeDocumentTreeGateway()
        gateway.createDirectoryError = IllegalStateException("provider unavailable")
        val resolver = DocumentDirectoryPathResolver(gateway)

        val error =
            assertThrows(IllegalStateException::class.java) {
                resolver.resolveOrCreateDirectory(
                    parentReference = RootReference,
                    directoryName = "line-backups",
                )
            }

        assertEquals("provider unavailable", error.message)
    }

    private class FakeDocumentTreeGateway : DocumentTreeGateway<String> {
        private val childrenByParent = mutableMapOf<String, MutableList<DocumentTreeEntry<String>>>()

        var createDirectoryCallsCount: Int = 0
            private set
        var createDirectoryError: RuntimeException? = null

        override fun listChildren(parentReference: String): List<DocumentTreeEntry<String>> {
            return childrenByParent[parentReference].orEmpty()
        }

        override fun createDirectory(
            parentReference: String,
            displayName: String,
        ): String {
            createDirectoryCallsCount += 1
            val currentError = createDirectoryError
            if (currentError != null) {
                throw currentError
            }

            val reference = "$parentReference/$displayName"
            addDirectory(
                parentReference = parentReference,
                displayName = displayName,
                reference = reference,
            )
            return reference
        }

        fun addDirectory(
            parentReference: String,
            displayName: String,
            reference: String,
        ) {
            addEntry(
                parentReference = parentReference,
                entry =
                    DocumentTreeEntry(
                        reference = reference,
                        displayName = displayName,
                        isDirectory = true,
                    ),
            )
        }

        fun addFile(
            parentReference: String,
            displayName: String,
            reference: String,
        ) {
            addEntry(
                parentReference = parentReference,
                entry =
                    DocumentTreeEntry(
                        reference = reference,
                        displayName = displayName,
                        isDirectory = false,
                    ),
            )
        }

        private fun addEntry(
            parentReference: String,
            entry: DocumentTreeEntry<String>,
        ) {
            val children = childrenByParent.getOrPut(parentReference) { mutableListOf() }
            children.add(entry)
        }
    }

    private companion object {
        const val RootReference = "root"
    }
}
