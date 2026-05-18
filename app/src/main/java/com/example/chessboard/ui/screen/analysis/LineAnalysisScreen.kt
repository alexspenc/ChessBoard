package com.example.chessboard.ui.screen.analysis

/**
 * Isolated line-analysis screen for exploring chess lines without saving them.
 *
 * Keep analysis screen state wiring, board controls, and analysis layout here. Do not add
 * app-level navigation registration, database persistence, or training-specific workflows here.
 */
import android.content.ClipData
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.chessboard.R
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.boardmodel.LineVariationLineState
import com.example.chessboard.boardmodel.InitialBoardFen
import com.example.chessboard.service.buildAnalysisPgn
import com.example.chessboard.ui.LineAnalysisContentTestTag
import com.example.chessboard.ui.LineAnalysisMoveControlsTestTag
import com.example.chessboard.ui.LineAnalysisNextMoveTestTag
import com.example.chessboard.ui.LineAnalysisPreviousMoveTestTag
import com.example.chessboard.ui.LineAnalysisResetMovesTestTag
import com.example.chessboard.ui.LineAnalysisSearchActionTestTag
import com.example.chessboard.ui.components.AppIconSizes
import com.example.chessboard.ui.components.AppLoadingDialog
import com.example.chessboard.ui.components.HomeIconButton
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BoardActionNavigationBar
import com.example.chessboard.ui.components.BoardActionNavigationItem
import com.example.chessboard.ui.components.ChessBoardSection
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.screen.EditableLineSide
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.components.LineMoveTreeSection
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.BottomBarContentColor
import com.github.bhlangonijr.chesslib.Piece
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface LineAnalysisInitialPosition {
    data object StartPosition : LineAnalysisInitialPosition

    data class FromFen(
        val fen: String,
    ) : LineAnalysisInitialPosition

    data class FromLineLine(
        val uciMoves: List<String>,
        val initialPly: Int = uciMoves.size,
    ) : LineAnalysisInitialPosition
}

@Composable
fun LineAnalysisScreenContainer(
    screenContext: ScreenContainerContext,
    initialPosition: LineAnalysisInitialPosition = LineAnalysisInitialPosition.StartPosition,
    onSearchByPositionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lineController = remember { LineController() }
    var variationState by remember { mutableStateOf(LineVariationLineState()) }
    var selectedSide by remember { mutableStateOf(EditableLineSide.AS_WHITE) }
    val startFen = resolveAnalysisStartFen(initialPosition)

    LaunchedEffect(initialPosition) {
        selectedSide = resolveInitialAnalysisSide(initialPosition)
        variationState = resolveInitialVariationState(initialPosition)
        loadInitialAnalysisPosition(
            lineController = lineController,
            initialPosition = initialPosition,
        )
    }

    LaunchedEffect(selectedSide) {
        lineController.setOrientation(selectedSide.orientation)
    }

    LaunchedEffect(lineController) {
        snapshotFlow { lineController.boardState }.collectLatest {
            variationState = resolveNextVariationState(
                variationState = variationState,
                lineController = lineController,
            )
        }
    }

    fun syncVariationState() {
        variationState = resolveNextVariationState(
            variationState = variationState,
            lineController = lineController,
        )
    }

    fun undoAnalysisMove() {
        if (!lineController.undoMove()) {
            return
        }

        syncVariationState()
    }

    fun redoAnalysisMove() {
        if (!lineController.redoMove()) {
            return
        }

        syncVariationState()
    }

    fun resetAnalysisPosition() {
        val backingLine = variationState.backingLineFor()
        if (backingLine.isEmpty()) {
            loadOriginAnalysisPosition(
                lineController = lineController,
                startFen = startFen,
            )
            variationState = variationState.selectPath(emptyList())
            syncVariationState()
            return
        }

        lineController.loadFromUciMoves(
            uciMoves = backingLine,
            targetPly = 0,
            startFen = startFen,
        )
        variationState = variationState.selectPath(emptyList())
        syncVariationState()
    }

    LineAnalysisScreen(
        lineController = lineController,
        variationLines = variationState.lines,
        startFen = startFen,
        selectedSide = selectedSide,
        onSideSelected = { selectedSide = it },
        onBackClick = screenContext.onBackClick,
        onHomeClick = { screenContext.onNavigate(ScreenType.Home) },
        onNavigate = screenContext.onNavigate,
        onSearchByPositionClick = { onSearchByPositionClick(lineController.getFen()) },
        onPreviousMoveClick = ::undoAnalysisMove,
        onNextMoveClick = ::redoAnalysisMove,
        onResetMovesClick = ::resetAnalysisPosition,
        modifier = modifier,
    )
}

