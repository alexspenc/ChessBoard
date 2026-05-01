package com.example.chessboard.service

/**
 * Builds exportable PGN text from the analysis move tree collected on the game-analysis screen.
 *
 * Keep pure analysis-tree to PGN serialization logic here. Do not add Compose UI, clipboard
 * access, navigation, or persistence workflows to this file. Validation date: 2026-05-01.
 */
import com.example.chessboard.entity.GameEntity
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

private data class AnalysisMoveNode(
    val uciMove: String,
    val children: MutableList<AnalysisMoveNode> = mutableListOf(),
)

fun buildAnalysisPgnFromGames(
    games: List<GameEntity>,
): String {
    val uciLines = games.mapNotNull { game ->
        parsePgnMoves(game.pgn).takeIf { line -> line.isNotEmpty() }
    }

    return buildAnalysisPgn(uciLines)
}

fun buildAnalysisPgn(uciLines: List<List<String>>): String {
    val normalizedLines = normalizeAnalysisPgnLines(uciLines)
    if (normalizedLines.isEmpty()) {
        return ""
    }

    val root = buildAnalysisMoveTree(normalizedLines)
    val builder = StringBuilder()
    val board = Board()

    appendAnalysisBranch(
        node = root,
        board = board,
        nextPly = 0,
        builder = builder,
        forceMoveNumber = false,
    )

    return builder.toString().trim()
}

private fun buildAnalysisMoveTree(
    uciLines: List<List<String>>,
): AnalysisMoveNode {
    val root = AnalysisMoveNode(uciMove = "")

    uciLines.forEach { line ->
        var current = root
        line.forEach { uciMove ->
            val existingChild = current.children.firstOrNull { it.uciMove == uciMove }
            if (existingChild != null) {
                current = existingChild
                return@forEach
            }

            val nextChild = AnalysisMoveNode(uciMove = uciMove)
            current.children.add(nextChild)
            current = nextChild
        }
    }

    return root
}

private fun normalizeAnalysisPgnLines(
    uciLines: List<List<String>>,
): List<List<String>> {
    val normalizedLines = uciLines
        .map { line -> line.map { move -> move.trim().lowercase() }.filter { it.isNotEmpty() } }
        .filter { it.isNotEmpty() }

    return normalizedLines.filterIndexed { index, line ->
        if (normalizedLines.indexOf(line) != index) {
            return@filterIndexed false
        }

        !normalizedLines.any { candidateLine ->
            candidateLine.size > line.size && candidateLine.take(line.size) == line
        }
    }
}

private fun appendAnalysisBranch(
    node: AnalysisMoveNode,
    board: Board,
    nextPly: Int,
    builder: StringBuilder,
    forceMoveNumber: Boolean,
) {
    val mainChild = node.children.firstOrNull() ?: return
    val branchFen = board.fen

    appendAnalysisMove(
        uciMove = mainChild.uciMove,
        board = board,
        ply = nextPly,
        builder = builder,
        forceMoveNumber = forceMoveNumber,
    )

    node.children.drop(1).forEach { variationChild ->
        builder.append(" (")
        val variationBoard = Board().also { it.loadFromFen(branchFen) }
        appendAnalysisVariation(
            node = variationChild,
            board = variationBoard,
            nextPly = nextPly,
            builder = builder,
        )
        builder.append(")")
    }

    appendAnalysisBranch(
        node = mainChild,
        board = board,
        nextPly = nextPly + 1,
        builder = builder,
        forceMoveNumber = node.children.size > 1,
    )
}

private fun appendAnalysisVariation(
    node: AnalysisMoveNode,
    board: Board,
    nextPly: Int,
    builder: StringBuilder,
) {
    appendAnalysisBranch(
        node = AnalysisMoveNode(
            uciMove = "",
            children = mutableListOf(node),
        ),
        board = board,
        nextPly = nextPly,
        builder = builder,
        forceMoveNumber = nextPly % 2 == 1,
    )
}

private fun appendAnalysisMove(
    uciMove: String,
    board: Board,
    ply: Int,
    builder: StringBuilder,
    forceMoveNumber: Boolean,
) {
    val move = resolveAnalysisMove(
        uciMove = uciMove,
        board = board,
    )
    val moveLabel = resolveAnalysisSanMove(
        move = move,
        board = board,
    )

    if (builder.isNotEmpty() && builder.last() != '(' && builder.last() != ' ') {
        builder.append(' ')
    }

    if (ply % 2 == 0) {
        builder.append("${ply / 2 + 1}. ")
    } else if (forceMoveNumber) {
        builder.append("${ply / 2 + 1}... ")
    }

    builder.append(moveLabel)
    board.doMove(move)
}

private fun resolveAnalysisMove(
    uciMove: String,
    board: Board,
): Move {
    val fromSquare = Square.fromValue(uciMove.take(2).uppercase())
    val toSquare = Square.fromValue(uciMove.drop(2).take(2).uppercase())
    val promotionPiece = resolveAnalysisPromotionPiece(
        promotionToken = uciMove.getOrNull(4),
        board = board,
    )
    val move = if (promotionPiece == Piece.NONE) {
        Move(fromSquare, toSquare)
    } else {
        Move(fromSquare, toSquare, promotionPiece)
    }

    require(board.legalMoves().contains(move)) {
        "Illegal analysis move: $uciMove from ${board.fen}"
    }

    return move
}

