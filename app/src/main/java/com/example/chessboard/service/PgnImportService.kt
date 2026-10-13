package com.example.chessboard.service

/**
 * File role: imports and normalizes PGN or stored UCI-like move text.
 * Allowed here:
 * - app-facing PGN/SAN import orchestration and conversion into UCI lines
 * - shared PGN-to-UCI parser calls, duplicate filtering, and app error formatting
 * - stored PGN construction and extraction of persisted UCI moves
 * Full PGN line parsing and main-line-only parsing are delegated to chesscore.
 * Not allowed here:
 * - Compose UI, screen navigation, Room DAO definitions, or board-controller state
 * Validation date: 2026-10-12
 */

import com.example.chessboard.boardmodel.buildChesslibMoveFromUci
import com.example.chessboard.chesscore.PositionFactory
import com.example.chessboard.chesscore.PgnLineParseException
import com.example.chessboard.chesscore.PgnVariationParseException
import com.example.chessboard.chesscore.SanLineParseErrorReason
import com.example.chessboard.chesscore.SanLineParseException
import com.example.chessboard.chesscore.extractUciMoveTokens
import com.example.chessboard.chesscore.model.Side
import com.example.chessboard.chesscore.parsePgnToUciLines as parseCorePgnToUciLines
import com.example.chessboard.chesscore.parsePgnMainLineToUci as parseCorePgnMainLineToUci
import com.example.chessboard.entity.LineEntity
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.move.Move

data class PgnParseErrorStrings(
    val mainLine: String,
    val variation: String,
    val whiteSide: String,
    val blackSide: String,
    val lineParseFailed: String,
    val unrecognizedNotation: String,
    val illegalMove: String,
)

private val DefaultPgnParseErrorStrings = PgnParseErrorStrings(
    mainLine = "main line",
    variation = "variation %1\$d",
    whiteSide = "White",
    blackSide = "Black",
    lineParseFailed = "%1\$s in the %2\$s",
    unrecognizedNotation = "Can't play %1\$s (move %2\$d, %3\$s): unrecognized notation",
    illegalMove = "Can't play %1\$s (move %2\$d, %3\$s): illegal move",
)

data class PgnRecord(
    val sourceIndex: Int,
    val text: String,
    val headers: Map<String, String>,
)

data class ParsedPgnGame(
    val sourceIndex: Int,
    val headers: Map<String, String>,
    val mainLineMoves: List<String>,
)

/**
 * Splits a PGN text that contains one or more lines/chapters into individual PGN strings.
 * A new chapter is detected by a fresh [Event ...] header block.
 * Returns a list with one entry per chapter; single-line files return a list of size 1.
 */
fun splitPgnChapters(pgnText: String): List<String> {
    return splitPgnRecords(pgnText).map { record -> record.text }
}

/**
 * Splits a PGN text into separate records and extracts headers for each record.
 * A new record is detected by a fresh [Event ...] header block.
 */
fun splitPgnRecords(pgnText: String): List<PgnRecord> {
    return pgnText
        .split(Regex("(?=\\[Event\\s)"))
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .mapIndexed { index, recordText ->
            PgnRecord(
                sourceIndex = index,
                text = recordText,
                headers = extractPgnHeaders(recordText),
            )
        }
}

/** Extracts PGN header tag values (e.g. "Event", "ECO") keyed by tag name. */
fun extractPgnHeaders(pgnText: String): Map<String, String> {
    val headerRegex = Regex("""\[(\w+)\s+"([^"]*)"\]""")
    return headerRegex.findAll(pgnText).associate { it.groupValues[1] to it.groupValues[2] }
}

/**
 * Parses only the main line of a standard PGN string (SAN notation) into UCI move strings.
 * Variations are ignored because callers use this for one concrete game, not opening branches.
 */
fun parsePgnToUci(positionFactory: PositionFactory, pgnText: String): List<String> {
    return parsePgnMainLineToUci(positionFactory, pgnText)
}

