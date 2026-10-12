package com.example.chessboard.chesscore

/**
 * Expands tokenized PGN variations into complete SAN token lines.
 * Contains variation structure validation and move-number-based branch positioning.
 * Does not contain tokenization, SAN replay, position access, app strings, UI, or persistence.
 * Validation date: 2026-10-12.
 */

import com.example.chessboard.chesscore.model.Side
import kotlin.collections.ArrayDeque

/**
 * Expands PGN move-text tokens into SAN lines, preserving equal branches.
 *
 * Unnumbered variations replace the preceding move. Numbered variations use move numbers relative
 * to the fragment's first move number and [startingSide]. No chess move legality is checked.
 * @param pgnTokens tokens from [tokenizePgnMoveText], for example
 * `listOf("1.", "e4", "e5", "(", "1...", "c5", ")", "2.", "Nf3")`.
 * @param startingSide side to move in the supplied starting position; no FEN counters are needed.
 * @return full SAN lines in variation-closing order, with the main line last. For the example above:
 * `listOf(listOf("e4", "c5"), listOf("e4", "e5", "Nf3"))`.
 * Move numbers, NAG tokens, parentheses, and results are excluded; duplicate lines are preserved.
 * @throws PgnVariationParseException when a variation precedes its containing line's first own
 * move, contains no own moves, or has a NAG before its first own move.
 */
fun extractSanLines(
    pgnTokens: List<String>,
    startingSide: Side,
): List<List<String>> {
    val initialAbsolutePly = resolveInitialAbsolutePly(
        tokens = pgnTokens,
        sideToMove = startingSide,
    )

    data class BranchState(
        val line: List<String>,
        val ownMoveCount: Int,
    )

    val lines = mutableListOf<List<String>>()
    val branchStack = ArrayDeque<BranchState>()
    var currentLine = mutableListOf<String>()
    var ownMoveCount = 0

    for (i in pgnTokens.indices) {
        val token = pgnTokens[i]
        when {
            token == "(" -> {
                if (ownMoveCount == 0) {
                    throw PgnVariationParseException(
                        token = token,
                        tokenIndex = i,
                        reason = PgnVariationParseErrorReason.MISSING_PRECEDING_MOVE,
                    )
                }
                branchStack.addLast(BranchState(currentLine.toList(), ownMoveCount))
                // Interpret a variation move number relative to the copied fragment's first number.
                // This keeps fragments such as "23..." independent from unavailable FEN counters.
                val targetPly = resolveVariationStartPly(
                    currentLine = currentLine,
                    firstVariationToken = pgnTokens.getOrNull(i + 1),
                    initialAbsolutePly = initialAbsolutePly,
                )
                currentLine = currentLine.take(targetPly).toMutableList()
                ownMoveCount = 0
                continue
            }
            token == ")" -> {
                if (branchStack.isNotEmpty() && ownMoveCount == 0) {
                    throw PgnVariationParseException(
                        token = token,
                        tokenIndex = i,
                        reason = PgnVariationParseErrorReason.EMPTY_VARIATION,
                    )
                }
                if (currentLine.isNotEmpty()) lines.add(currentLine.toList())
                val parent = branchStack.removeLastOrNull()
                currentLine = parent?.line?.toMutableList() ?: mutableListOf()
                ownMoveCount = parent?.ownMoveCount ?: 0
                continue
            }
            token.startsWith("$") -> {
                if (branchStack.isNotEmpty() && ownMoveCount == 0) {
                    throw PgnVariationParseException(
                        token = token,
                        tokenIndex = i,
                        reason = PgnVariationParseErrorReason.NAG_BEFORE_FIRST_MOVE,
                    )
                }
                continue
            }
            isPgnMoveNumberToken(token) || isPgnResultToken(token) -> {
                // Move numbers and result tokens do not count as moves in a variation.
                continue
            }
        }

        currentLine.add(token)
        ownMoveCount++
    }

    if (currentLine.isNotEmpty()) lines.add(currentLine.toList())
    return lines
}

private fun resolveInitialAbsolutePly(
    tokens: List<String>,
    sideToMove: Side,
): Int {
    var firstMoveNumber = 1
    if (tokens.isNotEmpty()) {
        val parsedMoveNumber = parsePgnMoveNumber(tokens[0])
        if (parsedMoveNumber != null) {
            firstMoveNumber = parsedMoveNumber.number
        }
    }

    var sideOffset = 0
    if (sideToMove == Side.BLACK) {
        sideOffset = 1
    }

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
