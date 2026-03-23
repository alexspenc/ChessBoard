package com.example.chessboard.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.chessboard.entity.TrainingTemplateEntity

@Dao
interface TrainingTemplateDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(template: TrainingTemplateEntity): Long

    @Query("SELECT * FROM training_templates WHERE id = :id")
    suspend fun getById(id: Long): TrainingTemplateEntity?

    @Query("DELETE FROM training_templates WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Update
    suspend fun update(template: TrainingTemplateEntity)
}
