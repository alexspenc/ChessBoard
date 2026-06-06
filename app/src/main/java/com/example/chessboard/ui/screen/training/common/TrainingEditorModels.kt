package com.example.chessboard.ui.screen.training.common

/*
 * Shared editor models and list operations for training-like collections.
 *
 * Keep reusable item models and pure list helpers here so create/edit screens can
 * share them. Do not add screen-specific Compose UI, loading, or save flows here.
 */

import com.example.chessboard.entity.LineEntity
import com.example.chessboard.entity.SideMask

data class TrainingLineEditorItem(
    val lineId: Long,
    val title: String,
    val weight: Int = 1,
    val eco: String? = null,
    val pgn: String = "",
    val sideMask: Int = SideMask.BOTH
)

internal fun decreaseTrainingLineWeight(
    lines: List<TrainingLineEditorItem>,
    lineId: Long
): List<TrainingLineEditorItem> {
    if (lines.none { it.lineId == lineId }) {
        return lines
    }

    return lines.map { line ->
        if (line.lineId != lineId) {
            return@map line
        }

        return@map line.copy(weight = (line.weight - 1).coerceAtLeast(1))
    }
}

internal fun increaseTrainingLineWeight(
    lines: List<TrainingLineEditorItem>,
    lineId: Long
): List<TrainingLineEditorItem> {
    if (lines.none { it.lineId == lineId }) {
        return lines
    }

    return lines.map { line ->
        if (line.lineId != lineId) {
            return@map line
        }

        return@map line.copy(weight = line.weight + 1)
    }
}

internal fun removeTrainingLine(
    lines: List<TrainingLineEditorItem>,
    lineId: Long
): List<TrainingLineEditorItem> {
    if (lines.none { it.lineId == lineId }) {
        return lines
    }

    return lines.filterNot { line ->
        line.lineId == lineId
    }
}

internal fun resolveNextSelectedTrainingLineId(
    lines: List<TrainingLineEditorItem>,
    removedLineId: Long,
): Long? {
    val removedIndex = lines.indexOfFirst { line -> line.lineId == removedLineId }
    if (removedIndex < 0) {
        return null
    }

    val remainingLines = removeTrainingLine(lines, removedLineId)
    if (remainingLines.isEmpty()) {
        return null
    }

    return remainingLines.getOrNull(removedIndex)?.lineId ?: remainingLines.last().lineId
}

internal fun LineEntity.toTrainingLineEditorItem(
    unnamedOpeningName: String,
    weight: Int = 1,
): TrainingLineEditorItem {
    return TrainingLineEditorItem(
        lineId = id,
        title = event ?: unnamedOpeningName,
        weight = weight,
        eco = eco,
        pgn = pgn,
        sideMask = sideMask
    )
}
