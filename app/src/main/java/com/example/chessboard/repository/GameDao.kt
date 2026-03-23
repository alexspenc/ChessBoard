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

    @Query("DELETE FROM games")
    suspend fun deleteAllGames()

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM games")
    suspend fun getCount(): Int
}
