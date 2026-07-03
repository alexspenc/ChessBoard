package com.example.chessboard.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.chessboard.entity.TrainingResultEntity

data class LineMistakesTotal(
    val lineId: Long,
    val totalMistakes: Int,
)

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

    @Query(
        """
        SELECT *
        FROM training_results
        ORDER BY trainedAt DESC, id DESC
        LIMIT :limit
        """
    )
    abstract suspend fun getRecentResults(limit: Int): List<TrainingResultEntity>

    @Query(
        """
        SELECT *
        FROM training_results
        WHERE gameId = :lineId
        ORDER BY trainedAt DESC, id DESC
        LIMIT :limit
        """
    )
    abstract suspend fun getResultsForLine(lineId: Long, limit: Int): List<TrainingResultEntity>

    @Query(
        """
        SELECT gameId AS lineId, SUM(mistakesCount) AS totalMistakes
        FROM training_results
        GROUP BY gameId
        ORDER BY totalMistakes DESC, gameId ASC
        """
    )
    abstract suspend fun getLineMistakeTotals(): List<LineMistakesTotal>

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
