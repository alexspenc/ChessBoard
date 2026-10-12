package com.example.chessboard.service

/**
 * File role: imports and normalizes PGN or stored UCI-like move text.
 * Allowed here:
 * - app-facing PGN/SAN import orchestration and conversion into UCI lines
 * - PGN variation expansion until that behavior is moved to chesscore
 * - stored PGN construction and extraction of persisted UCI moves
 * Main PGN tokenization and SAN-line replay are delegated to chesscore; variation structure is handled here.
 * Not allowed here:
 * - Compose UI, screen navigation, Room DAO definitions, or board-controller state
 * Validation date: 2026-10-12
 */

import com.example.chessboard.boardmodel.buildChesslibMoveFromUci
import com.example.chessboard.chesscore.Position
import com.example.chessboard.chesscore.PositionFactory
import com.example.chessboard.chesscore.SanLineParseErrorReason
import com.example.chessboard.chesscore.SanLineParseException
import com.example.chessboard.chesscore.extractMainSanTokens
import com.example.chessboard.chesscore.extractUciMoveTokens
import com.example.chessboard.chesscore.isPgnMoveNumberToken
import com.example.chessboard.chesscore.isPgnResultToken
import com.example.chessboard.chesscore.model.Side
import com.example.chessboard.chesscore.normalizeFenForPositionLoad
import com.example.chessboard.chesscore.parsePgnMoveNumber
import com.example.chessboard.chesscore.parseSanLineToUci as parseCoreSanLineToUci
import com.example.chessboard.chesscore.tokenizePgnMoveText
import com.example.chessboard.entity.LineEntity
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.move.Move
import kotlin.collections.ArrayDeque

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

/** Parses only the main line of one PGN record into UCI moves. */
fun parsePgnMainLineToUci(positionFactory: PositionFactory, pgnText: String): List<String> {
    return parsePgnMainLineToUci(
        positionFactory = positionFactory,
        pgnText = pgnText,
        errorStrings = DefaultPgnParseErrorStrings,
    )
}

