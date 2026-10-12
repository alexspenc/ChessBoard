package com.example.chessboard.chesscorechesslib

/**
 * Creates positions using chesslib and exposes them through the chesscore interface.
 *
 * Creates the standard starting position when the FEN is null.
 * The supplied FEN must contain exactly six fields, for example:
 * rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
 *
 * Checks the FEN field count and delegates position loading to chesslib.
 * Does not contain move or variation parsing logic.
 * Validation date: 2026-09-08.
 */

import com.example.chessboard.chesscore.Position
import com.example.chessboard.chesscore.PositionFactory
import com.github.bhlangonijr.chesslib.Board

class ChesslibPositionFactory : PositionFactory {

    override fun create(fen: String?): Position {
        if (fen == null) {
            return ChesslibPosition(Board())
        }

        val fields = fen.trim().split(Regex("\\s+"))
        require(fields.size == 6) {
            "FEN must contain six fields: $fen"
        }

        val normalizedFen = fields.joinToString(" ")
        val board = Board()
        try {
            board.loadFromFen(normalizedFen)
        } catch (exception: Exception) {
            throw IllegalArgumentException("Failed to load FEN: $fen", exception)
        }

        return ChesslibPosition(board)
    }
}