/** Parses each PGN record in [pgnText] as one game and returns only its main line moves. */
fun parsePgnGamesMainLines(positionFactory: PositionFactory, pgnText: String): List<ParsedPgnGame> {
    return parsePgnGamesMainLines(
        positionFactory = positionFactory,
        pgnText = pgnText,
        errorStrings = DefaultPgnParseErrorStrings,
    )
}

/** Parses each PGN record in [pgnText] as one game and returns only its main line moves. */
fun parsePgnGamesMainLines(
    positionFactory: PositionFactory,
    pgnText: String,
    errorStrings: PgnParseErrorStrings,
): List<ParsedPgnGame> {
    return splitPgnRecords(pgnText).mapNotNull { record ->
        val mainLineMoves = parsePgnMainLineToUci(
            positionFactory = positionFactory,
            pgnText = record.text,
            errorStrings = errorStrings,
        )
        if (mainLineMoves.isEmpty()) {
            return@mapNotNull null
        }

        ParsedPgnGame(
            sourceIndex = record.sourceIndex,
            headers = record.headers,
            mainLineMoves = mainLineMoves,
        )
    }
}

/**
 * Parses only the main line of one PGN record from the standard position, ignoring variations.
 *
 * @param positionFactory creates the start and replay positions, including start validation for empty input.
 * @param pgnText PGN/SAN text, for example `"1. e4 e5 (1... c5) 2. Nf3"`.
 * @return main-line UCI moves, or an empty list when no main-line moves are present.
 * @throws IllegalArgumentException for a position or SAN error, formatted for the application.
 */
fun parsePgnMainLineToUci(positionFactory: PositionFactory, pgnText: String): List<String> {
    return parsePgnMainLineToUci(
        positionFactory = positionFactory,
        pgnText = pgnText,
        errorStrings = DefaultPgnParseErrorStrings,
    )
}

/**
 * Parses only the main line of one PGN record from the standard position, ignoring variations.
 *
 * @param positionFactory creates the start and replay positions, including start validation for empty input.
 * @param pgnText PGN/SAN text, for example `"1. e4 e5 (1... c5) 2. Nf3"`.
 * @param errorStrings templates for app-facing move errors and the main-line label.
 * @return main-line UCI moves, or an empty list when no main-line moves are present.
 * @throws IllegalArgumentException for a position or SAN error, formatted for the application.
 */
fun parsePgnMainLineToUci(
    positionFactory: PositionFactory,
    pgnText: String,
    errorStrings: PgnParseErrorStrings,
): List<String> {
    try {
        return parseCorePgnMainLineToUci(
            positionFactory = positionFactory,
            pgnText = pgnText,
        )
    } catch (error: SanLineParseException) {
        val message = errorStrings.lineParseFailed.format(
            formatSanLineParseError(error, errorStrings),
            errorStrings.mainLine,
        )
        throw IllegalArgumentException(message, error)
    } catch (e: IllegalArgumentException) {
        val message = errorStrings.lineParseFailed.format(
            e.message.orEmpty(),
            errorStrings.mainLine,
        )
        throw IllegalArgumentException(message, e)
    }
}

/**
 * Parses the PGN into all unique playable lines, including nested variations.
 *
 * Unnumbered variations replace the preceding move; numbered variations use their explicit position.
 * @param positionFactory creates positions used to validate and replay each line.
 * @param pgnText PGN move text, for example `"1. e4 e5 (c5) 2. Nf3"`.
 * @return main line first, followed by unique variation lines in UCI notation.
 * @throws PgnVariationParseException for invalid variation structure, with token index and reason.
 * @throws IllegalArgumentException for illegal moves or position creation errors.
 */
fun parsePgnToUciLines(positionFactory: PositionFactory, pgnText: String): List<List<String>> {
    return parsePgnToUciLines(
        positionFactory = positionFactory,
        pgnText = pgnText,
        errorStrings = DefaultPgnParseErrorStrings,
    )
}

