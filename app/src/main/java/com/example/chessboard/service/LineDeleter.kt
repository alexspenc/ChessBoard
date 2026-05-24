package com.example.chessboard.service

/**
 * File role: groups persistence logic for deleting stored lines and cleaning up
 * dependent position data in the database.
 * Allowed here:
 * - transactional delete flows for one or more lines
 * - persistence cleanup that must happen because line records are removed
 * Not allowed here:
 * - UI state, dialogs, or navigation logic
 * - unrelated line save/load workflows
 * Validation date: 2026-05-24
 */
import androidx.room.withTransaction
import com.example.chessboard.repository.AppDatabase

class LineDeleter(
    private val database: AppDatabase
) {

    private val lineDao = database.lineDao()
    private val linePositionDao = database.linePositionDao()
    private val positionCleanupService = PositionCleanupService(database)

    /**
     * Deletes a line and updates positions accordingly.
     *
     * - Removes position usage rows
     * - Removes the line
     * - Updates or deletes positions depending on remaining usage
     */
    suspend fun deleteLine(lineId: Long) {
        database.withTransaction {
            deleteLineWithinTransaction(lineId)
        }
    }

    suspend fun deleteLines(lineIds: List<Long>) {
        if (lineIds.isEmpty()) {
            return
        }

        database.withTransaction {
            for (lineId in lineIds.distinct()) {
                deleteLineWithinTransaction(lineId)
            }
        }
    }

    private suspend fun deleteLineWithinTransaction(lineId: Long) {
        val affectedPositionIds = linePositionDao
            .getPositionsForLine(lineId)
            .map { it.positionId }
            .distinct()

        linePositionDao.deleteByLineId(lineId)
        lineDao.deleteById(lineId)
        positionCleanupService.cleanupPositions(affectedPositionIds)
    }
}
