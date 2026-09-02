package com.example.chessboard.ui.screen.fenpositions.continuations

/*
 * File role: owns one manually authored continuation for the add-continuations screen.
 * Allowed here:
 * - source-FEN board setup, current move extraction, SAN presentation, undo, and clear
 * Not allowed here:
 * - Compose layout, pasted-text parsing, duplicate checks, persistence, dialogs, or navigation
 * Validation date: 2026-09-02
 */

import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.boardmodel.buildUciFromChesslibMove
import com.example.chessboard.service.buildMoveLabels
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.screen.fenpositions.resolveFenPositionBoardOrientation
import com.example.chessboard.ui.screen.fenpositions.toLoadableFenPosition

internal class FenPositionContinuationManualInput(
    startFen: String,
) {
    private val normalizedStartFen = startFen.trim()
    private val loadableStartFen = toLoadableFenPosition(normalizedStartFen)

    val lineController = LineController(resolveFenPositionBoardOrientation(normalizedStartFen)).also {
        require(it.loadFromFen(loadableStartFen)) {
            "Cannot initialize manual continuation from FEN: $normalizedStartFen"
        }
    }

    val uciMoves: List<String>
        get() {
            return lineController.getMovesCopy()
                .take(lineController.currentMoveIndex)
                .map(::buildUciFromChesslibMove)
        }

    val sanLine: String
        get() {
            return buildFenPositionContinuationSanLine(
                uciMoves = uciMoves,
                startFen = normalizedStartFen,
            )
        }

    val canUndo: Boolean
        get() = lineController.canUndo

    val canClear: Boolean
        get() = lineController.currentMoveIndex > 0

    val canSave: Boolean
        get() = lineController.currentMoveIndex > 0

    fun undo(): Boolean {
        return lineController.undoMove()
    }

    fun clear() {
        check(lineController.loadFromFen(loadableStartFen)) {
            "Cannot reset manual continuation to FEN: $normalizedStartFen"
        }
    }
}

internal fun buildFenPositionContinuationSanLine(
    uciMoves: List<String>,
    startFen: String,
): String {
    if (uciMoves.isEmpty()) {
        return ""
    }

    val sanLabels = buildMoveLabels(
        uciMoves = uciMoves,
        startFen = startFen,
    )
    check(sanLabels.size == uciMoves.size) {
        "Cannot build SAN for the complete FEN position continuation"
    }

    val startsWithBlack = resolveFenPositionBoardOrientation(startFen) == BoardOrientation.BLACK
    return buildString {
        sanLabels.forEachIndexed { index, label ->
            if (isNotEmpty()) {
                append(' ')
            }

            val absolutePly = index + if (startsWithBlack) 1 else 0
            val moveNumber = absolutePly / 2 + 1
            val isWhiteMove = absolutePly % 2 == 0
            when {
                isWhiteMove -> append("$moveNumber. ")
                index == 0 -> append("$moveNumber... ")
            }
            append(label)
        }
    }
}
