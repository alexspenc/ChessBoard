package com.example.chessboard.ui.screen.fenpositions.continuations

/*
 * File role: displays one stored FEN-position continuation and its replay controls.
 * Allowed here:
 * - continuation loading, read-only board replay, SAN presentation, analysis, and deletion flow
 * Not allowed here:
 * - continuation parsing, insertion, or app-wide navigation
 * Validation date: 2026-09-03
 */

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.service.FenPositionContinuationService
import com.example.chessboard.service.FenPositionService
import com.example.chessboard.ui.components.AppConfirmDialog
import com.example.chessboard.ui.components.AppLoadingDialog
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BoardActionNavigationBar
import com.example.chessboard.ui.components.BoardActionNavigationItem
import com.example.chessboard.ui.components.ChessBoardSection
import com.example.chessboard.ui.components.HomeIconButton
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.LineMoveTreeSection
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.screen.fenpositions.resolveFenPositionBoardOrientation
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationDetailsBoardTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationDetailsAnalyzeTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationDetailsContentTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationDetailsDeleteTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationDetailsDeleteConfirmTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationDetailsLoadingTestTag
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingErrorRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class ContinuationDetailsState(
    val positionName: String,
    val startFen: String,
    val uciMoves: List<String>,
    val lineIndex: Int,
    val linesCount: Int,
    val previousId: Long?,
    val nextId: Long?,
)

@Composable
fun FenPositionContinuationDetailsScreenContainer(
    positionId: Long,
    continuationId: Long,
    fenPositionService: FenPositionService,
    continuationService: FenPositionContinuationService,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onOpenContinuation: (Long) -> Unit,
    onAnalyzeContinuation: (String, List<String>, Int) -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var state by remember(positionId, continuationId) { mutableStateOf<ContinuationDetailsState?>(null) }
    var loading by remember(positionId, continuationId) { mutableStateOf(true) }
    var deleteRequested by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var deleteFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            state = withContext(Dispatchers.IO) {
                val position = fenPositionService.getDetailsById(positionId) ?: return@withContext null
                val lines = continuationService.getByPositionId(positionId)
                val index = lines.indexOfFirst { line -> line.id == continuationId }
                val selected = lines.getOrNull(index) ?: return@withContext null
                val moves = selected.uciMoves.split(' ').filter { move -> move.isNotBlank() }
                ContinuationDetailsState(
                    positionName = position.name,
                    startFen = position.fen,
                    uciMoves = moves,
                    lineIndex = index,
                    linesCount = lines.size,
                    previousId = lines.getOrNull(index - 1)?.id,
                    nextId = lines.getOrNull(index + 1)?.id,
                )
            }
            loading = false
        }
    }

    LaunchedEffect(positionId, continuationId) { reload() }

    FenPositionContinuationDetailsScreen(
        state = state,
        loading = loading,
        deleting = deleting,
        onBackClick = onBackClick,
        onHomeClick = onHomeClick,
        onPreviousClick = { state?.previousId?.let(onOpenContinuation) },
        onNextClick = { state?.nextId?.let(onOpenContinuation) },
        onAnalyzeClick = onAnalyzeContinuation,
        onDeleteClick = { deleteRequested = true },
        modifier = modifier,
    )

    if (deleteRequested && state != null && !deleting) {
        AppConfirmDialog(
            title = stringResource(R.string.fen_position_continuation_delete_title),
            message = stringResource(R.string.fen_position_continuation_delete_message),
            confirmText = stringResource(R.string.common_delete),
            confirmButtonModifier = Modifier.testTag(
                FenPositionContinuationDetailsDeleteConfirmTestTag,
            ),
            isDestructive = true,
            onDismiss = { deleteRequested = false },
            onConfirm = {
                deleteRequested = false
                deleting = true
                scope.launch {
                    val success = withContext(Dispatchers.IO) {
                        continuationService.deleteById(continuationId)
                    }
                    deleting = false
                    if (success) onDeleted() else deleteFailed = true
                }
            },
        )
    }
    if (deleting) {
        AppLoadingDialog(
            title = stringResource(R.string.fen_position_continuation_deleting_title),
            message = stringResource(R.string.fen_position_continuation_deleting_message),
        )
    }
    if (deleteFailed) {
        AppMessageDialog(
            title = stringResource(R.string.fen_position_continuation_delete_failed_title),
            message = stringResource(R.string.fen_position_continuation_delete_failed_message),
            onDismiss = { deleteFailed = false },
        )
    }
}

