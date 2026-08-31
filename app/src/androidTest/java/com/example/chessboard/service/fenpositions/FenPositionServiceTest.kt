package com.example.chessboard.service.fenpositions

/*
 * File role: verifies Room-backed creation for the FEN positions feature.
 * Allowed here:
 * - position and description persistence assertions
 * - duplicate, invalid-FEN, and required-theme behavior
 * Not allowed here:
 * - Compose UI, runtime-context paging, or unrelated database services
 * Validation date: 2026-08-31
 */

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
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
        assertEquals(InitialPositionFen, description?.fen)
        assertEquals("Starting setup", description?.description)
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

    private companion object {
        const val InitialPositionFen =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -"
    }
}
