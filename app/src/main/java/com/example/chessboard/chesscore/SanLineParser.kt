package com.example.chessboard.chesscore

/**
 * Parses one SAN token line from a supplied start position into UCI moves.
 * Example token list: ["e4", "e5", "Nf3", "Nc6", "Bb5", "a6"].
 * PGN move numbers such as "1." or "1..." are not part of this input.
 * Contains chesscore-only SAN matching and move replay logic.
 * Does not contain PGN variation parsing, app error strings, UI, persistence.
 * Validation date: 2026-09-08.
 */

import com.example.chessboard.chesscore.model.Move
import com.example.chessboard.chesscore.model.PieceType
import com.example.chessboard.chesscore.model.PromotionPiece
import com.example.chessboard.chesscore.model.Side
import com.example.chessboard.chesscore.model.Square

enum class SanLineParseErrorReason {
    UNRECOGNIZED_NOTATION,
    ILLEGAL_MOVE,
}

/** Reports a failure caused by one SAN token during line replay. */
class SanLineParseException(
    val token: String,
    /** Local move number inside the supplied SAN line; it does not depend on the FEN fullmove number. */
    val localMoveNumber: Int,
    val sideToMove: Side,
    val reason: SanLineParseErrorReason,
) : IllegalArgumentException()

/**
 * Parses SAN tokens into UCI moves by replaying them from [startFen].
 *
 * @param positionFactory creates the mutable position used for legal move lookup and replay.
 * @param sanTokens SAN moves without PGN move numbers, for example:
 * ["e4", "e5", "Nf3", "Nc6", "Bb5", "a6"].
 * @param startFen six-field FEN for the supplied start position, or null for the standard position.
 * @return UCI moves in the same order as [sanTokens], for example:
 * ["e2e4", "e7e5", "g1f3", "b8c6", "f1b5", "a7a6"].
 * @throws SanLineParseException when an individual SAN token cannot be recognized or applied.
 * @throws IllegalArgumentException when [positionFactory] rejects [startFen].
 */
fun parseSanLineToUci(
    positionFactory: PositionFactory,
    sanTokens: List<String>,
    startFen: String? = null,
): List<String> {
    val position = positionFactory.create(startFen)
    val startingSide = position.getSideToMove()
    val uciMoves = mutableListOf<String>()

    for ((index, token) in sanTokens.withIndex()) {
        val localMoveNumber = resolveSanLineMoveNumber(
            index = index,
            startingSide = startingSide,
        )
        val sideToMove = position.getSideToMove()
        val move = sanToMove(token, position)
            ?: throw SanLineParseException(
                token = token,
                localMoveNumber = localMoveNumber,
                sideToMove = sideToMove,
                reason = SanLineParseErrorReason.UNRECOGNIZED_NOTATION,
            )

        if (!position.applyMove(move)) {
            throw SanLineParseException(
                token = token,
                localMoveNumber = localMoveNumber,
                sideToMove = sideToMove,
                reason = SanLineParseErrorReason.ILLEGAL_MOVE,
            )
        }

        uciMoves.add(move.toUci())
    }

    return uciMoves
}

private fun resolveSanLineMoveNumber(
    index: Int,
    startingSide: Side,
): Int {
    val startingSideOffset = if (startingSide == Side.BLACK) 1 else 0
    return (index + startingSideOffset) / 2 + 1
}