private fun resolveAnalysisSanMove(
    move: Move,
    board: Board,
): String {
    val movingPiece = board.getPiece(move.from)
    val destinationSquare = move.to.value().lowercase()
    val isCapture = resolveAnalysisCapture(
        move = move,
        board = board,
    )
    val piecePrefix = resolveAnalysisPiecePrefix(movingPiece.pieceType)
    val disambiguation = resolveAnalysisDisambiguation(
        move = move,
        board = board,
        movingPiece = movingPiece,
    )
    val castleNotation = resolveAnalysisCastleNotation(move)

    val baseNotation = if (castleNotation != null) {
        castleNotation
    } else if (movingPiece.pieceType == PieceType.PAWN) {
        buildString {
            if (isCapture) {
                append(move.from.value()[0].lowercaseChar())
                append('x')
            }
            append(destinationSquare)
        }
    } else {
        buildString {
            append(piecePrefix)
            append(disambiguation)
            if (isCapture) {
                append('x')
            }
            append(destinationSquare)
        }
    }

    val promotionSuffix = resolveAnalysisPromotionSuffix(move)
    val nextBoard = Board().also { it.loadFromFen(board.fen) }
    nextBoard.doMove(move)
    val checkSuffix = when {
        nextBoard.legalMoves().isEmpty() && nextBoard.isKingAttacked -> "#"
        nextBoard.isKingAttacked -> "+"
        else -> ""
    }

    return "$baseNotation$promotionSuffix$checkSuffix"
}

private fun resolveAnalysisCapture(
    move: Move,
    board: Board,
): Boolean {
    if (board.getPiece(move.to) != Piece.NONE) {
        return true
    }

    val movingPiece = board.getPiece(move.from)
    if (movingPiece.pieceType != PieceType.PAWN) {
        return false
    }

    return move.from.value()[0] != move.to.value()[0]
}

private fun resolveAnalysisPiecePrefix(
    pieceType: PieceType,
): String {
    if (pieceType == PieceType.KNIGHT) {
        return "N"
    }

    if (pieceType == PieceType.BISHOP) {
        return "B"
    }

    if (pieceType == PieceType.ROOK) {
        return "R"
    }

    if (pieceType == PieceType.QUEEN) {
        return "Q"
    }

    if (pieceType == PieceType.KING) {
        return "K"
    }

    return ""
}

private fun resolveAnalysisDisambiguation(
    move: Move,
    board: Board,
    movingPiece: Piece,
): String {
    if (movingPiece.pieceType == PieceType.PAWN || movingPiece.pieceType == PieceType.KING) {
        return ""
    }

    val candidates = board.legalMoves().filter { candidate ->
        if (candidate == move) {
            return@filter false
        }

        if (candidate.to != move.to) {
            return@filter false
        }

        if (candidate.promotion != move.promotion) {
            return@filter false
        }

        board.getPiece(candidate.from) == movingPiece
    }
    if (candidates.isEmpty()) {
        return ""
    }

    val sameFileExists = candidates.any { it.from.file == move.from.file }
    val sameRankExists = candidates.any { it.from.rank == move.from.rank }
    if (!sameFileExists) {
        return move.from.value()[0].lowercaseChar().toString()
    }

    if (!sameRankExists) {
        return move.from.value()[1].toString()
    }

    return move.from.value().lowercase()
}

private fun resolveAnalysisCastleNotation(
    move: Move,
): String? {
    if (move.from.value()[0] != 'E') {
        return null
    }

    if (move.to.value()[0] == 'G') {
        return "O-O"
    }

    if (move.to.value()[0] == 'C') {
        return "O-O-O"
    }

    return null
}

private fun resolveAnalysisPromotionSuffix(
    move: Move,
): String {
    if (move.promotion == Piece.NONE) {
        return ""
    }

    return "=${move.promotion.pieceType.name.first().uppercaseChar()}"
}

private fun resolveAnalysisPromotionPiece(
    promotionToken: Char?,
    board: Board,
): Piece {
    val normalizedToken = promotionToken?.lowercaseChar() ?: return Piece.NONE
    val isWhiteMove = board.sideToMove.name == "WHITE"

    if (normalizedToken == 'q') {
        return if (isWhiteMove) Piece.WHITE_QUEEN else Piece.BLACK_QUEEN
    }

    if (normalizedToken == 'r') {
        return if (isWhiteMove) Piece.WHITE_ROOK else Piece.BLACK_ROOK
    }

    if (normalizedToken == 'b') {
        return if (isWhiteMove) Piece.WHITE_BISHOP else Piece.BLACK_BISHOP
    }

    if (normalizedToken == 'n') {
        return if (isWhiteMove) Piece.WHITE_KNIGHT else Piece.BLACK_KNIGHT
    }

    return Piece.NONE
}
