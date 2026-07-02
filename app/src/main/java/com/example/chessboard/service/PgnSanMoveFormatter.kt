package com.example.chessboard.service

/*
 * File role: converts legal UCI moves into SAN labels for PGN text generation.
 * Allowed here:
 * - resolving a UCI move against a supplied chesslib board
 * - SAN notation details such as captures, castling, disambiguation, promotions, check, and mate
 * Not allowed here:
 * - PGN tree/variation building, PGN headers, Compose UI, database access, or file I/O
 * Validation date: 2026-06-30
 */

import com.example.chessboard.boardmodel.buildChesslibMoveFromUci
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

data class PgnSanMove(
    val move: Move,
    val san: String,
)

/**
 * Resolves [uciMove] on [board] and returns the legal chesslib move with its SAN label.
 * The supplied [board] is treated as the position before the move and is not mutated.
 */
fun resolvePgnSanMove(
    uciMove: String,
    board: Board,
): PgnSanMove {
    val move = buildChesslibMoveFromUci(uci = uciMove, board = board)
    require(board.legalMoves().contains(move)) {
        "Illegal PGN move: $uciMove from ${board.fen}"
    }

    return PgnSanMove(
        move = move,
        san = resolvePgnSanMoveLabel(move = move, board = board),
    )
}

private fun resolvePgnSanMoveLabel(
    move: Move,
    board: Board,
): String {
    val movingPiece = board.getPiece(move.from)
    val baseNotation = resolvePgnSanBaseNotation(
        move = move,
        board = board,
        movingPiece = movingPiece,
    )
    val promotionSuffix = resolvePgnSanPromotionSuffix(move)
    val checkSuffix = resolvePgnSanCheckSuffix(
        move = move,
        board = board,
    )

    return "$baseNotation$promotionSuffix$checkSuffix"
}

private fun resolvePgnSanBaseNotation(
    move: Move,
    board: Board,
    movingPiece: Piece,
): String {
    val destinationSquare = move.to.value().lowercase()
    val isCapture = resolvePgnSanCapture(
        move = move,
        board = board,
    )
    val castleNotation = resolvePgnSanCastleNotation(
        move = move,
        movingPiece = movingPiece,
    )
    if (castleNotation != null) {
        return castleNotation
    }

    if (movingPiece.pieceType == PieceType.PAWN) {
        return buildPgnSanPawnNotation(
            move = move,
            destinationSquare = destinationSquare,
            isCapture = isCapture,
        )
    }

    return buildPgnSanPieceNotation(
        move = move,
        board = board,
        movingPiece = movingPiece,
        destinationSquare = destinationSquare,
        isCapture = isCapture,
    )
}

private fun buildPgnSanPawnNotation(
    move: Move,
    destinationSquare: String,
    isCapture: Boolean,
): String {
    return buildString {
        if (isCapture) {
            append(move.from.value()[0].lowercaseChar())
            append('x')
        }
        append(destinationSquare)
    }
}

private fun buildPgnSanPieceNotation(
    move: Move,
    board: Board,
    movingPiece: Piece,
    destinationSquare: String,
    isCapture: Boolean,
): String {
    val piecePrefix = resolvePgnSanPiecePrefix(movingPiece.pieceType)
    val disambiguation = resolvePgnSanDisambiguation(
        move = move,
        board = board,
        movingPiece = movingPiece,
    )

    return buildString {
        append(piecePrefix)
        append(disambiguation)
        if (isCapture) {
            append('x')
        }
        append(destinationSquare)
    }
}

private fun resolvePgnSanCheckSuffix(
    move: Move,
    board: Board,
): String {
    val nextBoard = Board().also { it.loadFromFen(board.fen) }
    nextBoard.doMove(move)

    if (nextBoard.legalMoves().isEmpty() && nextBoard.isKingAttacked) {
        return "#"
    }

    if (nextBoard.isKingAttacked) {
        return "+"
    }

    return ""
}

private fun resolvePgnSanCapture(
    move: Move,
    board: Board,
): Boolean {
    if (board.getPiece(move.to) != Piece.NONE) {
        return true
    }

    val movingPiece = board.getPiece(move.from)
    if (movingPiece.pieceType != PieceType.PAWN) {
        return false
    }

    return move.from.value()[0] != move.to.value()[0]
}

private fun resolvePgnSanPiecePrefix(
    pieceType: PieceType,
): String {
    if (pieceType == PieceType.KNIGHT) {
        return "N"
    }

    if (pieceType == PieceType.BISHOP) {
        return "B"
    }

    if (pieceType == PieceType.ROOK) {
        return "R"
    }

    if (pieceType == PieceType.QUEEN) {
        return "Q"
    }

    if (pieceType == PieceType.KING) {
        return "K"
    }

    return ""
}

private fun resolvePgnSanDisambiguation(
    move: Move,
    board: Board,
    movingPiece: Piece,
): String {
    if (movingPiece.pieceType == PieceType.PAWN || movingPiece.pieceType == PieceType.KING) {
        return ""
    }

    val candidates = board.legalMoves().filter { candidate ->
        if (candidate == move) {
            return@filter false
        }

        if (candidate.to != move.to) {
            return@filter false
        }

        if (candidate.promotion != move.promotion) {
            return@filter false
        }

        board.getPiece(candidate.from) == movingPiece
    }
    if (candidates.isEmpty()) {
        return ""
    }

    val sameFileExists = candidates.any { it.from.file == move.from.file }
    val sameRankExists = candidates.any { it.from.rank == move.from.rank }
    if (!sameFileExists) {
        return move.from.value()[0].lowercaseChar().toString()
    }

    if (!sameRankExists) {
        return move.from.value()[1].toString()
    }

    return move.from.value().lowercase()
}

private fun resolvePgnSanCastleNotation(
    move: Move,
    movingPiece: Piece,
): String? {
    if (movingPiece.pieceType != PieceType.KING) {
        return null
    }

    if ((move.from == Square.E1 && move.to == Square.G1) || (move.from == Square.E8 && move.to == Square.G8)) {
        return "O-O"
    }

    if ((move.from == Square.E1 && move.to == Square.C1) || (move.from == Square.E8 && move.to == Square.C8)) {
        return "O-O-O"
    }

    return null
}

private fun resolvePgnSanPromotionSuffix(
    move: Move,
): String {
    if (move.promotion == Piece.NONE) {
        return ""
    }

    return "=${move.promotion.pieceType.name.first().uppercaseChar()}"
}
