package com.example.chessboard.repository

/*
 * File role: defines Room access for optional dubious-line markers.
 * Allowed here:
 * - DAO queries that create, read, update, or remove marker rows
 * - database-facing selection helpers for marker records
 * Not allowed here:
 * - UI state, screen workflow, or training recommendation logic
 * Validation date: 2026-05-25
 */

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chessboard.entity.DubiousLineEntity

@Dao
interface DubiousLineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(marker: DubiousLineEntity)

    @Query("DELETE FROM dubious_lines WHERE gameId = :lineId")
    suspend fun deleteByLineId(lineId: Long)

    @Query("SELECT * FROM dubious_lines WHERE gameId = :lineId")
    suspend fun getByLineId(lineId: Long): DubiousLineEntity?

    @Query("SELECT * FROM dubious_lines WHERE gameId IN (:lineIds)")
    suspend fun getByLineIds(lineIds: List<Long>): List<DubiousLineEntity>

    @Query("SELECT * FROM dubious_lines ORDER BY weight DESC, gameId DESC")
    suspend fun getAll(): List<DubiousLineEntity>
}
