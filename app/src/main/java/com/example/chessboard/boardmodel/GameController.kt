package com.example.chessboard.boardmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.chessboard.ui.BoardOrientation
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

data class LastMoveHighlight(
    val from: String,
    val to: String
)


class GameController (val inOrientation : BoardOrientation = BoardOrientation.WHITE) {

    private var board = Board()
    private var side = inOrientation
    private var previewFen: String? = null
    private var previewPosition: BoardPosition? = null
    private val moves = mutableListOf<Move>()
    private var allowedMoveUci: String? = null
    private var userMovesEnabled: Boolean = true
    var currentMoveIndex by mutableIntStateOf(0)
        private set
    private var startSquare : String? = null
    var boardState by mutableIntStateOf(0)
        private set

    var canUndo by mutableStateOf(false)
        private set

    var canRedo by mutableStateOf(false)
        private set

    fun resetToStartPosition() {
        clearPreviewState()
        canUndo = false
        canRedo = false
        allowedMoveUci = null
        userMovesEnabled = true
        startSquare = null
        currentMoveIndex = 0
        board = Board().also { it.loadFromFen(InitialBoardFen) }
        moves.clear()
        boardState++
    }

    private fun tryMove(move: Move): Boolean {
        if (!board.legalMoves().contains(move)) { return false }
        if (!isMoveAllowed(move)) { return false }

        // Trim future moves when branching from the middle of the game
        if (currentMoveIndex < moves.size) {
            moves.subList(currentMoveIndex, moves.size).clear()
        }

        board.doMove(move)
        moves.add(move)
        currentMoveIndex++
        updateState()
        boardState++

        return true
    }

    fun tryMove(from: String, to: String): Boolean {
        return try {
            val move = Move(
                Square.fromValue(from.uppercase()),
                Square.fromValue(to.uppercase())
            )
            tryMove(move)
        } catch (e: Exception) {
            false
        }
    }

    fun undoMove(): Boolean {
        if (currentMoveIndex == 0) { return false }

        currentMoveIndex--
        board.undoMove()
        updateState()
        boardState++

        return true
    }

    fun redoMove(): Boolean {
        if (currentMoveIndex >= moves.size) return false

        this.board.doMove(moves[currentMoveIndex])
        currentMoveIndex++
        updateState()
        boardState++

        return true
    }

    private fun updateState() {
        canUndo = currentMoveIndex > 0
        canRedo = currentMoveIndex < moves.size
    }

    /** Returns true if moving [from] → [to] is a pawn promotion (and is legal). */
    fun isPawnPromotion(from: String?, to: String?): Boolean {
        if (from == null || to == null) return false
        return try {
            val fromSq = Square.fromValue(from.uppercase())
            val toSq   = Square.fromValue(to.uppercase())
            val piece  = board.getPiece(fromSq)
            if (piece != Piece.WHITE_PAWN && piece != Piece.BLACK_PAWN) return false
            val toRank = to[1]
            if (piece == Piece.WHITE_PAWN && toRank != '8') return false
            if (piece == Piece.BLACK_PAWN && toRank != '1') return false
            board.legalMoves().any { it.from == fromSq && it.to == toSq && isMoveAllowed(it) }
        } catch (_: Exception) { false }
    }

    /**
     * Executes a pawn promotion move using the current [startSquare] → [to],
     * promoting to [promotionPiece]. Returns false if the move is illegal.
     */
    fun tryMoveWithPromotion(to: String, promotionPiece: PromotionPiece): Boolean {
        val from = startSquare ?: return false
        return try {
            val fromSq  = Square.fromValue(from.uppercase())
            val toSq    = Square.fromValue(to.uppercase())
            val isWhite = board.getPiece(fromSq) == Piece.WHITE_PAWN
            val piece   = when (promotionPiece) {
                PromotionPiece.QUEEN  -> if (isWhite) Piece.WHITE_QUEEN  else Piece.BLACK_QUEEN
                PromotionPiece.ROOK   -> if (isWhite) Piece.WHITE_ROOK   else Piece.BLACK_ROOK
                PromotionPiece.BISHOP -> if (isWhite) Piece.WHITE_BISHOP else Piece.BLACK_BISHOP
                PromotionPiece.KNIGHT -> if (isWhite) Piece.WHITE_KNIGHT else Piece.BLACK_KNIGHT
            }
            val result = tryMove(Move(fromSq, toSq, piece))
            startSquare = null
            result
        } catch (_: Exception) { false }
    }

    fun loadFromFen(fen: String): Boolean {
        startSquare = null
        return try {
            clearPreviewState()
            board = Board()
            board.loadFromFen(fen)
            moves.clear()
            currentMoveIndex = 0
            updateState()
            boardState++
            true
        } catch (_: Exception) {
            false
        }
    }

