package com.example.chessboard.ui.screen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.ui.theme.*
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Parses UCI move tokens from stored PGN (e.g. "1. e2e4 e7e5 2. g1f3 *")
private fun parsePgnMoves(pgn: String): List<String> {
    val uciRegex = Regex("[a-h][1-8][a-h][1-8][qrbnQRBN]?")
    return pgn.lines()
        .filterNot { it.trim().startsWith("[") }
        .joinToString(" ")
        .split("\\s+".toRegex())
        .filter { uciRegex.matches(it) }
}

private fun computeLabel(move: Move, boardBeforeFen: String): String {
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

@Composable
fun GameEditorScreenContainer(
    activity: Activity,
    game: GameEntity,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    inDbProvider : DatabaseProvider,
) {
    val dbProvider = inDbProvider
    val gameController = remember { GameController() }
    val fenHistory = remember { mutableStateListOf<String>() }
    val moveLabels = remember { mutableStateListOf<String>() }
    var isLoading by remember { mutableStateOf(true) }

    // Parse PGN and replay moves into controller
    LaunchedEffect(game.id) {
        val uciMoves = withContext(Dispatchers.Default) { parsePgnMoves(game.pgn) }

        gameController.resetToStartPosition()
        fenHistory.clear()
        fenHistory.add(gameController.getFen())
        moveLabels.clear()

        for (uci in uciMoves) {
            val from = uci.take(2)
            val to = uci.drop(2).take(2)
            val prevFen = fenHistory.last()
            try {
                val move = Move(Square.fromValue(from.uppercase()), Square.fromValue(to.uppercase()))
                val label = withContext(Dispatchers.Default) { computeLabel(move, prevFen) }
                if (gameController.tryMove(from, to)) {
                    fenHistory.add(gameController.getFen())
                    moveLabels.add(label)
                }
            } catch (_: Exception) {}
        }
        isLoading = false
    }

    // Keep fenHistory / moveLabels in sync when user makes or undoes moves on the board
    LaunchedEffect(gameController.boardState) {
        if (isLoading) return@LaunchedEffect
        val idx = gameController.currentMoveIndex
        val currentFen = gameController.getFen()

        // Trim tail when user went back then made a new move
        while (fenHistory.size > idx + 1) fenHistory.removeAt(fenHistory.size - 1)
        while (moveLabels.size > idx) moveLabels.removeAt(moveLabels.size - 1)

        when {
            fenHistory.size == idx -> {
                // New move added
                val prevFen = fenHistory.getOrNull(idx - 1)
                val lastMove = gameController.getMovesCopy().getOrNull(idx - 1)
                fenHistory.add(currentFen)
                if (prevFen != null && lastMove != null) {
                    val label = withContext(Dispatchers.Default) { computeLabel(lastMove, prevFen) }
                    moveLabels.add(label)
                }
            }
            fenHistory.size == idx + 1 -> fenHistory[idx] = currentFen
        }
    }

    GameEditorScreen(
        game = game,
        gameController = gameController,
        moveLabels = moveLabels,
        isLoading = isLoading,
        onBackClick = onBackClick,
        onSave = {
            val idx = gameController.currentMoveIndex
            val pgn = gameController.generatePgn(
                event = game.event ?: "Opening",
                upToIndex = idx
            )
            (activity as? LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.IO) {
                dbProvider.updateGamePgn(game.id, pgn)
                withContext(Dispatchers.Main) { onBackClick() }
            }
        },
        onDelete = {
            (activity as? LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.IO) {
                dbProvider.deleteGame(game.id)
                withContext(Dispatchers.Main) { onBackClick() }
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameEditorScreen(
    game: GameEntity,
    gameController: GameController,
    moveLabels: List<String>,
    isLoading: Boolean,
    onBackClick: () -> Unit = {},
    onSave: () -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentPly = gameController.currentMoveIndex
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = TrainingCardDark,
            title = { Text("Delete Opening", color = TrainingTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Delete \"${game.event ?: "this opening"}\"? This cannot be undone.",
                    color = TrainingTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { showDeleteDialog = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = TrainingErrorRed)
                ) { Text("Delete", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TrainingTextSecondary)
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = TrainingBackgroundDark,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TrainingBackgroundDark,
                    navigationIconContentColor = TrainingTextPrimary,
                    titleContentColor = TrainingTextPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.padding(start = 8.dp).size(40.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                            tint = TrainingTextPrimary, modifier = Modifier.size(20.dp)
                        )
                    }
                },
                title = {
                    Column {
                        Text(game.event ?: "Opening", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TrainingTextPrimary)
                        if (!game.eco.isNullOrBlank()) Text(game.eco, fontSize = 12.sp, color = TrainingTextSecondary)
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TrainingErrorRed)
                    }
                    Button(
                        onClick = onSave,
                        modifier = Modifier.padding(end = 12.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TrainingAccentTeal),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TrainingAccentTeal)
                }
            } else {
                ChessBoardSection(gameController = gameController, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔒", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Move Sequence", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TrainingTextSecondary)
                    }
                    Text("Move $currentPly", fontSize = 12.sp, color = TrainingTextSecondary)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    moveLabels.forEachIndexed { index, label ->
                        val ply = index + 1
                        val moveNumber = index / 2 + 1
                        val prefix = if (index % 2 == 0) "$moveNumber." else "$moveNumber..."
                        MoveChip(
                            label = "$prefix$label",
                            isSelected = ply == currentPly,
                            onClick = {
                                val diff = ply - currentPly
                                if (diff > 0) repeat(diff) { gameController.redoMove() }
                                else if (diff < 0) repeat(-diff) { gameController.undoMove() }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { gameController.undoMove() }, enabled = gameController.canUndo) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous",
                            tint = if (gameController.canUndo) TrainingTextPrimary else TrainingIconInactive,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    TextButton(onClick = { repeat(currentPly) { gameController.undoMove() } }) {
                        Text("Reset", color = TrainingTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    IconButton(onClick = { gameController.redoMove() }, enabled = gameController.canRedo) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next",
                            tint = if (gameController.canRedo) TrainingTextPrimary else TrainingIconInactive,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoveChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) TrainingAccentTeal else TrainingSurfaceDark)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else TrainingTextSecondary
        )
    }
}
