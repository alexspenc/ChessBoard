package com.example.chessboard.database

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.move.Move

class GameUniquenessChecker(
    private val positionDao: PositionDao
) {

    suspend fun hasUniquePosition(
        initialFen: String,
        moves: List<Move>
    ): Boolean {

        val board = Board()

        if (initialFen.isNotEmpty()) {
            board.loadFromFen(initialFen)
        }

        if (isPositionUnique(board)) {
            return true
        }

        for (move in moves) {
            board.doMove(move)

            if (isPositionUnique(board)) {
                return true
            }
        }

        return false
    }

    private suspend fun isPositionUnique(board: Board): Boolean {
        val fen = board.fen
        val hash = board.zobristKey

        val existingFens = positionDao.getFensByHash(hash)

        if (existingFens.isEmpty()) {
            return true
        }

        return existingFens.none { it == fen }
    }
}