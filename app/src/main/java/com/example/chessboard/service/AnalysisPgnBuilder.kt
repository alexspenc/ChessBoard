package com.example.chessboard.service

/**
 * Builds exportable PGN text from the analysis move tree collected on the line-analysis screen.
 *
 * Keep pure analysis-tree to PGN serialization logic here. Do not add Compose UI, clipboard
 * access, navigation, or persistence workflows to this file. Validation date: 2026-06-16.
 */
import com.example.chessboard.entity.LineEntity
import com.github.bhlangonijr.chesslib.Board

private data class AnalysisMoveNode(
    val uciMove: String,
    val children: MutableList<AnalysisMoveNode> = mutableListOf(),
)

fun buildAnalysisPgnFromLines(
    lines: List<LineEntity>,
): String {
    val uciLines = lines.mapNotNull { line ->
        parsePgnMoves(line.pgn).takeIf { moves -> moves.isNotEmpty() }
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
    val resolvedMove = resolvePgnSanMove(
        uciMove = uciMove,
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

    builder.append(resolvedMove.san)
    board.doMove(resolvedMove.move)
}
