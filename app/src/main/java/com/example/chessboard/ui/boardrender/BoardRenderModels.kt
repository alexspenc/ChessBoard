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
    val wrongMoveSquare: String? = null,
    val hintSquare: String? = null,
)
