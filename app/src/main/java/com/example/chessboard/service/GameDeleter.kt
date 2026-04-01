package com.example.chessboard.service

import androidx.room.withTransaction
import com.example.chessboard.repository.AppDatabase

class GameDeleter(
    private val database: AppDatabase
) {

    private val gameDao = database.gameDao()
    private val gamePositionDao = database.gamePositionDao()
    private val positionCleanupService = PositionCleanupService(database)

    /**
     * Deletes a game and updates positions accordingly.
     *
     * - Removes game_positions
     * - Removes the game
     * - Updates or deletes positions depending on remaining usage
     */
    suspend fun deleteGame(gameId: Long) {

        database.withTransaction {
            val affectedPositionIds = gamePositionDao
                .getPositionsForGame(gameId)
                .map { it.positionId }
                .distinct()

            gamePositionDao.deleteByGameId(gameId)
            gameDao.deleteById(gameId)
            positionCleanupService.cleanupPositions(affectedPositionIds)
        }
    }
}
