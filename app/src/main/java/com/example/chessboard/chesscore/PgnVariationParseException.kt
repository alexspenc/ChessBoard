package com.example.chessboard.chesscore

/**
 * Defines technical errors for PGN variation structure parsing.
 * Contains error reasons and token context, without localized app strings or position access.
 * Variation expansion remains in the application until its separate extraction step.
 * Validation date: 2026-10-12.
 */

/**
 * Describes a rejected PGN variation structure.
 *
 * [MISSING_PRECEDING_MOVE] means a variation opens before its containing line has an own move.
 * [EMPTY_VARIATION] means a variation closes without any own moves; inherited moves do not count.
 * [NAG_BEFORE_FIRST_MOVE] means a NAG occurs before the first own move in a variation.
 */
enum class PgnVariationParseErrorReason {
    MISSING_PRECEDING_MOVE,
    EMPTY_VARIATION,
    NAG_BEFORE_FIRST_MOVE,
}

/**
 * Reports a structural PGN variation error without depending on app error formatting.
 *
 * @param token token at which the error was detected: `"("`, `")"`, or a NAG such as `"$1"`.
 * @param tokenIndex zero-based index in the list returned by [tokenizePgnMoveText], not a character
 * offset in the original PGN text. Removed headers and comments are not counted.
 * @param reason technical reason for rejecting the variation structure.
 */
class PgnVariationParseException(
    val token: String,
    val tokenIndex: Int,
    val reason: PgnVariationParseErrorReason,
) : IllegalArgumentException(buildPgnVariationParseErrorMessage(reason))

private fun buildPgnVariationParseErrorMessage(reason: PgnVariationParseErrorReason): String {
    return when (reason) {
        PgnVariationParseErrorReason.MISSING_PRECEDING_MOVE ->
            "A PGN variation must follow a move in its containing line"
        PgnVariationParseErrorReason.EMPTY_VARIATION ->
            "A PGN variation must contain a move of its own"
        PgnVariationParseErrorReason.NAG_BEFORE_FIRST_MOVE ->
            "A PGN variation NAG must follow a move in that variation"
    }
}
