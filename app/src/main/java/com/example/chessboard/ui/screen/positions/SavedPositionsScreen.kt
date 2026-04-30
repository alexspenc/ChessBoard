package com.example.chessboard.ui.screen.positions

/**
 * Screen entry point for browsing saved search positions.
 *
 * Keep screen-level state, navigation callbacks, and position-list rendering in this file.
 * Do not add database schema, DAO queries, or reusable generic UI components here.
 */
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.chessboard.analysis.OpeningDeviationItemBuilder
import com.example.chessboard.analysis.OpeningSide
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.boardmodel.InitialBoardFen
import com.example.chessboard.entity.SavedSearchPositionEntity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.ui.SavedPositionsContentTestTag
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppConfirmDialog
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppMessageDialogAction
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.screen.openingDeviation.OpeningDeviationItem
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class SavedPositionsState(
    val listState: SavedPositionsListState = SavedPositionsListState(),
    val searchState: SavedPositionsSearchState = SavedPositionsSearchState(),
    val selectedPositionId: Long? = null,
    val positionToDelete: SavedPositionListItem? = null,
    val deviationDialog: SavedPositionsDeviationDialog? = null,
    val foundGameIds: List<Long>? = null,
    val templateNameDialogState: PositionTemplateNameDialogState? = null,
    val infoDialog: SavedPositionsInfoDialog? = null,
)

private data class SavedPositionsListState(
    val isLoading: Boolean = true,
    val positions: List<SavedPositionListItem> = emptyList(),
    val currentPage: Int = 1,
)

private data class SavedPositionsSearchState(
    val showDialog: Boolean = false,
    val activeFilterState: SavedPositionsFilterState = SavedPositionsFilterState(),
    val draftFilterState: SavedPositionsFilterState = SavedPositionsFilterState(),
)

private const val SavedPositionsPageLimit = 20

private data class SavedPositionsInfoDialog(
    val title: String,
    val message: String,
)

private data class SavedPositionsDeviationDialog(
    val sourcePositionFen: String,
    val deviationItems: List<OpeningDeviationItem>,
)

internal data class SavedPositionListItem(
    val id: Long,
    val name: String,
    val fenForSearch: String,
    val fenFull: String?,
)

