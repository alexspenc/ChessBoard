package com.example.chessboard.ui.screen

import com.example.chessboard.ui.components.AppIconSizes
import com.example.chessboard.ui.components.DeleteIconButton
import com.example.chessboard.ui.components.IconLg
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.IconSm
import com.example.chessboard.ui.components.IconXs
import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.ui.GameEditorMoveSequenceSectionTestTag
import com.example.chessboard.ui.GameEditorNextTestTag
import com.example.chessboard.ui.GameEditorPreviousTestTag
import com.example.chessboard.ui.GameEditorScrollContainerTestTag
import com.example.chessboard.ui.components.AppConfirmDialog
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppDivider
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingIconInactive
import com.example.chessboard.ui.theme.TrainingTextPrimary
import com.example.chessboard.ui.components.ChessBoardSection
import com.example.chessboard.ui.screen.training.DarkInputField
import com.example.chessboard.ui.components.MoveChip
import com.example.chessboard.service.computeLabel
import com.example.chessboard.service.parsePgnMoves
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GameEditorScreenContainer(
    activity: Activity,
    game: GameEntity,
    screenContext: ScreenContainerContext,
    modifier: Modifier = Modifier,
) {
    val dbProvider = screenContext.inDbProvider
    val gameController = remember { GameController() }
    val fenHistory = remember { mutableStateListOf<String>() }
    val moveLabels = remember { mutableStateListOf<String>() }
    var isLoading by remember { mutableStateOf(true) }

    // Parse PGN and replay moves into controller
    LaunchedEffect(game.id) {
        val uciMoves = withContext(Dispatchers.Default) { parsePgnMoves(game.pgn) }

        gameController.resetToStartPosition()
        gameController.setOrientation(EditableGameSide.fromSideMask(game.sideMask).orientation)
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

    // Keep fenHistory / moveLabels in sync with the full move list.
    // Undo/redo should not destroy the tail, only real branching should.
    LaunchedEffect(gameController.boardState) {
        if (isLoading) {
            return@LaunchedEffect
        }

        val currentPly = gameController.currentMoveIndex
        val moves = gameController.getMovesCopy()
        val totalPly = moves.size

        while (fenHistory.size > totalPly + 1) {
            fenHistory.removeAt(fenHistory.size - 1)
        }
        while (moveLabels.size > totalPly) {
            moveLabels.removeAt(moveLabels.size - 1)
        }

        if (currentPly != totalPly) {
            return@LaunchedEffect
        }

        if (moveLabels.size < totalPly) {
            val prevFen = fenHistory.getOrNull(totalPly - 1)
            val lastMove = moves.getOrNull(totalPly - 1)
            if (prevFen != null && lastMove != null) {
                val label = withContext(Dispatchers.Default) { computeLabel(lastMove, prevFen) }
                moveLabels.add(label)
            }
        }

        if (fenHistory.size < totalPly + 1) {
            fenHistory.add(gameController.getFen())
        }
    }

    GameEditorScreen(
        game = game,
        gameController = gameController,
        moveLabels = moveLabels,
        isLoading = isLoading,
        onBackClick = screenContext.onBackClick,
        onNavigate = screenContext.onNavigate,
        onSave = { name, eco, selectedSide ->
            val idx = gameController.currentMoveIndex
            val pgn = gameController.generatePgn(
                event = name.ifBlank { "Opening" },
                upToIndex = idx
            )
            val updatedGame = game.copy(
                event = name.ifBlank { null },
                eco = eco.ifBlank { null },
                pgn = pgn,
                sideMask = selectedSide.sideMask
            )
            val updatedMoves = gameController.getMovesCopy().take(idx)

            (activity as? LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.IO) {
                val updated = dbProvider.updateGame(
                    game = updatedGame,
                    moves = updatedMoves
                )

                if (!updated) {
                    return@launch
                }

                withContext(Dispatchers.Main) { screenContext.onBackClick() }
            }
        },
        onDelete = {
            (activity as? LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.IO) {
                dbProvider.deleteGame(game.id)
                withContext(Dispatchers.Main) { screenContext.onBackClick() }
            }
        },
        modifier = modifier
    )
}

private fun goToPly(
    gameController: GameController,
    currentPly: Int,
    targetPly: Int
) {
    if (targetPly == currentPly) {
        return
    }

    val safeTargetPly = targetPly.coerceAtLeast(0)
    val diff = safeTargetPly - currentPly
    if (diff > 0) {
        repeat(diff) { gameController.redoMove() }
        return
    }

    repeat(-diff) { gameController.undoMove() }
}

private fun goToStart(
    gameController: GameController,
    currentPly: Int
) {
    if (currentPly == 0) {
        return
    }

    goToPly(
        gameController = gameController,
        currentPly = currentPly,
        targetPly = 0
    )
}

private fun goToEnd(
    gameController: GameController,
    currentPly: Int,
    totalPly: Int
) {
    if (currentPly == totalPly) {
        return
    }

    goToPly(
        gameController = gameController,
        currentPly = currentPly,
        targetPly = totalPly
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
    onNavigate: (ScreenType) -> Unit = {},
    onSave: (name: String, eco: String, selectedSide: EditableGameSide) -> Unit = { _, _, _ -> },
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    @Suppress("UNUSED_VARIABLE")
    val boardState = gameController.boardState
    val currentPly = gameController.currentMoveIndex
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editedName by remember(game.id) { mutableStateOf(game.event ?: "") }
    var editedEco by remember(game.id) { mutableStateOf(game.eco ?: "") }
    var selectedSide by remember(game.id) { mutableStateOf(EditableGameSide.fromSideMask(game.sideMask)) }

    if (showDeleteDialog) {
        AppConfirmDialog(
            title = "Delete Opening",
            message = "Delete \"${game.event ?: "this opening"}\"? This cannot be undone.",
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            confirmText = "Delete",
            isDestructive = true
        )
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = editedName.ifBlank { "Opening" },
                subtitle = editedEco.ifBlank { null },
                onBackClick = onBackClick,
                actions = {
                    DeleteIconButton(onClick = { showDeleteDialog = true })
                    IconButton(onClick = { onSave(editedName, editedEco, selectedSide) }) {
                        IconMd(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save",
                            tint = TrainingAccentTeal,
                        )
                    }
                }
            )
        },
        bottomBar = {
            AppBottomNavigation(
                items = defaultAppBottomNavigationItems(),
                selectedItem = ScreenType.GamesExplorer,
                onItemSelected = onNavigate
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
                    .testTag(GameEditorScrollContainerTestTag)
                    .verticalScroll(rememberScrollState())
            ) {
                ChessBoardSection(gameController = gameController, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(AppDimens.spaceMd))

                Column(
                    modifier = Modifier.padding(horizontal = AppDimens.spaceLg)
                ) {
                    GameSideSelector(
                        selectedSide = selectedSide,
                        onSideSelected = {
                            selectedSide = it
                            gameController.setOrientation(it.orientation)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(AppDimens.spaceMd))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = AppDimens.spaceLg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔒", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(AppDimens.radiusXs))
                        SectionTitleText("Move Sequence", color = TextColor.Secondary)
                    }
                    CardMetaText("Move $currentPly")
                }

                Spacer(modifier = Modifier.height(AppDimens.spaceSm))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = AppDimens.spaceLg)
                        .testTag(GameEditorMoveSequenceSectionTestTag)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.radiusXs),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.radiusXs)
                    ) {
                        moveLabels.forEachIndexed { index, label ->
                            val ply = index + 1
                            val moveNumber = index / 2 + 1
                            val prefix = if (index % 2 == 0) "$moveNumber." else "$moveNumber..."
                            MoveChip(
                                label = "$prefix$label",
                                isSelected = ply == currentPly,
                                onClick = {
                                    goToPly(
                                        gameController = gameController,
                                        currentPly = currentPly,
                                        targetPly = ply
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimens.spaceSm))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = AppDimens.spaceLg),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { gameController.undoMove() },
                        enabled = gameController.canUndo,
                        modifier = Modifier
                            .testTag(GameEditorPreviousTestTag)
                            .semantics { contentDescription = "Previous" }
                    ) {
                        IconMd(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = null,
                            tint = if (gameController.canUndo) TrainingTextPrimary else TrainingIconInactive,
                        )
                    }
                    TextButton(
                        onClick = {
                            goToStart(
                                gameController = gameController,
                                currentPly = currentPly
                            )
                        },
                        enabled = gameController.canUndo
                    ) {
                        CardMetaText("<<")
                    }
                    TextButton(
                        onClick = {
                            goToStart(
                                gameController = gameController,
                                currentPly = currentPly
                            )
                        },
                        enabled = gameController.canUndo
                    ) {
                        CardMetaText("Reset")
                    }
                    TextButton(
                        onClick = {
                            goToEnd(
                                gameController = gameController,
                                currentPly = currentPly,
                                totalPly = moveLabels.size
                            )
                        },
                        enabled = gameController.canRedo
                    ) {
                        CardMetaText(">>")
                    }
                    IconButton(
                        onClick = { gameController.redoMove() },
                        enabled = gameController.canRedo,
                        modifier = Modifier
                            .testTag(GameEditorNextTestTag)
                            .semantics { contentDescription = "Next" }
                    ) {
                        IconMd(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = if (gameController.canRedo) TrainingTextPrimary else TrainingIconInactive,
                        )
                    }
                }

                AppDivider(
                    modifier = Modifier.padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceMd)
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
