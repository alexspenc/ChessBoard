package com.example.chessboard.ui.screen.gameNotation

/**
 * Reusable move-tree UI for screens that show one or more chess lines.
 *
 * Keep move-tree rendering and click-to-position wiring here. Do not add PGN import fields,
 * screen-specific save flows, navigation decisions, or database logic to this file.
 */
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.ui.MoveTreeBoxTestTag
import com.example.chessboard.ui.MoveTreeContentTestTag
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.MoveChip
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.moveTreeRowTestTag
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingIconInactive
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
    gameController: GameController,
    upToPly: Int = gameController.getMovesCopy().size,
): List<String> {
    return gameController.getMovesCopy().take(upToPly).map { move ->
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
internal fun GameMoveTreeSection(
    importedUciLines: List<List<String>>,
    gameController: GameController,
    modifier: Modifier = Modifier,
) {
    val boardState = gameController.boardState
    val authoredUciLine = remember(boardState) { resolveUciLine(gameController) }
    val currentPositionPath = remember(boardState, gameController.currentMoveIndex) {
        resolveUciLine(gameController, upToPly = gameController.currentMoveIndex)
    }
    val visibleLines = remember(importedUciLines, boardState) {
        resolveVisibleMoveLines(
            importedUciLines = importedUciLines,
            authoredUciLine = authoredUciLine,
        )
    }
    val segments = remember(visibleLines) { buildMoveTreeData(visibleLines) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
        SectionTitleText(text = "Move Tree", color = TrainingAccentTeal)
        Surface(
            shape = RoundedCornerShape(AppDimens.radiusMd),
            color = Background.SurfaceDark,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(MoveTreeBoxTestTag),
        ) {
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .testTag(MoveTreeContentTestTag),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (segments.isEmpty()) {
                    BodySecondaryText(
                        text = "No moves yet. Import a PGN or add moves on the board.",
                        color = TrainingIconInactive,
                    )
                }
                segments.forEachIndexed { segIndex, segment ->
                    when (segment) {
                        is TreeSegment.MainMoves -> {
                            val isContinuation = segIndex > 0 && segments[segIndex - 1] is TreeSegment.Variation
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.testTag(moveTreeRowTestTag(segIndex)),
                            ) {
                                segment.moves.forEachIndexed { moveIndex, move ->
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
                                            gameController.loadFromUciMoves(backingLine, move.uciPath.size)
                                        },
                                    )
                                }
                            }
                        }
                        is TreeSegment.Variation -> {
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
                                segment.moves.forEach { move ->
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
                                            gameController.loadFromUciMoves(backingLine, move.uciPath.size)
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
    MoveChip(
        label = label,
        isSelected = isSelected,
        onClick = onClick,
        unselectedBackground = Background.CardDark,
        unselectedTextColor = TextColor.Primary,
        textStyle = MaterialTheme.typography.bodyMedium,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
    )
}
