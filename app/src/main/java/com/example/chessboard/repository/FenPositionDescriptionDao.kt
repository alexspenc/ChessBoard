package com.example.chessboard.repository

/*
 * File role: defines Room access to descriptions attached to four-field FEN positions.
 * Allowed here:
 * - inserting or replacing a position description
 * - resolving a description by canonical position FEN
 * Not allowed here:
 * - FEN normalization, catalog paging, or UI state
 * Validation date: 2026-08-31
 */

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chessboard.entity.FenPositionDescriptionEntity

@Dao
interface FenPositionDescriptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(description: FenPositionDescriptionEntity): Long

    @Query("SELECT * FROM fen_position_descriptions WHERE fen = :fen LIMIT 1")
    suspend fun getByFen(fen: String): FenPositionDescriptionEntity?
}
