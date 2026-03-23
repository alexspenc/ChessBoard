package com.example.chessboard.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chessboard.entity.GamePositionEntity

data class PositionUsage(
    val positionId: Long,
    val sideMask: Int
)

@Dao
interface GamePositionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGamePosition(gamePosition: GamePositionEntity): Long

    @Query("DELETE FROM game_positions")
    suspend fun deleteAllGamePositions()

    @Query("DELETE FROM game_positions WHERE gameId = :gameId")
    suspend fun deleteByGameId(gameId: Long)

    @Query("SELECT COUNT(*) FROM game_positions")
    suspend fun getCount(): Int

    @Query("""
        SELECT positionId, sideMask
        FROM game_positions
        WHERE gameId = :gameId""")
    suspend fun getPositionsForGame(gameId: Long): List<PositionUsage>

    @Query("""
        SELECT gp.positionId, g.sideMask as sideMask
        FROM game_positions gp
        JOIN games g ON g.id = gp.gameId
        WHERE gp.positionId = :positionId""")
    suspend fun getUsage(positionId: Long): List<PositionUsage>
}
