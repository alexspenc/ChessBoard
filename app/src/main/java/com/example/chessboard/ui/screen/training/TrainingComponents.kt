package com.example.chessboard.ui.screen.training

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.ui.ChessBoardWithCoordinates
import com.example.chessboard.ui.components.AppTextField
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.ButtonColor
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

// ──────────────────────────────────────────────────────────────────────────────
// Shared data
// ──────────────────────────────────────────────────────────────────────────────

data class ParsedGame(
    val game: GameEntity,
    val uciMoves: List<String>,
    val moveLabels: List<String>
)

// ──────────────────────────────────────────────────────────────────────────────
// Shared PGN / label helpers
// ──────────────────────────────────────────────────────────────────────────────

/** Parses UCI move tokens from stored PGN (e.g. "1. e2e4 e7e5 2. g1f3 *"). */
fun parsePgnMoves(pgn: String): List<String> {
    val uciRegex = Regex("[a-h][1-8][a-h][1-8][qrbnQRBN]?")
    return pgn.lines()
        .filterNot { it.trim().startsWith("[") }
        .joinToString(" ")
        .split("\\s+".toRegex())
        .filter { uciRegex.matches(it) }
}

/** Computes an algebraic notation label for [move] given the FEN before the move. */
fun computeLabel(move: Move, boardBeforeFen: String): String {
    val board = Board()
    board.loadFromFen(boardBeforeFen)
    val piece = board.getPiece(move.from)
    val toSquare = move.to.value().lowercase()
    val isCapture = board.getPiece(move.to) != Piece.NONE
    val captureStr = if (isCapture) "x" else ""

    val base = when (piece.pieceType) {
        PieceType.PAWN -> if (isCapture) "${move.from.value()[0].lowercaseChar()}x$toSquare" else toSquare
        PieceType.KNIGHT -> "N$captureStr$toSquare"
        PieceType.BISHOP -> "B$captureStr$toSquare"
        PieceType.ROOK -> "R$captureStr$toSquare"
        PieceType.QUEEN -> "Q$captureStr$toSquare"
        PieceType.KING -> when {
            move.from.value()[0] == 'E' && move.to.value()[0] == 'G' -> "O-O"
            move.from.value()[0] == 'E' && move.to.value()[0] == 'C' -> "O-O-O"
            else -> "K$captureStr$toSquare"
        }
        else -> toSquare
    }

    board.doMove(move)
    val suffix = when {
        board.legalMoves().isEmpty() && board.isKingAttacked -> "#"
        board.isKingAttacked -> "+"
        else -> ""
    }
    return "$base$suffix"
}

/** Replays [uciMoves] and builds a list of algebraic notation labels. */
fun buildMoveLabels(uciMoves: List<String>): List<String> {
    val labels = mutableListOf<String>()
    val board = Board()
    for (uci in uciMoves) {
        val from = uci.take(2)
        val to = uci.drop(2).take(2)
        try {
            val move = Move(Square.fromValue(from.uppercase()), Square.fromValue(to.uppercase()))
            val label = computeLabel(move, board.fen)
            if (board.legalMoves().contains(move)) {
                board.doMove(move)
                labels.add(label)
            }
        } catch (_: Exception) {}
    }
    return labels
}

// ──────────────────────────────────────────────────────────────────────────────
// Shared composables
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun DarkInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    minLines: Int = 1,
) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        modifier = modifier,
        isError = isError,
        minLines = minLines
    )
}

@Composable
fun MoveChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    unselectedBackground: Color = Background.SurfaceDark,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(AppDimens.radiusSm))
            .background(if (isSelected) TrainingAccentTeal else unselectedBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else TextColor.Secondary
        )
    }
}

@Composable
fun ChessBoardSection(
    gameController: GameController,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(AppDimens.radiusXl))
    ) {
        ChessBoardWithCoordinates(
            gameController = gameController,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun TrainingActionButtons(
    onSaveGame: () -> Unit,
    onDatabaseClear: () -> Unit,
    gameController: GameController,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Primary actions: Save game, Clear database
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryButton("Save game", onClick = onSaveGame)
            PrimaryButton("Clear database", onClick = onDatabaseClear)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Secondary actions: Back, Forward, Reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryButton("Back", onClick = { gameController.undoMove() })
            PrimaryButton("Forward", onClick = { gameController.redoMove() })
            PrimaryButton("Reset", onClick = { gameController.resetToStartPosition() })
        }
    }
}

@Composable
fun ResetTrainingButton(
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onResetClick,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(AppDimens.radiusLg),
        colors = ButtonDefaults.buttonColors(
            containerColor = Background.SurfaceDark,
            contentColor = ButtonColor.Content
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        )
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Reset",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        SectionTitleText(text = "Reset Training", color = TextColor.Primary)
    }
}
