package com.example.chessboard.ui.boardrender

/**
 * Bridges application render scenes to chessboardui while callers still use the application models.
 * Keep type conversion and application highlight styles here.
 * Do not add drawing, gesture handling, animation calculation, or chess-rule validation.
 * Validation date: 2026-10-13.
 */

import androidx.compose.ui.graphics.Color
import com.example.chessboard.chessboardui.model.BoardSquareBorder
import com.example.chessboard.chessboardui.model.BoardSquareDecoration
import com.example.chessboard.chesscore.model.Piece
import com.example.chessboard.chesscore.model.PieceType
import com.example.chessboard.chesscore.model.Side
import com.example.chessboard.chesscore.model.Square
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.chessboardui.model.BoardOrientation as LibraryBoardOrientation
import com.example.chessboard.chessboardui.model.BoardRenderAnimatedPiece as LibraryBoardRenderAnimatedPiece
import com.example.chessboard.chessboardui.model.BoardRenderPiece as LibraryBoardRenderPiece
import com.example.chessboard.chessboardui.model.BoardRenderScene as LibraryBoardRenderScene

/**
 * Converts an application scene to a validated chessboardui scene without modifying the input.
 * Uppercase KQRBNP letters denote white pieces; lowercase kqrbnp letters denote black pieces.
 * Squares must use exactly two characters, such as "e4", with a file in a..h and a rank in 1..8.
 * Decorations retain the application's draw order: last move from/to, selection, drag, error, hint.
 * Repeated decorated squares are preserved, including the separate selection and drag fills.
 * Piece and animation order and supplied pixel centers are preserved in either orientation.
 * Finite centers outside the board are accepted; no chess-position legality is checked.
 *
 * @param scene the application placement, orientation, highlight metadata, and moving-piece inputs.
 * @return a new library scene with chesscore pieces and squares and ordered generic decorations.
 * @throws IllegalArgumentException if a letter or square is invalid; its field and value are reported.
 * Library scene validation exceptions propagate unchanged for duplicate occupied squares, missing
 * or repeated moving references, and non-finite centers, even a drag center with no dragged piece.
 */
fun adaptBoardRenderScene(scene: BoardRenderScene): LibraryBoardRenderScene {
    fun parseSquare(value: String, field: String): Square {
        require(value.length == 2 && value[0] in 'a'..'h' && value[1] in '1'..'8') {
            "Invalid square in $field: '$value'; expected a1..h8"
        }
        return Square(value[0], value[1] - '0')
    }

    fun parsePiece(letter: Char, field: String): Piece {
        val type = when (letter) {
            'K', 'k' -> PieceType.KING
            'Q', 'q' -> PieceType.QUEEN
            'R', 'r' -> PieceType.ROOK
            'B', 'b' -> PieceType.BISHOP
            'N', 'n' -> PieceType.KNIGHT
            'P', 'p' -> PieceType.PAWN
            else -> throw IllegalArgumentException("Invalid piece letter in $field: '$letter'")
        }
        var side = Side.BLACK
        if (letter in 'A'..'Z') {
            side = Side.WHITE
        }
        return Piece(side, type)
    }

    val pieces = mutableListOf<LibraryBoardRenderPiece>()
    for ((index, piece) in scene.pieces.withIndex()) {
        pieces.add(
            LibraryBoardRenderPiece(
                piece = parsePiece(piece.letter, "pieces[$index].letter"),
                square = parseSquare(piece.square, "pieces[$index].square"),
            ),
        )
    }

    val orientation = when (scene.orientation) {
        BoardOrientation.WHITE -> LibraryBoardOrientation.WHITE
        BoardOrientation.BLACK -> LibraryBoardOrientation.BLACK
    }

    val decorations = mutableListOf<BoardSquareDecoration>()
    fun addDecoration(
        square: String?,
        field: String,
        fillColor: Color,
        border: BoardSquareBorder? = null,
    ) {
        if (square == null) {
            return
        }
        decorations.add(BoardSquareDecoration(parseSquare(square, field), fillColor, border))
    }

    val lastMove = scene.lastMoveHighlight
    if (lastMove != null) {
        addDecoration(lastMove.from, "lastMoveHighlight.from", Color(0xFFFFD600).copy(alpha = 0.55f))
        addDecoration(lastMove.to, "lastMoveHighlight.to", Color(0xFFFFFF80).copy(alpha = 0.50f))
    }
    val selectionColor = Color.Yellow.copy(alpha = 0.4f)
    addDecoration(scene.selectedSquare, "selectedSquare", selectionColor)
    addDecoration(scene.dragFromSquare, "dragFromSquare", selectionColor)
    addDecoration(scene.wrongMoveSquare, "wrongMoveSquare", Color.Red.copy(alpha = 0.45f))
    val hintColor = Color(0xFF1DB584)
    addDecoration(
        scene.hintSquare,
        "hintSquare",
        hintColor.copy(alpha = 0.22f),
        BoardSquareBorder(hintColor.copy(alpha = 0.85f), 0.07f),
    )

    var dragFromSquare: Square? = null
    val dragSource = scene.dragFromSquare
    if (dragSource != null) {
        dragFromSquare = parseSquare(dragSource, "dragFromSquare")
    }
    val animatedPieces = mutableListOf<LibraryBoardRenderAnimatedPiece>()
    for ((index, animatedPiece) in scene.animatedPieces.withIndex()) {
        animatedPieces.add(
            LibraryBoardRenderAnimatedPiece(
                fromSquare = parseSquare(animatedPiece.fromSquare, "animatedPieces[$index].fromSquare"),
                centerOffset = animatedPiece.centerOffset,
            ),
        )
    }

    return LibraryBoardRenderScene(
        pieces = pieces,
        orientation = orientation,
        decorations = decorations,
        dragFromSquare = dragFromSquare,
        dragOffset = scene.dragOffset,
        animatedPieces = animatedPieces,
    )
}
