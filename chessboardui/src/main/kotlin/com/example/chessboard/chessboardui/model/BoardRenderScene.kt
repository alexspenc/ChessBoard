package com.example.chessboard.chessboardui.model

/**
 * Owns validated inputs for one board-rendering scene.
 * Keep collection ownership and drawing-input validation here.
 * Do not add chess legality, gesture handling, playback queues, or application models.
 * Validation date: 2026-10-13.
 */

import androidx.compose.ui.geometry.Offset
import com.example.chessboard.chesscore.model.Square
import java.util.Collections

/**
 * Stores a board placement and transient drawing inputs independently from caller-owned lists.
 * All three lists are copied and exposed as unmodifiable lists, preserving their input order.
 * Empty boards and arbitrary placements are accepted without chess-legality checks.
 * Coordinates use pixels from the board's top left, with X rightward and Y downward.
 *
 * @param pieces the pieces occupying distinct squares.
 * @param orientation the viewing orientation, independent from the side to move.
 * @param decorations the ordered square fills and borders, including decorations of empty squares.
 * @param dragFromSquare the square identifying the dragged piece, or null when none is dragged.
 * @param dragOffset the dragged piece's center; must be finite even when dragFromSquare is null.
 * @param animatedPieces the animated piece references and their finite current centers.
 * Moving centers may lie outside the board.
 * @throws IllegalArgumentException if pieces share a square, a moving reference has no piece,
 * a piece is referenced as moving twice, or a moving-center coordinate is non-finite.
 */
class BoardRenderScene(
    pieces: List<BoardRenderPiece>,
    val orientation: BoardOrientation,
    decorations: List<BoardSquareDecoration> = emptyList(),
    val dragFromSquare: Square? = null,
    val dragOffset: Offset = Offset.Zero,
    animatedPieces: List<BoardRenderAnimatedPiece> = emptyList(),
) {
    /** The scene's own ordered, unmodifiable copy of the piece placement. */
    val pieces: List<BoardRenderPiece> = Collections.unmodifiableList(pieces.toList())

    /** The scene's own ordered, unmodifiable copy of square decorations. */
    val decorations: List<BoardSquareDecoration> = Collections.unmodifiableList(decorations.toList())

    /** The scene's own ordered, unmodifiable copy of animated piece inputs. */
    val animatedPieces: List<BoardRenderAnimatedPiece> = Collections.unmodifiableList(animatedPieces.toList())

    init {
        val occupiedSquares = mutableSetOf<Square>()
        for (placedPiece in this.pieces) {
            val square = placedPiece.square
            require(occupiedSquares.add(square)) {
                "Multiple pieces on square ${square.file}${square.rank}"
            }
        }

        val movingSquares = mutableSetOf<Square>()

        fun validateMovingSquare(square: Square) {
            require(square in occupiedSquares) {
                "No piece on moving square ${square.file}${square.rank}"
            }
            require(movingSquares.add(square)) {
                "Multiple moving references to square ${square.file}${square.rank}"
            }
        }

        fun validateCenter(offset: Offset, name: String) {
            require(offset.x.isFinite() && offset.y.isFinite()) {
                "$name must have finite coordinates: $offset"
            }
        }

        validateCenter(dragOffset, "dragOffset")
        if (dragFromSquare != null) {
            validateMovingSquare(dragFromSquare)
        }

        for (animatedPiece in this.animatedPieces) {
            validateMovingSquare(animatedPiece.fromSquare)
            validateCenter(animatedPiece.centerOffset, "animated centerOffset")
        }
    }
}
