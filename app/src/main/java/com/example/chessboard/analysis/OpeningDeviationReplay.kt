package com.example.chessboard.analysis

/**
 * Shares pure board-replay helpers for opening deviation analysis.
 * Keep move parsing, board advancement, and normalized FEN helpers here.
 * Do not add UI state, navigation, or database access to this file.
 */
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.service.normalizeFenWithoutMoveNumbers
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

internal object OpeningDeviationReplay {

    fun buildInitialBoard(initialFen: String): Board {
        val board = Board()
        if (initialFen.isBlank()) {
            return board
        }

        board.loadFromFen(initialFen)
        return board
    }

    fun buildPositionKey(board: Board): String {
        return normalizeFenWithoutMoveNumbers(
            fen = board.fen,
            includeEnPassant = true,
        )
    }

    fun isSelectedSideToMove(
        board: Board,
        selectedSide: OpeningSide,
    ): Boolean {
        return board.sideToMove.name == selectedSide.name
    }

    fun buildMoveFromUci(
        uci: String,
        board: Board,
        game: GameEntity,
        moveIndex: Int,
    ): Move {
        val from = uci.take(2).uppercase()
        val to = uci.drop(2).take(2).uppercase()
        val promotionPiece = resolvePromotionPiece(uci.getOrNull(4), board)
        val move = buildMove(from = from, to = to, promotionPiece = promotionPiece)

        if (board.legalMoves().contains(move)) {
            return move
        }

        throw IllegalArgumentException(
            "Illegal move $uci at index $moveIndex in game ${game.id}"
        )
    }

    private fun buildMove(
        from: String,
        to: String,
        promotionPiece: Piece,
    ): Move {
        if (promotionPiece == Piece.NONE) {
            return Move(Square.fromValue(from), Square.fromValue(to))
        }

        return Move(Square.fromValue(from), Square.fromValue(to), promotionPiece)
    }

    private fun resolvePromotionPiece(
        promotionChar: Char?,
        board: Board,
    ): Piece {
        val isWhite = board.sideToMove.name == OpeningSide.WHITE.name
        return when (promotionChar?.lowercaseChar()) {
            'q' -> if (isWhite) Piece.WHITE_QUEEN else Piece.BLACK_QUEEN
            'r' -> if (isWhite) Piece.WHITE_ROOK else Piece.BLACK_ROOK
            'b' -> if (isWhite) Piece.WHITE_BISHOP else Piece.BLACK_BISHOP
            'n' -> if (isWhite) Piece.WHITE_KNIGHT else Piece.BLACK_KNIGHT
            else -> Piece.NONE
        }
    }
}
