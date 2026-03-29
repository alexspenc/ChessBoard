package com.example.chessboard.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.chessboard.entity.TrainingResultEntity

@Dao
abstract class TrainingResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertInternal(result: TrainingResultEntity): Long

    @Query(
        """
        DELETE FROM training_results
        WHERE id NOT IN (
            SELECT id
            FROM training_results
            ORDER BY trainedAt DESC, id DESC
            LIMIT :limit
        )
        """
    )
    protected abstract suspend fun trimToLatestInternal(limit: Int)

    @Transaction
    open suspend fun insertAndTrim(
        result: TrainingResultEntity,
        limit: Int = MAX_TRAINING_RESULTS
    ): Long {
        val insertedId = insertInternal(result)
        trimToLatestInternal(limit)
        return insertedId
    }

    companion object {
        const val MAX_TRAINING_RESULTS = 10_000
    }
}