private fun sanToMove(san: String, position: Position): Move? {
    data class SanMoveParts(
        val sanCore: String,
        val promotionPiece: PromotionPiece?,
    )

    fun findCastlingMove(cleaned: String): Move? {
        fun castleToSquare(): Square? {
            val rank = if (position.getSideToMove() == Side.WHITE) 1 else 8
            if (cleaned == "O-O-O" || cleaned == "0-0-0") {
                return Square('c', rank)
            }
            if (cleaned == "O-O" || cleaned == "0-0") {
                return Square('g', rank)
            }

            return null
        }

        val rank = if (position.getSideToMove() == Side.WHITE) 1 else 8
        val from = Square('e', rank)
        val to = castleToSquare() ?: return null
        return position.getLegalMoves().find { move ->
            move.from == from && move.to == to
        }
    }

    fun parseSanMoveParts(cleaned: String): SanMoveParts {
        val eqIdx = cleaned.indexOf('=')
        if (eqIdx != -1) {
            return SanMoveParts(
                sanCore = cleaned.substring(0, eqIdx),
                promotionPiece = charToPromotionPiece(cleaned.getOrNull(eqIdx + 1)),
            )
        }

        val hasTrailingPromotion =
            cleaned.length >= 3 &&
                cleaned.last() in "QRBNqrbn" &&
                cleaned[cleaned.length - 2].isDigit() &&
                cleaned[cleaned.length - 3].isLetter()
        if (hasTrailingPromotion) {
            return SanMoveParts(
                sanCore = cleaned.dropLast(1),
                promotionPiece = charToPromotionPiece(cleaned.last()),
            )
        }

        return SanMoveParts(sanCore = cleaned, promotionPiece = null)
    }

    fun findNonPawnMove(
        legalMoves: List<Move>,
        prefix: String,
        destSquare: Square,
    ): Move? {
        return findPieceMove(
            position = position,
            legalMoves = legalMoves,
            sanPiece = prefix[0],
            disambiguation = prefix.drop(1),
            destSquare = destSquare,
        )
    }

    fun findPawnMove(
        legalMoves: List<Move>,
        isCapture: Boolean,
        prefix: String,
        destSquare: Square,
        promotionPiece: PromotionPiece?,
    ): Move? {
        val sideToMove = position.getSideToMove()
        val effectivePromotionPiece = promotionPiece ?: resolveImplicitPromotionPiece(
            position = position,
            legalMoves = legalMoves,
            sideToMove = sideToMove,
            destSquare = destSquare,
        )
        val candidates = legalMoves.filter { move ->
            val piece = position.getPiece(move.from) ?: return@filter false
            piece.side == sideToMove &&
                piece.type == PieceType.PAWN &&
                move.to == destSquare &&
                move.promotion == effectivePromotionPiece
        }
        if (isCapture && prefix.isNotEmpty()) {
            return candidates.find { it.from.file == prefix[0].lowercaseChar() }
        }

        return candidates.firstOrNull()
    }

    val cleaned = san.trimEnd('+', '#', '!', '?', ' ')
    if (cleaned.isBlank()) return null

    val castlingMove = findCastlingMove(cleaned)
    if (castlingMove != null) {
        return castlingMove
    }

    val parts = parseSanMoveParts(cleaned)
    val isCapture = parts.sanCore.contains('x')
    val withoutCapture = parts.sanCore.replace("x", "")
    if (withoutCapture.length < 2) return null

    val destSquare = parseSquare(withoutCapture.takeLast(2)) ?: return null
    val prefix = withoutCapture.dropLast(2)
    val legalMoves = position.getLegalMoves()
    if (prefix.isNotEmpty() && prefix[0].isUpperCase()) {
        return findNonPawnMove(
            legalMoves = legalMoves,
            prefix = prefix,
            destSquare = destSquare,
        )
    }

    return findPawnMove(
        legalMoves = legalMoves,
        isCapture = isCapture,
        prefix = prefix,
        destSquare = destSquare,
        promotionPiece = parts.promotionPiece,
    )
}

private fun findPieceMove(
    position: Position,
    legalMoves: List<Move>,
    sanPiece: Char,
    disambiguation: String,
    destSquare: Square,
): Move? {
    val pieceType = when (sanPiece) {
        'N' -> PieceType.KNIGHT
        'B' -> PieceType.BISHOP
        'R' -> PieceType.ROOK
        'Q' -> PieceType.QUEEN
        'K' -> PieceType.KING
        else -> return null
    }
    val candidates = legalMoves.filter { move ->
        position.getPiece(move.from)?.type == pieceType &&
            move.to == destSquare &&
            move.promotion == null
    }
    return when {
        candidates.size == 1 -> candidates[0]
        disambiguation.isEmpty() -> candidates.firstOrNull()
        disambiguation.length == 1 && disambiguation[0].isDigit() ->
            candidates.find { it.from.rank.digitToChar() == disambiguation[0] }
        disambiguation.length == 1 ->
            candidates.find { it.from.file == disambiguation[0].lowercaseChar() }
        disambiguation.length == 2 ->
            candidates.find { it.from == parseSquare(disambiguation) }
        else -> null
    }
}

private fun resolveImplicitPromotionPiece(
    position: Position,
    legalMoves: List<Move>,
    sideToMove: Side,
    destSquare: Square,
): PromotionPiece? {
    val anyCandidatePromotes = legalMoves.any { move ->
        val piece = position.getPiece(move.from) ?: return@any false
        piece.side == sideToMove &&
            piece.type == PieceType.PAWN &&
            move.to == destSquare &&
            move.promotion != null
    }
    if (anyCandidatePromotes) {
        return PromotionPiece.QUEEN
    }

    return null
}

private fun parseSquare(value: String): Square? {
    if (value.length != 2) {
        return null
    }

    val rank = value[1].digitToIntOrNull() ?: return null
    return runCatching {
        Square(file = value[0].lowercaseChar(), rank = rank)
    }.getOrNull()
}

/** Converts a move to UCI, for example e2-e4 to "e2e4" and e7-e8 queen promotion to "e7e8q". */
private fun Move.toUci(): String {
    return buildString {
        append(from.file)
        append(from.rank)
        append(to.file)
        append(to.rank)
        promotion?.let { append(it.toUciToken()) }
    }
}

private fun PromotionPiece.toUciToken(): Char {
    return when (this) {
        PromotionPiece.QUEEN -> 'q'
        PromotionPiece.ROOK -> 'r'
        PromotionPiece.BISHOP -> 'b'
        PromotionPiece.KNIGHT -> 'n'
    }
}

private fun charToPromotionPiece(c: Char?): PromotionPiece? = when (c?.uppercaseChar()) {
    'Q' -> PromotionPiece.QUEEN
    'R' -> PromotionPiece.ROOK
    'B' -> PromotionPiece.BISHOP
    'N' -> PromotionPiece.KNIGHT
    else -> null
}