@Composable
private fun FenPositionContinuationDetailsScreen(
    state: ContinuationDetailsState?,
    loading: Boolean,
    deleting: Boolean,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onAnalyzeClick: (String, List<String>, Int) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier,
) {
    var currentPly by remember(state?.uciMoves) { mutableIntStateOf(0) }

    fun analyzeCurrentLine() {
        val currentState = state ?: return
        onAnalyzeClick(currentState.startFen, currentState.uciMoves, currentPly)
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.fen_position_continuation_details_title),
                subtitleLines = listOfNotNull(state?.positionName),
                onBackClick = onBackClick,
                handleSystemBack = true,
                filledBackButton = true,
                actions = {
                    HomeIconButton(onClick = onHomeClick)
                    IconButton(onClick = onPreviousClick, enabled = state?.previousId != null) {
                        IconMd(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            stringResource(R.string.fen_position_continuation_previous_content_description),
                        )
                    }
                    IconButton(onClick = onNextClick, enabled = state?.nextId != null) {
                        IconMd(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            stringResource(R.string.fen_position_continuation_next_content_description),
                        )
                    }
                },
            )
        },
        bottomBar = {
            BoardActionNavigationBar(
                items = listOf(
                    BoardActionNavigationItem(
                        label = stringResource(R.string.common_back),
                        enabled = currentPly > 0 && !deleting,
                        onClick = { currentPly -= 1 },
                    ) {
                        IconMd(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            stringResource(R.string.common_back),
                        )
                    },
                    BoardActionNavigationItem(
                        label = stringResource(R.string.common_forward),
                        enabled = state != null && currentPly < state.uciMoves.size && !deleting,
                        onClick = { currentPly += 1 },
                    ) {
                        IconMd(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            stringResource(R.string.common_forward),
                        )
                    },
                    BoardActionNavigationItem(
                        label = stringResource(R.string.fen_position_analysis_board_label),
                        enabled = !deleting && state != null,
                        modifier = Modifier.testTag(FenPositionContinuationDetailsAnalyzeTestTag),
                        onClick = ::analyzeCurrentLine,
                    ) {
                        IconMd(
                            Icons.Default.Analytics,
                            stringResource(R.string.fen_position_analysis_board_content_description),
                        )
                    },
                    BoardActionNavigationItem(
                        label = stringResource(R.string.common_delete),
                        enabled = !deleting && state != null,
                        modifier = Modifier.testTag(FenPositionContinuationDetailsDeleteTestTag),
                        onClick = onDeleteClick,
                    ) {
                        IconMd(Icons.Default.Delete, stringResource(R.string.common_delete), tint = TrainingErrorRed)
                    },
                ),
            )
        },
    ) { paddingValues ->
        if (loading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag(FenPositionContinuationDetailsLoadingTestTag),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator(color = TrainingAccentTeal) }
            return@AppScreenScaffold
        }
        val currentState = state ?: return@AppScreenScaffold
        val controller = remember(currentState.startFen, currentState.uciMoves, currentPly) {
            LineController().also { lineController ->
                lineController.loadFromUciMoves(
                    currentState.uciMoves,
                    targetPly = currentPly,
                    startFen = currentState.startFen,
                )
                lineController.setOrientation(resolveFenPositionBoardOrientation(currentState.startFen))
                lineController.setUserMovesEnabled(false)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(AppDimens.spaceLg)
                .testTag(FenPositionContinuationDetailsContentTestTag),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
        ) {
            ScreenTitleText(text = "${currentState.lineIndex + 1} / ${currentState.linesCount}")
            ChessBoardSection(
                lineController = controller,
                modifier = Modifier.testTag(FenPositionContinuationDetailsBoardTestTag),
            )
            LineMoveTreeSection(
                importedUciLines = listOf(currentState.uciMoves),
                lineController = controller,
                startFen = currentState.startFen,
                onMoveSelected = { _, targetPly -> currentPly = targetPly },
            )
        }
    }
}
