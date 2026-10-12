package com.example.chessboard.chesscorechesslib

/**
 * Implements Position using a dedicated chesslib board instance.
 * Exposes position state and legal moves using chesscore types.
 * Applying a move modifies the current position.
 * Does not contain SAN, UCI, or variation structure parsing.
 * Validation date: 2026-09-08.
 */

import com.example.chessboard.chesscore.model.Move
import com.example.chessboard.chesscore.model.Piece
import com.example.chessboard.chesscore.model.PieceType
import com.example.chessboard.chesscore.Position
import com.example.chessboard.chesscore.model.PromotionPiece
import com.example.chessboard.chesscore.model.Side
import com.example.chessboard.chesscore.model.Square
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece as ChesslibPiece
import com.github.bhlangonijr.chesslib.PieceType as ChesslibPieceType
import com.github.bhlangonijr.chesslib.Side as ChesslibSide
import com.github.bhlangonijr.chesslib.Square as ChesslibSquare
import com.github.bhlangonijr.chesslib.move.Move as ChesslibMove

internal class ChesslibPosition(
    private val board: Board,
) : Position {

    /**
     * Returns the complete six-field FEN of the current position, for example:
     * rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
     */
    override fun getFen(): String {
        return board.fen
    }

    override fun getSideToMove(): Side {
        return when (board.sideToMove) {
            ChesslibSide.WHITE -> Side.WHITE
            ChesslibSide.BLACK -> Side.BLACK
        }
    }

    override fun getPiece(square: Square): Piece? {
        fun toPieceType(type: ChesslibPieceType): PieceType {
            return when (type) {
                ChesslibPieceType.PAWN -> PieceType.PAWN
                ChesslibPieceType.KNIGHT -> PieceType.KNIGHT
                ChesslibPieceType.BISHOP -> PieceType.BISHOP
                ChesslibPieceType.ROOK -> PieceType.ROOK
                ChesslibPieceType.QUEEN -> PieceType.QUEEN
                ChesslibPieceType.KING -> PieceType.KING
                else -> error("Unsupported chesslib piece type: $type")
            }
        }

        val chesslibSquare = ChesslibSquare.fromValue(
            "${square.file.uppercaseChar()}${square.rank}",
        )
        val piece = board.getPiece(chesslibSquare)
        if (piece == ChesslibPiece.NONE) {
            return null
        }

        val side = when (piece.pieceSide) {
            ChesslibSide.WHITE -> Side.WHITE
            ChesslibSide.BLACK -> Side.BLACK
        }
        return Piece(side = side, type = toPieceType(piece.pieceType))
    }

    override fun getLegalMoves(): List<Move> {
        return board.legalMoves().map(::toMove)
    }

    override fun applyMove(move: Move): Boolean {
        val legalMove = board.legalMoves().firstOrNull { candidate ->
            toMove(candidate) == move
        } ?: return false

        return board.doMove(legalMove)
    }

    private fun toMove(move: ChesslibMove): Move {
        fun toSquare(square: ChesslibSquare): Square {
            val coordinates = square.value()
            return Square(
                file = coordinates[0].lowercaseChar(),
                rank = coordinates[1].digitToInt(),
            )
        }

        fun toPromotionPiece(piece: ChesslibPiece): PromotionPiece? {
            return when (piece) {
                ChesslibPiece.NONE -> null
                ChesslibPiece.WHITE_QUEEN,
                ChesslibPiece.BLACK_QUEEN -> PromotionPiece.QUEEN
                ChesslibPiece.WHITE_ROOK,
                ChesslibPiece.BLACK_ROOK -> PromotionPiece.ROOK
                ChesslibPiece.WHITE_BISHOP,
                ChesslibPiece.BLACK_BISHOP -> PromotionPiece.BISHOP
                ChesslibPiece.WHITE_KNIGHT,
                ChesslibPiece.BLACK_KNIGHT -> PromotionPiece.KNIGHT
                else -> error("Unsupported chesslib promotion piece: $piece")
            }
        }

        return Move(
            from = toSquare(move.from),
            to = toSquare(move.to),
            promotion = toPromotionPiece(move.promotion),
        )
    }
}
