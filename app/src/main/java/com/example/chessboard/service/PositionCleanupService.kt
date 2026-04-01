package com.example.chessboard.service

import com.example.chessboard.entity.SideMask
import com.example.chessboard.repository.AppDatabase

class PositionCleanupService(
    private val database: AppDatabase
) {

    private val positionDao = database.positionDao()
    private val gamePositionDao = database.gamePositionDao()

    suspend fun cleanupPositions(positionIds: List<Long>) {
        for (positionId in positionIds) {
            cleanupPosition(positionId)
        }
    }

    private suspend fun cleanupPosition(positionId: Long) {
        val usage = gamePositionDao.getUsage(positionId)

        if (usage.isEmpty()) {
            positionDao.deleteById(positionId)
            return
        }

        var newMask = 0

        for (positionUsage in usage) {
            newMask = newMask or positionUsage.sideMask

            if (newMask == SideMask.BOTH) {
                positionDao.updateSideMask(positionId, newMask)
                return
            }
        }

        positionDao.updateSideMask(positionId, newMask)
    }
}
