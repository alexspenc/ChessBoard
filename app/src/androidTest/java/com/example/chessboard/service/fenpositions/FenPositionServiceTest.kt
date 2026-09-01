package com.example.chessboard.service.fenpositions

/*
 * File role: verifies Room-backed creation and description ownership for FEN positions.
 * Allowed here:
 * - position and description persistence and relation constraints
 * - duplicate, invalid-FEN, required-theme, and newest-first details navigation behavior
 * Not allowed here:
 * - Compose UI, runtime-context paging, or unrelated database services
 * Validation date: 2026-09-01
 */

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.chessboard.entity.FenPositionDescriptionEntity
import com.example.chessboard.repository.AppDatabase
import com.example.chessboard.service.CreateFenPositionResult
import com.example.chessboard.service.FenPositionService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FenPositionServiceTest {
    private lateinit var database: AppDatabase
    private lateinit var service: FenPositionService

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        service = FenPositionService(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createStoresCanonicalPositionAndDescriptionTogether() = runBlocking {
        val result = service.create(
            fen = "$InitialPositionFen 17 42",
            name = "  Initial position  ",
            theme = "  Basics  ",
            description = "  Starting setup  ",
        )

        val success = result as CreateFenPositionResult.Success
        val position = service.getById(success.id)
        val description = service.getDescriptionByFen(InitialPositionFen)

        assertEquals(InitialPositionFen, position?.fen)
        assertEquals("Initial position", position?.name)
        assertEquals("Basics", position?.theme)
        assertEquals(success.id, description?.positionId)
        assertEquals("Starting setup", description?.description)
    }

    @Test
    fun getDetailsByIdReturnsPositionAndDescription() = runBlocking {
        val createResult = service.create(
            fen = InitialPositionFen,
            name = "Initial position",
            theme = "Basics",
            description = "Starting setup",
        ) as CreateFenPositionResult.Success

        val details = service.getDetailsById(createResult.id)

        assertEquals(createResult.id, details?.id)
        assertEquals(InitialPositionFen, details?.fen)
        assertEquals("Initial position", details?.name)
        assertEquals("Basics", details?.theme)
        assertEquals("Starting setup", details?.description)
    }

    @Test
    fun getDetailsByIdReturnsPositionWithoutDescription() = runBlocking {
        val createResult = service.create(
            fen = InitialPositionFen,
            name = "Initial position",
            theme = "Basics",
            description = "",
        ) as CreateFenPositionResult.Success

        val details = service.getDetailsById(createResult.id)

        assertEquals(createResult.id, details?.id)
        assertNull(details?.description)
    }

    @Test
    fun getDetailsByIdReturnsNullForMissingPosition() = runBlocking {
        assertNull(service.getDetailsById(MissingPositionId))
    }

    @Test
    fun detailsNavigationFollowsNewestFirstCatalogOrder() = runBlocking {
        val oldest = createPosition(OldestPositionFen, "Oldest")
        val middle = createPosition(MiddlePositionFen, "Middle")
        val newest = createPosition(NewestPositionFen, "Newest")

        val newestDetails = service.getDetailsById(newest.id)
        val middleDetails = service.getDetailsById(middle.id)
        val oldestDetails = service.getDetailsById(oldest.id)

        assertEquals(0, newestDetails?.catalogIndex)
        assertNull(newestDetails?.previousPositionId)
        assertEquals(middle.id, newestDetails?.nextPositionId)
        assertEquals(1, middleDetails?.catalogIndex)
        assertEquals(newest.id, middleDetails?.previousPositionId)
        assertEquals(oldest.id, middleDetails?.nextPositionId)
        assertEquals(2, oldestDetails?.catalogIndex)
        assertEquals(middle.id, oldestDetails?.previousPositionId)
        assertNull(oldestDetails?.nextPositionId)
    }

    @Test
    fun detailsNavigationSkipsDeletedPositionIds() = runBlocking {
        val oldest = createPosition(OldestPositionFen, "Oldest")
        val deletedMiddle = createPosition(MiddlePositionFen, "Middle")
        val newest = createPosition(NewestPositionFen, "Newest")
        assertTrue(service.deleteById(deletedMiddle.id))

        val newestDetails = service.getDetailsById(newest.id)
        val oldestDetails = service.getDetailsById(oldest.id)

        assertEquals(oldest.id, newestDetails?.nextPositionId)
        assertEquals(newest.id, oldestDetails?.previousPositionId)
        assertEquals(1, oldestDetails?.catalogIndex)
    }

    @Test
    fun positionCannotHaveMoreThanOneDescription() = runBlocking {
        val createResult = service.create(
            fen = InitialPositionFen,
            name = "Position",
            theme = "Basics",
            description = "Original description",
        ) as CreateFenPositionResult.Success

        val duplicateInsert = runCatching {
            database.fenPositionDescriptionDao().insert(
                FenPositionDescriptionEntity(
                    positionId = createResult.id,
                    description = "Second description",
                ),
            )
        }

        assertTrue(duplicateInsert.isFailure)
        assertEquals(
            "Original description",
            service.getDescriptionByFen(InitialPositionFen)?.description,
        )
    }

    @Test
    fun descriptionRequiresExistingPosition() = runBlocking {
        val orphanInsert = runCatching {
            database.fenPositionDescriptionDao().insert(
                FenPositionDescriptionEntity(
                    positionId = MissingPositionId,
                    description = "Orphan description",
                ),
            )
        }

        assertTrue(orphanInsert.isFailure)
    }

    @Test
    fun duplicateFenDoesNotReplaceExistingDescription() = runBlocking {
        service.create(
            fen = InitialPositionFen,
            name = "First",
            theme = "Basics",
            description = "Original description",
        )

        val duplicateResult = service.create(
            fen = "$InitialPositionFen 0 9",
            name = "Second",
            theme = "Other",
            description = "Replacement description",
        )

        assertEquals(CreateFenPositionResult.DuplicateFen, duplicateResult)
        assertEquals(1, service.getAll().size)
        assertEquals(
            "Original description",
            service.getDescriptionByFen(InitialPositionFen)?.description,
        )
    }

    @Test
    fun invalidFenAndBlankThemeDoNotCreateRows() = runBlocking {
        assertEquals(
            CreateFenPositionResult.InvalidFen,
            service.create(
                fen = "not a fen",
                name = "Invalid",
                theme = "Tactics",
                description = "Description",
            ),
        )
        assertEquals(
            CreateFenPositionResult.BlankTheme,
            service.create(
                fen = InitialPositionFen,
                name = "No theme",
                theme = "   ",
                description = "Description",
            ),
        )

        assertEquals(emptyList<com.example.chessboard.entity.FenPositionEntity>(), service.getAll())
        assertNull(service.getDescriptionByFen(InitialPositionFen))
    }

    @Test
    fun deletePositionCascadesToItsDescription() = runBlocking {
        val createResult = service.create(
            fen = InitialPositionFen,
            name = "Position",
            theme = "Basics",
            description = "Description",
        ) as CreateFenPositionResult.Success

        assertTrue(service.deleteById(createResult.id))

        assertNull(service.getById(createResult.id))
        assertNull(service.getDescriptionByFen(InitialPositionFen))
    }

    private suspend fun createPosition(
        fen: String,
        name: String,
    ): CreateFenPositionResult.Success {
        return service.create(
            fen = fen,
            name = name,
            theme = "Test",
            description = "",
        ) as CreateFenPositionResult.Success
    }

    private companion object {
        const val MissingPositionId = 99L
        const val InitialPositionFen =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -"
        const val OldestPositionFen = "4k3/8/8/8/8/8/8/4K3 w - -"
        const val MiddlePositionFen = "4k3/8/8/8/8/8/8/4K3 b - -"
        const val NewestPositionFen = "4k3/8/8/8/8/8/P7/4K3 w - -"
    }
}