@Composable
internal fun SavedPositionsScreenContainer(
    screenContext: ScreenContainerContext,
    modifier: Modifier = Modifier,
    onOpenPositionEditor: (String) -> Unit = {},
    onShowOpeningDeviationSelection: (String, List<OpeningDeviationItem>) -> Unit = { _, _ -> },
) {
    val savedSearchPositionService = remember(screenContext.inDbProvider) {
        screenContext.inDbProvider.createSavedSearchPositionService()
    }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(SavedPositionsState()) }

    fun resolveSearchDialogVisibilityState(isVisible: Boolean): SavedPositionsState {
        if (!isVisible) {
            return state.copy(
                searchState = state.searchState.copy(showDialog = false),
            )
        }

        return state.copy(
            searchState = state.searchState.copy(
                showDialog = true,
                draftFilterState = state.searchState.activeFilterState,
            )
        )
    }

    fun searchGamesForPosition(position: SavedPositionListItem) {
        scope.launch {
            val foundGameIds = withContext(Dispatchers.IO) {
                findGameIdsForSavedPosition(
                    dbProvider = screenContext.inDbProvider,
                    position = position,
                )
            }

            state = state.copy(foundGameIds = foundGameIds)
        }
    }

    fun findOpeningDeviations(position: SavedPositionListItem) {
        scope.launch {
            val deviationItems = withContext(Dispatchers.IO) {
                buildOpeningDeviationItemsForSavedPosition(
                    dbProvider = screenContext.inDbProvider,
                    position = position,
                )
            }

            if (deviationItems.isEmpty()) {
                state = state.copy(
                    deviationDialog = null,
                    infoDialog = resolveNoDeviationsInfoDialog(),
                )
                return@launch
            }

            state = state.copy(
                infoDialog = null,
                deviationDialog = SavedPositionsDeviationDialog(
                    sourcePositionFen = resolveDisplayedFen(position),
                    deviationItems = deviationItems,
                ),
            )
        }
    }

    fun openTrainingFromFoundGames() {
        val foundGameIds = state.foundGameIds ?: return
        state = state.copy(foundGameIds = null)
        screenContext.onNavigate(
            ScreenType.CreateTrainingFromGameIds(
                gameIds = foundGameIds,
                backTarget = ScreenType.SavedPositions,
            )
        )
    }

    fun openTemplateNameDialog() {
        val foundGameIds = state.foundGameIds ?: return
        state = state.copy(
            foundGameIds = null,
            templateNameDialogState = PositionTemplateNameDialogState(gameIds = foundGameIds),
        )
    }

    fun createTemplateFromFoundGames() {
        val currentDialogState = state.templateNameDialogState ?: return
        state = state.copy(templateNameDialogState = null)

        scope.launch {
            val templateId = withContext(Dispatchers.IO) {
                createPositionTemplateFromGameIds(
                    dbProvider = screenContext.inDbProvider,
                    gameIds = currentDialogState.gameIds,
                    templateName = currentDialogState.templateName,
                )
            }

            state = state.copy(
                infoDialog = resolveCreateTemplateInfoDialog(
                    templateId = templateId,
                    foundGameIds = currentDialogState.gameIds,
                )
            )
        }
    }

    LaunchedEffect(savedSearchPositionService) {
        val positions = withContext(Dispatchers.IO) {
            savedSearchPositionService.getAll().map(::toSavedPositionListItem)
        }
        state = state.copy(
            listState = state.listState.copy(
                isLoading = false,
                positions = positions,
                currentPage = resolveSavedPositionsCurrentPage(
                    totalPositionsCount = positions.size,
                    currentPage = state.listState.currentPage,
                ),
            ),
        )
    }

    SavedPositionsScreen(
        state = state,
        modifier = modifier,
        onBackClick = screenContext.onBackClick,
        onNavigate = screenContext.onNavigate,
        onOpenPosition = { position ->
            onOpenPositionEditor(resolveDisplayedFen(position))
        },
        onPositionSelected = { positionId ->
            state = state.copy(selectedPositionId = positionId)
        },
        onCreateFromPositionClick = ::searchGamesForPosition,
        onFindDeviationsClick = ::findOpeningDeviations,
        onOpenPreviousPageClick = {
            state = state.copy(
                listState = state.listState.copy(
                    currentPage = resolveSavedPositionsCurrentPage(
                        totalPositionsCount = state.listState.positions.size,
                        currentPage = state.listState.currentPage - 1,
                    ),
                ),
                selectedPositionId = null,
            )
        },
        onOpenNextPageClick = {
            state = state.copy(
                listState = state.listState.copy(
                    currentPage = resolveSavedPositionsCurrentPage(
                        totalPositionsCount = state.listState.positions.size,
                        currentPage = state.listState.currentPage + 1,
                    ),
                ),
                selectedPositionId = null,
            )
        },
        onPositionToDeleteChange = { position ->
            state = state.copy(positionToDelete = position)
        },
        onSearchDialogVisibilityChange = { isVisible ->
            state = resolveSearchDialogVisibilityState(isVisible)
        },
        onDraftFilterStateChange = { filterState ->
            state = state.copy(
                searchState = state.searchState.copy(draftFilterState = filterState),
            )
        },
        onApplyFilter = {
            state = state.copy(
                searchState = state.searchState.copy(
                    activeFilterState = state.searchState.draftFilterState,
                    showDialog = false,
                )
            )
        },
        onDeletePosition = { position ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    savedSearchPositionService.deleteById(position.id)
                }
                val updatedPositions = state.listState.positions.filterNot {
                    it.id == position.id
                }
                state = state.copy(
                    listState = state.listState.copy(
                        positions = updatedPositions,
                        currentPage = resolveSavedPositionsCurrentPage(
                            totalPositionsCount = updatedPositions.size,
                            currentPage = state.listState.currentPage,
                        ),
                    ),
                    selectedPositionId = resolveSelectedPositionIdAfterDelete(
                        state = state,
                        deletedPosition = position,
                    ),
                    positionToDelete = null,
                )
            }
        },
        onFoundGamesDismiss = {
            state = state.copy(foundGameIds = null)
        },
        onCreateTrainingFromFoundGames = ::openTrainingFromFoundGames,
        onCreateTemplateFromFoundGames = ::openTemplateNameDialog,
        onTemplateNameDialogStateChange = { dialogState ->
            state = state.copy(templateNameDialogState = dialogState)
        },
        onConfirmTemplateName = ::createTemplateFromFoundGames,
        onInfoDialogDismiss = {
            state = state.copy(infoDialog = null)
        },
        onDeviationDialogDismiss = {
            state = state.copy(deviationDialog = null)
        },
        onShowOpeningDeviationSelection = { dialog ->
            state = state.copy(deviationDialog = null)
            onShowOpeningDeviationSelection(
                dialog.sourcePositionFen,
                dialog.deviationItems,
            )
        },
    )
}

