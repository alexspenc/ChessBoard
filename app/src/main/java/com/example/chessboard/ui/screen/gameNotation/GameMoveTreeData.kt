package com.example.chessboard.ui.screen.gameNotation

/**
 * Reusable move-tree data builder for screens that display chess lines with variations.
 *
 * Keep pure UCI-line to move-tree transformation logic here. Do not add composable UI,
 * screen container state, navigation, import workflows, or persistence logic to this file.
 */
import com.example.chessboard.service.computeLabel
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

private class MoveTrieNode(
    val uciMove: String,
    val label: String,
    val children: MutableList<MoveTrieNode> = mutableListOf(),
)

internal data class MoveItem(
    val label: String,
    val uciPath: List<String>,
    val ply: Int,
)

internal sealed class TreeSegment {
    data class MainMoves(val moves: List<MoveItem>) : TreeSegment()
    data class Variation(val moves: List<MoveItem>) : TreeSegment()
}

private fun buildMoveTrie(uciLines: List<List<String>>): MoveTrieNode {
    val root = MoveTrieNode("", "")
    for (line in uciLines) {
        val board = Board()
        var current = root
        for (uci in line) {
            val from = uci.take(2)
            val to = uci.drop(2).take(2)
            val move = try {
                Move(Square.fromValue(from.uppercase()), Square.fromValue(to.uppercase()))
            } catch (_: Exception) {
                break
            }
            var child = current.children.find { it.uciMove == uci }
            if (child == null) {
                val label = try {
                    computeLabel(move, board.fen)
                } catch (_: Exception) {
                    to
                }
                child = MoveTrieNode(uciMove = uci, label = label)
                current.children.add(child)
            }
            try {
                board.doMove(move)
            } catch (_: Exception) {
                break
            }
            current = child
        }
    }
    return root
}

internal fun buildMoveTreeData(uciLines: List<List<String>>): List<TreeSegment> {
    if (uciLines.isEmpty()) {
        return emptyList()
    }

    val root = buildMoveTrie(uciLines)
    val segments = mutableListOf<TreeSegment>()
    var currentMainMoves = mutableListOf<MoveItem>()
    var current = root
    val mainPath = mutableListOf<String>()
    var ply = 0

    while (current.children.isNotEmpty()) {
        val mainChild = current.children[0]
        mainPath.add(mainChild.uciMove)
        currentMainMoves.add(MoveItem(mainChild.label, mainPath.toList(), ply))
        if (current.children.size > 1) {
            segments.add(TreeSegment.MainMoves(currentMainMoves.toList()))
            currentMainMoves = mutableListOf()
            for (varChild in current.children.drop(1)) {
                val varBase = mainPath.dropLast(1).toMutableList()
                val varMoves = mutableListOf<MoveItem>()
                val subVariations = collectVariationMoves(varChild, ply, varBase, varMoves)
                if (varMoves.isNotEmpty()) {
                    segments.add(TreeSegment.Variation(varMoves))
                }
                for (subVar in subVariations) {
                    if (subVar.isNotEmpty()) {
                        segments.add(TreeSegment.Variation(subVar))
                    }
                }
            }
        }
        current = mainChild
        ply++
    }
    if (currentMainMoves.isNotEmpty()) {
        segments.add(TreeSegment.MainMoves(currentMainMoves.toList()))
    }
    return segments
}

private fun collectVariationMoves(
    node: MoveTrieNode,
    ply: Int,
    pathSoFar: MutableList<String>,
    moves: MutableList<MoveItem>,
): List<List<MoveItem>> {
    pathSoFar.add(node.uciMove)
    moves.add(MoveItem(node.label, pathSoFar.toList(), ply))
    val subVariations = mutableListOf<List<MoveItem>>()
    if (node.children.isNotEmpty()) {
        val pathBeforeBranch = pathSoFar.toList()
        subVariations.addAll(collectVariationMoves(node.children[0], ply + 1, pathSoFar, moves))
        for (varChild in node.children.drop(1)) {
            val subVarBase = pathBeforeBranch.toMutableList()
            val subVarMoves = mutableListOf<MoveItem>()
            val deeperSubs = collectVariationMoves(varChild, ply + 1, subVarBase, subVarMoves)
            if (subVarMoves.isNotEmpty()) {
                subVariations.add(subVarMoves)
            }
            subVariations.addAll(deeperSubs)
        }
    }
    return subVariations
}
