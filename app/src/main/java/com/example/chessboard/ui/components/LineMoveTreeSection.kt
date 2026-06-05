package com.example.chessboard.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.chessboard.R
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.ui.MoveTreeBoxTestTag
import com.example.chessboard.ui.MoveTreeContentTestTag
import com.example.chessboard.ui.moveChipTestTag
import com.example.chessboard.ui.moveTreeRowTestTag
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.MutedContentColor
import com.github.bhlangonijr.chesslib.Piece

private fun resolveVisibleMoveLines(
    importedUciLines: List<List<String>>,
    authoredUciLine: List<String>,
): List<List<String>> {
    if (authoredUciLine.isEmpty()) {
        return importedUciLines
    }

    if (importedUciLines.any { it == authoredUciLine }) {
        return importedUciLines
    }

    return importedUciLines + listOf(authoredUciLine)
}

private fun resolveUciLine(
    lineController: LineController,
    upToPly: Int = lineController.getMovesCopy().size,
): List<String> {
    return lineController.getMovesCopy().take(upToPly).map { move ->
        buildString {
            append(move.from.value().lowercase())
            append(move.to.value().lowercase())
            if (move.promotion != Piece.NONE) {
                append(move.promotion.pieceType.name.first().lowercaseChar())
            }
        }
    }
}

private fun resolveBackingLine(
    visibleLines: List<List<String>>,
    selectedPath: List<String>,
): List<String> {
    return visibleLines.firstOrNull { line ->
        line.size >= selectedPath.size && line.take(selectedPath.size) == selectedPath
    } ?: selectedPath
}

@Composable
fun LineMoveTreeSection(
    importedUciLines: List<List<String>>,
    lineController: LineController,
    modifier: Modifier = Modifier,
    startFen: String? = null,
    maxVisiblePly: Int? = null,
    maxContentHeight: Dp? = null,
    onMoveSelected: ((backingLine: List<String>, targetPly: Int) -> Unit)? = null,
) {
    val emptyText = stringResource(R.string.line_move_tree_empty)
    val boardState = lineController.boardState
    val authoredUciLine = remember(boardState) { resolveUciLine(lineController) }
    val currentPositionPath = remember(boardState, lineController.currentMoveIndex) {
        resolveUciLine(lineController, upToPly = lineController.currentMoveIndex)
    }
    val visibleLines = remember(importedUciLines, boardState) {
        resolveVisibleMoveLines(
            importedUciLines = importedUciLines,
            authoredUciLine = authoredUciLine,
        )
    }
    val segments = remember(visibleLines, startFen) {
        buildMoveTreeData(
            uciLines = visibleLines,
            startFen = startFen,
        )
    }
    val moveTreeContentModifier = rememberMoveTreeContentModifier(maxContentHeight)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
        Surface(
            shape = RoundedCornerShape(AppDimens.radiusMd),
            color = Background.SurfaceDark,
            border = BorderStroke(1.dp, TrainingAccentTeal),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(MoveTreeBoxTestTag),
        ) {
            Column(
                modifier = Modifier
                    .then(moveTreeContentModifier)
                    .padding(14.dp)
                    .testTag(MoveTreeContentTestTag),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (segments.isEmpty() && maxVisiblePly == null) {
                    BodySecondaryText(
                        text = emptyText,
                        color = MutedContentColor,
                    )
                }
                segments.forEachIndexed { segIndex, segment ->
                    when (segment) {
                        is TreeSegment.MainMoves -> {
                            val visibleMoves = if (maxVisiblePly != null) {
                                segment.moves.filter { it.ply < maxVisiblePly }
                            } else {
                                segment.moves
                            }
                            if (visibleMoves.isEmpty()) return@forEachIndexed
                            val isContinuation = segIndex > 0 && segments[segIndex - 1] is TreeSegment.Variation
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.testTag(moveTreeRowTestTag(segIndex)),
                            ) {
                                visibleMoves.forEachIndexed { moveIndex, move ->
                                    val isWhite = move.ply % 2 == 0
                                    val showNumber = isWhite || (moveIndex == 0 && isContinuation)
                                    if (showNumber) {
                                        Text(
                                            text = if (isWhite) "${move.ply / 2 + 1}." else "${move.ply / 2 + 1}...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextColor.Secondary,
                                            modifier = Modifier.align(Alignment.CenterVertically),
                                        )
                                    }
                                    TreeMoveChip(
                                        label = move.label,
                                        isSelected = currentPositionPath == move.uciPath,
                                        onClick = {
                                            val backingLine = resolveBackingLine(visibleLines, move.uciPath)
                                            if (onMoveSelected != null) {
                                                onMoveSelected(backingLine, move.uciPath.size)
                                            } else {
                                                lineController.loadFromUciMoves(
                                                    uciMoves = backingLine,
                                                    targetPly = move.uciPath.size,
                                                    startFen = startFen,
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                        }
                        is TreeSegment.Variation -> {
                            val visibleMoves = if (maxVisiblePly != null) {
                                segment.moves.filter { it.ply < maxVisiblePly }
                            } else {
                                segment.moves
                            }
                            if (visibleMoves.isEmpty()) return@forEachIndexed
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.testTag(moveTreeRowTestTag(segIndex)),
                            ) {
                                Text(
                                    text = "—",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextColor.Secondary,
                                    modifier = Modifier.align(Alignment.CenterVertically),
                                )
                                visibleMoves.forEach { move ->
                                    if (move.ply % 2 == 0) {
                                        Text(
                                            text = "${move.ply / 2 + 1}.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextColor.Secondary,
                                            modifier = Modifier.align(Alignment.CenterVertically),
                                        )
                                    }
                                    TreeMoveChip(
                                        label = move.label,
                                        isSelected = currentPositionPath == move.uciPath,
                                        onClick = {
                                            val backingLine = resolveBackingLine(visibleLines, move.uciPath)
                                            if (onMoveSelected != null) {
                                                onMoveSelected(backingLine, move.uciPath.size)
                                            } else {
                                                lineController.loadFromUciMoves(
                                                    uciMoves = backingLine,
                                                    targetPly = move.uciPath.size,
                                                    startFen = startFen,
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TreeMoveChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isSelected) TrainingAccentTeal else TextColor.Primary,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(moveChipTestTag(label))
            .padding(horizontal = 2.dp, vertical = 3.dp),
    )
}

@Composable
private fun rememberMoveTreeContentModifier(maxContentHeight: Dp?): Modifier {
    val defaultMaxHeight = LocalConfiguration.current.screenHeightDp.dp / 4
    val resolvedMax = maxContentHeight ?: defaultMaxHeight
    return Modifier
        .heightIn(max = resolvedMax)
        .verticalScroll(rememberScrollState())
}
