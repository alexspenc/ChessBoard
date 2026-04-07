package com.example.chessboard.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chessboard.entity.PositionEntity

data class PositionIdWithMask(
    val id: Long,
    val sideMask: Int
)

@Dao
interface PositionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosition(position: PositionEntity): Long

    @Query("DELETE FROM positions WHERE id = :id")
    suspend fun deleteById(id: Long)

    // Possible to have different fen by same hash
    // So select list of fen
    // Uses bitmask overlap: BOTH must match WHITE and BLACK lookups.
    @Query("""SELECT fen FROM positions
            WHERE hash = :hash AND (sideMask & :sideMask) != 0""")
    suspend fun getFensByHashAndSideMaskOverlap(hash: Long, sideMask: Int): List<String>

    @Query("""
        SELECT *
        FROM positions
        WHERE hashNoMoveNumber = :hashNoMoveNumber
    """)
    suspend fun getPositionsByHashNoMoveNumber(hashNoMoveNumber: Long): List<PositionEntity>

    @Query("""SELECT id, sideMask FROM positions
        WHERE hash = :hash AND fen = :fen
        LIMIT 1""")
    suspend fun getIdAndSideByHashAndFen(hash: Long, fen: String): PositionIdWithMask?

    @Query("""
        UPDATE positions
        SET sideMask = :newSide
        WHERE id = :id""")
    suspend fun updateSideMask(id: Long, newSide: Int)
}
