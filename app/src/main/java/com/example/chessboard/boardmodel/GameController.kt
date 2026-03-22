package com.example.chessboard.boardmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.chessboard.ui.BoardOrientation
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

class GameController (val inOrientation : BoardOrientation = BoardOrientation.WHITE) {

    private var board = Board()
    private var orientation = inOrientation
    private val moves = mutableListOf<Move>()
    private var currentMoveIndex = 0
    private var startSquare : String? = null

    var canUndo by mutableStateOf(false)
        private set

    var canRedo by mutableStateOf(false)
        private set

    fun resetToStartPosition() {
        canUndo = false
        canRedo = false
        startSquare = null
        currentMoveIndex = 0
        board = Board()
        moves.clear()
    }

    private fun tryMove(move : Move) : Boolean {
        if (!board.legalMoves().contains(move)) { return false }

        println("Move ${move} is fucking legal")
        this.board.doMove(move)
        moves.add(move)

        // If in middle game - need delete tails moves
        if (currentMoveIndex < moves.size) {
            moves.subList(currentMoveIndex, moves.size).clear()
        }
        updateState()

        return true
    }

    fun tryMove(from: String, to: String): Boolean {
        return try {
            println("try move from ${from} to ${to}")
            val move = Move(
                Square.fromValue(from.uppercase()),
                Square.fromValue(to.uppercase())
            )

            currentMoveIndex++
            tryMove(move)
        } catch (e: Exception) {
            println("Error on try move. Err ${e}")
            return false
        }
    }

    fun undoMove(): Boolean {
        if (currentMoveIndex == 0) { return false }

        currentMoveIndex--
        board.undoMove()
        updateState()

        return true
    }

    fun redoMove(): Boolean {
        if (currentMoveIndex >= moves.size) return false

        this.board.doMove(moves[currentMoveIndex])
        currentMoveIndex++
        updateState()

        return true
    }

    fun goToMove(index: Int) : Boolean {
        if (index < 0 || index > moves.size) { return false }

        board = Board()
        for (i in 0 until index) {
            tryMove(moves[i])
        }

        return true
    }

    private fun updateState() {
        canUndo = currentMoveIndex > 0
        canRedo = currentMoveIndex < moves.size
    }

    // const function
    fun getOrientation() : BoardOrientation {
        return this.orientation
    }

    // const function
    fun getFen(): String {
        return board.fen
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

        val moves = board.legalMoves()
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
        val piece = board.getPiece(sq)
        if (piece == Piece.NONE) {
            return null
        }
        if (piece.pieceSide != board.sideToMove) {
            return null
        }

        return piece
    }
}