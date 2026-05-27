package com.example.chessboard.repository

/*
 * File role: defines Room DAO access for persisted opening lines.
 * Allowed here:
 * - SQL queries and insert/delete operations for the games table
 * - narrowly scoped query variants needed by line browsing and search
 * Not allowed here:
 * - UI state, screen workflow, or persistence orchestration beyond DAO contracts
 * Validation date: 2026-05-27
 */

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chessboard.entity.LineEntity

@Dao
interface LineDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLine(line: LineEntity): Long

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM games ORDER BY id DESC")
    suspend fun getAllLines(): List<LineEntity>

    @Query("SELECT * FROM games ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun getLinesPage(limit: Int, offset: Int): List<LineEntity>

    @Query("SELECT id FROM games ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun getLineIdsPage(limit: Int, offset: Int): List<Long>

    @Query("""
        SELECT id FROM games
        WHERE (sideMask & :sideMask) != 0
        ORDER BY id DESC
        LIMIT :limit OFFSET :offset
        """)
    suspend fun getLineIdsPageBySideMask(sideMask: Int, limit: Int, offset: Int): List<Long>

    @Query("SELECT COUNT(*) FROM games")
    suspend fun getLinesCount(): Int

    @Query("SELECT COUNT(*) FROM games WHERE (sideMask & :sideMask) != 0")
    suspend fun countLinesBySideMask(sideMask: Int): Int

    @Query(
        """
        SELECT * FROM games
        WHERE instr(lower(ifnull(event, '')), lower(:query)) > 0
        ORDER BY id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun searchLinesByEvent(query: String, limit: Int, offset: Int): List<LineEntity>

    @Query(
        """
        SELECT id FROM games
        WHERE instr(lower(ifnull(event, '')), lower(:query)) > 0
        ORDER BY id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun searchLineIdsByEvent(query: String, limit: Int, offset: Int): List<Long>

    @Query(
        """
        SELECT COUNT(*) FROM games
        WHERE instr(lower(ifnull(event, '')), lower(:query)) > 0
        """
    )
    suspend fun countLinesByEvent(query: String): Int

    @Query(
        """
        SELECT id FROM games
        WHERE instr(lower(coalesce(event, char(0))), lower(:query)) > 0
            AND (sideMask & :sideMask) != 0
        ORDER BY id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun searchLineIdsByEventAndSideMask(
        query: String,
        sideMask: Int,
        limit: Int,
        offset: Int
    ): List<Long>

    @Query(
        """
        SELECT COUNT(*) FROM games
        WHERE instr(lower(coalesce(event, char(0))), lower(:query)) > 0
            AND (sideMask & :sideMask) != 0
        """
    )
    suspend fun countLinesByEventAndSideMask(query: String, sideMask: Int): Int

    @Query(
        """
        SELECT * FROM games
        WHERE instr(ifnull(event, ''), :query) > 0
        ORDER BY id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun searchLinesByEventCaseSensitive(
        query: String,
        limit: Int,
        offset: Int
    ): List<LineEntity>

    @Query(
        """
        SELECT id FROM games
        WHERE instr(ifnull(event, ''), :query) > 0
        ORDER BY id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun searchLineIdsByEventCaseSensitive(
        query: String,
        limit: Int,
        offset: Int
    ): List<Long>

    @Query(
        """
        SELECT COUNT(*) FROM games
        WHERE instr(ifnull(event, ''), :query) > 0
        """
    )
    suspend fun countLinesByEventCaseSensitive(query: String): Int

    @Query(
        """
        SELECT id FROM games
        WHERE instr(coalesce(event, char(0)), :query) > 0
            AND (sideMask & :sideMask) != 0
        ORDER BY id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun searchLineIdsByEventAndSideMaskCaseSensitive(
        query: String,
        sideMask: Int,
        limit: Int,
        offset: Int
    ): List<Long>

    @Query(
        """
        SELECT COUNT(*) FROM games
        WHERE instr(coalesce(event, char(0)), :query) > 0
            AND (sideMask & :sideMask) != 0
        """
    )
    suspend fun countLinesByEventAndSideMaskCaseSensitive(query: String, sideMask: Int): Int

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getById(id: Long): LineEntity?

    @Query("SELECT * FROM games WHERE id IN (:lineIds)")
    suspend fun getByIds(lineIds: List<Long>): List<LineEntity>
}
