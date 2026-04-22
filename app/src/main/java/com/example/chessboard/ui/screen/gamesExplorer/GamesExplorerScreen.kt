package com.example.chessboard.ui.screen.gamesExplorer

/**
 * Screen and container for browsing saved games.
 *
 * Keep in this file:
 * - container wiring, loading data, and delete orchestration
 * - screen-level state such as selected game and active filter state
 * - navigation callbacks and integration with shared screen infrastructure
 *
 * It is acceptable to add here:
 * - small screen-local helpers for selection and board orientation
 * - screen-specific rendering flow that coordinates components from this package
 *
 * Do not add here:
 * - reusable visual blocks that belong in package component files
 * - unrelated search/filter helpers for other screens
 * - database logic beyond the narrow container orchestration for this screen
 */
import com.example.chessboard.RuntimeContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.chessboard.boardmodel.GameDraft
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.boardmodel.buildGameDraftFromSourceGame
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.entity.SideMask
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.ParsedGame
import com.example.chessboard.service.buildMoveLabels
import com.example.chessboard.service.parsePgnMoves
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppConfirmDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.components.ChessBoardSection
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingIconInactive
import com.example.chessboard.ui.theme.TrainingTextPrimary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GamesExplorerScreenContainer(
    observableGamesPage: RuntimeContext.ObservableGamesPage,
    modifier: Modifier = Modifier,
    screenContext: ScreenContainerContext,
    initialSelectedGameId: Long? = null,
    onOpenGameEditor: (GameEntity) -> Unit = {},
    onCloneGameClick: (GameDraft) -> Unit = {},
) {
    val inDbProvider = screenContext.inDbProvider
    val gameListService = remember { inDbProvider.createGameListService() }
    val gameController = remember { GameController() }
    val parsedGames = remember { mutableStateListOf<ParsedGame>() }
    val scope = rememberCoroutineScope()
    val observableGamesState = observableGamesPage.state
    var isLoading by remember { mutableStateOf(true) }
    var selectedGameIdx by remember { mutableIntStateOf(-1) }
    val totalGamesCount = observableGamesState.gameIds.size
    var currentPage = 1
    if (totalGamesCount != 0) {
        currentPage = observableGamesState.offset / RuntimeContext.GamesExplorerPageLimit + 1
    }
    val totalPages = (totalGamesCount + RuntimeContext.GamesExplorerPageLimit - 1) /
        RuntimeContext.GamesExplorerPageLimit

    suspend fun loadVisibleGames() {
        isLoading = true

        val visibleGameIds = observableGamesPage.visibleGameIds()
        val games = withContext(Dispatchers.IO) {
            gameListService.getGamesByIds(visibleGameIds)
        }
        val parsed = withContext(Dispatchers.Default) {
            games.map { game ->
                val uciMoves = parsePgnMoves(game.pgn)
                ParsedGame(game, uciMoves, buildMoveLabels(uciMoves))
            }
        }

        parsedGames.clear()
        parsedGames.addAll(parsed)

        selectedGameIdx = -1
        if (initialSelectedGameId != null) {
            val restoredIndex = parsed.indexOfFirst { game -> game.game.id == initialSelectedGameId }
            if (restoredIndex >= 0) {
                selectedGameIdx = restoredIndex
                gameController.loadFromUciMoves(parsed[restoredIndex].uciMoves, 0)
                isLoading = false
                return
            }
        }

        gameController.resetToStartPosition()
        isLoading = false
    }

    LaunchedEffect(initialSelectedGameId, observableGamesState.gameIds) {
        observableGamesPage.ensureVisible(initialSelectedGameId)
    }

    LaunchedEffect(observableGamesState.gameIds, observableGamesState.offset) {
        loadVisibleGames()
    }

    GamesExplorerScreen(
        gameController = gameController,
        parsedGames = parsedGames,
        isLoading = isLoading,
        selectedGameIdx = selectedGameIdx,
        totalGamesCount = totalGamesCount,
        currentPage = currentPage,
        totalPages = totalPages.coerceAtLeast(1),
        canOpenPreviousPage = observableGamesPage.canOpenPreviousPage(),
        canOpenNextPage = observableGamesPage.canOpenNextPage(),
        modifier = modifier,
        onBackClick = screenContext.onBackClick,
        onNavigate = screenContext.onNavigate,
        onOpenGameEditor = onOpenGameEditor,
        onOpenPreviousPageClick = { observableGamesPage.openPreviousPage() },
        onOpenNextPageClick = { observableGamesPage.openNextPage() },
        onCloneGameClick = { game ->
            onCloneGameClick(
                buildGameDraftFromSourceGame(
                    sourceGame = game
                )
            )
        },
        onMovePlyClick = { gameIdx, ply ->
            selectedGameIdx = gameIdx
            gameController.loadFromUciMoves(parsedGames[gameIdx].uciMoves, ply)
        },
        onDeleteGameClick = createDeleteGameAction(
            scope = scope,
            inDbProvider = inDbProvider,
            observableGamesPage = observableGamesPage,
            gameController = gameController,
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
    totalGamesCount: Int = 0,
    currentPage: Int = 1,
    totalPages: Int = 1,
    canOpenPreviousPage: Boolean = false,
    canOpenNextPage: Boolean = false,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onOpenGameEditor: (GameEntity) -> Unit = {},
    onCloneGameClick: (GameEntity) -> Unit = {},
    onOpenPreviousPageClick: () -> Unit = {},
    onOpenNextPageClick: () -> Unit = {},
    onMovePlyClick: (gameIdx: Int, ply: Int) -> Unit = { _, _ -> },
    onDeleteGameClick: (gameId: Long) -> Unit = {},
) {
    fun resolveDisplayedGames(
        games: List<ParsedGame>,
        filterState: GamesExplorerFilterState
    ): List<IndexedValue<ParsedGame>> {
        if (filterState.query.isBlank()) {
            return games.withIndex().toList()
        }

        return games.withIndex().filter { indexedGame ->
            matchesGamesExplorerFilter(
                parsedGame = indexedGame.value,
                filterState = filterState
            )
        }
    }

    fun resolvePageArrowTint(isEnabled: Boolean) = if (isEnabled) {
        TrainingTextPrimary
    } else {
        TrainingIconInactive
    }

    fun resolveGamesExplorerSubtitle(): String {
        return "Games: $totalGamesCount • Page $currentPage/$totalPages"
    }

    val currentPly = gameController.currentMoveIndex
    var showSearchDialog by remember { mutableStateOf(false) }
    var activeFilterState by remember { mutableStateOf(GamesExplorerFilterState()) }
    var draftFilterState by remember { mutableStateOf(activeFilterState) }
    val displayedGames = resolveDisplayedGames(
        games = parsedGames,
        filterState = activeFilterState
    )
    val selectedGame = resolveDisplayedSelectedGame(
        displayedGames = displayedGames,
        selectedGameIdx = selectedGameIdx
    )
    var showDeleteDialog by remember(selectedGame?.game?.id) { mutableStateOf(false) }

    SideEffect {
        gameController.setUserMovesEnabled(false)
        gameController.setOrientation(resolveGamesExplorerBoardOrientation(selectedGame))
    }

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

    RenderGamesExplorerSearchDialog(
        visible = showSearchDialog,
        filterState = draftFilterState,
        onDismiss = { showSearchDialog = false },
        onFilterStateChange = { draftFilterState = it },
        onApplyClick = {
            activeFilterState = draftFilterState
            showSearchDialog = false
        }
    )

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Games Explorer",
                subtitle = resolveGamesExplorerSubtitle(),
                onBackClick = onBackClick,
                filledBackButton = true,
                actions = {
                    IconButton(
                        onClick = {
                            draftFilterState = activeFilterState
                            showSearchDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search games",
                            tint = TrainingTextPrimary
                        )
                    }
                    IconButton(
                        onClick = onOpenPreviousPageClick,
                        enabled = canOpenPreviousPage
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous games page",
                            tint = resolvePageArrowTint(canOpenPreviousPage)
                        )
                    }
                    IconButton(
                        onClick = onOpenNextPageClick,
                        enabled = canOpenNextPage
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next games page",
                            tint = resolvePageArrowTint(canOpenNextPage)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.spaceLg)
        ) {
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))

            if (selectedGame == null) {
                ChessBoardSection(gameController = gameController)
                Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            }

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

                displayedGames.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BodySecondaryText(
                            text = "No games match the current filter.",
                            color = TextColor.Secondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    displayedGames.forEach { indexedGame ->
                        val gameIdx = indexedGame.index
                        val parsedGame = indexedGame.value
                        val isSelected = gameIdx == selectedGameIdx

                        if (isSelected) {
                            ChessBoardSection(gameController = gameController)
                            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
                        }

                        GameBlock(
                            parsedGame = parsedGame,
                            isSelected = isSelected,
                            currentPly = if (isSelected) currentPly else 0,
                            onSelectClick = { onMovePlyClick(gameIdx, 0) },
                            canUndo = isSelected && gameController.canUndo,
                            canRedo = isSelected && gameController.canRedo,
                            onMovePlyClick = { ply -> onMovePlyClick(gameIdx, ply) },
                            onPrevClick = { gameController.undoMove() },
                            onNextClick = { gameController.redoMove() },
                            onResetClick = { onMovePlyClick(gameIdx, 0) },
                            onCloneClick = { onCloneGameClick(parsedGame.game) },
                            onEditClick = { onOpenGameEditor(parsedGame.game) },
                            onDeleteClick = { showDeleteDialog = true }
                        )
                        Spacer(modifier = Modifier.height(AppDimens.spaceMd))
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceXl))
        }
    }
}