private suspend fun findGameIdsForSavedPosition(
    dbProvider: DatabaseProvider,
    position: SavedPositionListItem,
): List<Long> {
    return dbProvider.findGameIdsByFenWithoutMoveNumber(
        toLoadableSavedPositionFen(resolveDisplayedFen(position))
    )
}

private fun resolveCreateTemplateInfoDialog(
    templateId: Long?,
    foundGameIds: List<Long>,
): SavedPositionsInfoDialog {
    if (templateId == null) {
        return SavedPositionsInfoDialog(
            title = "Template Error",
            message = "Found games could not be saved as a template.",
        )
    }

    return SavedPositionsInfoDialog(
        title = "Template Created",
        message = "Template ID: $templateId\nGames added: ${foundGameIds.size}",
    )
}

private fun resolveNoDeviationsInfoDialog(): SavedPositionsInfoDialog {
    return SavedPositionsInfoDialog(
        title = "No Deviations",
        message = "No opening deviations found for this saved position.",
    )
}

@Composable
private fun SavedPositionsScreen(
    state: SavedPositionsState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onOpenPosition: (SavedPositionListItem) -> Unit = {},
    onPositionSelected: (Long) -> Unit = {},
    onCreateFromPositionClick: (SavedPositionListItem) -> Unit = {},
    onFindDeviationsClick: (SavedPositionListItem) -> Unit = {},
    onOpenPreviousPageClick: () -> Unit = {},
    onOpenNextPageClick: () -> Unit = {},
    onPositionToDeleteChange: (SavedPositionListItem?) -> Unit = {},
    onSearchDialogVisibilityChange: (Boolean) -> Unit = {},
    onDraftFilterStateChange: (SavedPositionsFilterState) -> Unit = {},
    onApplyFilter: () -> Unit = {},
    onDeletePosition: (SavedPositionListItem) -> Unit = {},
    onFoundGamesDismiss: () -> Unit = {},
    onCreateTrainingFromFoundGames: () -> Unit = {},
    onCreateTemplateFromFoundGames: () -> Unit = {},
    onTemplateNameDialogStateChange: (PositionTemplateNameDialogState?) -> Unit = {},
    onConfirmTemplateName: () -> Unit = {},
    onInfoDialogDismiss: () -> Unit = {},
    onDeviationDialogDismiss: () -> Unit = {},
    onShowOpeningDeviationSelection: (SavedPositionsDeviationDialog) -> Unit = {},
) {
    val selectedPosition = resolveSelectedPosition(state)
    val previewGameController = remember { GameController() }
    val currentPage = resolveSavedPositionsCurrentPage(
        totalPositionsCount = state.listState.positions.size,
        currentPage = state.listState.currentPage,
    )
    val totalPages = resolveSavedPositionsTotalPages(state.listState.positions.size)
    val pagePositions = resolveSavedPositionsPage(
        positions = state.listState.positions,
        currentPage = currentPage,
    )
    val displayedPositions = resolveDisplayedPositions(
        positions = pagePositions,
        filterState = state.searchState.activeFilterState,
    )

    LaunchedEffect(selectedPosition?.id, selectedPosition?.fenForSearch, selectedPosition?.fenFull) {
        val position = selectedPosition ?: return@LaunchedEffect
        previewGameController.loadPreviewFen(
            toLoadableSavedPositionFen(resolveDisplayedFen(position))
        )
        previewGameController.setOrientation(
            resolveSavedPositionBoardOrientation(position)
        )
        previewGameController.setUserMovesEnabled(false)
    }

    RenderDeleteSavedPositionDialog(
        positionToDelete = state.positionToDelete,
        onDismiss = { onPositionToDeleteChange(null) },
        onConfirm = { position ->
            onDeletePosition(position)
        },
    )
    RenderSavedPositionsSearchDialog(
        visible = state.searchState.showDialog,
        filterState = state.searchState.draftFilterState,
        onDismiss = { onSearchDialogVisibilityChange(false) },
        onFilterStateChange = onDraftFilterStateChange,
        onApplyClick = onApplyFilter,
    )
    RenderPositionSearchResultDialog(
        foundGameIds = state.foundGameIds,
        actions = PositionSearchResultDialogActions(
            onDismiss = onFoundGamesDismiss,
            onCreateTrainingClick = onCreateTrainingFromFoundGames,
            onCreateTemplateClick = onCreateTemplateFromFoundGames,
            templateNameDialogState = state.templateNameDialogState,
            onTemplateNameChange = { templateName ->
                state.templateNameDialogState?.let { currentDialogState ->
                    onTemplateNameDialogStateChange(
                        currentDialogState.copy(templateName = templateName)
                    )
                }
            },
            onTemplateNameDismiss = { onTemplateNameDialogStateChange(null) },
            onConfirmTemplateName = onConfirmTemplateName,
        ),
    )
    RenderSavedPositionsInfoDialog(
        infoDialog = state.infoDialog,
        onDismiss = onInfoDialogDismiss,
    )
    RenderSavedPositionsDeviationDialog(
        deviationDialog = state.deviationDialog,
        onDismiss = onDeviationDialogDismiss,
        onShowDeviations = onShowOpeningDeviationSelection,
    )

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SavedPositionsTopBar(
                onBackClick = onBackClick,
                paginationState = SavedPositionsTopBarPaginationState(
                    totalPositionsCount = state.listState.positions.size,
                    currentPage = currentPage,
                    totalPages = totalPages,
                    canOpenPreviousPage = currentPage > 1,
                    canOpenNextPage = currentPage < totalPages,
                ),
                onSearchClick = { onSearchDialogVisibilityChange(true) },
                onOpenPreviousPageClick = onOpenPreviousPageClick,
                onOpenNextPageClick = onOpenNextPageClick,
            )
        },
        bottomBar = {
            AppBottomNavigation(
                items = defaultAppBottomNavigationItems(),
                selectedItem = ScreenType.Home,
                onItemSelected = onNavigate,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag(SavedPositionsContentTestTag),
            contentPadding = PaddingValues(
                horizontal = AppDimens.spaceLg,
                vertical = AppDimens.spaceLg,
            ),
        ) {
            renderSavedPositionsContent(
                state = state,
                displayedPositions = displayedPositions,
                selectedPosition = selectedPosition,
                previewGameController = previewGameController,
                onOpenPosition = onOpenPosition,
                onPositionSelected = onPositionSelected,
                onCreateFromPositionClick = onCreateFromPositionClick,
                onFindDeviationsClick = onFindDeviationsClick,
                onPositionToDeleteChange = onPositionToDeleteChange,
            )
        }
    }
}

