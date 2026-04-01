package com.example.chessboard.service

import androidx.room.withTransaction
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.repository.AppDatabase
import com.github.bhlangonijr.chesslib.move.Move

class GameUpdater(
    private val database: AppDatabase
) {

    private val gameDao = database.gameDao()
    private val gamePositionDao = database.gamePositionDao()
    private val gameSaver = GameSaver(database)
    private val positionCleanupService = PositionCleanupService(database)

    /**
     * Replaces an existing game with the edited version while preserving uniqueness guarantees.
     *
     * The update flow is transactional:
     * 1. Remove links to all positions used by the edited game
     * 2. Delete or update affected positions depending on remaining usage
     * 3. Delete the old game row
     * 4. Save the edited game again through [GameSaver]
     *
     * If saving the edited game fails, the whole transaction is rolled back.
     */
    suspend fun updateGame(
        game: GameEntity,
        moves: List<Move>
    ): Boolean {
        return database.withTransaction {
            val affectedPositionIds = gamePositionDao
                .getPositionsForGame(game.id)
                .map { it.positionId }
                .distinct()

            gamePositionDao.deleteByGameId(game.id)
            positionCleanupService.cleanupPositions(affectedPositionIds)

            gameDao.deleteById(game.id)

            gameSaver.trySaveGame(
                game = game,
                moves = moves,
                sideMask = game.sideMask
            )
        }
    }
}
