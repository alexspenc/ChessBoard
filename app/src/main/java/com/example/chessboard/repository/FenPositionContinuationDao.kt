package com.example.chessboard.repository

/*
 * File role: defines Room access to continuations owned by FEN catalog positions.
 * Allowed here:
 * - single and atomic batch insertion, reading, ordering, and deletion of continuation rows
 * - relying on database constraints to reject per-position duplicates
 * Not allowed here:
 * - UCI normalization, chess move validation, SAN formatting, or UI state
 * Validation date: 2026-09-02
 */

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chessboard.entity.FenPositionContinuationEntity

@Dao
interface FenPositionContinuationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(continuation: FenPositionContinuationEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(continuations: List<FenPositionContinuationEntity>): List<Long>

    @Query("SELECT * FROM fen_position_continuations WHERE id = :id")
    suspend fun getById(id: Long): FenPositionContinuationEntity?

    @Query(
        """
        SELECT * FROM fen_position_continuations
        WHERE positionId = :positionId
        ORDER BY id ASC
        """
    )
    suspend fun getByPositionId(positionId: Long): List<FenPositionContinuationEntity>

    @Query("DELETE FROM fen_position_continuations WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
