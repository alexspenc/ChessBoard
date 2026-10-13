package com.example.chessboard.chesscore

/**
 * Defines the context of a SAN replay failure while parsing multiple PGN lines.
 * Contains a result-order line index and the original SAN error.
 * Does not contain parsing algorithms, localized app strings, UI, or persistence.
 * Validation date: 2026-10-12.
 */

/**
 * Reports a SAN error in one expanded PGN line, aborting the complete parse.
 *
 * @param lineIndex zero-based index before duplicate removal, in the result order with the main
 * line first: `0` is the main line, `1` is the first variation, and so on.
 * @param sanError original SAN replay error, also retained as [cause]. Its token, local move
 * number, side to move, and reason describe the failed move without app-specific formatting.
 * FEN loading and PGN structure errors are not wrapped in this exception.
 */
class PgnLineParseException(
    val lineIndex: Int,
    val sanError: SanLineParseException,
) : IllegalArgumentException(
    "Cannot parse PGN line: lineIndex=$lineIndex token=${sanError.token}",
    sanError,
)
