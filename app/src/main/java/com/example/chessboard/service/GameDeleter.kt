package com.example.chessboard.service

import androidx.room.withTransaction
import com.example.chessboard.repository.AppDatabase
import com.example.chessboard.entity.SideMask

class GameDeleter(
    private val database: AppDatabase
) {

    private val gameDao = database.gameDao()
    private val positionDao = database.positionDao()
    private val gamePositionDao = database.gamePositionDao()

    /**
     * Deletes a game and updates positions accordingly.
     *
     * - Removes game_positions
     * - Removes the game
     * - Updates or deletes positions depending on remaining usage
     */
    suspend fun deleteGame(gameId: Long) {

        database.withTransaction {

            // 1. All positions for this game
            val positionIds = gamePositionDao
                .getPositionsForGame(gameId)
                .map { it.positionId }
                .distinct()

            // 2. Remove all links
            gamePositionDao.deleteByGameId(gameId)

            // 3. Delete game
            gameDao.deleteById(gameId)

            // 4. Handle positions
            for (positionId in positionIds) {
                handlePositionAfterDeletion(positionId)
            }
        }
    }

    private suspend fun handlePositionAfterDeletion(positionId: Long) {

        val usage = gamePositionDao.getUsage(positionId)

        // 1. No position usage, so can delete
        if (usage.isEmpty()) {
            positionDao.deleteById(positionId)
            return
        }

        // 2. Update sideMask
        var newMask = 0

        for (u in usage) {
            newMask = newMask or u.sideMask
            if (newMask == SideMask.BOTH) { break }
        }

        positionDao.updateSideMask(positionId, newMask)
    }
}
