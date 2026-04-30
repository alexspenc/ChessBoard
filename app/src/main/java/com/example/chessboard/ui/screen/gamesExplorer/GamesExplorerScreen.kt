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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.boardmodel.GameDraft
import com.example.chessboard.boardmodel.buildGameDraftFromSourceGame
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.entity.SideMask
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.runtimecontext.RuntimeContext
import com.example.chessboard.service.ParsedGame
import com.example.chessboard.service.buildMoveLabels
import com.example.chessboard.service.parsePgnMoves
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppConfirmDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.ChessBoardSection
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
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
    onAnalyzeGameClick: (List<String>, Int) -> Unit,
) {
    val inDbProvider = screenContext.inDbProvider
    val gameListService = remember { inDbProvider.createGameListService() }
    val gameController = remember { GameController() }
    val parsedGames = remember { mutableStateListOf<ParsedGame>() }
    val scope = rememberCoroutineScope()
    val observableGamesState = observableGamesPage.state
    var isLoading by remember { mutableStateOf(true) }
    var selectedGameIdx by remember { mutableIntStateOf(-1) }
    var activeFilterState by remember { mutableStateOf(GamesExplorerFilterState()) }
    var filteredGameIds by remember { mutableStateOf<List<Long>?>(null) }
    var filteredOffset by remember { mutableIntStateOf(0) }

    val activeGameIds = filteredGameIds ?: observableGamesState.gameIds
    val activeOffset = resolveGamesExplorerActiveOffset(
        filteredGameIds = filteredGameIds,
        filteredOffset = filteredOffset,
        defaultOffset = observableGamesState.offset,
    )
    val totalGamesCount = activeGameIds.size
    val currentPage = resolveGamesExplorerCurrentPage(
        totalGamesCount = totalGamesCount,
        activeOffset = activeOffset,
    )
    val totalPages = resolveGamesExplorerTotalPages(totalGamesCount)
    val canOpenPreviousPage = activeOffset > 0
    val canOpenNextPage = activeOffset + RuntimeContext.GamesExplorerPageLimit < totalGamesCount

    suspend fun loadVisibleGames() {
        isLoading = true

        val visibleGameIds = activeGameIds
            .drop(activeOffset)
            .take(RuntimeContext.GamesExplorerPageLimit)
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

    fun clearGamesFilter() {
        activeFilterState = GamesExplorerFilterState()
        filteredGameIds = null
        filteredOffset = 0
    }

    fun applyGamesFilter(filterState: GamesExplorerFilterState) {
        scope.launch {
            if (!hasGamesExplorerActiveFilter(filterState)) {
                clearGamesFilter()
                return@launch
            }

            isLoading = true
            val matchingGameIds = withContext(Dispatchers.IO) {
                val gamesCount = gameListService.countGamesByName(
                    query = filterState.query,
                    isCaseSensitive = filterState.isCaseSensitive,
                )
                if (gamesCount <= 0) {
                    return@withContext emptyList()
                }

                gameListService.searchGameIdsByName(
                    query = filterState.query,
                    isCaseSensitive = filterState.isCaseSensitive,
                    limit = gamesCount,
                    offset = 0,
                )
            }

            activeFilterState = filterState
            filteredGameIds = matchingGameIds
            filteredOffset = 0
        }
    }

    LaunchedEffect(initialSelectedGameId, observableGamesState.gameIds, filteredGameIds) {
        if (filteredGameIds != null) {
            return@LaunchedEffect
        }

        observableGamesPage.ensureVisible(initialSelectedGameId)
    }

    LaunchedEffect(activeGameIds, activeOffset) {
        loadVisibleGames()
    }

    GamesExplorerScreen(
        gameController = gameController,
        parsedGames = parsedGames,
        isLoading = isLoading,
        activeFilterState = activeFilterState,
        selectedGameIdx = selectedGameIdx,
        totalGamesCount = totalGamesCount,
        currentPage = currentPage,
        totalPages = totalPages,
        canOpenPreviousPage = canOpenPreviousPage,
        canOpenNextPage = canOpenNextPage,
        modifier = modifier,
        onBackClick = screenContext.onBackClick,
        onNavigate = screenContext.onNavigate,
        onOpenGameEditor = onOpenGameEditor,
        onAnalyzeGameClick = onAnalyzeGameClick,
        onApplyFilter = ::applyGamesFilter,
        onClearFilter = ::clearGamesFilter,
        onOpenPreviousPageClick = {
            if (filteredGameIds != null) {
                filteredOffset = (filteredOffset - RuntimeContext.GamesExplorerPageLimit).coerceAtLeast(0)
            } else {
                observableGamesPage.openPreviousPage()
            }
        },
        onOpenNextPageClick = {
            if (filteredGameIds != null) {
                filteredOffset += RuntimeContext.GamesExplorerPageLimit
            } else {
                observableGamesPage.openNextPage()
            }
        },
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
            onSelectedGameIdxChange = { selectedGameIdx = it },
            onDeletedGameId = { deletedGameId ->
                filteredGameIds?.let { currentFilteredIds ->
                    filteredGameIds = currentFilteredIds.filterNot { gameId -> gameId == deletedGameId }
                    filteredOffset = resolveOffsetAfterFilteredRemove(
                        currentOffset = filteredOffset,
                        nextGamesCount = filteredGameIds.orEmpty().size,
                    )
                }
            },
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GamesExplorerScreen(
    gameController: GameController,
    parsedGames: List<ParsedGame> = emptyList(),
    isLoading: Boolean = false,
    activeFilterState: GamesExplorerFilterState = GamesExplorerFilterState(),
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
    onAnalyzeGameClick: (List<String>, Int) -> Unit = { _, _ -> },
    onApplyFilter: (GamesExplorerFilterState) -> Unit = {},
    onClearFilter: () -> Unit = {},
    onOpenPreviousPageClick: () -> Unit = {},
    onOpenNextPageClick: () -> Unit = {},
    onMovePlyClick: (gameIdx: Int, ply: Int) -> Unit = { _, _ -> },
    onDeleteGameClick: (gameId: Long) -> Unit = {},
) {
    fun resolvePageArrowTint(isEnabled: Boolean) = if (isEnabled) {
        TrainingTextPrimary
    } else {
        TrainingIconInactive
    }

    fun resolveGamesExplorerSubtitle(): String {
        if (hasGamesExplorerActiveFilter(activeFilterState)) {
            return "Found: $totalGamesCount • Page $currentPage/$totalPages"
        }

        return "Games: $totalGamesCount • Page $currentPage/$totalPages"
    }

    val currentPly = gameController.currentMoveIndex
    var showSearchDialog by remember { mutableStateOf(false) }
    var draftFilterState by remember { mutableStateOf(activeFilterState) }
    val displayedGames = parsedGames.withIndex().toList()
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
            onApplyFilter(draftFilterState)
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
                        IconMd(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search games",
                            tint = TrainingTextPrimary,
                        )
                    }
                    if (hasGamesExplorerActiveFilter(activeFilterState)) {
                        IconButton(onClick = onClearFilter) {
                            IconMd(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Clear games search",
                                tint = TrainingTextPrimary,
                            )
                        }
                    }
                    IconButton(
                        onClick = onOpenPreviousPageClick,
                        enabled = canOpenPreviousPage
                    ) {
                        IconMd(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous games page",
                            tint = resolvePageArrowTint(canOpenPreviousPage),
                        )
                    }
                    IconButton(
                        onClick = onOpenNextPageClick,
                        enabled = canOpenNextPage
                    ) {
                        IconMd(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next games page",
                            tint = resolvePageArrowTint(canOpenNextPage),
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
                    val emptyMessage = if (hasGamesExplorerActiveFilter(activeFilterState)) {
                        "No games match the current filter."
                    } else {
                        "No saved games.\nGo to Home to create openings."
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BodySecondaryText(
                            text = emptyMessage,
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
                            onAnalyzeClick = {
                                onAnalyzeGameClick(
                                    parsedGame.uciMoves,
                                    currentPly.coerceIn(0, parsedGame.uciMoves.size),
                                )
                            },
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
    onSelectedGameIdxChange: (Int) -> Unit,
    onDeletedGameId: (Long) -> Unit = {},
): (Long) -> Unit {
    return { gameId ->
        scope.launch {
            withContext(Dispatchers.IO) {
                inDbProvider.deleteGame(gameId)
            }

            observableGamesPage.removeGameId(gameId)
            onDeletedGameId(gameId)
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

private fun hasGamesExplorerActiveFilter(filterState: GamesExplorerFilterState): Boolean {
    return filterState.query.isNotBlank()
}

private fun resolveGamesExplorerCurrentPage(
    totalGamesCount: Int,
    activeOffset: Int,
): Int {
    if (totalGamesCount == 0) {
        return 1
    }

    return activeOffset / RuntimeContext.GamesExplorerPageLimit + 1
}

private fun resolveGamesExplorerTotalPages(totalGamesCount: Int): Int {
    if (totalGamesCount <= 0) {
        return 1
    }

    return (totalGamesCount + RuntimeContext.GamesExplorerPageLimit - 1) /
        RuntimeContext.GamesExplorerPageLimit
}

private fun resolveGamesExplorerActiveOffset(
    filteredGameIds: List<Long>?,
    filteredOffset: Int,
    defaultOffset: Int,
): Int {
    if (filteredGameIds != null) {
        return filteredOffset
    }

    return defaultOffset
}

private fun resolveOffsetAfterFilteredRemove(
    currentOffset: Int,
    nextGamesCount: Int,
): Int {
    if (nextGamesCount <= 0) {
        return 0
    }

    if (currentOffset < nextGamesCount) {
        return currentOffset
    }

    return ((nextGamesCount - 1) / RuntimeContext.GamesExplorerPageLimit) *
        RuntimeContext.GamesExplorerPageLimit
}
