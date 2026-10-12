package com.example.chessboard.chesscore

/**
 * Normalizes FEN text before it is passed to a position implementation.
 * Allowed here:
 * - FEN field-count checks and whitespace normalization
 * - conversion of accepted position-only FEN text into loadable six-field FEN text
 * Not allowed here:
 * - chess position legality checks, chesslib access, Android, UI, persistence, or app strings
 * Validation date: 2026-09-09.
 */

/**
 * Describes why a FEN string could not be normalized for position loading.
 *
 * [EMPTY_FEN] means the input is blank after trimming.
 * [MISSING_FULLMOVE_NUMBER] means the input has five fields: halfmove clock is present, but
 * fullmove number is missing.
 * [UNSUPPORTED_FIELD_COUNT] means the input has any field count other than four, five, or six.
 */
enum class FenParseErrorReason {
    EMPTY_FEN,
    MISSING_FULLMOVE_NUMBER,
    UNSUPPORTED_FIELD_COUNT,
}

/**
 * Reports a FEN shape error found before a position implementation validates chess legality.
 *
 * @param fen original FEN input supplied by the caller.
 * @param fieldCount number of whitespace-separated fields after trimming, or `0` for blank input.
 * @param reason technical reason describing why the input shape is unsupported.
 */
class FenParseException(
    val fen: String,
    val fieldCount: Int,
    val reason: FenParseErrorReason,
) : IllegalArgumentException(buildFenParseErrorMessage(fen, fieldCount, reason))

/**
 * Normalizes a FEN string into the six-field form expected by position loaders.
 *
 * @param fen FEN text. Accepted examples:
 * `8/8/8/8/8/8/4K3/6k1 b - -` with four fields, or
 * `8/8/8/8/8/8/4K3/6k1 b - - 0 23` with six fields.
 * Five-field input such as `8/8/8/8/8/8/4K3/6k1 b - - 0` is rejected because it
 * has no fullmove number.
 * @return a six-field FEN string with normalized single spaces. Four-field input is completed
 * with `0 1`; six-field input keeps its supplied counters.
 * @throws FenParseException when [fen] is blank or has an unsupported number of fields.
 */
fun normalizeFenForPositionLoad(fen: String): String {
    fun unsupportedFieldCountReason(fieldCount: Int): FenParseErrorReason {
        if (fieldCount == 5) {
            return FenParseErrorReason.MISSING_FULLMOVE_NUMBER
        }

        return FenParseErrorReason.UNSUPPORTED_FIELD_COUNT
    }

    val trimmedFen = fen.trim()
    if (trimmedFen.isEmpty()) {
        throw FenParseException(
            fen = fen,
            fieldCount = 0,
            reason = FenParseErrorReason.EMPTY_FEN,
        )
    }

    val fields = trimmedFen.split(Regex("\\s+"))
    if (fields.size == 4) {
        return "${fields.joinToString(separator = " ")} 0 1"
    }

    if (fields.size == 6) {
        return fields.joinToString(separator = " ")
    }

    throw FenParseException(
        fen = fen,
        fieldCount = fields.size,
        reason = unsupportedFieldCountReason(fields.size),
    )
}

private fun buildFenParseErrorMessage(
    fen: String,
    fieldCount: Int,
    reason: FenParseErrorReason,
): String {
    return "Cannot normalize FEN for position load: reason=$reason fieldCount=$fieldCount fen=$fen"
}
