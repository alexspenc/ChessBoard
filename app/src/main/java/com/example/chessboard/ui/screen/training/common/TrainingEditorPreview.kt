package com.example.chessboard.ui.screen.training.common

/**
 * File role: groups reusable preview-session helpers for training-like editors.
 * Allowed here:
 * - parsed move previews, selected-line board state, and preview-board helpers
 * - editor-session utilities reused by training and template editors
 * Not allowed here:
 * - screen scaffolds, save flows, or card layout logic
 * - persistence helpers or training launch orchestration
 * Validation date: 2026-04-25
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.entity.SideMask
import com.example.chessboard.service.buildMoveLabels
import com.example.chessboard.service.parsePgnMoves
import com.example.chessboard.ui.BoardOrientation

internal data class ParsedTrainingEditorLine(
    val uciMoves: List<String>,
    val moveLabels: List<String>
)

internal data class TrainingEditorBoardSession(
    val lineController: LineController,
    val parsedLinesById: Map<Long, ParsedTrainingEditorLine>,
)

@Composable
internal fun rememberTrainingEditorBoardSession(
    lines: List<TrainingLineEditorItem>,
    selectedLineId: Long?,
    selectedPly: Int,
    selectionRevision: Int,
): TrainingEditorBoardSession {
    val lineController = remember { LineController() }
    val lineIds = remember(lines) { lines.map { it.lineId } }
    val linesById = remember(lineIds) {
        lines.associateBy { line -> line.lineId }
    }
    val parsedLinesById = remember(lineIds) {
        lines.associate { line ->
            val uciMoves = parsePgnMoves(line.pgn)
            val moveLabels = buildMoveLabels(uciMoves)
            line.lineId to ParsedTrainingEditorLine(
                uciMoves = uciMoves,
                moveLabels = moveLabels
            )
        }
    }

    fun loadLineAtPly(lineId: Long, ply: Int) {
        val line = linesById[lineId] ?: return
        val parsedLine = parsedLinesById[lineId] ?: return
        lineController.setOrientation(resolveTrainingPreviewBoardOrientation(line))
        lineController.loadFromUciMoves(parsedLine.uciMoves, targetPly = ply)
    }

    SideEffect {
        lineController.setUserMovesEnabled(false)
    }

    LaunchedEffect(selectedLineId, selectedPly, selectionRevision, parsedLinesById) {
        val lineId = selectedLineId ?: return@LaunchedEffect
        loadLineAtPly(lineId, ply = selectedPly)
    }

    return TrainingEditorBoardSession(
        lineController = lineController,
        parsedLinesById = parsedLinesById,
    )
}

internal fun resolveTrainingPreviewBoardOrientation(
    line: TrainingLineEditorItem
): BoardOrientation {
    if (line.sideMask == SideMask.BLACK) {
        return BoardOrientation.BLACK
    }

    return BoardOrientation.WHITE
}
