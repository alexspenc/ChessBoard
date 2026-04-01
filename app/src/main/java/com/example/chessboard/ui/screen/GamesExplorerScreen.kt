package com.example.chessboard.ui.screen

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppConfirmDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingErrorRed
import com.example.chessboard.ui.theme.TrainingIconInactive
import com.example.chessboard.ui.theme.TrainingTextPrimary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GamesExplorerScreenContainer(
    modifier: Modifier = Modifier,
    screenContext: ScreenContainerContext,
) {
    val inDbProvider = screenContext.inDbProvider
    val gameController = remember { GameController() }
    val parsedGames = remember { mutableStateListOf<ParsedGame>() }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var selectedGameIdx by remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        val games = withContext(Dispatchers.IO) { inDbProvider.getAllGames() }
        val parsed = withContext(Dispatchers.Default) {
            games.map { game ->
                val uciMoves = parsePgnMoves(game.pgn)
                ParsedGame(game, uciMoves, buildMoveLabels(uciMoves))
            }
        }
        parsedGames.addAll(parsed)
        isLoading = false
    }

    GamesExplorerScreen(
        gameController = gameController,
        parsedGames = parsedGames,
        isLoading = isLoading,
        selectedGameIdx = selectedGameIdx,
        modifier = modifier,
        onBackClick = screenContext.onBackClick,
        onNavigate = screenContext.onNavigate,
        onMovePlyClick = { gameIdx, ply ->
            selectedGameIdx = gameIdx
            gameController.loadFromUciMoves(parsedGames[gameIdx].uciMoves, ply)
        },

        onDeleteGameClick = createDeleteGameAction(
            scope = scope,
            inDbProvider = inDbProvider,
            parsedGames = parsedGames,
            gameController = gameController,
            selectedGameIdx = { selectedGameIdx },
            onSelectedGameIdxChange = { selectedGameIdx = it }
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesExplorerScreen(
    gameController: GameController,
    parsedGames: List<ParsedGame> = emptyList(),
    isLoading: Boolean = false,
    selectedGameIdx: Int = -1,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onMovePlyClick: (gameIdx: Int, ply: Int) -> Unit = { _, _ -> },
    onDeleteGameClick: (gameId: Long) -> Unit = {},
) {
    val currentPly = gameController.currentMoveIndex
    val selectedGame = resolveSelectedGame(
        parsedGames = parsedGames,
        selectedGameIdx = selectedGameIdx
    )
    var showDeleteDialog by remember(selectedGame?.game?.id) { mutableStateOf(false) }

    if (showDeleteDialog && selectedGame != null) {
        AppConfirmDialog(
            title = "Delete Game",
            message = resolveDeleteGameMessage(selectedGame),
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                onDeleteGameClick(selectedGame.game.id)
            },
            confirmText = "Delete",
            isDestructive = true
        )
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Games Explorer",
                onBackClick = onBackClick,
                filledBackButton = true,
                actions = {
                    if (selectedGame != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete game",
                                tint = TrainingErrorRed
                            )
                        }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.spaceLg)
        ) {
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))

            ChessBoardSection(gameController = gameController)

            Spacer(modifier = Modifier.height(AppDimens.spaceLg))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = TrainingAccentTeal)
                    }
                }

                parsedGames.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BodySecondaryText(
                            text = "No saved games.\nGo to Home to create openings.",
                            color = TextColor.Secondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    parsedGames.forEachIndexed { gameIdx, parsedGame ->
                        val isSelected = gameIdx == selectedGameIdx
                        GameBlock(
                            parsedGame = parsedGame,
                            isSelected = isSelected,
                            currentPly = if (isSelected) currentPly else 0,
                            canUndo = isSelected && gameController.canUndo,
                            canRedo = isSelected && gameController.canRedo,
                            onMovePlyClick = { ply -> onMovePlyClick(gameIdx, ply) },
                            onPrevClick = { gameController.undoMove() },
                            onNextClick = { gameController.redoMove() },
                            onResetClick = { onMovePlyClick(gameIdx, 0) }
                        )
                        Spacer(modifier = Modifier.height(AppDimens.spaceMd))
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceXl))
        }
    }
}