@Composable
private fun RenderDeleteSavedPositionDialog(
    positionToDelete: SavedPositionListItem?,
    onDismiss: () -> Unit,
    onConfirm: (SavedPositionListItem) -> Unit,
) {
    val position = positionToDelete ?: return

    AppConfirmDialog(
        title = "Delete Position",
        message = resolveDeletePositionMessage(position),
        onDismiss = onDismiss,
        onConfirm = {
            onConfirm(position)
        },
        confirmText = "Delete",
        isDestructive = true,
    )
}

@Composable
private fun RenderSavedPositionsInfoDialog(
    infoDialog: SavedPositionsInfoDialog?,
    onDismiss: () -> Unit,
) {
    val currentDialog = infoDialog ?: return

    AppMessageDialog(
        title = currentDialog.title,
        message = currentDialog.message,
        onDismiss = onDismiss,
    )
}

@Composable
private fun RenderSavedPositionsDeviationDialog(
    deviationDialog: SavedPositionsDeviationDialog?,
    onDismiss: () -> Unit,
    onShowDeviations: (SavedPositionsDeviationDialog) -> Unit,
) {
    val currentDialog = deviationDialog ?: return

    AppMessageDialog(
        title = "Opening Deviations Found",
        message = "Deviation positions found: ${currentDialog.deviationItems.size}",
        onDismiss = onDismiss,
        actions = listOf(
            AppMessageDialogAction(
                text = "Show Deviations",
                onClick = { onShowDeviations(currentDialog) },
            ),
            AppMessageDialogAction(
                text = "Close",
                onClick = onDismiss,
            ),
        ),
    )
}

