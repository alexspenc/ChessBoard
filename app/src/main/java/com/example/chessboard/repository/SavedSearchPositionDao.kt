package com.example.chessboard.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.chessboard.entity.SavedSearchPositionEntity

@Dao
interface SavedSearchPositionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(position: SavedSearchPositionEntity): Long

    @Update
    suspend fun update(position: SavedSearchPositionEntity)

    @Query("SELECT * FROM saved_search_positions ORDER BY name")
    suspend fun getAll(): List<SavedSearchPositionEntity>

    @Query("SELECT * FROM saved_search_positions WHERE id = :id")
    suspend fun getById(id: Long): SavedSearchPositionEntity?

    @Query("SELECT * FROM saved_search_positions WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): SavedSearchPositionEntity?

    @Query(
        """
        SELECT *
        FROM saved_search_positions
        WHERE hashForSearch = :hashForSearch AND fenForSearch = :fenForSearch
        LIMIT 1
        """
    )
    suspend fun getBySearchFen(
        hashForSearch: Long,
        fenForSearch: String
    ): SavedSearchPositionEntity?

    @Query(
        """
        SELECT *
        FROM saved_search_positions
        WHERE hashFull = :hashFull AND fenFull = :fenFull
        LIMIT 1
        """
    )
    suspend fun getByFullFen(
        hashFull: Long,
        fenFull: String
    ): SavedSearchPositionEntity?

    @Query("DELETE FROM saved_search_positions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
