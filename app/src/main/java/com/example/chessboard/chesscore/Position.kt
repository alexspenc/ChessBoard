package com.example.chessboard.chesscore

/**
 * Defines position operations for the shared line parser.
 * Does not contain chess rule implementations, UI, or persistence.
 * Validation date: 2026-09-08.
 */

import com.example.chessboard.chesscore.model.Move
import com.example.chessboard.chesscore.model.Piece
import com.example.chessboard.chesscore.model.Side
import com.example.chessboard.chesscore.model.Square

interface Position {

    /**
     * Returns the complete six-field FEN of the current position, for example:
     * rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
     */
    fun getFen(): String

    fun getSideToMove(): Side

    /** Returns null if the square is empty. */
    fun getPiece(square: Square): Piece?

    /**
     * Returns a snapshot of the legal moves in the current position.
     * Subsequent position changes do not alter this list.
     */
    fun getLegalMoves(): List<Move>

    /**
     * Applies a legal move, modifying the current position.
     *
     * Returns false if the move is illegal.
     * In that case, the position remains unchanged.
     */
    fun applyMove(move: Move): Boolean
}
