package com.example.chessboard.ui.screen

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.ui.components.AppDivider
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.NavLabelText
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.ChessBoardTheme
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingBackgroundDark
import com.example.chessboard.ui.theme.TrainingCardDark
import com.example.chessboard.ui.theme.TrainingDividerColor
import com.example.chessboard.ui.theme.TrainingIconInactive
import com.example.chessboard.ui.theme.TrainingSurfaceDark
import com.example.chessboard.ui.theme.TrainingTextPrimary
import com.example.chessboard.ui.theme.TrainingTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ──────────────────────────────────────────────────────────────────────────────
// Container
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun TrainingScreenContainer(
    activity: Activity,
    modifier: Modifier = Modifier,
    inDbProvider: DatabaseProvider,
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
) {
    val gameController = remember { GameController() }
    val parsedGames = remember { mutableStateListOf<ParsedGame>() }
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

    TrainingScreen(
        gameController = gameController,
        parsedGames = parsedGames,
        isLoading = isLoading,
        selectedGameIdx = selectedGameIdx,
        modifier = modifier,
        onBackClick = onBackClick,
        onNavigate = onNavigate,
        onMovePlyClick = { gameIdx, ply ->
            selectedGameIdx = gameIdx
            gameController.loadFromUciMoves(parsedGames[gameIdx].uciMoves, ply)
        }
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// Screen
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    gameController: GameController,
    parsedGames: List<ParsedGame> = emptyList(),
    isLoading: Boolean = false,
    selectedGameIdx: Int = -1,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onMovePlyClick: (gameIdx: Int, ply: Int) -> Unit = { _, _ -> },
) {
    var selectedNavItem by remember { mutableStateOf<ScreenType>(ScreenType.Training) }
    val currentPly = gameController.currentMoveIndex

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = TrainingBackgroundDark,
        topBar = {
            TrainingTopBar(onBackClick = onBackClick)
        },
        bottomBar = {
            TrainingBottomNavigation(
                selectedItem = selectedNavItem,
                onItemSelected = { selectedNavItem = it; onNavigate(it) }
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
                            color = TrainingTextSecondary,
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

// ──────────────────────────────────────────────────────────────────────────────
// Game block
// ──────────────────────────────────────────────────────────────────────────────

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
        color = if (isSelected) TrainingCardDark else TrainingSurfaceDark,
        border = if (isSelected) BorderStroke(1.dp, TrainingAccentTeal) else null
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SectionTitleText(
                    text = parsedGame.game.event ?: "Opening",
                    color = TrainingTextPrimary
                )
                if (!parsedGame.game.eco.isNullOrBlank()) {
                    CardMetaText(
                        text = parsedGame.game.eco,
                        color = TrainingTextSecondary
                    )
                }
            }
            CardMetaText(
                text = "${parsedGame.moveLabels.size} moves",
                color = TrainingTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(AppDimens.spaceSm))

        // Move sequence chips
        if (parsedGame.moveLabels.isEmpty()) {
            BodySecondaryText(
                text = "No moves recorded",
                color = TrainingTextSecondary
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.radiusXs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                parsedGame.moveLabels.forEachIndexed { index, label ->
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

        // Navigation controls – only shown for the selected game
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
                    CardMetaText(
                        text = "Reset",
                        color = TrainingTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
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

// ──────────────────────────────────────────────────────────────────────────────
// Top bar
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainingTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
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
                    .background(color = TrainingSurfaceDark, shape = RoundedCornerShape(AppDimens.radiusMd))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TrainingTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        title = {
            ScreenTitleText(
                text = "Training",
                color = TrainingTextPrimary
            )
        }
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// Bottom navigation
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun TrainingBottomNavigation(
    selectedItem: ScreenType,
    onItemSelected: (ScreenType) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem(ScreenType.Home, Icons.Outlined.Home, Icons.Filled.Home),
        BottomNavItem(ScreenType.Training, Icons.Outlined.AccountBox, Icons.Filled.AccountBox),
        BottomNavItem(ScreenType.Stats, Icons.Outlined.Info, Icons.Filled.Info),
        BottomNavItem(ScreenType.Profile, Icons.Outlined.Person, Icons.Filled.Person)
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = TrainingSurfaceDark,
        tonalElevation = 8.dp
    ) {
        Column {
            AppDivider(color = TrainingDividerColor)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppDimens.spaceSm),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                items.forEach { item ->
                    BottomNavItemView(
                        item = item,
                        isSelected = selectedItem == item.label,
                        onClick = { onItemSelected(item.label) }
                    )
                }
            }
        }
    }
}

private data class BottomNavItem(
    val label: ScreenType,
    val iconUnselected: ImageVector,
    val iconSelected: ImageVector
)

@Composable
private fun BottomNavItemView(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (isSelected) TrainingAccentTeal else TrainingIconInactive
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(AppDimens.spaceSm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (isSelected) item.iconSelected else item.iconUnselected,
            contentDescription = item.label.toString(),
            tint = color,
            modifier = Modifier.size(AppDimens.navIconSize)
        )
        Spacer(modifier = Modifier.height(AppDimens.spaceXs))
        NavLabelText(
            text = item.label.toString(),
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Preview
// ──────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TrainingScreenPreview() {
    ChessBoardTheme {
        TrainingScreen(gameController = GameController())
    }
}
