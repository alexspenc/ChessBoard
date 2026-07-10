package com.example.chessboard.ui.boardrender

/**
 * Maps boardmodel snapshots into board-render scenes.
 * Keep only lightweight translation from board state to UI render inputs here.
 * Do not add Compose drawing code, animation queue logic, or controller mutations to this file.
 * Validation date: 2026-07-10
 */

import androidx.compose.ui.geometry.Offset
import com.example.chessboard.boardmodel.BoardPosition
import com.example.chessboard.boardmodel.LastMoveHighlight
import com.example.chessboard.ui.BoardOrientation

fun buildBoardRenderScene(
    position: BoardPosition,
    orientation: BoardOrientation,
    lastMoveHighlight: LastMoveHighlight? = null,
    selectedSquare: String? = null,
    dragFromSquare: String? = null,
    dragOffset: Offset = Offset.Zero,
    wrongMoveSquare: String? = null,
    hintSquare: String? = null,
): BoardRenderScene {
    return BoardRenderScene(
        pieces = mapBoardPositionToRenderPieces(position),
        orientation = orientation,
        lastMoveHighlight = lastMoveHighlight,
        selectedSquare = selectedSquare,
        dragFromSquare = dragFromSquare,
        dragOffset = dragOffset,
        wrongMoveSquare = wrongMoveSquare,
        hintSquare = hintSquare,
    )
}

private fun mapBoardPositionToRenderPieces(position: BoardPosition): List<BoardRenderPiece> {
    return position.pieces.map { piece ->
        BoardRenderPiece(
            letter = piece.letter,
            square = piece.field,
        )
    }
}
