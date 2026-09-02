package com.example.chessboard.service.fenpositions

/*
 * File role: verifies Room-backed validation and persistence of FEN position continuations.
 * Allowed here:
 * - single and batch validation, canonical UCI storage, duplicate results, reads, and deletion
 * Not allowed here:
 * - DAO constraint coverage, SAN presentation, Compose UI, or app navigation
 * Validation date: 2026-09-02
 */

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.chessboard.entity.FenPositionEntity
import com.example.chessboard.repository.AppDatabase
import com.example.chessboard.service.CreateFenPositionContinuationBatchResult
import com.example.chessboard.service.CreateFenPositionContinuationResult
import com.example.chessboard.service.FenPositionContinuationService
import com.example.chessboard.service.prepareFenPositionContinuationBatch
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FenPositionContinuationServiceTest {
    private lateinit var database: AppDatabase
    private lateinit var service: FenPositionContinuationService

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        service = FenPositionContinuationService(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createStoresValidatedWhiteContinuationAsCanonicalUci() = runBlocking {
        val positionId = createPosition(InitialPositionFen)

        val result = service.create(
            positionId = positionId,
            moves = listOf(
                Move(Square.E2, Square.E4),
                Move(Square.E7, Square.E5),
                Move(Square.G1, Square.F3),
            ),
        )

        val success = result as CreateFenPositionContinuationResult.Success
        assertEquals("e2e4 e7e5 g1f3", service.getById(success.id)?.uciMoves)
    }

    @Test
    fun createValidatesContinuationFromBlackToMovePosition() = runBlocking {
        val positionId = createPosition(BlackToMovePositionFen)

        val result = service.create(
            positionId = positionId,
            moves = listOf(
                Move(Square.E7, Square.E6),
                Move(Square.E2, Square.E3),
            ),
        )

        val success = result as CreateFenPositionContinuationResult.Success
        assertEquals("e7e6 e2e3", service.getById(success.id)?.uciMoves)
    }

    @Test
    fun createRejectsEmptyContinuation() = runBlocking {
        val positionId = createPosition(InitialPositionFen)

        val result = service.create(positionId = positionId, moves = emptyList())

        assertEquals(CreateFenPositionContinuationResult.EmptyMoves, result)
        assertTrue(service.getByPositionId(positionId).isEmpty())
    }

    @Test
    fun createRejectsMissingPosition() = runBlocking {
        val result = service.create(
            positionId = MissingPositionId,
            moves = listOf(Move(Square.E2, Square.E4)),
        )

        assertEquals(CreateFenPositionContinuationResult.PositionNotFound, result)
    }

    @Test
    fun createRejectsIllegalFirstMoveWithoutSaving() = runBlocking {
        val positionId = createPosition(InitialPositionFen)

        val result = service.create(
            positionId = positionId,
            moves = listOf(Move(Square.E7, Square.E5)),
        )

        assertEquals(CreateFenPositionContinuationResult.InvalidMove(plyIndex = 0), result)
        assertTrue(service.getByPositionId(positionId).isEmpty())
    }

    @Test
    fun createRejectsIllegalLaterMoveWithoutSavingPartialContinuation() = runBlocking {
        val positionId = createPosition(InitialPositionFen)

        val result = service.create(
            positionId = positionId,
            moves = listOf(
                Move(Square.E2, Square.E4),
                Move(Square.G1, Square.F3),
            ),
        )

        assertEquals(CreateFenPositionContinuationResult.InvalidMove(plyIndex = 1), result)
        assertTrue(service.getByPositionId(positionId).isEmpty())
    }

    @Test
    fun createRejectsDuplicateContinuation() = runBlocking {
        val positionId = createPosition(InitialPositionFen)
        val moves = listOf(
            Move(Square.E2, Square.E4),
            Move(Square.E7, Square.E5),
        )
        val firstResult = service.create(positionId = positionId, moves = moves)

        val duplicateResult = service.create(positionId = positionId, moves = moves)

        assertTrue(firstResult is CreateFenPositionContinuationResult.Success)
        assertEquals(
            CreateFenPositionContinuationResult.DuplicateContinuation,
            duplicateResult,
        )
        assertEquals(1, service.getByPositionId(positionId).size)
    }

    @Test
    fun createBatchStoresAllValidatedContinuationsInOrder() = runBlocking {
        val positionId = createPosition(InitialPositionFen)
        val firstLine = listOf("e2e4", "e7e5", "g1f3")
        val secondLine = listOf("d2d4", "d7d5", "c2c4")

        val result = service.createBatch(
            positionId = positionId,
            preparation = prepareFenPositionContinuationBatch(
                parsedUciLines = listOf(firstLine, secondLine),
            ),
        )

        val success = result as CreateFenPositionContinuationBatchResult.Success
        assertEquals(2, success.insertedIds.size)
        assertEquals(0, success.coveredByStoredLinesCount)
        assertEquals(
            listOf(firstLine.joinToString(" "), secondLine.joinToString(" ")),
            service.getByPositionId(positionId).map { continuation -> continuation.uciMoves },
        )
    }

    @Test
    fun createBatchSkipsShortLineCoveredByStoredLongerLine() = runBlocking {
        val positionId = createPosition(InitialPositionFen)
        service.create(
            positionId = positionId,
            moves = listOf(
                Move(Square.E2, Square.E4),
                Move(Square.E7, Square.E5),
                Move(Square.G1, Square.F3),
            ),
        )
        val newLine = listOf("d2d4", "d7d5")

        val result = service.createBatch(
            positionId = positionId,
            preparation = prepareFenPositionContinuationBatch(
                parsedUciLines = listOf(listOf("e2e4", "e7e5"), newLine),
            ),
        )

        val success = result as CreateFenPositionContinuationBatchResult.Success
        assertEquals(1, success.insertedIds.size)
        assertEquals(1, success.coveredByStoredLinesCount)
        assertEquals(
            listOf("e2e4 e7e5 g1f3", newLine.joinToString(" ")),
            service.getByPositionId(positionId).map { continuation -> continuation.uciMoves },
        )
    }

    @Test
    fun createBatchStoresLongerLineWhenStoredLineIsItsShortPrefix() = runBlocking {
        val positionId = createPosition(InitialPositionFen)
        service.create(
            positionId = positionId,
            moves = listOf(
                Move(Square.E2, Square.E4),
                Move(Square.E7, Square.E5),
            ),
        )
        val longerLine = listOf("e2e4", "e7e5", "g1f3")

        val result = service.createBatch(
            positionId = positionId,
            preparation = prepareFenPositionContinuationBatch(
                parsedUciLines = listOf(longerLine),
            ),
        )

        val success = result as CreateFenPositionContinuationBatchResult.Success
        assertEquals(1, success.insertedIds.size)
        assertEquals(0, success.coveredByStoredLinesCount)
        assertEquals(
            listOf("e2e4 e7e5", longerLine.joinToString(" ")),
            service.getByPositionId(positionId).map { continuation -> continuation.uciMoves },
        )
    }

    @Test
    fun createBatchRejectsInvalidMoveWithoutSavingAnyContinuation() = runBlocking {
        val positionId = createPosition(InitialPositionFen)

        val result = service.createBatch(
            positionId = positionId,
            preparation = prepareFenPositionContinuationBatch(
                parsedUciLines = listOf(
                    listOf("e2e4", "e7e5"),
                    listOf("d2d4", "d7d5", "e2e5"),
                    listOf("c2c4", "e7e5"),
                ),
            ),
        )

        assertEquals(
            CreateFenPositionContinuationBatchResult.InvalidMove(
                lineIndex = 1,
                plyIndex = 2,
            ),
            result,
        )
        assertTrue(service.getByPositionId(positionId).isEmpty())
    }

    @Test
    fun createBatchRejectsMalformedUciWithoutSavingAnyContinuation() = runBlocking {
        val positionId = createPosition(InitialPositionFen)

        val result = service.createBatch(
            positionId = positionId,
            preparation = prepareFenPositionContinuationBatch(
                parsedUciLines = listOf(
                    listOf("e2e4", "e7e5"),
                    listOf("not-a-move"),
                ),
            ),
        )

        assertEquals(
            CreateFenPositionContinuationBatchResult.InvalidMove(
                lineIndex = 1,
                plyIndex = 0,
            ),
            result,
        )
        assertTrue(service.getByPositionId(positionId).isEmpty())
    }

    @Test
    fun createBatchRejectsEmptyContinuationWithoutSaving() = runBlocking {
        val positionId = createPosition(InitialPositionFen)

        val result = service.createBatch(
            positionId = positionId,
            preparation = prepareFenPositionContinuationBatch(
                parsedUciLines = listOf(emptyList()),
            ),
        )

        assertEquals(
            CreateFenPositionContinuationBatchResult.EmptyContinuation(lineIndex = 0),
            result,
        )
        assertTrue(service.getByPositionId(positionId).isEmpty())
    }

    @Test
    fun createBatchRejectsMissingPositionWithoutSaving() = runBlocking {
        val result = service.createBatch(
            positionId = MissingPositionId,
            preparation = prepareFenPositionContinuationBatch(
                parsedUciLines = listOf(listOf("e2e4")),
            ),
        )

        assertEquals(CreateFenPositionContinuationBatchResult.PositionNotFound, result)
    }

    @Test
    fun createBatchReportsSuccessWhenStoredLineCoversWholeBatch() = runBlocking {
        val positionId = createPosition(InitialPositionFen)
        service.create(
            positionId = positionId,
            moves = listOf(
                Move(Square.E2, Square.E4),
                Move(Square.E7, Square.E5),
                Move(Square.G1, Square.F3),
            ),
        )

        val result = service.createBatch(
            positionId = positionId,
            preparation = prepareFenPositionContinuationBatch(
                parsedUciLines = listOf(listOf("e2e4", "e7e5")),
            ),
        )

        val success = result as CreateFenPositionContinuationBatchResult.Success
        assertTrue(success.insertedIds.isEmpty())
        assertEquals(1, success.coveredByStoredLinesCount)
        assertEquals(1, service.getByPositionId(positionId).size)
    }

    @Test
    fun createBatchRejectsEmptyBatch() = runBlocking {
        val positionId = createPosition(InitialPositionFen)

        val result = service.createBatch(
            positionId = positionId,
            preparation = prepareFenPositionContinuationBatch(parsedUciLines = emptyList()),
        )

        assertEquals(CreateFenPositionContinuationBatchResult.EmptyBatch, result)
        assertTrue(service.getByPositionId(positionId).isEmpty())
    }

    @Test
    fun getByPositionIdReturnsStoredContinuationsInDaoOrder() = runBlocking {
        val positionId = createPosition(InitialPositionFen)
        service.create(
            positionId = positionId,
            moves = listOf(Move(Square.E2, Square.E4)),
        )
        service.create(
            positionId = positionId,
            moves = listOf(Move(Square.D2, Square.D4)),
        )

        assertEquals(
            listOf("e2e4", "d2d4"),
            service.getByPositionId(positionId).map { it.uciMoves },
        )
    }

    @Test
    fun deleteByIdReportsWhetherContinuationExisted() = runBlocking {
        val positionId = createPosition(InitialPositionFen)
        val createResult = service.create(
            positionId = positionId,
            moves = listOf(Move(Square.E2, Square.E4)),
        ) as CreateFenPositionContinuationResult.Success

        assertTrue(service.deleteById(createResult.id))
        assertFalse(service.deleteById(createResult.id))
        assertNull(service.getById(createResult.id))
    }

    private suspend fun createPosition(fen: String): Long {
        return database.fenPositionDao().insert(
            FenPositionEntity(
                fen = fen,
                name = "Position",
                theme = "Strategy",
            ),
        )
    }

    private companion object {
        const val InitialPositionFen =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -"
        const val BlackToMovePositionFen = "8/4k3/8/8/8/8/4K3/8 b - -"
        const val MissingPositionId = -1L
    }
}
