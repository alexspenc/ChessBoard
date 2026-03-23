package com.example.chessboard.service

import com.example.chessboard.repository.PositionDao
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.move.Move

class GameUniqueChecker(
    private val positionDao: PositionDao
) {

    suspend fun hasUniquePosition(
        initialFen: String,
        moves: List<Move>,
        sideMask: Int
    ): Boolean {

        val board = Board()

        if (initialFen.isNotEmpty()) {
            board.loadFromFen(initialFen)
        }

        if (isPositionUnique(board, sideMask)) {
            return true
        }

        for (move in moves) {
            board.doMove(move)
            if (isPositionUnique(board, sideMask)) {
                return true
            }
        }

        return false
    }

    private suspend fun isPositionUnique(board: Board, sideMask: Int): Boolean {
        val fen = board.fen
        val hash = board.zobristKey
        val existingFens = positionDao.getFensByHashAndSide(hash, sideMask)

        if (existingFens.isEmpty()) {
            return true
        }

        return existingFens.none { it == fen }
    }
}