private fun resolveSelectedPosition(state: SavedPositionsState): SavedPositionListItem? {
    val selectedPositionId = state.selectedPositionId ?: return null
    return state.listState.positions.firstOrNull { it.id == selectedPositionId }
}

private fun resolveSavedPositionsCurrentPage(
    totalPositionsCount: Int,
    currentPage: Int,
): Int {
    return currentPage.coerceIn(1, resolveSavedPositionsTotalPages(totalPositionsCount))
}

private fun resolveSavedPositionsTotalPages(totalPositionsCount: Int): Int {
    if (totalPositionsCount == 0) {
        return 1
    }

    return (totalPositionsCount + SavedPositionsPageLimit - 1) / SavedPositionsPageLimit
}

private fun resolveSavedPositionsPage(
    positions: List<SavedPositionListItem>,
    currentPage: Int,
): List<SavedPositionListItem> {
    return positions
        .drop((currentPage - 1) * SavedPositionsPageLimit)
        .take(SavedPositionsPageLimit)
}

private fun LazyListScope.renderSavedPositionsContent(
    state: SavedPositionsState,
    displayedPositions: List<SavedPositionListItem>,
    selectedPosition: SavedPositionListItem?,
    previewGameController: GameController,
    onOpenPosition: (SavedPositionListItem) -> Unit,
    onPositionSelected: (Long) -> Unit,
    onCreateFromPositionClick: (SavedPositionListItem) -> Unit,
    onFindDeviationsClick: (SavedPositionListItem) -> Unit,
    onPositionToDeleteChange: (SavedPositionListItem?) -> Unit,
) {
    if (state.listState.isLoading) {
        item {
            SavedPositionsLoadingState()
        }
        return
    }

    if (state.listState.positions.isEmpty()) {
        item {
            SavedPositionsEmptyState()
        }
        return
    }

    if (displayedPositions.isEmpty()) {
        item {
            SavedPositionsNoFilterMatchesState()
        }
        return
    }

    items(displayedPositions, key = { it.id }) { position ->
        if (position.id == selectedPosition?.id) {
            SavedPositionBoardPreview(
                position = position,
                gameController = previewGameController,
            )
            Spacer(modifier = Modifier.height(AppDimens.spaceMd))
        }

        SavedPositionCard(
            position = position,
            isSelected = position.id == state.selectedPositionId,
            onClick = { onPositionSelected(position.id) },
            onOpenClick = { onOpenPosition(position) },
            onCreateClick = { onCreateFromPositionClick(position) },
            onFindDeviationsClick = { onFindDeviationsClick(position) },
            onDeleteClick = { onPositionToDeleteChange(position) },
        )
        Spacer(modifier = Modifier.height(AppDimens.spaceMd))
    }
}

