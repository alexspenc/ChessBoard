package com.example.chessboard.database

import androidx.room.withTransaction
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.move.Move

class GameSaver(
    private val database: AppDatabase
) {

    private val gameDao = database.gameDao()
    private val positionDao = database.positionDao()
    private val gamePositionDao = database.gamePositionDao()
    private val uniquenessChecker = GameUniquenessChecker(positionDao)

    suspend fun trySaveGame(
        game: GameEntity,
        moves: List<Move>
    ): Boolean {

        return database.withTransaction {

            val isUnique = uniquenessChecker.hasUniquePosition(
                game.initialFen,
                moves
            )

            if (!isUnique) {
                return@withTransaction false
            }

            val gameId = gameDao.insertGame(game)

            if (gameId == -1L) {
                return@withTransaction false
            }

            val board = Board()

            if (game.initialFen.isNotEmpty()) {
                board.loadFromFen(game.initialFen)
            }

            var ply = 0

            // 3. Начальная позиция
            ply = savePositionAndLink(gameId, board, ply)

            // 4. Все ходы
            for (move in moves) {
                board.doMove(move)
                ply++
                savePositionAndLink(gameId, board, ply)
            }

            true
        }
    }

    private suspend fun savePositionAndLink(
        gameId: Long,
        board: Board,
        ply: Int
    ): Int {
        val positionId = getOrInsertPositionId(board.zobristKey, board.fen)

        gamePositionDao.insertGamePosition(
            GamePositionEntity(
                gameId = gameId,
                positionId = positionId,
                ply = ply
            )
        )

        return ply
    }

    private suspend fun getOrInsertPositionId(
        hash: Long,
        fen: String
    ): Long {

        val existingId = positionDao.getIdByHashAndFen(hash, fen)

        if (existingId != null) {
            return existingId
        }

        return positionDao.insertPosition(
            PositionEntity(
                hash = hash,
                fen = fen
            )
        )
    }
}