@Composable
internal fun LineAnalysisScreen(
    lineController: LineController,
    variationLines: List<List<String>>,
    startFen: String?,
    selectedSide: EditableLineSide,
    onSideSelected: (EditableLineSide) -> Unit,
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onSearchByPositionClick: () -> Unit,
    onPreviousMoveClick: () -> Unit = {},
    onNextMoveClick: () -> Unit = {},
    onResetMovesClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    @Suppress("UNUSED_VARIABLE")
    val boardState = lineController.boardState
    val canUndo = lineController.canUndo
    val canRedo = lineController.canRedo
    val moveTreeMaxHeight = LocalConfiguration.current.screenHeightDp.dp / 3
    val clipboardManager = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val canCopyAnalysisPgn = variationLines.isNotEmpty()
    var isBuildingAnalysisPgn by remember { mutableStateOf(false) }
    var showPgnCopiedDialog by remember { mutableStateOf(false) }
    var showPgnUnavailableDialog by remember { mutableStateOf(false) }

    fun copyAnalysisPgn() {
        if (!canCopyAnalysisPgn || isBuildingAnalysisPgn) {
            return
        }

        coroutineScope.launch {
            isBuildingAnalysisPgn = true
            try {
                val analysisPgn = withContext(Dispatchers.Default) {
                    buildAnalysisPgn(variationLines)
                }

                if (analysisPgn.isBlank()) {
                    showPgnUnavailableDialog = true
                    return@launch
                }

                clipboardManager.setClipEntry(
                    ClipEntry(
                        ClipData.newPlainText(
                            "Analysis PGN",
                            analysisPgn,
                        )
                    )
                )
                showPgnCopiedDialog = true
            } finally {
                isBuildingAnalysisPgn = false
            }
        }
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Analyze Line",
                subtitle = "Explore lines without saving",
                onBackClick = onBackClick,
                actions = {
                    HomeIconButton(onClick = onHomeClick)
                    IconButton(
                        onClick = ::copyAnalysisPgn,
                        enabled = canCopyAnalysisPgn,
                    ) {
                        IconMd(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Copy analysis PGN",
                            tint = if (canCopyAnalysisPgn) BottomBarContentColor else BottomBarContentColor.copy(alpha = 0.5f),
                        )
                    }
                    IconButton(
                        onClick = onSearchByPositionClick,
                        modifier = Modifier.testTag(LineAnalysisSearchActionTestTag),
                    ) {
                        IconMd(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search by position",
                        )
                    }
                },
            )
        },
        bottomBar = {
            LineAnalysisBoardControlsBar(
                selectedSide = selectedSide,
                onSideSelected = onSideSelected,
                canUndo = canUndo,
                canRedo = canRedo,
                onPreviousMoveClick = onPreviousMoveClick,
                onNextMoveClick = onNextMoveClick,
                onResetMovesClick = onResetMovesClick,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag(LineAnalysisContentTestTag),
            contentPadding = PaddingValues(
                horizontal = AppDimens.spaceLg,
                vertical = AppDimens.spaceLg,
            ),
        ) {
            item {
                ChessBoardSection(lineController = lineController)
            }

            item {
                Spacer(modifier = Modifier.height(AppDimens.spaceLg))
                LineMoveTreeSection(
                    importedUciLines = variationLines,
                    lineController = lineController,
                    startFen = startFen,
                    maxContentHeight = moveTreeMaxHeight,
                )
            }

            item {
                Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            }
        }
    }

    if (showPgnCopiedDialog) {
        AppMessageDialog(
            title = "PGN copied",
            message = "Analysis PGN was copied to the clipboard.",
            onDismiss = { showPgnCopiedDialog = false },
        )
    }

    if (showPgnUnavailableDialog) {
        AppMessageDialog(
            title = "PGN unavailable",
            message = "Analysis PGN could not be built.",
            onDismiss = { showPgnUnavailableDialog = false },
        )
    }

    if (isBuildingAnalysisPgn) {
        AppLoadingDialog(
            title = "Building PGN",
            message = "Preparing analysis PGN...",
        )
    }

}

@Composable
private fun LineAnalysisBoardControlsBar(
    selectedSide: EditableLineSide,
    onSideSelected: (EditableLineSide) -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onPreviousMoveClick: () -> Unit,
    onNextMoveClick: () -> Unit,
    onResetMovesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoardActionNavigationBar(
        modifier = modifier.testTag(LineAnalysisMoveControlsTestTag),
        items = EditableLineSide.entries.map { side ->
            BoardActionNavigationItem(
                label = if (side == EditableLineSide.AS_WHITE) "White" else "Black",
                selected = side == selectedSide,
                onClick = { onSideSelected(side) },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_king),
                    contentDescription = side.toDisplayText(),
                    tint = if (side == selectedSide) TrainingAccentTeal else BottomBarContentColor,
                    modifier = Modifier.size(AppIconSizes.Lg),
                )
            }
        } + listOf(
            BoardActionNavigationItem(
                label = "Reset",
                enabled = canUndo,
                modifier = Modifier.testTag(LineAnalysisResetMovesTestTag),
                onClick = onResetMovesClick,
            ) {
                IconMd(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = if (canUndo) BottomBarContentColor else BottomBarContentColor.copy(alpha = 0.5f),
                )
            },
            BoardActionNavigationItem(
                label = "Back",
                enabled = canUndo,
                modifier = Modifier.testTag(LineAnalysisPreviousMoveTestTag),
                onClick = onPreviousMoveClick,
            ) {
                IconMd(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous move",
                    tint = if (canUndo) BottomBarContentColor else BottomBarContentColor.copy(alpha = 0.5f),
                )
            },
            BoardActionNavigationItem(
                label = "Forward",
                enabled = canRedo,
                modifier = Modifier.testTag(LineAnalysisNextMoveTestTag),
                onClick = onNextMoveClick,
            ) {
                IconMd(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next move",
                    tint = if (canRedo) BottomBarContentColor else BottomBarContentColor.copy(alpha = 0.5f),
                )
            },
        ),
    )
}

