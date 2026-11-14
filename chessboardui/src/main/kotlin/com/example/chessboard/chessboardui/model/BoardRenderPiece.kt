package com.example.chessboard.chessboardui.model

/**
 * Describes pieces placed on squares for board rendering.
 * Keep chesscore-based drawing inputs here; do not add chess rules or application models.
 * Validation date: 2026-10-13.
 */

import com.example.chessboard.chesscore.model.Piece
import com.example.chessboard.chesscore.model.Square

/**
 * Places a chesscore piece on a square without validating chess-position legality.
 *
 * @param piece the piece to display.
 * @param square the occupied square.
 */
data class BoardRenderPiece(
    val piece: Piece,
    val square: Square,
)