/** Parses only the main line of one PGN record into UCI moves. */
fun parsePgnMainLineToUci(
    positionFactory: PositionFactory,
    pgnText: String,
    errorStrings: PgnParseErrorStrings,
): List<String> {
    val pgnTokens = tokenizePgnMoveText(pgnText)
    val sanLine = extractMainSanTokens(pgnTokens)
    if (sanLine.isEmpty()) {
        return emptyList()
    }

    try {
        return parseSanLineToUci(
            positionFactory = positionFactory,
            tokens = sanLine,
            errorStrings = errorStrings,
        )
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
 * @throws IllegalArgumentException for illegal moves, invalid variation structure, or position creation errors.
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
 * @throws IllegalArgumentException for illegal moves, invalid variation structure, or position creation errors.
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
 * @throws IllegalArgumentException for invalid FEN, illegal moves, or invalid variation structure.
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
 * @throws IllegalArgumentException for invalid FEN, illegal moves, or invalid variation structure.
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
 * @throws IllegalArgumentException for invalid FEN, illegal moves, or invalid variation structure.
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
    val startPosition = resolvePgnImportStartPosition(startFen, positionFactory)
    val sanLines = extractSanLines(
        pgnText = pgnText,
        startPosition = startPosition,
    )

    val parsedLines = sanLines
        .reversed() // extractSanLines adds the main line last; reverse so it comes first
        .mapIndexed { idx, line ->
            val lineLabel = resolvePgnLineLabel(
                index = idx,
                errorStrings = errorStrings,
            )
            try {
                parseSanLineToUci(
                    positionFactory = positionFactory,
                    tokens = line,
                    startPosition = startPosition,
                    errorStrings = errorStrings,
                )
            } catch (e: IllegalArgumentException) {
                val message = errorStrings.lineParseFailed.format(
                    e.message.orEmpty(),
                    lineLabel,
                )
                throw IllegalArgumentException(message, e)
            }
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

private fun extractSanLines(
    pgnText: String,
    startPosition: PgnImportStartPosition,
): List<List<String>> {
    val tokens = tokenizePgnMoveText(pgnText)
    val initialAbsolutePly = resolveInitialAbsolutePly(
        tokens = tokens,
        sideToMove = startPosition.sideToMove,
    )

    data class BranchState(
        val line: List<String>,
        val ownMoveCount: Int,
    )

    val lines = mutableListOf<List<String>>()
    val branchStack = ArrayDeque<BranchState>()
    var currentLine = mutableListOf<String>()
    var ownMoveCount = 0

    var i = 0
    while (i < tokens.size) {
        val token = tokens[i]
        when {
            token == "(" -> {
                require(ownMoveCount > 0) {
                    "A PGN variation must follow a move in its containing line"
                }
                branchStack.addLast(BranchState(currentLine.toList(), ownMoveCount))
                // Interpret a variation move number relative to the copied fragment's first number.
                // This keeps fragments such as "23..." independent from unavailable FEN counters.
                val targetPly = resolveVariationStartPly(
                    currentLine = currentLine,
                    firstVariationToken = tokens.getOrNull(i + 1),
                    initialAbsolutePly = initialAbsolutePly,
                )
                currentLine = currentLine.take(targetPly).toMutableList()
                ownMoveCount = 0
            }
            token == ")" -> {
                if (branchStack.isNotEmpty()) {
                    require(ownMoveCount > 0) {
                        "A PGN variation must contain a move of its own"
                    }
                }
                if (currentLine.isNotEmpty()) lines.add(currentLine.toList())
                val parent = branchStack.removeLastOrNull()
                currentLine = parent?.line?.toMutableList() ?: mutableListOf()
                ownMoveCount = parent?.ownMoveCount ?: 0
            }
            token.startsWith("$") -> {
                require(branchStack.isEmpty() || ownMoveCount > 0) {
                    "A PGN variation NAG must follow a move in that variation"
                }
            }
            isPgnMoveNumberToken(token) || isPgnResultToken(token) -> {
                // Move numbers and result tokens do not count as moves in a variation.
            }
            else -> {
                currentLine.add(token)
                ownMoveCount++
            }
        }
        i++
    }

    if (currentLine.isNotEmpty()) lines.add(currentLine.toList())
    return lines
}

private data class PgnImportStartPosition(
    val fen: String,
    val sideToMove: Side,
)

private fun resolvePgnImportStartPosition(
    startFen: String?,
    positionFactory: PositionFactory,
): PgnImportStartPosition {
    fun createStartPosition(): Position {
        if (startFen.isNullOrBlank()) {
            return positionFactory.create()
        }

        return positionFactory.create(normalizeFenForPositionLoad(startFen))
    }

    val position = createStartPosition()
    return PgnImportStartPosition(
        fen = position.getFen(),
        sideToMove = position.getSideToMove(),
    )
}

private fun resolveInitialAbsolutePly(
    tokens: List<String>,
    sideToMove: Side,
): Int {
    val firstMoveNumber = tokens.firstOrNull()
        ?.let(::parsePgnMoveNumber)
        ?.number
        ?: 1
    val sideOffset = if (sideToMove == Side.BLACK) 1 else 0
    return (firstMoveNumber - 1) * 2 + sideOffset
}

/** Returns the local half-move index at which a numbered variation starts. */
private fun variationStartPly(
    token: String?,
    initialAbsolutePly: Int,
): Int? {
    val moveNumber = token?.let(::parsePgnMoveNumber) ?: return null
    val sideOffset = if (moveNumber.side == Side.BLACK) 1 else 0
    val absolutePly = (moveNumber.number - 1) * 2 + sideOffset
    return (absolutePly - initialAbsolutePly).coerceAtLeast(0)
}

/** Numbered variants use their explicit position; unnumbered variants replace the preceding move. */
private fun resolveVariationStartPly(
    currentLine: List<String>,
    firstVariationToken: String?,
    initialAbsolutePly: Int,
): Int {
    val explicitPly = variationStartPly(
        token = firstVariationToken,
        initialAbsolutePly = initialAbsolutePly,
    )
    if (explicitPly != null) return explicitPly.coerceIn(0, currentLine.size)

    return currentLine.size - 1
}

private fun parseSanLineToUci(
    positionFactory: PositionFactory,
    tokens: List<String>,
    errorStrings: PgnParseErrorStrings,
): List<String> {
    return parseSanLineToUci(
        positionFactory = positionFactory,
        tokens = tokens,
        startFen = null,
        errorStrings = errorStrings,
    )
}

private fun parseSanLineToUci(
    positionFactory: PositionFactory,
    tokens: List<String>,
    startPosition: PgnImportStartPosition,
    errorStrings: PgnParseErrorStrings,
): List<String> {
    return parseSanLineToUci(
        positionFactory = positionFactory,
        tokens = tokens,
        startFen = startPosition.fen,
        errorStrings = errorStrings,
    )
}

private fun parseSanLineToUci(
    positionFactory: PositionFactory,
    tokens: List<String>,
    startFen: String?,
    errorStrings: PgnParseErrorStrings,
): List<String> {
    try {
        return parseCoreSanLineToUci(
            positionFactory = positionFactory,
            sanTokens = tokens,
            startFen = startFen,
        )
    } catch (e: SanLineParseException) {
        throw IllegalArgumentException(formatSanLineParseError(e, errorStrings), e)
    }
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
