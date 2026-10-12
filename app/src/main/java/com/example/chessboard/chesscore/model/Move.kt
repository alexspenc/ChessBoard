package com.example.chessboard.chesscore.model

/**
 * Defines the chesscore Move type.
 * Validation date: 2026-09-08.
 */

data class Move(
    val from: Square,
    val to: Square,
    val promotion: PromotionPiece? = null,
)
