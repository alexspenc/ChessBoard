package com.example.chessboard.service

import androidx.room.withTransaction
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.entity.SideMask
import com.example.chessboard.repository.AppDatabase
import com.github.bhlangonijr.chesslib.move.Move

class GameUpdater(
    private val database: AppDatabase
) {

    private val gameDao = database.gameDao()
    private val positionDao = database.positionDao()
    private val gamePositionDao = database.gamePositionDao()
    private val gameSaver = GameSaver(database)

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

            for (positionId in affectedPositionIds) {
                handlePositionAfterUnlink(positionId)
            }

            gameDao.deleteById(game.id)

            gameSaver.trySaveGame(
                game = game,
                moves = moves,
                sideMask = game.sideMask
            )
        }
    }

    private suspend fun handlePositionAfterUnlink(positionId: Long) {
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
