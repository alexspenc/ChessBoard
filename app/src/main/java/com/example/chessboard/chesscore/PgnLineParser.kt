package com.example.chessboard.chesscore

/**
 * Coordinates shared PGN/SAN text parsing into UCI lines from a supplied starting position.
 * Contains FEN normalization, main-line extraction, variation expansion, SAN replay, and line errors.
 * Does not contain duplicate filtering, app strings, concrete adapters, UI, or persistence.
 * Validation date: 2026-10-12.
 */

/**
 * Parses PGN/SAN text with variations into complete UCI lines.
 *
 * @param positionFactory creates independent positions for start-position resolution and line replay.
 * @param pgnText PGN/SAN text, for example `"1. e4 e5 (1... c5) 2. Nf3"`.
 * @param startFen four- or six-field FEN. Four fields are completed with `0 1`; six-field counters
 * are preserved. Null or blank input uses the standard position, matching the existing app import.
 * @return main line first, followed by variations in reverse variation-closing order. Equal lines
 * are preserved. The example returns `listOf(listOf("e2e4", "e7e5", "g1f3"), listOf("e2e4", "c7c5"))`.
 * Empty move text returns an empty list after the starting position has been created.
 * @throws FenParseException when a nonblank FEN has an unsupported field count.
 * @throws PgnVariationParseException when a variation has invalid structure.
 * @throws PgnLineParseException when SAN replay fails in any line; no partial result is returned.
 * @throws IllegalArgumentException when [positionFactory] rejects a position. Factory errors are
 * propagated without line error wrapping, including errors during replay position creation.
 */
fun parsePgnToUciLines(
    positionFactory: PositionFactory,
    pgnText: String,
    startFen: String? = null,
): List<List<String>> {
    val startPosition = createParserStartPosition(positionFactory, startFen)
    val replayFen = startPosition.getFen()
    val sanLines = extractSanLines(
        pgnTokens = tokenizePgnMoveText(pgnText),
        startingSide = startPosition.getSideToMove(),
    ).reversed()
    val uciLines = mutableListOf<List<String>>()

    for (lineIndex in sanLines.indices) {
        try {
            val uciLine = parseSanLineToUci(
                positionFactory = positionFactory,
                sanTokens = sanLines[lineIndex],
                startFen = replayFen,
            )
            uciLines.add(uciLine)
        } catch (error: SanLineParseException) {
            throw PgnLineParseException(lineIndex = lineIndex, sanError = error)
        }
    }

    return uciLines
}

/**
 * Parses only the main SAN line of PGN text into UCI moves, without replaying variations.
 *
 * @param positionFactory creates the starting position and the position used for main-line replay.
 * @param pgnText PGN/SAN text, for example `"1. e4 e5 (1... Qa5) 2. Nf3"`. Illegal moves inside
 * variations are ignored; variation structure is not validated by this entry point.
 * @param startFen four- or six-field FEN. Four fields are completed with `0 1`; six-field counters
 * are preserved. Null or blank input uses the standard position.
 * @return main-line UCI moves in order, for example `listOf("e2e4", "e7e5", "g1f3")`.
 * An empty main line returns an empty list after the starting position has been created.
 * @throws FenParseException when a nonblank FEN has an unsupported field count.
 * @throws SanLineParseException when a main-line move cannot be recognized or applied. The error
 * is propagated without a PGN line wrapper; its move number is local to the main line.
 * @throws IllegalArgumentException when [positionFactory] rejects a position. Factory errors
 * are propagated without wrapping.
 */
fun parsePgnMainLineToUci(
    positionFactory: PositionFactory,
    pgnText: String,
    startFen: String? = null,
): List<String> {
    val startPosition = createParserStartPosition(positionFactory, startFen)
    val sanTokens = extractMainSanTokens(tokenizePgnMoveText(pgnText))
    if (sanTokens.isEmpty()) {
        return emptyList()
    }

    return parseSanLineToUci(
        positionFactory = positionFactory,
        sanTokens = sanTokens,
        startFen = startPosition.getFen(),
    )
}

private fun createParserStartPosition(
    positionFactory: PositionFactory,
    startFen: String?,
): Position {
    if (startFen.isNullOrBlank()) {
        return positionFactory.create()
    }

    return positionFactory.create(normalizeFenForPositionLoad(startFen))
}
