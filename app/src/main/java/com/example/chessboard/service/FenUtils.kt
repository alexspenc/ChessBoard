package com.example.chessboard.service

fun normalizeFenWithoutMoveNumbers(fen: String): String {
    val fenParts = fen.trim().split(Regex("\\s+"))
    if (fenParts.size <= 4) {
        return fen.trim()
    }

    return fenParts.take(4).joinToString(separator = " ")
}

fun calculateFenHashWithoutMoveNumbers(fen: String): Long {
    return normalizeFenWithoutMoveNumbers(fen).hashCode().toLong()
}
