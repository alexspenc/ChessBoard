package com.example.chessboard.analysis

/**
 * Shares pure board-replay helpers for opening deviation analysis.
 * Keep move parsing, board advancement, and normalized FEN helpers here.
 * Do not add UI state, navigation, or database access to this file.
 * Validation date: 2026-06-16.
 */
import com.example.chessboard.boardmodel.buildChesslibMoveFromUci
import com.example.chessboard.entity.LineEntity
import com.example.chessboard.service.normalizeFenWithoutMoveNumbers
import com.github.bhlangonijr.chesslib.Board
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
        line: LineEntity,
        moveIndex: Int,
    ): Move {
        val move = buildChesslibMoveFromUci(uci = uci, board = board)

        if (board.legalMoves().contains(move)) {
            return move
        }

        throw IllegalArgumentException(
            "Illegal move $uci at index $moveIndex in line ${line.id}"
        )
    }
}
