package com.example.chessboard.service

/**
 * File role: imports and normalizes PGN or stored UCI-like move text.
 * Allowed here:
 * - app-facing PGN/SAN import orchestration and conversion into UCI lines
 * - PGN variation expansion until that behavior is moved to chesscore
 * - stored PGN construction and extraction of persisted UCI moves
 * Main PGN tokenization and SAN-line replay are delegated to chesscore; variation-start inference still uses chesslib.
 * Not allowed here:
 * - Compose UI, screen navigation, Room DAO definitions, or board-controller state
 * Validation date: 2026-09-09
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
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
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

/** Parses the PGN into all unique playable lines, including nested variations. */
fun parsePgnToUciLines(positionFactory: PositionFactory, pgnText: String): List<List<String>> {
    return parsePgnToUciLines(
        positionFactory = positionFactory,
        pgnText = pgnText,
        errorStrings = DefaultPgnParseErrorStrings,
    )
}

/** Parses the PGN into all unique playable lines, including nested variations. */
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

/** Parses all unique PGN lines from the supplied position, including nested variations. */
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

/** Parses all unique PGN lines from the supplied position, including nested variations. */
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

/** Parses every PGN branch while preserving equal lines for caller-owned duplicate statistics. */
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

    val lines = mutableListOf<List<String>>()
    val branchStack = ArrayDeque<List<String>>()
    var currentLine = mutableListOf<String>()

    var i = 0
    while (i < tokens.size) {
        val token = tokens[i]
        when {
            token == "(" -> {
                branchStack.addLast(currentLine.toList())
                // Interpret a variation move number relative to the copied fragment's first number.
                // This keeps fragments such as "23..." independent from unavailable FEN counters.
                val targetPly = inferVariationStartPly(
                    currentLine = currentLine,
                    firstVariationToken = tokens.getOrNull(i + 1),
                    initialAbsolutePly = initialAbsolutePly,
                    startPosition = startPosition,
                )
                currentLine = currentLine.take(targetPly).toMutableList()
            }
            token == ")" -> {
                if (currentLine.isNotEmpty()) lines.add(currentLine.toList())
                currentLine = branchStack.removeLastOrNull()?.toMutableList() ?: mutableListOf()
            }
            token.startsWith("$") || isPgnMoveNumberToken(token) || isPgnResultToken(token) -> {
                // skip move numbers, NAG annotations and result tokens
            }
            else -> currentLine.add(token)
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

private fun inferVariationStartPly(
    currentLine: List<String>,
    firstVariationToken: String?,
    initialAbsolutePly: Int,
    startPosition: PgnImportStartPosition,
): Int {
    val explicitPly = variationStartPly(
        token = firstVariationToken,
        initialAbsolutePly = initialAbsolutePly,
    )
    if (explicitPly != null) return explicitPly.coerceIn(0, currentLine.size)
    if (currentLine.isEmpty()) return 0

    val token = firstVariationToken ?: return currentLine.size
    if (token == "(" || token == ")" || isPgnResultToken(token) || token.startsWith("$")) {
        return currentLine.size
    }

    val candidatePlies = buildList {
        add(currentLine.size)
        add((currentLine.size - 1).coerceAtLeast(0))
    }.distinct()

    val legalCandidates = candidatePlies.filter { ply ->
        val board = Board().also { it.loadFromFen(startPosition.fen) }
        currentLine.take(ply).forEach { san ->
            val uci = sanToUci(san, board) ?: return@filter false
            val move = runCatching {
                buildChesslibMoveFromUci(uci = uci, board = board)
            }.getOrNull() ?: return@filter false
            if (!board.legalMoves().contains(move)) return@filter false
            board.doMove(move)
        }
        sanToUci(token, board) != null
    }

    return when {
        legalCandidates.size == 1 -> legalCandidates.first()
        legalCandidates.contains(currentLine.size) -> currentLine.size
        else -> (currentLine.size - 1).coerceAtLeast(0)
    }
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

/**
 * Converts a single SAN token (e.g. "Nf3", "exd5", "O-O", "e8=Q") to a UCI string
 * given the current board state. Returns null if no legal move matches.
 */
private fun sanToUci(san: String, board: Board): String? {
    val cleaned = san.trimEnd('+', '#', '!', '?', ' ')
    if (cleaned.isBlank()) return null

    if (cleaned == "O-O-O" || cleaned == "0-0-0") {
        val isWhite = board.sideToMove.name == "WHITE"
        val move = board.legalMoves().find { m ->
            m.from.value().lowercase() == (if (isWhite) "e1" else "e8") &&
            m.to.value().lowercase() == (if (isWhite) "c1" else "c8")
        }
        return move?.let { "${it.from.value().lowercase()}${it.to.value().lowercase()}" }
    }
    if (cleaned == "O-O" || cleaned == "0-0") {
        val isWhite = board.sideToMove.name == "WHITE"
        val move = board.legalMoves().find { m ->
            m.from.value().lowercase() == (if (isWhite) "e1" else "e8") &&
            m.to.value().lowercase() == (if (isWhite) "g1" else "g8")
        }
        return move?.let { "${it.from.value().lowercase()}${it.to.value().lowercase()}" }
    }

    val promotionType: PieceType?
    val sanCore: String
    val eqIdx = cleaned.indexOf('=')
    if (eqIdx != -1) {
        sanCore = cleaned.substring(0, eqIdx)
        promotionType = charToPieceType(cleaned.getOrNull(eqIdx + 1))
    } else if (cleaned.length >= 3 && cleaned.last() in "QRBNqrbn" &&
        cleaned[cleaned.length - 2].isDigit() && cleaned[cleaned.length - 3].isLetter()) {
        sanCore = cleaned.dropLast(1)
        promotionType = charToPieceType(cleaned.last())
    } else {
        sanCore = cleaned
        promotionType = null
    }

    val isCapture = sanCore.contains('x')
    val withoutCapture = sanCore.replace("x", "")
    if (withoutCapture.length < 2) return null

    val destSquare = withoutCapture.takeLast(2).lowercase()
    val prefix = withoutCapture.dropLast(2)
    val legalMoves = board.legalMoves()
    val isWhite = board.sideToMove.name == "WHITE"

    return if (prefix.isNotEmpty() && prefix[0].isUpperCase()) {
        val pieceType = when (prefix[0]) {
            'N' -> PieceType.KNIGHT
            'B' -> PieceType.BISHOP
            'R' -> PieceType.ROOK
            'Q' -> PieceType.QUEEN
            'K' -> PieceType.KING
            else -> return null
        }
        val disambiguation = prefix.drop(1)
        val candidates = legalMoves.filter { m ->
            board.getPiece(m.from).pieceType == pieceType &&
            m.to.value().lowercase() == destSquare &&
            m.promotion == Piece.NONE
        }
        val matched = when {
            candidates.size == 1 -> candidates[0]
            disambiguation.isEmpty() -> candidates.firstOrNull()
            disambiguation.length == 1 && disambiguation[0].isDigit() ->
                candidates.find { it.from.value()[1] == disambiguation[0] }
            disambiguation.length == 1 ->
                candidates.find { it.from.value()[0].lowercaseChar() == disambiguation[0] }
            disambiguation.length == 2 ->
                candidates.find { it.from.value().lowercase() == disambiguation }
            else -> null
        }
        matched?.let { "${it.from.value().lowercase()}${it.to.value().lowercase()}" }
    } else {
        val pawnPiece = if (isWhite) Piece.WHITE_PAWN else Piece.BLACK_PAWN
        val effectivePromotionType = promotionType ?: run {
            val anyCandidatePromotes = legalMoves.any { m ->
                board.getPiece(m.from) == pawnPiece &&
                m.to.value().lowercase() == destSquare &&
                m.promotion != Piece.NONE
            }
            if (anyCandidatePromotes) PieceType.QUEEN else null
        }
        val promotionPiece = when (effectivePromotionType) {
            PieceType.QUEEN  -> if (isWhite) Piece.WHITE_QUEEN  else Piece.BLACK_QUEEN
            PieceType.ROOK   -> if (isWhite) Piece.WHITE_ROOK   else Piece.BLACK_ROOK
            PieceType.BISHOP -> if (isWhite) Piece.WHITE_BISHOP else Piece.BLACK_BISHOP
            PieceType.KNIGHT -> if (isWhite) Piece.WHITE_KNIGHT else Piece.BLACK_KNIGHT
            else -> Piece.NONE
        }
        val candidates = legalMoves.filter { m ->
            board.getPiece(m.from) == pawnPiece &&
            m.to.value().lowercase() == destSquare &&
            (promotionPiece == Piece.NONE && m.promotion == Piece.NONE ||
             promotionPiece != Piece.NONE && m.promotion == promotionPiece)
        }
        val matched = if (isCapture && prefix.isNotEmpty()) {
            candidates.find { it.from.value()[0].lowercaseChar() == prefix[0] }
        } else {
            candidates.firstOrNull()
        }
        matched?.let {
            buildString {
                append(it.from.value().lowercase())
                append(it.to.value().lowercase())
                if (it.promotion != Piece.NONE) {
                    append(it.promotion.pieceType.name.first().lowercaseChar())
                }
            }
        }
    }
}

private fun charToPieceType(c: Char?): PieceType? = when (c?.uppercaseChar()) {
    'Q' -> PieceType.QUEEN
    'R' -> PieceType.ROOK
    'B' -> PieceType.BISHOP
    'N' -> PieceType.KNIGHT
    else -> null
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