    fun loadPreviewFen(fen: String) {
        startSquare = null
        previewFen = fen
        previewPosition = ChesslibMapper.fromFen(fen)
        boardState++
    }

    /** Limits user interaction to a single expected UCI move. Pass null to remove the restriction. */
    fun setAllowedMoveUci(uci: String?) {
        allowedMoveUci = uci?.lowercase()
        startSquare = null
    }

    fun setUserMovesEnabled(enabled: Boolean) {
        userMovesEnabled = enabled
        if (!enabled) {
            startSquare = null
        }
    }

    /**
     * Loads a game from UCI move strings and places the board at [targetPly].
     * All moves are stored so undo/redo still works across the full game.
     */
    fun loadFromUciMoves(uciMoves: List<String>, targetPly: Int = uciMoves.size, startFen: String? = null) {
        clearPreviewState()
        startSquare = null
        // Parse all UCI strings into Move objects using a temp board
        val allMoves = mutableListOf<Move>()
        val tempBoard = if (startFen != null) Board().also { it.loadFromFen(startFen) } else Board()
        for (uci in uciMoves) {
            val from = uci.take(2)
            val to   = uci.drop(2).take(2)
            try {
                val move = Move(Square.fromValue(from.uppercase()), Square.fromValue(to.uppercase()))
                if (tempBoard.legalMoves().contains(move)) {
                    tempBoard.doMove(move)
                    allMoves.add(move)
                }
            } catch (_: Exception) {}
        }
        val limit = targetPly.coerceIn(0, allMoves.size)
        // Reset and replay board to targetPly
        board = if (startFen != null) Board().also { it.loadFromFen(startFen) } else Board()
        moves.clear()
        currentMoveIndex = 0
        for (i in 0 until limit) {
            board.doMove(allMoves[i])
            currentMoveIndex++
        }
        // Keep all moves so redo works beyond targetPly
        moves.addAll(allMoves)
        updateState()
        boardState++
    }

    // const function
    fun getMovesCopy(): List<Move> {
        return moves.toList()
    }

    fun getLastMoveHighlight(): LastMoveHighlight? {
        if (currentMoveIndex == 0) {
            return null
        }

        val move = moves.getOrNull(currentMoveIndex - 1) ?: return null
        return LastMoveHighlight(
            from = move.from.value().lowercase(),
            to = move.to.value().lowercase()
        )
    }

    // const function
    fun getSide() : BoardOrientation {
        return this.side
    }

    fun setOrientation(orientation: BoardOrientation) {
        if (side == orientation) {
            return
        }

        side = orientation
        startSquare = null
        boardState++
    }

    // const function
    fun getFen(): String {
        return previewFen ?: board.fen
    }

    fun generatePgn(
        whiteName: String = "White",
        blackName: String = "Black",
        event: String = "Casual Game",
        upToIndex: Int = -1
    ): String {
        val sb = StringBuilder()

        // Headers
        sb.append("[Event \"$event\"]\n")
        sb.append("[White \"$whiteName\"]\n")
        sb.append("[Black \"$blackName\"]\n")
        sb.append("[Result \"*\"]\n\n")

        val count = if (upToIndex < 0) moves.size else upToIndex
        moves.take(count).forEachIndexed { index, move ->
            if (index % 2 == 0) {
                sb.append("${index / 2 + 1}. ")
            }
            sb.append("${move.from.value().lowercase()}${move.to.value().lowercase()} ")
        }

        sb.append("*")

        return sb.toString().trim()
    }

    // const function
    fun getBoardPosition() : BoardPosition {
        return previewPosition ?: ChesslibMapper.fromFen(board.fen)
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
        return moves.any { it.from == sq && isMoveAllowed(it) }
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

    private fun clearPreviewState() {
        previewFen = null
        previewPosition = null
    }

    private fun getPieceWithLegalMovesFromSquare(square: String?) : Piece? {
        if (square == null) { return null }
        if (!isSquareAllowed(square)) { return null }

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

    private fun isMoveAllowed(move: Move): Boolean {
        if (!userMovesEnabled) {
            return false
        }

        val expectedMove = allowedMoveUci ?: return true
        return moveToUci(move) == expectedMove
    }

    private fun isSquareAllowed(square: String): Boolean {
        if (!userMovesEnabled) {
            return false
        }

        val expectedMove = allowedMoveUci ?: return true
        return square.lowercase() == expectedMove.take(2)
    }

    private fun moveToUci(move: Move): String {
        val promotion = move.promotion
        val promotionSuffix = if (promotion == Piece.NONE) {
            ""
        } else {
            promotion.pieceType.name.first().lowercaseChar().toString()
        }

        return buildString {
            append(move.from.value().lowercase())
            append(move.to.value().lowercase())
            append(promotionSuffix)
        }
    }
}
