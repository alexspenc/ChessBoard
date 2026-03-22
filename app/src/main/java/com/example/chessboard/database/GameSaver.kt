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
    private val uniquenessChecker = GameUniqueChecker(positionDao)

    /**
     * Attempts to save a chess game into the database.
     *
     * The game will be saved only if it contains at least one unique position
     * (i.e., a position that does not yet exist in the database).
     *
     * All operations are executed inside a single transaction to guarantee consistency.
     *
     * @param game Game metadata and PGN
     * @param moves List of moves to reconstruct the game
     * @param sideMask Side(s) for which this game/positions are relevant
     *
     * @return true if the game was successfully saved, false otherwise
     */
    suspend fun trySaveGame(
        game: GameEntity,
        moves: List<Move>,
        sideMask: Int
    ): Boolean {

        return database.withTransaction {

            println("Moves count ${moves.count()} on trySaveGame")
            val isUnique = uniquenessChecker.hasUniquePosition(
                game.initialFen,
                moves, sideMask
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
            savePositionAndLink(gameId, board, ply, sideMask)
            for (move in moves) {
                board.doMove(move)
                ply++
                savePositionAndLink(gameId, board, ply, sideMask)
            }

            println("Count games ${gameDao.getCount()}")
            println("Count positions ${positionDao.getCount()}")
            println("Count positions to game ${gamePositionDao.getCount()}")

            true
        }
    }

    /**
     * Saves the current board position (if needed) and creates a link
     * between the game and the position.
     *
     * @param gameId ID of the game
     * @param board Current board state
     * @param ply Move index (half-move number)
     * @param sideMask Side(s) for which this position is relevant
     */
    private suspend fun savePositionAndLink(
        gameId: Long,
        board: Board,
        ply: Int,
        sideMask: Int
    ) {
        val positionId = getOrInsertPositionId(board.zobristKey, board.fen, sideMask)
        gamePositionDao.insertGamePosition(
            GamePositionEntity(
                gameId = gameId,
                positionId = positionId,
                ply = ply
            )
        )
    }

    /**
     * Retrieves an existing position ID by (hash + FEN),
     * or inserts a new position if it does not exist.
     *
     * If the position already exists but was previously stored for a different side,
     * the sideMask is updated to include both sides.
     *
     * @param hash Zobrist hash of the position
     * @param fen Full FEN string of the position
     * @param sideMask Side(s) for which this position is currently being stored
     *
     * @return ID of the existing or newly inserted position
     */
    private suspend fun getOrInsertPositionId(
        hash: Long,
        fen: String,
        sideMask: Int
    ): Long {
        println("try save fen $fen")
        val existingIdAndSide = positionDao.getIdAndSideByHashAndFen(hash, fen)

        if (existingIdAndSide == null) {
            val posEntity = PositionEntity(hash = hash, fen = fen, sideMask = sideMask)
            return positionDao.insertPosition(posEntity)
        }

        if (existingIdAndSide.sideMask != sideMask) {
            positionDao.updateSideMask(existingIdAndSide.id, SideMask.BOTH)
        }

        return existingIdAndSide.id
    }
}