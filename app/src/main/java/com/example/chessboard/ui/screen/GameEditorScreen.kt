package com.example.chessboard.ui.screen

import android.app.Activity
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.theme.*
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        onSave = { name, eco ->
            val idx = gameController.currentMoveIndex
            val pgn = gameController.generatePgn(
                event = name.ifBlank { "Opening" },
                upToIndex = idx
            )
            (activity as? LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.IO) {
                dbProvider.updateGamePgn(game.id, pgn)
                dbProvider.updateGameMeta(game.id, name.ifBlank { null }, eco.ifBlank { null })
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
    onSave: (name: String, eco: String) -> Unit = { _, _ -> },
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentPly = gameController.currentMoveIndex
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editedName by remember(game.id) { mutableStateOf(game.event ?: "") }
    var editedEco by remember(game.id) { mutableStateOf(game.eco ?: "") }

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
                PrimaryButton("Delete", onClick = { showDeleteDialog = false; onDelete() })
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
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(start = AppDimens.spaceSm)
                            .size(AppDimens.iconButtonSize)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                            tint = TrainingTextPrimary, modifier = Modifier.size(20.dp)
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = editedName.ifBlank { "Opening" },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TrainingTextPrimary
                        )
                        if (editedEco.isNotBlank()) Text(editedEco, fontSize = 12.sp, color = TrainingTextSecondary)
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TrainingErrorRed)
                    }
                    PrimaryButton("Save", onClick = { onSave(editedName, editedEco) })
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TrainingAccentTeal)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                ChessBoardSection(gameController = gameController, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(AppDimens.spaceMd))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = AppDimens.spaceLg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔒", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(AppDimens.radiusXs))
                        Text("Move Sequence", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TrainingTextSecondary)
                    }
                    Text("Move $currentPly", fontSize = 12.sp, color = TrainingTextSecondary)
                }

                Spacer(modifier = Modifier.height(AppDimens.spaceSm))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = AppDimens.spaceLg),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.radiusXs),
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

                Spacer(modifier = Modifier.height(AppDimens.spaceSm))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = AppDimens.spaceLg),
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

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceMd),
                    color = TrainingDividerColor
                )

                DarkInputField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = "Opening Name",
                    placeholder = "e.g., Sicilian Defense",
                    modifier = Modifier.padding(horizontal = AppDimens.spaceLg)
                )

                Spacer(modifier = Modifier.height(AppDimens.spaceMd))

                DarkInputField(
                    value = editedEco,
                    onValueChange = { editedEco = it },
                    label = "ECO Code",
                    placeholder = "e.g., B20",
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .padding(start = AppDimens.spaceLg)
                )

                Spacer(modifier = Modifier.height(AppDimens.spaceXl))
            }
        }
    }
}
