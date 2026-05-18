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
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.example.chessboard.service.parsePgnMoves
import com.example.chessboard.ui.BoardOrientation
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
    val lineController = remember { LineController() }
    val clipboard = LocalClipboard.current
    val parsedLines = remember { mutableStateListOf<ParsedLine>() }
    val scope = rememberCoroutineScope()
    val observableLinesState = observableLinesPage.state
    var isLoading by remember { mutableStateOf(true) }
    var selectedLineIdx by remember { mutableIntStateOf(-1) }
    var activeFilterState by remember { mutableStateOf(LinesExplorerFilterState()) }
    var filteredLineIds by remember { mutableStateOf<List<Long>?>(null) }
    var filteredOffset by remember { mutableIntStateOf(0) }
    var isBuildingLinesPgn by remember { mutableStateOf(false) }
    var linesPgnMessage by remember { mutableStateOf<LinesExplorerPgnMessage?>(null) }

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

    fun clearLinesFilter() {
        activeFilterState = LinesExplorerFilterState()
        filteredLineIds = null
        filteredOffset = 0
    }

    fun applyLinesFilter(filterState: LinesExplorerFilterState) {
        scope.launch {
            if (!hasLinesExplorerActiveFilter(filterState)) {
                clearLinesFilter()
                return@launch
            }

            isLoading = true
            val matchingLineIds = withContext(Dispatchers.IO) {
                val linesCount = lineListService.countLinesByName(
                    query = filterState.query,
                    isCaseSensitive = filterState.isCaseSensitive,
                )
                if (linesCount <= 0) {
                    return@withContext emptyList()
                }

                lineListService.searchLineIdsByName(
                    query = filterState.query,
                    isCaseSensitive = filterState.isCaseSensitive,
                    limit = linesCount,
                    offset = 0,
                )
            }

            activeFilterState = filterState
            filteredLineIds = matchingLineIds
            filteredOffset = 0
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
                        title = "PGN unavailable",
                        message = "Lines PGN could not be built.",
                    )
                    return@launch
                }

                clipboard.setClipEntry(
                    ClipEntry(
                        ClipData.newPlainText(
                            "Lines Explorer PGN",
                            linesPgn,
                        )
                    )
                )
                linesPgnMessage = LinesExplorerPgnMessage(
                    title = "PGN copied",
                    message = "Lines PGN was copied to the clipboard.",
                )
            } finally {
                isBuildingLinesPgn = false
            }
        }
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
            title = "Building PGN",
            message = "Preparing lines PGN...",
        )
    }

    LinesExplorerScreen(
        lineController = lineController,
        parsedLines = parsedLines,
        isLoading = isLoading,
        activeFilterState = activeFilterState,
        selectedLineIdx = selectedLineIdx,
        totalLinesCount = totalLinesCount,
        currentPage = currentPage,
        totalPages = totalPages,
        canOpenPreviousPage = canOpenPreviousPage,
        canOpenNextPage = canOpenNextPage,
        simpleViewEnabled = simpleViewEnabled,
        canCopyLinesPgn = activeLineIds.isNotEmpty() && !isBuildingLinesPgn,
        modifier = modifier,
        onBackClick = screenContext.onBackClick,
        onNavigate = screenContext.onNavigate,
        onOpenLineEditor = onOpenLineEditor,
        onAnalyzeLineClick = onAnalyzeLineClick,
        onCopyLinesPgnClick = ::copyExplorerLinesPgn,
        onApplyFilter = ::applyLinesFilter,
        onClearFilter = ::clearLinesFilter,
        onOpenPreviousPageClick = {
            if (filteredLineIds != null) {
                filteredOffset = (filteredOffset - RuntimeContext.LinesExplorerPageLimit).coerceAtLeast(0)
            } else {
                observableLinesPage.openPreviousPage()
            }
        },
        onOpenNextPageClick = {
            if (filteredLineIds != null) {
                filteredOffset += RuntimeContext.LinesExplorerPageLimit
            } else {
                observableLinesPage.openNextPage()
            }
        },
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
                    filteredLineIds = currentFilteredIds.filterNot { lineId -> lineId == deletedLineId }
                    filteredOffset = resolveOffsetAfterFilteredRemove(
                        currentOffset = filteredOffset,
                        nextLinesCount = filteredLineIds.orEmpty().size,
                    )
                }
            },
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LinesExplorerScreen(
    lineController: LineController,
    parsedLines: List<ParsedLine> = emptyList(),
    isLoading: Boolean = false,
    activeFilterState: LinesExplorerFilterState = LinesExplorerFilterState(),
    selectedLineIdx: Int = -1,
    totalLinesCount: Int = 0,
    currentPage: Int = 1,
    totalPages: Int = 1,
    canOpenPreviousPage: Boolean = false,
    canOpenNextPage: Boolean = false,
    simpleViewEnabled: Boolean = false,
    canCopyLinesPgn: Boolean,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onOpenLineEditor: (LineEntity) -> Unit = {},
    onCloneLineClick: (LineEntity) -> Unit = {},
    onAnalyzeLineClick: (List<String>, Int) -> Unit = { _, _ -> },
    onCopyLinesPgnClick: () -> Unit,
    onApplyFilter: (LinesExplorerFilterState) -> Unit = {},
    onClearFilter: () -> Unit = {},
    onOpenPreviousPageClick: () -> Unit = {},
    onOpenNextPageClick: () -> Unit = {},
    onMovePlyClick: (lineIdx: Int, ply: Int) -> Unit = { _, _ -> },
    onDeleteLineClick: (lineId: Long) -> Unit = {},
) {
    fun resolvePageArrowTint(isEnabled: Boolean) = if (isEnabled) {
        TrainingTextPrimary
    } else {
        MutedContentColor
    }

    fun resolveLinesExplorerSubtitle(): String {
        if (hasLinesExplorerActiveFilter(activeFilterState)) {
            return "Found: $totalLinesCount • Page $currentPage/$totalPages"
        }

        return "Lines: $totalLinesCount • Page $currentPage/$totalPages"
    }

    val currentPly = lineController.currentMoveIndex
    var showSearchDialog by remember { mutableStateOf(false) }
    var draftFilterState by remember { mutableStateOf(activeFilterState) }
    val displayedLines = parsedLines.withIndex().toList()
    val selectedLine = resolveDisplayedSelectedLine(
        displayedLines = displayedLines,
        selectedLineIdx = selectedLineIdx
    )
    val hasSelectedLine = selectedLine != null && selectedLineIdx >= 0
    val hasLineActions = hasSelectedLine || canCopyLinesPgn
    var showDeleteDialog by remember(selectedLine?.line?.id) { mutableStateOf(false) }
    var showLineActionsDialog by remember(selectedLine?.line?.id) { mutableStateOf(false) }

    SideEffect {
        lineController.setUserMovesEnabled(false)
        lineController.setOrientation(resolveLinesExplorerBoardOrientation(selectedLine))
    }

    if (showDeleteDialog && selectedLine != null) {
        AppConfirmDialog(
            title = "Delete Line",
            message = resolveDeleteLineMessage(selectedLine),
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                onDeleteLineClick(selectedLine.line.id)
            },
            confirmText = "Delete",
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
        visible = showLineActionsDialog && hasLineActions,
        onDismiss = { showLineActionsDialog = false },
        onResetClick = {
            showLineActionsDialog = false
            if (hasSelectedLine) {
                onMovePlyClick(selectedLineIdx, 0)
            }
        },
        onAnalyzeClick = {
            showLineActionsDialog = false
            selectedLine?.let { line ->
                onAnalyzeLineClick(
                    line.uciMoves,
                    currentPly.coerceIn(0, line.uciMoves.size),
                )
            }
        },
        onCloneClick = {
            showLineActionsDialog = false
            selectedLine?.let { line -> onCloneLineClick(line.line) }
        },
        canUseSelectedLineActions = hasSelectedLine,
        canCopyLinesPgn = canCopyLinesPgn,
        onCopyLinesPgnClick = {
            showLineActionsDialog = false
            onCopyLinesPgnClick()
        },
    )

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Lines Explorer",
                subtitle = resolveLinesExplorerSubtitle(),
                onBackClick = onBackClick,
                filledBackButton = true,
                actions = {
                    HomeIconButton(onClick = { onNavigate(ScreenType.Home) })
                    IconButton(
                        onClick = {
                            draftFilterState = activeFilterState
                            showSearchDialog = true
                        }
                    ) {
                        IconMd(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search lines",
                            tint = TrainingTextPrimary,
                        )
                    }
                    if (hasLinesExplorerActiveFilter(activeFilterState)) {
                        IconButton(onClick = onClearFilter) {
                            IconMd(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Clear lines search",
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
                            contentDescription = "Previous lines page",
                            tint = resolvePageArrowTint(canOpenPreviousPage),
                        )
                    }
                    IconButton(
                        onClick = onOpenNextPageClick,
                        enabled = canOpenNextPage
                    ) {
                        IconMd(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next lines page",
                            tint = resolvePageArrowTint(canOpenNextPage),
                        )
                    }
                }
            )
        },
        bottomBar = {
            LinesExplorerBoardControlsBar(
                canUndo = lineController.canUndo,
                canRedo = lineController.canRedo,
                hasSelection = hasSelectedLine,
                hasLineActions = hasLineActions,
                simpleViewEnabled = simpleViewEnabled,
                onPrevClick = { lineController.undoMove() },
                onLineActionsClick = {
                    if (hasLineActions) {
                        showLineActionsDialog = true
                    }
                },
                onNextClick = { lineController.redoMove() },
                onEditClick = {
                    selectedLine?.let { line -> onOpenLineEditor(line.line) }
                },
                onDeleteClick = {
                    if (hasSelectedLine) {
                        showDeleteDialog = true
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
                ChessBoardSection(lineController = lineController)
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

                parsedLines.isEmpty() -> {
                    val emptyMessage = if (hasLinesExplorerActiveFilter(activeFilterState)) {
                        "No lines match the current filter."
                    } else {
                        "No saved lines.\nGo to Home to create openings."
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
                    displayedLines.forEach { indexedLine ->
                        val lineIdx = indexedLine.index
                        val parsedLine = indexedLine.value
                        val isSelected = lineIdx == selectedLineIdx

                        if (isSelected) {
                            ChessBoardSection(lineController = lineController)
                            Spacer(modifier = Modifier.height(AppDimens.spaceMd))
                            SectionTitleText(text = parsedLine.line.event ?: "Opening")
                            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
                        }

                        LineBlock(
                            parsedLine = parsedLine,
                            isSelected = isSelected,
                            lineController = lineController,
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
    onDeletedLineId: (Long) -> Unit = {},
): (Long) -> Unit {
    return { lineId ->
        scope.launch {
            withContext(Dispatchers.IO) {
                inDbProvider.deleteLine(lineId)
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

private fun resolveDeleteLineMessage(parsedLine: ParsedLine): String {
    val lineName = parsedLine.line.event ?: "this line"
    return "Delete \"$lineName\"?\nLine ID: ${parsedLine.line.id}"
}

private fun resolveLinesExplorerBoardOrientation(parsedLine: ParsedLine?): BoardOrientation {
    if (parsedLine?.line?.sideMask == SideMask.BLACK) {
        return BoardOrientation.BLACK
    }

    return BoardOrientation.WHITE
}

private fun hasLinesExplorerActiveFilter(filterState: LinesExplorerFilterState): Boolean {
    return filterState.query.isNotBlank()
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
