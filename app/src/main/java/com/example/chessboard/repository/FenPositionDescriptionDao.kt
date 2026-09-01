package com.example.chessboard.repository

/*
 * File role: defines Room access to descriptions attached to four-field FEN positions.
 * Allowed here:
 * - inserting, updating, or deleting a position-owned description
 * - resolving a description by owner id or by joining to a canonical position FEN
 * Not allowed here:
 * - FEN normalization, catalog paging, or UI state
 * Validation date: 2026-09-01
 */

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.chessboard.entity.FenPositionDescriptionEntity

@Dao
interface FenPositionDescriptionDao {
    @Insert
    suspend fun insert(description: FenPositionDescriptionEntity): Long

    @Query("SELECT * FROM fen_position_descriptions WHERE positionId = :positionId")
    suspend fun getByPositionId(positionId: Long): FenPositionDescriptionEntity?

    @Query(
        """
        UPDATE fen_position_descriptions
        SET description = :description
        WHERE positionId = :positionId
        """
    )
    suspend fun updateByPositionId(
        positionId: Long,
        description: String,
    ): Int

    @Query("DELETE FROM fen_position_descriptions WHERE positionId = :positionId")
    suspend fun deleteByPositionId(positionId: Long): Int

    @Query(
        """
        SELECT d.*
        FROM fen_position_descriptions AS d
        INNER JOIN fen_positions AS p
            ON p.id = d.positionId
        WHERE p.fen = :fen
        LIMIT 1
        """
    )
    suspend fun getByFen(fen: String): FenPositionDescriptionEntity?
}