private fun resolveDisplayedPositions(
    positions: List<SavedPositionListItem>,
    filterState: SavedPositionsFilterState,
): List<SavedPositionListItem> {
    if (filterState.query.isBlank()) {
        return positions
    }

    return positions.filter { position ->
        matchesSavedPositionsFilter(
            position = position,
            filterState = filterState,
        )
    }
}

@Composable
private fun SavedPositionsLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = TrainingAccentTeal)
    }
}

@Composable
private fun SavedPositionsEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        BodySecondaryText(
            text = "No saved positions available.",
            color = TextColor.Secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SavedPositionsNoFilterMatchesState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        BodySecondaryText(
            text = "No saved positions match the current filter.",
            color = TextColor.Secondary,
            textAlign = TextAlign.Center,
        )
    }
}

private fun resolveSelectedPositionIdAfterDelete(
    state: SavedPositionsState,
    deletedPosition: SavedPositionListItem,
): Long? {
    if (state.selectedPositionId == deletedPosition.id) {
        return null
    }

    return state.selectedPositionId
}

private fun resolveDeletePositionMessage(position: SavedPositionListItem): String {
    return "Delete \"${position.name}\"?\nPosition ID: ${position.id}"
}

private suspend fun buildOpeningDeviationItemsForSavedPosition(
    dbProvider: DatabaseProvider,
    position: SavedPositionListItem,
    builder: OpeningDeviationItemBuilder = OpeningDeviationItemBuilder(),
): List<OpeningDeviationItem> {
    val displayedFen = resolveDisplayedFen(position)
    val gameIds = findGameIdsForSavedPosition(
        dbProvider = dbProvider,
        position = position,
    )
    val games = dbProvider.createGameListService().getGamesByIds(gameIds)

    return builder.build(
        games = games,
        selectedSide = resolveOpeningDeviationSide(displayedFen),
    )
}

internal fun resolveDisplayedFen(position: SavedPositionListItem): String {
    val fullFen = position.fenFull
    if (!fullFen.isNullOrBlank()) {
        return fullFen
    }

    return position.fenForSearch
}

internal fun toLoadableSavedPositionFen(fen: String): String {
    val normalizedFen = fen.trim()
    if (normalizedFen.isBlank()) {
        return InitialBoardFen
    }

    val fenParts = normalizedFen.split(Regex("\\s+"))

    if (fenParts.size >= 6) {
        return normalizedFen
    }

    if (fenParts.size == 5) {
        return "$normalizedFen 1"
    }

    if (fenParts.size == 4) {
        return "$normalizedFen 0 1"
    }

    if (fenParts.size == 3) {
        return "$normalizedFen - 0 1"
    }

    if (fenParts.size == 2) {
        return "$normalizedFen - - 0 1"
    }

    return "$normalizedFen w - - 0 1"
}

private fun resolveOpeningDeviationSide(fen: String): OpeningSide {
    val sideToMove = fen.trim().split(Regex("\\s+")).getOrNull(1)
    if (sideToMove == "b") {
        return OpeningSide.BLACK
    }

    return OpeningSide.WHITE
}

private fun toSavedPositionListItem(
    entity: SavedSearchPositionEntity
): SavedPositionListItem {
    return SavedPositionListItem(
        id = entity.id,
        name = entity.name.ifBlank { "Unnamed Position" },
        fenForSearch = entity.fenForSearch,
        fenFull = entity.fenFull,
    )
}
