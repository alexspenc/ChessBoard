package com.example.chessboard.ui.screen.linesExplorer

/**
 * Screen and container for browsing saved lines.
 *
 * Keep in this file:
 * - container wiring, loading data, and delete orchestration
 * - screen-level state such as selected line and active filter state
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
import android.content.ClipData
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.chessboard.R
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.boardmodel.LineDraft
import com.example.chessboard.boardmodel.buildLineDraftFromSourceLine
import com.example.chessboard.entity.LineEntity
import com.example.chessboard.entity.SideMask
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.runtimecontext.RuntimeContext
import com.example.chessboard.service.ParsedLine
import com.example.chessboard.service.buildAnalysisPgnFromLines
import com.example.chessboard.service.buildMoveLabels
import com.example.chessboard.service.filterDubiousLineIdsByName
import com.example.chessboard.service.parsePgnMoves
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.LinesExplorerBulkDeleteConfirmTestTag
import com.example.chessboard.ui.components.AppConfirmDialog
import com.example.chessboard.ui.components.AppLoadingDialog
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.ChessBoardSection
import com.example.chessboard.ui.components.HomeIconButton
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.MutedContentColor
import com.example.chessboard.ui.theme.TrainingTextPrimary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class LinesExplorerPgnMessage(
    val title: String,
    val message: String,
)

internal data class LinesExplorerScreenState(
    val lineController: LineController,
    val parsedLines: List<ParsedLine>,
    val isLoading: Boolean,
    val activeFilterState: LinesExplorerFilterState,
    val selectedLineIdx: Int,
    val totalLinesCount: Int,
    val currentPage: Int,
    val totalPages: Int,
    val simpleViewEnabled: Boolean,
)

@Composable
fun LinesExplorerScreenContainer(
    observableLinesPage: RuntimeContext.ObservableLinesPage,
    modifier: Modifier = Modifier,
    screenContext: ScreenContainerContext,
    initialSelectedLineId: Long? = null,
    simpleViewEnabled: Boolean = false,
    onOpenLineEditor: (LineEntity) -> Unit = {},
    onCloneLineClick: (LineDraft) -> Unit = {},
    onAnalyzeLineClick: (List<String>, Int) -> Unit,
) {
    val inDbProvider = screenContext.inDbProvider
    val lineListService = remember { inDbProvider.createLineListService() }
    val dubiousLineService = remember { inDbProvider.createDubiousLineService() }
    val lineController = remember { LineController() }
    val clipboard = LocalClipboard.current
    val parsedLines = remember { mutableStateListOf<ParsedLine>() }
    val scope = rememberCoroutineScope()
    val observableLinesState = observableLinesPage.state
    val activeFilterState = observableLinesPage.filterCriteria.toLinesExplorerFilterState()
    val filteredOffset = observableLinesPage.filteredOffset
    val linesExplorerTrainingName = stringResource(R.string.lines_explorer_training_name)
    val linesExplorerTrainingScreenTitle = stringResource(R.string.lines_explorer_training_screen_title)
    val linesExplorerTrainingLinesCountLabel = stringResource(R.string.lines_explorer_training_lines_count_label)
    val pgnUnavailableTitle = stringResource(R.string.lines_explorer_pgn_unavailable_title)
    val pgnUnavailableMessage = stringResource(R.string.lines_explorer_pgn_unavailable_message)
    val pgnClipLabel = stringResource(R.string.lines_explorer_pgn_clip_label)
    val pgnCopiedTitle = stringResource(R.string.lines_explorer_pgn_copied_title)
    val pgnCopiedMessage = stringResource(R.string.lines_explorer_pgn_copied_message)
    var isLoading by remember { mutableStateOf(true) }
    var selectedLineIdx by remember { mutableIntStateOf(-1) }
    var filteredLineIds by remember {
        mutableStateOf<List<Long>?>(
            if (hasLinesExplorerActiveFilter(activeFilterState)) emptyList() else null
        )
    }
    var isBuildingLinesPgn by remember { mutableStateOf(false) }
    var linesPgnMessage by remember { mutableStateOf<LinesExplorerPgnMessage?>(null) }
    var isDeletingExplorerLines by remember { mutableStateOf(false) }

    val activeLineIds = filteredLineIds ?: observableLinesState.lineIds
    val activeOffset = resolveLinesExplorerActiveOffset(
        filteredLineIds = filteredLineIds,
        filteredOffset = filteredOffset,
        defaultOffset = observableLinesState.offset,
    )
    val totalLinesCount = activeLineIds.size
    val currentPage = resolveLinesExplorerCurrentPage(
        totalLinesCount = totalLinesCount,
        activeOffset = activeOffset,
    )
    val totalPages = resolveLinesExplorerTotalPages(totalLinesCount)
    val canOpenPreviousPage = activeOffset > 0
    val canOpenNextPage = activeOffset + RuntimeContext.LinesExplorerPageLimit < totalLinesCount

    suspend fun loadVisibleLines() {
        isLoading = true

        val visibleLineIds = activeLineIds
            .drop(activeOffset)
            .take(RuntimeContext.LinesExplorerPageLimit)
        val lines = withContext(Dispatchers.IO) {
            lineListService.getLinesByIds(visibleLineIds)
        }
        val parsed = withContext(Dispatchers.Default) {
            lines.map { line ->
                val uciMoves = parsePgnMoves(line.pgn)
                ParsedLine(line, uciMoves, buildMoveLabels(uciMoves))
            }
        }

        parsedLines.clear()
        parsedLines.addAll(parsed)

        selectedLineIdx = -1
        if (initialSelectedLineId != null) {
            val restoredIndex = parsed.indexOfFirst { line -> line.line.id == initialSelectedLineId }
            if (restoredIndex >= 0) {
                selectedLineIdx = restoredIndex
                lineController.loadFromUciMoves(parsed[restoredIndex].uciMoves, 0)
                isLoading = false
                return
            }
        }

        lineController.resetToStartPosition()
        isLoading = false
    }

    suspend fun findMatchingLineIds(filterState: LinesExplorerFilterState): List<Long> {
        return withContext(Dispatchers.IO) {
            val sideMask = filterState.sideFilter.sideMask
            if (filterState.dubiousOnly) {
                val dubiousLines = dubiousLineService.getAll()
                val dubiousLineIds = dubiousLines.map { dubiousLine -> dubiousLine.lineId }
                val lines = lineListService.getLinesByIds(dubiousLineIds)
                return@withContext filterDubiousLineIdsByName(
                    dubiousLines = dubiousLines,
                    lines = lines,
                    query = filterState.query,
                    isCaseSensitive = filterState.isCaseSensitive,
                    sideMask = sideMask,
                )
            }

            val linesCount = lineListService.countLinesByName(
                query = filterState.query,
                isCaseSensitive = filterState.isCaseSensitive,
                sideMask = sideMask,
            )
            if (linesCount <= 0) {
                return@withContext emptyList()
            }

            lineListService.searchLineIdsByName(
                query = filterState.query,
                isCaseSensitive = filterState.isCaseSensitive,
                limit = linesCount,
                offset = 0,
                sideMask = sideMask,
            )
        }
    }

    fun clearLinesFilter() {
        observableLinesPage.clearFilter()
        filteredLineIds = null
    }

    fun applyLinesFilter(filterState: LinesExplorerFilterState) {
        if (!hasLinesExplorerActiveFilter(filterState)) {
            clearLinesFilter()
            return
        }

        val nextFilterCriteria = filterState.toRuntimeFilterCriteria()
        val shouldReloadImmediately = nextFilterCriteria == observableLinesPage.filterCriteria
        filteredLineIds = emptyList()
        observableLinesPage.updateFilterCriteria(nextFilterCriteria)
        if (!shouldReloadImmediately) {
            return
        }

        scope.launch {
            isLoading = true
            filteredLineIds = findMatchingLineIds(filterState)
        }
    }

    fun copyExplorerLinesPgn() {
        if (activeLineIds.isEmpty() || isBuildingLinesPgn) {
            return
        }

        scope.launch {
            isBuildingLinesPgn = true
            try {
                val lines = withContext(Dispatchers.IO) {
                    lineListService.getLinesByIds(activeLineIds)
                }
                val linesPgn = withContext(Dispatchers.Default) {
                    buildAnalysisPgnFromLines(lines)
                }
                if (linesPgn.isBlank()) {
                    linesPgnMessage = LinesExplorerPgnMessage(
                        title = pgnUnavailableTitle,
                        message = pgnUnavailableMessage,
                    )
                    return@launch
                }

                clipboard.setClipEntry(
                    ClipEntry(
                        ClipData.newPlainText(
                            pgnClipLabel,
                            linesPgn,
                        )
                    )
                )
                linesPgnMessage = LinesExplorerPgnMessage(
                    title = pgnCopiedTitle,
                    message = pgnCopiedMessage,
                )
            } finally {
                isBuildingLinesPgn = false
            }
        }
    }

    fun deleteExplorerLines() {
        val lineIdsToDelete = activeLineIds.distinct()
        if (lineIdsToDelete.isEmpty() || isDeletingExplorerLines) {
            return
        }

        scope.launch {
            isDeletingExplorerLines = true
            try {
                withContext(Dispatchers.IO) {
                    inDbProvider.createLineDeleter().deleteLines(lineIdsToDelete)
                }

                val deletedLineIds = lineIdsToDelete.toSet()
                observableLinesPage.removeLineIds(deletedLineIds)
                filteredLineIds?.let { currentFilteredIds ->
                    val nextFilteredLineIds = currentFilteredIds.filterNot { lineId -> lineId in deletedLineIds }
                    filteredLineIds = nextFilteredLineIds
                    observableLinesPage.updateFilteredOffset(
                        resolveOffsetAfterFilteredRemove(
                            currentOffset = filteredOffset,
                            nextLinesCount = nextFilteredLineIds.size,
                        )
                    )
                }
                selectedLineIdx = -1
                lineController.resetToStartPosition()
            } finally {
                isDeletingExplorerLines = false
            }
        }
    }

    fun openPreviousPage() {
        if (!canOpenPreviousPage) {
            return
        }

        if (filteredLineIds != null) {
            observableLinesPage.openPreviousFilteredPage()
            return
        }

        observableLinesPage.openPreviousPage()
    }

    fun openNextPage() {
        if (!canOpenNextPage) {
            return
        }

        if (filteredLineIds != null) {
            observableLinesPage.openNextFilteredPage(totalLinesCount)
            return
        }

        observableLinesPage.openNextPage()
    }

    LaunchedEffect(activeFilterState) {
        if (!hasLinesExplorerActiveFilter(activeFilterState)) {
            filteredLineIds = null
            return@LaunchedEffect
        }

        isLoading = true
        filteredLineIds = findMatchingLineIds(activeFilterState)
    }

    LaunchedEffect(initialSelectedLineId, observableLinesState.lineIds, filteredLineIds) {
        if (filteredLineIds != null) {
            return@LaunchedEffect
        }

        observableLinesPage.ensureVisible(initialSelectedLineId)
    }

    LaunchedEffect(activeLineIds, activeOffset) {
        loadVisibleLines()
    }

    linesPgnMessage?.let { message ->
        AppMessageDialog(
            title = message.title,
            message = message.message,
            onDismiss = { linesPgnMessage = null },
        )
    }

    if (isBuildingLinesPgn) {
        AppLoadingDialog(
            title = stringResource(R.string.lines_explorer_building_pgn_title),
            message = stringResource(R.string.lines_explorer_building_pgn_message),
        )
    }

    if (isDeletingExplorerLines) {
        AppLoadingDialog(
            title = stringResource(R.string.lines_explorer_deleting_lines_title),
            message = stringResource(R.string.lines_explorer_deleting_lines_message),
        )
    }

    LinesExplorerScreen(
        state = LinesExplorerScreenState(
            lineController = lineController,
            parsedLines = parsedLines,
            isLoading = isLoading,
            activeFilterState = activeFilterState,
            selectedLineIdx = selectedLineIdx,
            totalLinesCount = totalLinesCount,
            currentPage = currentPage,
            totalPages = totalPages,
            simpleViewEnabled = simpleViewEnabled,
        ),
        copyLinesPgnAction = CallbackWithCfg(
            canUse = activeLineIds.isNotEmpty() && !isBuildingLinesPgn,
            onClick = ::copyExplorerLinesPgn,
        ),
        createTrainingAction = CallbackWithCfg(
            canUse = activeLineIds.isNotEmpty(),
            onClick = {
                screenContext.onNavigate(
                    ScreenType.CreateTrainingFromLineIds(
                        lineIds = activeLineIds,
                        backTarget = ScreenType.LinesExplorer,
                        initialTrainingName = linesExplorerTrainingName,
                        screenTitle = linesExplorerTrainingScreenTitle,
                        linesCountLabel = linesExplorerTrainingLinesCountLabel,
                    )
                )
            },
        ),
        deleteExplorerLinesAction = CallbackWithCfg(
            canUse = activeLineIds.isNotEmpty() && !isDeletingExplorerLines,
            onClick = ::deleteExplorerLines,
        ),
        openPreviousPageAction = CallbackWithCfg(
            canUse = canOpenPreviousPage,
            onClick = ::openPreviousPage,
        ),
        openNextPageAction = CallbackWithCfg(
            canUse = canOpenNextPage,
            onClick = ::openNextPage,
        ),
        modifier = modifier,
        onBackClick = screenContext.onBackClick,
        onNavigate = screenContext.onNavigate,
        onOpenLineEditor = onOpenLineEditor,
        onAnalyzeLineClick = onAnalyzeLineClick,
        onApplyFilter = ::applyLinesFilter,
        onClearFilter = ::clearLinesFilter,
        onCloneLineClick = { line ->
            onCloneLineClick(
                buildLineDraftFromSourceLine(
                    sourceLine = line
                )
            )
        },
        onMovePlyClick = { lineIdx, ply ->
            selectedLineIdx = lineIdx
            lineController.loadFromUciMoves(parsedLines[lineIdx].uciMoves, ply)
        },
        onDeleteLineClick = createDeleteLineAction(
            scope = scope,
            inDbProvider = inDbProvider,
            observableLinesPage = observableLinesPage,
            lineController = lineController,
            onSelectedLineIdxChange = { selectedLineIdx = it },
            onDeletedLineId = { deletedLineId ->
                filteredLineIds?.let { currentFilteredIds ->
                    val nextFilteredLineIds = currentFilteredIds.filterNot { lineId -> lineId == deletedLineId }
                    filteredLineIds = nextFilteredLineIds
                    observableLinesPage.updateFilteredOffset(
                        resolveOffsetAfterFilteredRemove(
                            currentOffset = filteredOffset,
                            nextLinesCount = nextFilteredLineIds.size,
                        )
                    )
                }
            },
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LinesExplorerScreen(
    state: LinesExplorerScreenState,
    copyLinesPgnAction: CallbackWithCfg,
    createTrainingAction: CallbackWithCfg,
    deleteExplorerLinesAction: CallbackWithCfg,
    openPreviousPageAction: CallbackWithCfg,
    openNextPageAction: CallbackWithCfg,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onOpenLineEditor: (LineEntity) -> Unit = {},
    onCloneLineClick: (LineEntity) -> Unit = {},
    onAnalyzeLineClick: (List<String>, Int) -> Unit = { _, _ -> },
    onApplyFilter: (LinesExplorerFilterState) -> Unit = {},
    onClearFilter: () -> Unit = {},
    onMovePlyClick: (lineIdx: Int, ply: Int) -> Unit = { _, _ -> },
    onDeleteLineClick: (lineId: Long) -> Unit = {},
) {
    fun resolvePageArrowTint(isEnabled: Boolean): Color {
        if (isEnabled) {
            return TrainingTextPrimary
        }

        return MutedContentColor
    }

    val currentPly = state.lineController.currentMoveIndex
    var showSearchDialog by remember { mutableStateOf(false) }
    var draftFilterState by remember { mutableStateOf(state.activeFilterState) }
    val displayedLines = state.parsedLines.withIndex().toList()
    val selectedLine = resolveDisplayedSelectedLine(
        displayedLines = displayedLines,
        selectedLineIdx = state.selectedLineIdx
    )
    val hasSelectedLine = selectedLine != null && state.selectedLineIdx >= 0
    val hasLineActions = hasSelectedLine ||
        copyLinesPgnAction.canUse ||
        createTrainingAction.canUse ||
        deleteExplorerLinesAction.canUse
    val showDeleteDialog = remember(selectedLine?.line?.id) { mutableStateOf(false) }
    val showDeleteExplorerLinesDialog = remember { mutableStateOf(false) }
    val showLineActionsDialog = remember(selectedLine?.line?.id) { mutableStateOf(false) }
    val hasActiveFilter = hasLinesExplorerActiveFilter(state.activeFilterState)
    val linesExplorerSubtitle = if (hasActiveFilter) {
        stringResource(
            R.string.lines_explorer_filtered_subtitle,
            state.totalLinesCount,
            state.currentPage,
            state.totalPages,
        )
    } else {
        stringResource(
            R.string.lines_explorer_subtitle,
            state.totalLinesCount,
            state.currentPage,
            state.totalPages,
        )
    }
    val emptyLinesMessage = if (hasActiveFilter) {
        stringResource(R.string.lines_explorer_empty_filtered)
    } else {
        stringResource(R.string.lines_explorer_empty)
    }

    SideEffect {
        state.lineController.setUserMovesEnabled(false)
        state.lineController.setOrientation(resolveLinesExplorerBoardOrientation(selectedLine))
    }

    if (showDeleteDialog.value && selectedLine != null) {
        val lineName = selectedLine.line.event ?: stringResource(R.string.lines_explorer_this_line)
        AppConfirmDialog(
            title = stringResource(R.string.lines_explorer_delete_line_title),
            message = stringResource(
                R.string.lines_explorer_delete_line_message,
                lineName,
                selectedLine.line.id,
            ),
            onDismiss = { showDeleteDialog.value = false },
            onConfirm = {
                showDeleteDialog.value = false
                onDeleteLineClick(selectedLine.line.id)
            },
            confirmText = stringResource(R.string.common_delete),
            isDestructive = true
        )
    }

    if (showDeleteExplorerLinesDialog.value && deleteExplorerLinesAction.canUse) {
        AppConfirmDialog(
            title = stringResource(R.string.lines_explorer_delete_lines_title),
            message = pluralStringResource(
                R.plurals.lines_explorer_delete_lines_message,
                state.totalLinesCount,
                state.totalLinesCount,
            ),
            onDismiss = { showDeleteExplorerLinesDialog.value = false },
            onConfirm = {
                showDeleteExplorerLinesDialog.value = false
                deleteExplorerLinesAction.onClick()
            },
            confirmText = stringResource(R.string.common_delete),
            confirmButtonModifier = Modifier.testTag(LinesExplorerBulkDeleteConfirmTestTag),
            isDestructive = true
        )
    }

    RenderLinesExplorerSearchDialog(
        visible = showSearchDialog,
        filterState = draftFilterState,
        onDismiss = { showSearchDialog = false },
        onFilterStateChange = { draftFilterState = it },
        onApplyClick = {
            onApplyFilter(draftFilterState)
            showSearchDialog = false
        }
    )

    RenderLinesExplorerLineActionsDialog(
        visible = showLineActionsDialog.value && hasLineActions,
        onDismiss = { showLineActionsDialog.value = false },
        resetAction = CallbackWithCfg(
            canUse = hasSelectedLine,
            onClick = {
                showLineActionsDialog.value = false
                if (hasSelectedLine) {
                    onMovePlyClick(state.selectedLineIdx, 0)
                }
            },
        ),
        analyzeAction = CallbackWithCfg(
            canUse = hasSelectedLine,
            onClick = {
                showLineActionsDialog.value = false
                selectedLine?.let { line ->
                    onAnalyzeLineClick(
                        line.uciMoves,
                        currentPly.coerceIn(0, line.uciMoves.size),
                    )
                }
            },
        ),
        cloneAction = CallbackWithCfg(
            canUse = hasSelectedLine,
            onClick = {
                showLineActionsDialog.value = false
                selectedLine?.let { line -> onCloneLineClick(line.line) }
            },
        ),
        createTrainingAction = CallbackWithCfg(
            canUse = createTrainingAction.canUse,
            onClick = {
                showLineActionsDialog.value = false
                createTrainingAction.onClick()
            },
        ),
        copyLinesPgnAction = CallbackWithCfg(
            canUse = copyLinesPgnAction.canUse,
            onClick = {
                showLineActionsDialog.value = false
                copyLinesPgnAction.onClick()
            },
        ),
        deleteExplorerLinesAction = CallbackWithCfg(
            canUse = deleteExplorerLinesAction.canUse,
            onClick = {
                showLineActionsDialog.value = false
                if (deleteExplorerLinesAction.canUse) {
                    showDeleteExplorerLinesDialog.value = true
                }
            },
        ),
    )

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.lines_explorer_title),
                subtitle = linesExplorerSubtitle,
                onBackClick = onBackClick,
                filledBackButton = true,
                actions = {
                    HomeIconButton(onClick = { onNavigate(ScreenType.Home) })
                    IconButton(
                        onClick = {
                            draftFilterState = state.activeFilterState
                            showSearchDialog = true
                        }
                    ) {
                        IconMd(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.lines_explorer_search_lines),
                            tint = TrainingTextPrimary,
                        )
                    }
                    if (hasLinesExplorerActiveFilter(state.activeFilterState)) {
                        IconButton(onClick = onClearFilter) {
                            IconMd(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.lines_explorer_clear_search),
                                tint = TrainingTextPrimary,
                            )
                        }
                    }
                    IconButton(
                        onClick = openPreviousPageAction.onClick,
                        enabled = openPreviousPageAction.canUse
                    ) {
                        IconMd(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.lines_explorer_previous_page),
                            tint = resolvePageArrowTint(openPreviousPageAction.canUse),
                        )
                    }
                    IconButton(
                        onClick = openNextPageAction.onClick,
                        enabled = openNextPageAction.canUse
                    ) {
                        IconMd(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.lines_explorer_next_page),
                            tint = resolvePageArrowTint(openNextPageAction.canUse),
                        )
                    }
                }
            )
        },
        bottomBar = {
            LinesExplorerBoardControlsBar(
                canUndo = state.lineController.canUndo,
                canRedo = state.lineController.canRedo,
                hasSelection = hasSelectedLine,
                hasLineActions = hasLineActions,
                simpleViewEnabled = state.simpleViewEnabled,
                onPrevClick = { state.lineController.undoMove() },
                onLineActionsClick = {
                    if (hasLineActions) {
                        showLineActionsDialog.value = true
                    }
                },
                onNextClick = { state.lineController.redoMove() },
                onEditClick = {
                    selectedLine?.let { line -> onOpenLineEditor(line.line) }
                },
                onDeleteClick = {
                    if (hasSelectedLine) {
                        showDeleteDialog.value = true
                    }
                },
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

            if (selectedLine == null) {
                ChessBoardSection(lineController = state.lineController)
                Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            }

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = TrainingAccentTeal)
                    }
                }

                state.parsedLines.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BodySecondaryText(
                            text = emptyLinesMessage,
                            color = TextColor.Secondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    displayedLines.forEach { indexedLine ->
                        val lineIdx = indexedLine.index
                        val parsedLine = indexedLine.value
                        val isSelected = lineIdx == state.selectedLineIdx

                        if (isSelected) {
                            ChessBoardSection(lineController = state.lineController)
                            Spacer(modifier = Modifier.height(AppDimens.spaceMd))
                            SectionTitleText(
                                text = parsedLine.line.event ?: stringResource(R.string.lines_explorer_default_line_name)
                            )
                            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
                        }

                        LineBlock(
                            parsedLine = parsedLine,
                            isSelected = isSelected,
                            lineController = state.lineController,
                            onSelectClick = { onMovePlyClick(lineIdx, 0) },
                            onMovePlyClick = { ply -> onMovePlyClick(lineIdx, ply) },
                        )
                        Spacer(modifier = Modifier.height(AppDimens.spaceMd))
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceXl))
        }
    }
}

private fun createDeleteLineAction(
    scope: CoroutineScope,
    inDbProvider: DatabaseProvider,
    observableLinesPage: RuntimeContext.ObservableLinesPage,
    lineController: LineController,
    onSelectedLineIdxChange: (Int) -> Unit,
    onDeletedLineId: (Long) -> Unit,
): (Long) -> Unit {
    return { lineId ->
        scope.launch {
            withContext(Dispatchers.IO) {
                inDbProvider.createLineDeleter().deleteLine(lineId)
            }

            observableLinesPage.removeLineId(lineId)
            onDeletedLineId(lineId)
            onSelectedLineIdxChange(-1)
            lineController.resetToStartPosition()
        }
    }
}

private fun resolveDisplayedSelectedLine(
    displayedLines: List<IndexedValue<ParsedLine>>,
    selectedLineIdx: Int
): ParsedLine? {
    return displayedLines.firstOrNull { indexedLine ->
        indexedLine.index == selectedLineIdx
    }?.value
}

private fun resolveLinesExplorerBoardOrientation(parsedLine: ParsedLine?): BoardOrientation {
    if (parsedLine?.line?.sideMask == SideMask.BLACK) {
        return BoardOrientation.BLACK
    }

    return BoardOrientation.WHITE
}

private fun RuntimeContext.ObservableLinesPage.FilterCriteria.toLinesExplorerFilterState(): LinesExplorerFilterState {
    return LinesExplorerFilterState(
        query = query,
        isCaseSensitive = isCaseSensitive,
        dubiousOnly = dubiousOnly,
        sideFilter = LinesExplorerSideFilter.fromSideMask(sideMask),
    )
}

private fun LinesExplorerFilterState.toRuntimeFilterCriteria(): RuntimeContext.ObservableLinesPage.FilterCriteria {
    return RuntimeContext.ObservableLinesPage.FilterCriteria(
        query = query,
        isCaseSensitive = isCaseSensitive,
        dubiousOnly = dubiousOnly,
        sideMask = sideFilter.sideMask,
    )
}

private fun hasLinesExplorerActiveFilter(filterState: LinesExplorerFilterState): Boolean {
    return filterState.query.isNotBlank() ||
        filterState.dubiousOnly ||
        filterState.sideFilter != LinesExplorerSideFilter.ANY
}

private fun resolveLinesExplorerCurrentPage(
    totalLinesCount: Int,
    activeOffset: Int,
): Int {
    if (totalLinesCount == 0) {
        return 1
    }

    return activeOffset / RuntimeContext.LinesExplorerPageLimit + 1
}

private fun resolveLinesExplorerTotalPages(totalLinesCount: Int): Int {
    if (totalLinesCount <= 0) {
        return 1
    }

    return (totalLinesCount + RuntimeContext.LinesExplorerPageLimit - 1) /
        RuntimeContext.LinesExplorerPageLimit
}

private fun resolveLinesExplorerActiveOffset(
    filteredLineIds: List<Long>?,
    filteredOffset: Int,
    defaultOffset: Int,
): Int {
    if (filteredLineIds != null) {
        return filteredOffset
    }

    return defaultOffset
}

private fun resolveOffsetAfterFilteredRemove(
    currentOffset: Int,
    nextLinesCount: Int,
): Int {
    if (nextLinesCount <= 0) {
        return 0
    }

    if (currentOffset < nextLinesCount) {
        return currentOffset
    }

    return ((nextLinesCount - 1) / RuntimeContext.LinesExplorerPageLimit) *
        RuntimeContext.LinesExplorerPageLimit
}
