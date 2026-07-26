package com.example.chessboard.ui.boardrender

/**
 * Render-scene models for the chess board UI.
 * Keep immutable board drawing inputs here so different board hosts can share one renderer.
 * Do not add gesture handling, queue orchestration, or chess-rule mutations to this file.
 * Validation date: 2026-07-10
 */

import androidx.compose.ui.geometry.Offset
import com.example.chessboard.boardmodel.LastMoveHighlight
import com.example.chessboard.ui.BoardOrientation

data class BoardRenderPiece(
    val letter: Char,
    val square: String,
)

data class BoardRenderAnimatedPiece(
    val fromSquare: String,
    val centerOffset: Offset,
)

data class BoardRenderScene(
    val pieces: List<BoardRenderPiece>,
    val orientation: BoardOrientation,
    val lastMoveHighlight: LastMoveHighlight? = null,
    val selectedSquare: String? = null,
    val dragFromSquare: String? = null,
    val dragOffset: Offset = Offset.Zero,
    val animatedPieces: List<BoardRenderAnimatedPiece> = emptyList(),
    // TODO: Replace wrongMoveSquare and hintSquare with an ordered
    // List<BoardSquareDecoration>. Define generic decoration styles in boardrender,
    // update BoardRenderMapper and BoardSceneRenderer to map and draw that list,
    // and keep the conversion from TrainSingleLine state to decorations inside the
    // TrainSingleLine screen package. Preserve the current draw order when both
    // decorations target the same square.
    val wrongMoveSquare: String? = null,
    val hintSquare: String? = null,
)
