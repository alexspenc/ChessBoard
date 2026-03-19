package com.example.chessboard.boardmodel

import com.example.chessboard.ui.BoardOrientation
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.game.Game
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.game.Round
import com.github.bhlangonijr.chesslib.game.Event
import com.github.bhlangonijr.chesslib.game.Player

class GameController (val inOrientation : BoardOrientation = BoardOrientation.WHITE) {

    private val game = Game("Dummy", Round(Event()))
    private var orientation = inOrientation
    private val moves = mutableListOf<Move>()

    private var startSquare : String? = null

    init {
        game.board = Board()
    }

    fun tryMove(from: String, to: String): Boolean {
        return try {
            println("try move from ${from} to ${to}")
            val move = Move(
                Square.fromValue(from.uppercase()),
                Square.fromValue(to.uppercase())
            )

            if (game.board.legalMoves().contains(move)) {
                println("Move ${move} is fucking legal")
                this.game.board.doMove(move)
                moves.add(move)
                return true
            }
            false
        } catch (e: Exception) {
            println("Error on try move. Err ${e}")
            return false
        }
    }

    // const function
    fun getOrientation() : BoardOrientation {
        return this.orientation
    }

    // const function
    fun getFen(): String {
        if(game.board == null) {
            throw Exception("Board is null when try gen fen")
        }
        return game.board.fen
    }

    fun generatePgn(
        whiteName: String = "White",
        blackName: String = "Black",
        event: String = "Casual Game"
    ): String {
        val sb = StringBuilder()

        // Headers
        sb.append("[Event \"$event\"]\n")
        sb.append("[White \"$whiteName\"]\n")
        sb.append("[Black \"$blackName\"]\n")
        sb.append("[Result \"*\"]\n\n")

        moves.forEachIndexed { index, move ->
            if (index % 2 == 0) {
                sb.append("${index / 2 + 1}. ")
            }
        }

        sb.append("*")

        return sb.toString().trim()
    }

    // const function
    fun getBoardPosition() : BoardPosition {
        return ChesslibMapper.fromFen(getFen())
    }

    // const function
    fun getStartSquare() : String? {
        return startSquare
    }

    // const function
    fun canSelectSquare(square: String?): Boolean {
        val sqStr = square ?: return false
        val sq = Square.fromValue(sqStr.uppercase())

        val piece = getPieceWithLegalMovesFromSquare(square)
        if (piece == null) { return false }

        val moves = game.board.legalMoves()
        return moves.any { it.from == sq }
    }

    fun setStartSquare(square: String?) : Boolean {
        if (canSelectSquare(square)) {
            startSquare = square
            return true
        }
        startSquare = null
        return false
    }

    fun setDestinationSquareAndTryMove(destinationSquare: String?) : Boolean {
        if (destinationSquare == null) {
            return false
        }
        val piece = getPieceWithLegalMovesFromSquare(startSquare)
        if (piece == null) { return false }
        val wasMove = tryMove(startSquare!!, destinationSquare)

        startSquare = null

        return wasMove
    }

    private fun getPieceWithLegalMovesFromSquare(square: String?) : Piece? {
        if (square == null) { return null }

        val sq = Square.fromValue(square.uppercase())
        val piece = game.board.getPiece(sq)
        if (piece == Piece.NONE) {
            return null
        }
        if (piece.pieceSide != game.board.sideToMove) {
            return null
        }

        return piece
    }
}