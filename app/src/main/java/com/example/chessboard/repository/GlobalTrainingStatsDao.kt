package com.example.chessboard.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chessboard.entity.GlobalTrainingStatsEntity

@Dao
interface GlobalTrainingStatsDao {

    @Query("SELECT * FROM global_training_stats WHERE id = :id")
    suspend fun getById(id: Long = GlobalTrainingStatsEntity.SINGLE_ROW_ID): GlobalTrainingStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: GlobalTrainingStatsEntity)
}
