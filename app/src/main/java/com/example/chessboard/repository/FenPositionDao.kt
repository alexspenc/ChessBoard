package com.example.chessboard.repository

/*
 * File role: defines Room access to positions in the FEN catalog.
 * Allowed here:
 * - database queries that create, read, count, page, or delete position rows
 * - the agreed newest-first ordering of catalog queries
 * Not allowed here:
 * - FEN normalization, descriptions, continuations, or UI state
 * Validation date: 2026-08-31
 */

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chessboard.entity.FenPositionEntity

@Dao
interface FenPositionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(position: FenPositionEntity): Long

    @Query("SELECT * FROM fen_positions ORDER BY id DESC")
    suspend fun getAll(): List<FenPositionEntity>

    @Query(
        """
        SELECT * FROM fen_positions
        ORDER BY id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getPage(limit: Int, offset: Int): List<FenPositionEntity>

    @Query("SELECT COUNT(*) FROM fen_positions")
    suspend fun getCount(): Int

    @Query("SELECT * FROM fen_positions WHERE id = :id")
    suspend fun getById(id: Long): FenPositionEntity?

    @Query("SELECT * FROM fen_positions WHERE fen = :fen LIMIT 1")
    suspend fun getByFen(fen: String): FenPositionEntity?

    @Query("DELETE FROM fen_positions WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