/**
 * Parses the PGN into all unique playable lines, including nested variations.
 *
 * Unnumbered variations replace the preceding move; numbered variations use their explicit position.
 * @param positionFactory creates positions used to validate and replay each line.
 * @param pgnText PGN move text, for example `"1. e4 e5 (c5) 2. Nf3"`.
 * @param errorStrings templates for app-facing SAN replay errors.
 * @return main line first, followed by unique variation lines in UCI notation.
 * @throws PgnVariationParseException for invalid variation structure, with token index and reason.
 * @throws IllegalArgumentException for illegal moves or position creation errors.
 */
fun parsePgnToUciLines(
    positionFactory: PositionFactory,
    pgnText: String,
    errorStrings: PgnParseErrorStrings,
): List<List<String>> {
    return parsePgnToUciLinesFromStart(
        positionFactory = positionFactory,
        pgnText = pgnText,
        startFen = null,
        errorStrings = errorStrings,
    )
}

/**
 * Parses all unique PGN lines from the supplied position, including nested variations.
 *
 * Unnumbered variations replace the preceding move; numbered variations use their explicit position.
 * @param positionFactory creates positions used to validate and replay each line.
 * @param pgnText PGN move text, for example `"23... e5 (c5) 24. Nf3"`.
 * @param startFen four- or six-field FEN describing the starting position.
 * @return main line first, followed by unique variation lines in UCI notation.
 * @throws PgnVariationParseException for invalid variation structure, with token index and reason.
 * @throws IllegalArgumentException for invalid FEN or illegal moves.
 */
fun parsePgnToUciLines(
    positionFactory: PositionFactory,
    pgnText: String,
    startFen: String,
): List<List<String>> {
    return parsePgnToUciLinesFromStart(
        positionFactory = positionFactory,
        pgnText = pgnText,
        startFen = startFen,
        errorStrings = DefaultPgnParseErrorStrings,
    )
}

/**
 * Parses all unique PGN lines from the supplied position, including nested variations.
 *
 * Unnumbered variations replace the preceding move; numbered variations use their explicit position.
 * @param positionFactory creates positions used to validate and replay each line.
 * @param pgnText PGN move text, for example `"23... e5 (c5) 24. Nf3"`.
 * @param startFen four- or six-field FEN describing the starting position.
 * @param errorStrings templates for app-facing SAN replay errors.
 * @return main line first, followed by unique variation lines in UCI notation.
 * @throws PgnVariationParseException for invalid variation structure, with token index and reason.
 * @throws IllegalArgumentException for invalid FEN or illegal moves.
 */
fun parsePgnToUciLines(
    positionFactory: PositionFactory,
    pgnText: String,
    startFen: String,
    errorStrings: PgnParseErrorStrings,
): List<List<String>> {
    return parsePgnToUciLinesFromStart(
        positionFactory = positionFactory,
        pgnText = pgnText,
        startFen = startFen,
        errorStrings = errorStrings,
        preserveDuplicateLines = false,
    )
}

/**
 * Parses every PGN branch while preserving equal lines for caller-owned duplicate statistics.
 *
 * Unnumbered variations replace the preceding move; numbered variations use their explicit position.
 * @param positionFactory creates positions used to validate and replay each line.
 * @param pgnText PGN move text, for example `"23... e5 (c5) 24. Nf3"`.
 * @param startFen four- or six-field FEN describing the starting position.
 * @param errorStrings templates for app-facing SAN replay errors.
 * @return main line first, followed by variation lines in UCI notation, including duplicates.
 * @throws PgnVariationParseException for invalid variation structure, with token index and reason.
 * @throws IllegalArgumentException for invalid FEN or illegal moves.
 */
fun parsePgnToUciLinesPreservingDuplicates(
    positionFactory: PositionFactory,
    pgnText: String,
    startFen: String,
    errorStrings: PgnParseErrorStrings,
): List<List<String>> {
    return parsePgnToUciLinesFromStart(
        positionFactory = positionFactory,
        pgnText = pgnText,
        startFen = startFen,
        errorStrings = errorStrings,
        preserveDuplicateLines = true,
    )
}

