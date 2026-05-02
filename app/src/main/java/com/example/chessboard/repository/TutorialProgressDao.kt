package com.example.chessboard.repository

/**
 * File role: defines Room access for persisted tutorial progress rows.
 * Allowed here:
 * - simple CRUD queries for tutorial progress
 * - query shapes needed by tutorial persistence services
 * Not allowed here:
 * - tutorial business rules or stage-transition logic
 * - screen-specific workflow orchestration
 * Validation date: 2026-05-02
 */
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.chessboard.entity.TutorialProgressEntity

@Dao
interface TutorialProgressDao {

    @Insert
    suspend fun insert(progress: TutorialProgressEntity): Long

    @Update
    suspend fun update(progress: TutorialProgressEntity)

    @Query("SELECT * FROM tutorial_progress WHERE runStatus = 'ACTIVE' ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActive(): TutorialProgressEntity?

    @Query("SELECT * FROM tutorial_progress ORDER BY startedAt DESC LIMIT 1")
    suspend fun getLatest(): TutorialProgressEntity?

    @Query("SELECT * FROM tutorial_progress WHERE id = :id")
    suspend fun getById(id: Long): TutorialProgressEntity?
}
