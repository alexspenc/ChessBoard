package com.example.chessboard.chesscore

/**
 * Parses PGN move text into reusable, chesscore-level text tokens.
 * Allowed here:
 * - PGN move-text tokenization that does not need a chess position
 * - extraction of SAN token lists from already supplied PGN text
 * - small PGN token classifiers used by higher-level parsers
 * Not allowed here:
 * - SAN move resolution, move replay, variation-start inference, app strings, UI, persistence
 * Validation date: 2026-09-09.
 */

import com.example.chessboard.chesscore.model.Side

private val pgnTokenRegex = Regex("""\(|\)|\d+\.(?:\.\.)?|1-0|0-1|1/2-1/2|\*|\$\d+|[^\s()]+""")
private val pgnMoveNumberTokenRegex = Regex("""\d+\.?(?:\.\.)?""")

/**
 * PGN move number token resolved to a concrete side.
 *
 * @param number PGN fullmove number parsed from a token such as `"1."` or `"23..."`.
 * @param side side indicated by the token; `"1."` means White and `"1..."` means Black.
 */
data class PgnMoveNumber(
    val number: Int,
    val side: Side,
)

/**
 * Splits the move-text part of a PGN record into lexical tokens without resolving SAN.
 *
 * @param pgnText PGN text that may include headers, comments, NAG tokens, variations, and a result,
 * for example `"[Event \"Test\"]\n\n1. e4 {comment} e5 (1... c5) 2. Nf3 *"`.
 * @return PGN move-text tokens in source order, for example
 * `listOf("1.", "e4", "e5", "(", "1...", "c5", ")", "2.", "Nf3", "*")`.
 */
fun tokenizePgnMoveText(pgnText: String): List<String> {
    val withoutComments = pgnText.removePrefix("\uFEFF")
        .replace(Regex("\\{[^}]*\\}"), " ")
        .replace(Regex(";[^\\n]*"), " ")

    val movesText = withoutComments.lines()
        .filterNot { it.trim().startsWith("[") }
        .joinToString(" ")

    return pgnTokenRegex
        .findAll(movesText)
        .map { it.value.trim() }
        .filter { it.isNotBlank() }
        .toList()
}

/**
 * Extracts only the main SAN token line from already tokenized PGN move text.
 *
 * @param pgnTokens tokens returned by [tokenizePgnMoveText], for example
 * `listOf("1.", "e4", "e5", "(", "1...", "c5", ")", "2.", "Nf3", "*")`.
 * @return SAN tokens from variation depth zero, without PGN move numbers, NAG tokens, or results,
 * for example `listOf("e4", "e5", "Nf3")`.
 */
fun extractMainSanTokens(pgnTokens: List<String>): List<String> {
    val mainLine = mutableListOf<String>()
    var variationDepth = 0

    pgnTokens.forEach { token ->
        when {
            token == "(" -> variationDepth++
            token == ")" -> {
                if (variationDepth > 0) {
                    variationDepth--
                }
            }
            variationDepth > 0 -> Unit
            token.startsWith("$") || isPgnMoveNumberToken(token) || isPgnResultToken(token) -> Unit
            else -> mainLine.add(token)
        }
    }

    return mainLine
}

/**
 * Parses a PGN move-number token into fullmove number and side.
 *
 * @param token PGN move-number token, for example `"1."` for White or `"23..."` for Black.
 * Plain numbers such as `"23"` are recognized as move-number-like tokens by
 * [isPgnMoveNumberToken], but return `null` here because they do not specify a side.
 * @return parsed move number and side, or `null` when [token] is not a side-specific PGN
 * move-number token.
 */
fun parsePgnMoveNumber(token: String): PgnMoveNumber? {
    if (token.matches(Regex("""\d+\."""))) {
        val moveNumber = token.dropLast(1).toIntOrNull() ?: return null
        return PgnMoveNumber(number = moveNumber, side = Side.WHITE)
    }

    if (token.matches(Regex("""\d+\.\.\."""))) {
        val moveNumber = token.dropLast(3).toIntOrNull() ?: return null
        return PgnMoveNumber(number = moveNumber, side = Side.BLACK)
    }

    return null
}

/**
 * Checks whether a token is syntactically shaped like a PGN move number.
 *
 * @param token token from PGN move text, for example `"1"`, `"1."`, or `"1..."`.
 * @return `true` when callers should treat [token] as PGN move numbering rather than SAN.
 */
fun isPgnMoveNumberToken(token: String): Boolean {
    return token.matches(pgnMoveNumberTokenRegex)
}

/**
 * Checks whether a token is a PGN game result marker.
 *
 * @param token token from PGN move text, for example `"*"`, `"1-0"`, `"0-1"`, or `"1/2-1/2"`.
 * @return `true` when [token] is one of the PGN result markers.
 */
fun isPgnResultToken(token: String): Boolean {
    return token == "*" || token == "1-0" || token == "0-1" || token == "1/2-1/2"
}
