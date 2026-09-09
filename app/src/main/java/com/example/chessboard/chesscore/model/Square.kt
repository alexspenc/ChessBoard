package com.example.chessboard.chesscore.model

/**
 * Defines the chesscore Square type.
 * Validation date: 2026-09-08.
 */

data class Square(
    val file: Char,
    val rank: Int,
) {
    init {
        require(file in 'a'..'h')
        require(rank in 1..8)
    }
}