private fun createDeleteGameAction(
    scope: CoroutineScope,
    inDbProvider: DatabaseProvider,
    observableGamesPage: RuntimeContext.ObservableGamesPage,
    gameController: GameController,
    onSelectedGameIdxChange: (Int) -> Unit
): (Long) -> Unit {
    return { gameId ->
        scope.launch {
            withContext(Dispatchers.IO) {
                inDbProvider.deleteGame(gameId)
            }

            observableGamesPage.removeGameId(gameId)
            onSelectedGameIdxChange(-1)
            gameController.resetToStartPosition()
        }
    }
}

private fun resolveDisplayedSelectedGame(
    displayedGames: List<IndexedValue<ParsedGame>>,
    selectedGameIdx: Int
): ParsedGame? {
    return displayedGames.firstOrNull { indexedGame ->
        indexedGame.index == selectedGameIdx
    }?.value
}

private fun resolveDeleteGameMessage(parsedGame: ParsedGame): String {
    val gameName = parsedGame.game.event ?: "this game"
    return "Delete \"$gameName\"?\nGame ID: ${parsedGame.game.id}"
}

private fun resolveGamesExplorerBoardOrientation(parsedGame: ParsedGame?): BoardOrientation {
    if (parsedGame?.game?.sideMask == SideMask.BLACK) {
        return BoardOrientation.BLACK
    }

    return BoardOrientation.WHITE
}
