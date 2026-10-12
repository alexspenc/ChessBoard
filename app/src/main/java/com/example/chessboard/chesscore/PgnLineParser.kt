package com.example.chessboard.chesscore

/**
 * Coordinates shared PGN/SAN text parsing into UCI lines from a supplied starting position.
 * Contains FEN normalization, variation expansion, SAN replay, and technical line error context.
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
    fun createStartPosition(): Position {
        if (startFen.isNullOrBlank()) {
            return positionFactory.create()
        }

        return positionFactory.create(normalizeFenForPositionLoad(startFen))
    }

    val startPosition = createStartPosition()
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
