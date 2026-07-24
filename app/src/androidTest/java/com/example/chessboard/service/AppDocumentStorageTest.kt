package com.example.chessboard.service

/*
 * File role: verifies coordinated app document root persistence, permission, and structure behavior.
 * Keep tests for storage state, setup rollback, replacement, disconnection, and serialization here.
 * Do not add system picker UI, real document-provider, backup-format, or Compose tests.
 * Validation date: 2026-07-24
 */

import android.net.Uri
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDocumentStorageTest {
    @Test
    fun loadStateReturnsNotConfiguredWhenRootIsNotStored() = runBlocking {
        val fixture = createFixture()

        val state = fixture.storage.loadState()

        assertSame(AppDocumentStorage.State.NotConfigured, state)
        assertEquals(0, fixture.structureService.ensureCallsCount)
    }

    @Test
    fun loadStateReturnsPermissionLostWhenPersistedPermissionIsMissing() = runBlocking {
        val fixture = createFixture(storedRootUri = FirstRootUri)

        val state = fixture.storage.loadState()

        assertEquals(AppDocumentStorage.State.PermissionLost(FirstRootUri), state)
        assertEquals(0, fixture.structureService.ensureCallsCount)
    }

    @Test
    fun loadStateEnsuresStructureWhenRootAndPermissionAreAvailable() = runBlocking {
        val fixture =
            createFixture(
                storedRootUri = FirstRootUri,
                permittedRootUris = setOf(FirstRootUri),
            )

        val state = fixture.storage.loadState()

        assertEquals(AppDocumentStorage.State.Ready(firstStructure()), state)
        assertEquals(listOf(FirstRootUri), fixture.structureService.ensuredRootUris)
    }

    @Test
    fun configureRootPersistsOnlyAfterPermissionAndStructureAreReady() = runBlocking {
        val events = mutableListOf<String>()
        val fixture = createFixture(events = events)

        val state = fixture.storage.configureRoot(FirstRootUri)

        assertEquals(AppDocumentStorage.State.Ready(firstStructure()), state)
        assertEquals(FirstRootUri.toString(), fixture.rootStore.rootUri)
        assertEquals(
            listOf("take:$FirstRootUri", "ensure:$FirstRootUri", "save:$FirstRootUri"),
            events,
        )
    }

    @Test
    fun configureRootKeepsPreviousRootAndReleasesNewPermissionAfterStructureFailure() {
        val fixture =
            createFixture(
                storedRootUri = FirstRootUri,
                permittedRootUris = setOf(FirstRootUri),
            )
        fixture.structureService.ensureError = IllegalStateException("provider unavailable")

        val error =
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    fixture.storage.configureRoot(SecondRootUri)
                }
            }

        assertEquals("provider unavailable", error.message)
        assertEquals(FirstRootUri.toString(), fixture.rootStore.rootUri)
        assertTrue(fixture.permissionGateway.hasReadWritePermission(FirstRootUri))
        assertFalse(fixture.permissionGateway.hasReadWritePermission(SecondRootUri))
        assertEquals(listOf(SecondRootUri), fixture.permissionGateway.releasedRootUris)
    }

    @Test
    fun configureRootKeepsPreviousRootAndReleasesNewPermissionAfterPreferenceFailure() {
        val fixture =
            createFixture(
                storedRootUri = FirstRootUri,
                permittedRootUris = setOf(FirstRootUri),
            )
        fixture.rootStore.saveError = IllegalStateException("preferences unavailable")

        val error =
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    fixture.storage.configureRoot(SecondRootUri)
                }
            }

        assertEquals("preferences unavailable", error.message)
        assertEquals(FirstRootUri.toString(), fixture.rootStore.rootUri)
        assertTrue(fixture.permissionGateway.hasReadWritePermission(FirstRootUri))
        assertFalse(fixture.permissionGateway.hasReadWritePermission(SecondRootUri))
        assertEquals(listOf(SecondRootUri), fixture.permissionGateway.releasedRootUris)
    }

    @Test
    fun configureRootReleasesPreviousPermissionAfterSuccessfulReplacement() = runBlocking {
        val fixture =
            createFixture(
                storedRootUri = FirstRootUri,
                permittedRootUris = setOf(FirstRootUri),
            )

        fixture.storage.configureRoot(SecondRootUri)

        assertEquals(SecondRootUri.toString(), fixture.rootStore.rootUri)
        assertFalse(fixture.permissionGateway.hasReadWritePermission(FirstRootUri))
        assertTrue(fixture.permissionGateway.hasReadWritePermission(SecondRootUri))
        assertEquals(listOf(FirstRootUri), fixture.permissionGateway.releasedRootUris)
    }

    @Test
    fun configureRootDoesNotReleasePermissionWhenRootIsUnchanged() = runBlocking {
        val fixture =
            createFixture(
                storedRootUri = FirstRootUri,
                permittedRootUris = setOf(FirstRootUri),
            )

        fixture.storage.configureRoot(FirstRootUri)

        assertTrue(fixture.permissionGateway.releasedRootUris.isEmpty())
        assertTrue(fixture.permissionGateway.takenRootUris.isEmpty())
    }

    @Test
    fun disconnectRootReleasesPermissionAndClearsStoredRoot() = runBlocking {
        val fixture =
            createFixture(
                storedRootUri = FirstRootUri,
                permittedRootUris = setOf(FirstRootUri),
            )

        fixture.storage.disconnectRoot()

        assertNull(fixture.rootStore.rootUri)
        assertFalse(fixture.permissionGateway.hasReadWritePermission(FirstRootUri))
        assertEquals(listOf(FirstRootUri), fixture.permissionGateway.releasedRootUris)
    }

    @Test
    fun documentTreeOperationsAreSerialized() = runBlocking {
        val fixture = createFixture()
        fixture.structureService.blockFirstEnsure = true

        val firstConfiguration =
            async {
                fixture.storage.configureRoot(FirstRootUri)
            }
        fixture.structureService.firstEnsureStarted.await()
        val secondConfiguration =
            async {
                fixture.storage.configureRoot(SecondRootUri)
            }
        yield()

        assertEquals(1, fixture.structureService.ensureCallsCount)

        fixture.structureService.continueFirstEnsure.complete(Unit)
        firstConfiguration.await()
        secondConfiguration.await()

        assertEquals(2, fixture.structureService.ensureCallsCount)
        assertEquals(1, fixture.structureService.maximumConcurrentEnsures)
    }

    private fun createFixture(
        storedRootUri: Uri? = null,
        permittedRootUris: Set<Uri> = emptySet(),
        events: MutableList<String> = mutableListOf(),
    ): Fixture {
        val rootStore = FakeAppDocumentRootStore(storedRootUri?.toString(), events)
        val permissionGateway = FakeAppDocumentPermissionGateway(permittedRootUris, events)
        val structureService = FakeAppDocumentStructureService(events)
        val storage =
            AndroidAppDocumentStorage(
                structureService = structureService,
                rootStore = rootStore,
                permissionGateway = permissionGateway,
            )
        return Fixture(
            storage = storage,
            rootStore = rootStore,
            permissionGateway = permissionGateway,
            structureService = structureService,
        )
    }

    private fun firstStructure(): AppDocumentStructure {
        return buildStructure(FirstRootUri)
    }

    private fun buildStructure(rootUri: Uri): AppDocumentStructure {
        return AppDocumentStructure(
            rootUri = rootUri,
            lineBackupsUri = Uri.parse("$rootUri/line-backups"),
            databaseBackupsUri = Uri.parse("$rootUri/database-backups"),
            gameAnalysisUri = Uri.parse("$rootUri/game-analysis"),
        )
    }

    private data class Fixture(
        val storage: AppDocumentStorage,
        val rootStore: FakeAppDocumentRootStore,
        val permissionGateway: FakeAppDocumentPermissionGateway,
        val structureService: FakeAppDocumentStructureService,
    )

    private class FakeAppDocumentRootStore(
        var rootUri: String?,
        private val events: MutableList<String>,
    ) : AppDocumentRootStore {
        var saveError: RuntimeException? = null

        override suspend fun getRootUri(): String? {
            return rootUri
        }

        override suspend fun saveRootUri(rootUri: String) {
            events.add("save:$rootUri")
            val currentError = saveError
            if (currentError != null) {
                throw currentError
            }

            this.rootUri = rootUri
        }

        override suspend fun clearRootUri() {
            events.add("clear")
            rootUri = null
        }
    }

    private class FakeAppDocumentPermissionGateway(
        permittedRootUris: Set<Uri>,
        private val events: MutableList<String>,
    ) : AppDocumentPermissionGateway {
        private val permissions = permittedRootUris.toMutableSet()

        val takenRootUris = mutableListOf<Uri>()
        val releasedRootUris = mutableListOf<Uri>()

        override fun hasReadWritePermission(rootUri: Uri): Boolean {
            return permissions.contains(rootUri)
        }

        override fun takeReadWritePermission(rootUri: Uri) {
            events.add("take:$rootUri")
            takenRootUris.add(rootUri)
            permissions.add(rootUri)
        }

        override fun releasePersistedPermission(rootUri: Uri) {
            events.add("release:$rootUri")
            releasedRootUris.add(rootUri)
            permissions.remove(rootUri)
        }
    }

    private class FakeAppDocumentStructureService(
        private val events: MutableList<String>,
    ) : AppDocumentStructureService {
        val firstEnsureStarted = CompletableDeferred<Unit>()
        val continueFirstEnsure = CompletableDeferred<Unit>()
        val ensuredRootUris = mutableListOf<Uri>()

        var blockFirstEnsure = false
        var ensureError: RuntimeException? = null
        var ensureCallsCount = 0
            private set
        var maximumConcurrentEnsures = 0
            private set

        private var concurrentEnsures = 0

        override suspend fun ensureAppStructure(rootUri: Uri): AppDocumentStructure {
            ensureCallsCount += 1
            concurrentEnsures += 1
            maximumConcurrentEnsures = maxOf(maximumConcurrentEnsures, concurrentEnsures)
            events.add("ensure:$rootUri")
            ensuredRootUris.add(rootUri)

            try {
                if (blockFirstEnsure && ensureCallsCount == 1) {
                    firstEnsureStarted.complete(Unit)
                    continueFirstEnsure.await()
                }

                val currentError = ensureError
                if (currentError != null) {
                    throw currentError
                }

                return AppDocumentStructure(
                    rootUri = rootUri,
                    lineBackupsUri = Uri.parse("$rootUri/line-backups"),
                    databaseBackupsUri = Uri.parse("$rootUri/database-backups"),
                    gameAnalysisUri = Uri.parse("$rootUri/game-analysis"),
                )
            } finally {
                concurrentEnsures -= 1
            }
        }
    }

    private companion object {
        val FirstRootUri: Uri = Uri.parse("content://documents/tree/first")
        val SecondRootUri: Uri = Uri.parse("content://documents/tree/second")
    }
}
