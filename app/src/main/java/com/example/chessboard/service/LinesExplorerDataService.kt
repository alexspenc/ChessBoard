package com.example.chessboard.service

/*
 * File role: loads database-backed snapshots needed to open the lines explorer.
 * Allowed here:
 * - transaction-wrapped reads that combine line ids with line-related statistics
 * - persistence-facing data snapshots for the lines explorer container/runtime state
 * Not allowed here:
 * - Compose UI state, screen navigation, filtering UI, or rendering logic
 * Validation date: 2026-07-03
 */

import androidx.room.withTransaction
import com.example.chessboard.repository.AppDatabase
import com.example.chessboard.repository.LineMistakesTotal

data class LinesExplorerDataSnapshot(
    val lineIds: List<Long>,
    val lineMistakeTotals: List<LineMistakesTotal>,
)

class LinesExplorerDataService(
    private val database: AppDatabase,
) {

    suspend fun loadAllLinesSnapshot(): LinesExplorerDataSnapshot {
        return database.withTransaction {
            val lineDao = database.lineDao()
            val linesCount = lineDao.getLinesCount()
            val lineIds = lineDao.getLineIdsPage(
                limit = linesCount.coerceAtLeast(1),
                offset = 0,
            )
            val lineMistakeTotals = database.trainingResultDao().getLineMistakeTotals()

            LinesExplorerDataSnapshot(
                lineIds = lineIds,
                lineMistakeTotals = lineMistakeTotals,
            )
        }
    }
}
