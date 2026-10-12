package com.example.chessboard.chesscore

/**
 * Defines the position creation contract using chesscore types.
 * Does not contain chess rule implementations or move parsing.
 * Validation date: 2026-09-08.
 */

interface PositionFactory {

    /**
     * Creates an independent position instance.
     *
     * null means the standard starting position.
     * The supplied FEN must contain six fields, for example:
     * rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
     *
     * @throws IllegalArgumentException if the implementation rejects the FEN.
     */
    fun create(fen: String? = null): Position
}
