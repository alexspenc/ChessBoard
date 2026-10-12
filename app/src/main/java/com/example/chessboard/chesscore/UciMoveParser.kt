package com.example.chessboard.chesscore

/**
 * Parses UCI move tokens without depending on chess position state.
 * Allowed here:
 * - UCI token recognition and extraction from plain text
 * - conversion from a syntactic UCI token into chesscore Move values
 * Not allowed here:
 * - legal move validation, chesslib access, app stored-PGN headers, UI, persistence, or app strings
 * Validation date: 2026-09-09.
 */

import com.example.chessboard.chesscore.model.Move
import com.example.chessboard.chesscore.model.PromotionPiece
import com.example.chessboard.chesscore.model.Square

/**
 * Describes why a UCI move token could not be parsed.
 *
 * [INVALID_FORMAT] means the token is not shaped like long algebraic UCI notation such as
 * `"e2e4"` or `"e7e8q"`.
 */
enum class UciMoveParseErrorReason {
    INVALID_FORMAT,
}

/**
 * Reports a syntactic UCI token parsing failure.
 *
 * @param token original token supplied by the caller.
 * @param reason technical reason describing why the token cannot be parsed as a UCI move.
 */
class UciMoveParseException(
    val token: String,
    val reason: UciMoveParseErrorReason,
) : IllegalArgumentException(buildUciMoveParseErrorMessage(token, reason))

/**
 * Checks whether a token is shaped like a UCI move.
 *
 * @param token candidate token, for example `"e2e4"` or `"e7e8q"`.
 * @return `true` when [token] is a four-square UCI move with an optional promotion suffix
 * `q`, `r`, `b`, or `n`; uppercase promotion suffixes are also accepted.
 */
fun isUciMoveToken(token: String): Boolean {
    return token.trim().matches(uciMoveRegex)
}

/**
 * Parses one syntactic UCI move token into a chesscore [Move].
 *
 * @param token UCI move token, for example `"e2e4"` or `"e7e8q"`.
 * @return [Move] with source square, target square, and optional promotion piece.
 * @throws UciMoveParseException when [token] is not a syntactic UCI move token.
 */
fun parseUciMove(token: String): Move {
    val trimmedToken = token.trim()
    if (!isUciMoveToken(trimmedToken)) {
        throw UciMoveParseException(
            token = token,
            reason = UciMoveParseErrorReason.INVALID_FORMAT,
        )
    }

    return Move(
        from = parseUciSquare(trimmedToken.substring(0, 2)),
        to = parseUciSquare(trimmedToken.substring(2, 4)),
        promotion = trimmedToken.getOrNull(4)?.let(::parsePromotionPiece),
    )
}

/**
 * Extracts syntactic UCI move tokens from text.
 *
 * @param text text that may contain PGN-style headers, move numbers, results, SAN tokens, and UCI
 * tokens, for example `"[Event \"Test\"]\n\n1. e2e4 e7e5 2. g1f3 *"`.
 * @return UCI move tokens in source order, for example `listOf("e2e4", "e7e5", "g1f3")`.
 */
fun extractUciMoveTokens(text: String): List<String> {
    return text.lines()
        .filterNot { line -> line.trim().startsWith("[") }
        .joinToString(" ")
        .split(Regex("\\s+"))
        .filter(::isUciMoveToken)
}

private val uciMoveRegex = Regex("[a-h][1-8][a-h][1-8][qrbnQRBN]?")

private fun parseUciSquare(token: String): Square {
    return Square(
        file = token[0],
        rank = token[1].digitToInt(),
    )
}

private fun parsePromotionPiece(token: Char): PromotionPiece {
    return when (token.lowercaseChar()) {
        'q' -> PromotionPiece.QUEEN
        'r' -> PromotionPiece.ROOK
        'b' -> PromotionPiece.BISHOP
        'n' -> PromotionPiece.KNIGHT
        else -> error("Unsupported UCI promotion piece: $token")
    }
}

private fun buildUciMoveParseErrorMessage(
    token: String,
    reason: UciMoveParseErrorReason,
): String {
    return "Cannot parse UCI move token: reason=$reason token=$token"
}