private fun parsePgnToUciLinesFromStart(
    positionFactory: PositionFactory,
    pgnText: String,
    startFen: String?,
    errorStrings: PgnParseErrorStrings,
    preserveDuplicateLines: Boolean = false,
): List<List<String>> {
    val parsedLines = try {
        parseCorePgnToUciLines(
            positionFactory = positionFactory,
            pgnText = pgnText,
            startFen = startFen,
        )
    } catch (error: PgnLineParseException) {
        val message = errorStrings.lineParseFailed.format(
            formatSanLineParseError(error.sanError, errorStrings),
            resolvePgnLineLabel(error.lineIndex, errorStrings),
        )
        throw IllegalArgumentException(message, error)
    }
    if (preserveDuplicateLines) {
        return parsedLines
    }

    return parsedLines.distinctBy { it.joinToString(" ") }
}

private fun resolvePgnLineLabel(
    index: Int,
    errorStrings: PgnParseErrorStrings,
): String {
    if (index == 0) {
        return errorStrings.mainLine
    }

    return errorStrings.variation.format(index)
}

/** Converts UCI strings into chesslib moves for persistence. */
fun uciMovesToMoves(uciMoves: List<String>): List<Move> {
    val board = Board()

    return uciMoves.map { uci ->
        val move = buildChesslibMoveFromUci(uci = uci, board = board)
        board.doMove(move)
        move
    }
}

/** Builds the stored PGN format used by this app from a list of UCI strings. */
fun buildStoredPgnFromUci(
    uciMoves: List<String>,
    event: String,
    whiteName: String = "White",
    blackName: String = "Black"
): String {
    val sb = StringBuilder()

    sb.append("[Event \"$event\"]\n")
    sb.append("[White \"$whiteName\"]\n")
    sb.append("[Black \"$blackName\"]\n")
    sb.append("[Result \"*\"]\n\n")

    uciMoves.forEachIndexed { index, move ->
        if (index % 2 == 0) {
            sb.append("${index / 2 + 1}. ")
        }
        sb.append("$move ")
    }

    sb.append("*")
    return sb.toString().trim()
}

private fun formatSanLineParseError(
    error: SanLineParseException,
    errorStrings: PgnParseErrorStrings,
): String {
    val side = resolveSanLineParseErrorSide(
        sideToMove = error.sideToMove,
        errorStrings = errorStrings,
    )
    val template = when (error.reason) {
        SanLineParseErrorReason.UNRECOGNIZED_NOTATION -> errorStrings.unrecognizedNotation
        SanLineParseErrorReason.ILLEGAL_MOVE -> errorStrings.illegalMove
    }
    return template.format(error.token, error.localMoveNumber, side)
}

private fun resolveSanLineParseErrorSide(
    sideToMove: Side,
    errorStrings: PgnParseErrorStrings,
): String {
    if (sideToMove == Side.WHITE) {
        return errorStrings.whiteSide
    }

    return errorStrings.blackSide
}

// ──────────────────────────────────────────────────────────────────────────────
// Stored-PGN parsing (app's own UCI-notation PGN format)
// ──────────────────────────────────────────────────────────────────────────────

/** Line entity bundled with its pre-computed UCI moves and algebraic labels. */
data class ParsedLine(
    val line: LineEntity,
    val uciMoves: List<String>,
    val moveLabels: List<String>
)

/**
 * Extracts UCI move tokens from the app's stored PGN-like format.
 *
 * @param pgn text that may include PGN headers, move numbers, results, and UCI tokens,
 * for example `"1. e2e4 e7e5 2. g1f3 *"`.
 * @return UCI tokens in source order, for example `listOf("e2e4", "e7e5", "g1f3")`.
 */
fun parsePgnMoves(pgn: String): List<String> {
    return extractUciMoveTokens(pgn)
}