private fun resolveInitialAnalysisSide(
    initialPosition: LineAnalysisInitialPosition,
): EditableLineSide {
    if (initialPosition == LineAnalysisInitialPosition.StartPosition) {
        return EditableLineSide.AS_WHITE
    }

    if (initialPosition is LineAnalysisInitialPosition.FromFen) {
        if (resolveAnalysisFenSideToken(initialPosition.fen) == "b") {
            return EditableLineSide.AS_BLACK
        }

        return EditableLineSide.AS_WHITE
    }

    val lineLinePosition = initialPosition as LineAnalysisInitialPosition.FromLineLine
    if (lineLinePosition.initialPly.coerceAtLeast(0) % 2 == 1) {
        return EditableLineSide.AS_BLACK
    }

    return EditableLineSide.AS_WHITE
}

private fun resolveAnalysisStartFen(initialPosition: LineAnalysisInitialPosition): String? {
    if (initialPosition is LineAnalysisInitialPosition.FromFen) {
        return normalizeAnalysisFen(initialPosition.fen)
    }

    return null
}

private fun resolveInitialVariationState(
    initialPosition: LineAnalysisInitialPosition,
): LineVariationLineState {
    if (initialPosition !is LineAnalysisInitialPosition.FromLineLine) {
        return LineVariationLineState()
    }

    val normalizedMoves = normalizeUciPath(initialPosition.uciMoves)
    val selectedPath = normalizedMoves.take(initialPosition.initialPly.coerceAtLeast(0))
    return LineVariationLineState()
        .recordPlayedPath(normalizedMoves)
        .selectPath(selectedPath)
}

private fun loadInitialAnalysisPosition(
    lineController: LineController,
    initialPosition: LineAnalysisInitialPosition,
) {
    when (initialPosition) {
        LineAnalysisInitialPosition.StartPosition -> {
            lineController.resetToStartPosition()
        }
        is LineAnalysisInitialPosition.FromFen -> {
            loadOriginAnalysisPosition(
                lineController = lineController,
                startFen = normalizeAnalysisFen(initialPosition.fen),
            )
        }
        is LineAnalysisInitialPosition.FromLineLine -> {
            lineController.loadFromUciMoves(
                uciMoves = normalizeUciPath(initialPosition.uciMoves),
                targetPly = initialPosition.initialPly,
            )
        }
    }
}

private fun loadOriginAnalysisPosition(
    lineController: LineController,
    startFen: String?,
) {
    val normalizedFen = normalizeAnalysisFen(startFen)
    if (normalizedFen.isBlank()) {
        lineController.resetToStartPosition()
        return
    }

    if (lineController.loadFromFen(normalizedFen)) {
        return
    }

    lineController.loadFromFen(InitialBoardFen)
}

internal fun resolveNextVariationState(
    variationState: LineVariationLineState,
    lineController: LineController,
): LineVariationLineState {
    val fullLine = resolveControllerUciLine(lineController)
    val currentPath = resolveControllerUciLine(
        lineController = lineController,
        upToPly = lineController.currentMoveIndex,
    )

    if (currentPath.isEmpty()) {
        if (fullLine.isNotEmpty()) {
            return variationState
                .recordPlayedPath(fullLine)
                .selectPath(emptyList())
        }

        return variationState.selectPath(emptyList())
    }

    if (currentPath.size < fullLine.size) {
        return variationState.selectPath(currentPath)
    }

    return variationState.recordPlayedPath(currentPath)
}

private fun resolveControllerUciLine(
    lineController: LineController,
    upToPly: Int = lineController.getMovesCopy().size,
): List<String> {
    return lineController.getMovesCopy().take(upToPly).map { move ->
        buildString {
            append(move.from.value().lowercase())
            append(move.to.value().lowercase())
            if (move.promotion != Piece.NONE) {
                append(move.promotion.pieceType.name.first().lowercaseChar())
            }
        }
    }
}

private fun normalizeUciPath(moves: List<String>): List<String> {
    return moves.map { it.trim().lowercase() }.filter { it.isNotEmpty() }
}

private fun resolveAnalysisFenSideToken(fen: String): String {
    return fen.trim().split(Regex("\\s+")).getOrNull(1) ?: "w"
}

private fun normalizeAnalysisFen(fen: String?): String {
    val normalizedFen = fen?.trim().orEmpty()
    if (normalizedFen.isBlank()) {
        return ""
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
