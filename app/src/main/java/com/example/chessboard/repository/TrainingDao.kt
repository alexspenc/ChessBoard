package com.example.chessboard.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.chessboard.entity.TrainingEntity

@Dao
interface TrainingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(training: TrainingEntity): Long

    @Query("SELECT * FROM trainings WHERE id = :id")
    suspend fun getById(id: Long): TrainingEntity?

    @Query("SELECT * FROM trainings ORDER BY id LIMIT 1")
    suspend fun getFirst(): TrainingEntity?

    @Query("DELETE FROM trainings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Update
    suspend fun update(training: TrainingEntity)
}
