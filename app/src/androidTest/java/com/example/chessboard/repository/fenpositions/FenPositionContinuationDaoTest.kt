package com.example.chessboard.repository.fenpositions

/*
 * File role: verifies Room storage constraints for FEN position continuations.
 * Allowed here:
 * - continuation ordering, ownership, uniqueness, foreign keys, and cascading deletion
 * Not allowed here:
 * - migration tests, UCI validation, SAN formatting, services, or Compose UI
 * Validation date: 2026-09-02
 */

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.chessboard.entity.FenPositionContinuationEntity
import com.example.chessboard.entity.FenPositionEntity
import com.example.chessboard.repository.AppDatabase
import com.example.chessboard.repository.FenPositionContinuationDao
import com.example.chessboard.repository.FenPositionDao
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class FenPositionContinuationDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var positionDao: FenPositionDao
    private lateinit var continuationDao: FenPositionContinuationDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        positionDao = database.fenPositionDao()
        continuationDao = database.fenPositionContinuationDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun continuationsAreStoredAndLoadedByOwnerInCreationOrder() = runBlocking {
        val firstPositionId = createPosition(FirstPositionFen, "First")
        val secondPositionId = createPosition(SecondPositionFen, "Second")
        val firstContinuationId = continuationDao.insert(
            continuation(positionId = firstPositionId, uciMoves = "e2e4 e7e5"),
        )
        val secondContinuationId = continuationDao.insert(
            continuation(positionId = firstPositionId, uciMoves = "d2d4 d7d5"),
        )
        continuationDao.insert(
            continuation(positionId = secondPositionId, uciMoves = "e2e4 e7e5"),
        )

        assertEquals(
            listOf(firstContinuationId, secondContinuationId),
            continuationDao.getByPositionId(firstPositionId).map { it.id },
        )
        assertEquals(
            "e2e4 e7e5",
            continuationDao.getById(firstContinuationId)?.uciMoves,
        )
    }

    @Test
    fun duplicateIsRejectedOnlyForTheSamePosition() = runBlocking {
        val firstPositionId = createPosition(FirstPositionFen, "First")
        val secondPositionId = createPosition(SecondPositionFen, "Second")
        val firstId = continuationDao.insert(
            continuation(positionId = firstPositionId, uciMoves = SharedMoves),
        )

        val duplicateId = continuationDao.insert(
            continuation(positionId = firstPositionId, uciMoves = SharedMoves),
        )
        val otherPositionContinuationId = continuationDao.insert(
            continuation(positionId = secondPositionId, uciMoves = SharedMoves),
        )

        assertNotNull(continuationDao.getById(firstId))
        assertEquals(-1L, duplicateId)
        assertNotNull(continuationDao.getById(otherPositionContinuationId))
    }

    @Test
    fun deletingContinuationKeepsOwningPosition() = runBlocking {
        val positionId = createPosition(FirstPositionFen, "First")
        val continuationId = continuationDao.insert(
            continuation(positionId = positionId, uciMoves = SharedMoves),
        )

        assertEquals(1, continuationDao.deleteById(continuationId))
        assertNull(continuationDao.getById(continuationId))
        assertNotNull(positionDao.getById(positionId))
    }

    @Test
    fun deletingPositionCascadesToOwnedContinuations() = runBlocking {
        val positionId = createPosition(FirstPositionFen, "First")
        val firstContinuationId = continuationDao.insert(
            continuation(positionId = positionId, uciMoves = "e2e4 e7e5"),
        )
        val secondContinuationId = continuationDao.insert(
            continuation(positionId = positionId, uciMoves = "d2d4 d7d5"),
        )

        assertEquals(1, positionDao.deleteById(positionId))
        assertNull(continuationDao.getById(firstContinuationId))
        assertNull(continuationDao.getById(secondContinuationId))
        assertEquals(
            emptyList<FenPositionContinuationEntity>(),
            continuationDao.getByPositionId(positionId),
        )
    }

    @Test
    fun continuationCannotReferenceMissingPosition() {
        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking {
                continuationDao.insert(
                    continuation(positionId = MissingPositionId, uciMoves = SharedMoves),
                )
            }
        }
    }

    private suspend fun createPosition(fen: String, name: String): Long {
        return positionDao.insert(
            FenPositionEntity(
                fen = fen,
                name = name,
                theme = "Strategy",
            ),
        )
    }

    private fun continuation(
        positionId: Long,
        uciMoves: String,
    ): FenPositionContinuationEntity {
        return FenPositionContinuationEntity(
            positionId = positionId,
            uciMoves = uciMoves,
        )
    }

    private companion object {
        const val FirstPositionFen =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -"
        const val SecondPositionFen =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - -"
        const val SharedMoves = "e2e4 e7e5"
        const val MissingPositionId = -1L
    }
}
