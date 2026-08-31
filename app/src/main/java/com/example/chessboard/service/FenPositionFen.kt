package com.example.chessboard.service

/*
 * File role: validates and canonicalizes FEN values used by the FEN positions feature.
 * Allowed here:
 * - accepting four-field position FEN or standard six-field FEN input
 * - returning the canonical four-field position identity
 * Not allowed here:
 * - persistence, UI messages, or move-sequence handling
 * Validation date: 2026-08-31
 */

import com.github.bhlangonijr.chesslib.Board

private const val PositionFenFieldCount = 4
private const val FullFenFieldCount = 6

fun normalizeValidFenPosition(fen: String): String? {
    val fields = fen.trim().split(Regex("\\s+")).filter(String::isNotEmpty)
    if (fields.size != PositionFenFieldCount && fields.size != FullFenFieldCount) {
        return null
    }

    val normalizedFen = fields.take(PositionFenFieldCount).joinToString(separator = " ")
    return try {
        Board().loadFromFen("$normalizedFen 0 1")
        normalizedFen
    } catch (_: Exception) {
        null
    }
}