@Composable
private fun GameBlock(
    parsedGame: ParsedGame,
    isSelected: Boolean,
    currentPly: Int,
    canUndo: Boolean,
    canRedo: Boolean,
    onMovePlyClick: (ply: Int) -> Unit,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CardSurface(
        modifier = modifier.fillMaxWidth(),
        color = if (isSelected) Background.CardDark else Background.SurfaceDark,
        border = if (isSelected) BorderStroke(1.dp, TrainingAccentTeal) else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SectionTitleText(
                    text = parsedGame.game.event ?: "Opening"
                )
                GameBlockMetaRow(
                    eco = parsedGame.game.eco,
                    gameId = parsedGame.game.id
                )
            }
            CardMetaText(text = "${parsedGame.moveLabels.size} moves")
        }

        Spacer(modifier = Modifier.height(AppDimens.spaceSm))

        GameMoveChips(
            moveLabels = parsedGame.moveLabels,
            isSelected = isSelected,
            currentPly = currentPly,
            onMovePlyClick = onMovePlyClick
        )

        if (isSelected) {
            Spacer(modifier = Modifier.height(AppDimens.spaceSm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevClick, enabled = canUndo) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous",
                        tint = if (canUndo) TrainingTextPrimary else TrainingIconInactive,
                        modifier = Modifier.size(28.dp)
                    )
                }
                TextButton(onClick = onResetClick) {
                    CardMetaText(text = "Reset")
                }
                IconButton(onClick = onNextClick, enabled = canRedo) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next",
                        tint = if (canRedo) TrainingTextPrimary else TrainingIconInactive,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun GameBlockMetaRow(
    eco: String?,
    gameId: Long
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!eco.isNullOrBlank()) {
            CardMetaText(text = eco)
        }

        CardMetaText(text = "ID: $gameId")
    }
}


@Composable
private fun GameMoveChips(
    moveLabels: List<String>,
    isSelected: Boolean,
    currentPly: Int,
    onMovePlyClick: (Int) -> Unit
) {
    if (moveLabels.isEmpty()) {
        BodySecondaryText(text = "No moves recorded")
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.radiusXs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        moveLabels.forEachIndexed { index, label ->
            val ply = index + 1
            val moveNumber = index / 2 + 1
            val prefix = if (index % 2 == 0) "$moveNumber." else "$moveNumber..."
            MoveChip(
                label = "$prefix$label",
                isSelected = isSelected && ply == currentPly,
                onClick = { onMovePlyClick(ply) }
            )
        }
    }
}

private fun createDeleteGameAction(
    scope: CoroutineScope,
    inDbProvider: DatabaseProvider,
    parsedGames: SnapshotStateList<ParsedGame>,
    gameController: GameController,
    selectedGameIdx: () -> Int,
    onSelectedGameIdxChange: (Int) -> Unit
): (Long) -> Unit {
    return { gameId ->
        scope.launch {
            withContext(Dispatchers.IO) {
                inDbProvider.deleteGame(gameId)
            }

            val deletedGameIdx = parsedGames.indexOfFirst { it.game.id == gameId }
            if (deletedGameIdx < 0) {
                return@launch
            }

            parsedGames.removeAt(deletedGameIdx)
            if (selectedGameIdx() == deletedGameIdx) {
                onSelectedGameIdxChange(-1)
                gameController.resetToStartPosition()
                return@launch
            }

            if (selectedGameIdx() > deletedGameIdx) {
                onSelectedGameIdxChange(selectedGameIdx() - 1)
            }
        }
    }
}

private fun resolveSelectedGame(
    parsedGames: List<ParsedGame>,
    selectedGameIdx: Int
): ParsedGame? {
    if (selectedGameIdx !in parsedGames.indices) {
        return null
    }

    return parsedGames[selectedGameIdx]
}

private fun resolveDeleteGameMessage(parsedGame: ParsedGame): String {
    val gameName = parsedGame.game.event ?: "this game"
    return "Delete \"$gameName\"?\nGame ID: ${parsedGame.game.id}"
}
