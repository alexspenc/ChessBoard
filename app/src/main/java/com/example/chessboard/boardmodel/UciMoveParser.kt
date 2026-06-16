package com.example.chessboard.boardmodel

/**
 * File role: converts UCI move strings into chesslib move objects.
 * Allowed here:
 * - pure UCI-to-Move conversion helpers that need the current board side to resolve promotions
 * - small chesslib adapter logic reused by board, import, analysis, and move-tree replay code
 * Not allowed here:
 * - PGN/SAN parsing, persistence workflows, Compose UI, or database access
 * Validation date: 2026-06-16
 */

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

internal fun buildChesslibMoveFromUci(
    uci: String,
    board: Board,
): Move {
    val fromSquare = Square.fromValue(uci.take(2).uppercase())
    val toSquare = Square.fromValue(uci.drop(2).take(2).uppercase())
    val promotionPiece = resolvePromotionPiece(
        promotionToken = uci.getOrNull(4),
        board = board,
    )

    if (promotionPiece == Piece.NONE) {
        return Move(fromSquare, toSquare)
    }

    return Move(fromSquare, toSquare, promotionPiece)
}

private fun resolvePromotionPiece(
    promotionToken: Char?,
    board: Board,
): Piece {
    val normalizedToken = promotionToken?.lowercaseChar() ?: return Piece.NONE
    val isWhiteMove = board.sideToMove.name == "WHITE"
    return when (normalizedToken) {
        'q' -> resolvePromotionPieceColor(isWhiteMove, Piece.WHITE_QUEEN, Piece.BLACK_QUEEN)
        'r' -> resolvePromotionPieceColor(isWhiteMove, Piece.WHITE_ROOK, Piece.BLACK_ROOK)
        'b' -> resolvePromotionPieceColor(isWhiteMove, Piece.WHITE_BISHOP, Piece.BLACK_BISHOP)
        'n' -> resolvePromotionPieceColor(isWhiteMove, Piece.WHITE_KNIGHT, Piece.BLACK_KNIGHT)
        else -> Piece.NONE
    }
}

private fun resolvePromotionPieceColor(
    isWhiteMove: Boolean,
    whitePiece: Piece,
    blackPiece: Piece,
): Piece {
    if (isWhiteMove) {
        return whitePiece
    }

    return blackPiece
}
