package com.example.chessboard.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chessboard.entity.GameEntity

@Dao
interface GameDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGame(game: GameEntity): Long

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM games ORDER BY id DESC")
    suspend fun getAllGames(): List<GameEntity>

    @Query("SELECT * FROM games ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun getGamesPage(limit: Int, offset: Int): List<GameEntity>

    @Query("SELECT id FROM games ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun getGameIdsPage(limit: Int, offset: Int): List<Long>

    @Query("SELECT COUNT(*) FROM games")
    suspend fun getGamesCount(): Int

    @Query(
        """
        SELECT * FROM games
        WHERE instr(lower(ifnull(event, '')), lower(:query)) > 0
        ORDER BY id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun searchGamesByEvent(query: String, limit: Int, offset: Int): List<GameEntity>

    @Query(
        """
        SELECT id FROM games
        WHERE instr(lower(ifnull(event, '')), lower(:query)) > 0
        ORDER BY id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun searchGameIdsByEvent(query: String, limit: Int, offset: Int): List<Long>

    @Query(
        """
        SELECT COUNT(*) FROM games
        WHERE instr(lower(ifnull(event, '')), lower(:query)) > 0
        """
    )
    suspend fun countGamesByEvent(query: String): Int

    @Query(
        """
        SELECT * FROM games
        WHERE instr(ifnull(event, ''), :query) > 0
        ORDER BY id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun searchGamesByEventCaseSensitive(
        query: String,
        limit: Int,
        offset: Int
    ): List<GameEntity>

    @Query(
        """
        SELECT id FROM games
        WHERE instr(ifnull(event, ''), :query) > 0
        ORDER BY id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun searchGameIdsByEventCaseSensitive(
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
    suspend fun countGamesByEventCaseSensitive(query: String): Int

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getById(id: Long): GameEntity?

    @Query("SELECT * FROM games WHERE id IN (:gameIds)")
    suspend fun getByIds(gameIds: List<Long>): List<GameEntity>
}
