package com.example.chessboard.ui.screen.fenpositions.details

/*
 * File role: loads one FEN position and connects it to the details screen.
 * Allowed here:
 * - loading details and continuations by database id and mapping service data to screen state
 * - coordinating edit/delete flows for the loaded position and forwarding screen actions
 * Not allowed here:
 * - app-wide routing or details presentation
 * Validation date: 2026-09-02
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.chessboard.service.FenPositionDetailsData
import com.example.chessboard.service.FenPositionContinuationService
import com.example.chessboard.service.FenPositionService
import com.example.chessboard.ui.screen.fenpositions.continuations.buildFenPositionContinuationSanLine
import com.example.chessboard.ui.screen.fenpositions.FenPositionDeletionFlow
import com.example.chessboard.ui.screen.fenpositions.fenPositionDeletionStrings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FenPositionDetailsScreenContainer(
    positionId: Long,
    fenPositionService: FenPositionService,
    fenPositionContinuationService: FenPositionContinuationService,
    onBackClick: () -> Unit,
    onOpenPosition: (Long, Int) -> Unit,
    onAddContinuation: (Long) -> Unit,
    onPositionDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var uiState by remember(positionId, fenPositionService, fenPositionContinuationService) {
        mutableStateOf<FenPositionDetailsUiState>(FenPositionDetailsUiState.Loading)
    }
    var reloadRevision by remember(positionId, fenPositionService, fenPositionContinuationService) { mutableIntStateOf(0) }
    var isEditDialogVisible by remember(positionId) { mutableStateOf(false) }
    var isDeleteDialogVisible by remember(positionId) { mutableStateOf(false) }
    val strings = fenPositionDetailsStrings()
    val deletionStrings = fenPositionDeletionStrings()

    fun openPreviousPosition() {
        val position = (uiState as? FenPositionDetailsUiState.Content)?.position ?: return
        val previousPositionId = position.previousPositionId ?: return
        onOpenPosition(previousPositionId, position.catalogIndex - 1)
    }

    fun openNextPosition() {
        val position = (uiState as? FenPositionDetailsUiState.Content)?.position ?: return
        val nextPositionId = position.nextPositionId ?: return
        onOpenPosition(nextPositionId, position.catalogIndex + 1)
    }

    LaunchedEffect(positionId, fenPositionService, fenPositionContinuationService, reloadRevision) {
        uiState = FenPositionDetailsUiState.Loading
        val details = try {
            withContext(Dispatchers.IO) {
                val details = fenPositionService.getDetailsById(positionId)
                    ?: return@withContext null
                val continuationSanLines = fenPositionContinuationService
                    .getUciLinesByPositionId(positionId)
                    .map { uciMoves ->
                        buildFenPositionContinuationSanLine(
                            uciMoves = uciMoves,
                            startFen = details.fen,
                        )
                    }
                details to continuationSanLines
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (_: Exception) {
            uiState = FenPositionDetailsUiState.LoadFailed
            return@LaunchedEffect
        }

        if (details == null) {
            uiState = FenPositionDetailsUiState.NotFound
            return@LaunchedEffect
        }

        uiState = FenPositionDetailsUiState.Content(
            details.first.toDetailsItem(details.second),
        )
    }

    FenPositionDetailsScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onPreviousPositionClick = ::openPreviousPosition,
        onNextPositionClick = ::openNextPosition,
        onEditPositionClick = {
            isEditDialogVisible = true
        },
        onDeletePositionClick = {
            isDeleteDialogVisible = true
        },
        onAddContinuationClick = {
            onAddContinuation(positionId)
        },
        modifier = modifier,
    )

    val loadedPosition = (uiState as? FenPositionDetailsUiState.Content)?.position
    if (isEditDialogVisible && loadedPosition != null) {
        FenPositionEditFlow(
            position = loadedPosition,
            fenPositionService = fenPositionService,
            strings = strings.editDialog,
            onDismiss = {
                isEditDialogVisible = false
            },
            onUpdated = {
                isEditDialogVisible = false
                reloadRevision += 1
            },
            onPositionMissing = {
                isEditDialogVisible = false
                reloadRevision += 1
            },
        )
    }

    if (isDeleteDialogVisible && loadedPosition != null) {
        FenPositionDeletionFlow(
            positionId = loadedPosition.id,
            fenPositionService = fenPositionService,
            strings = deletionStrings,
            onDismiss = {
                isDeleteDialogVisible = false
            },
            onDeleted = {
                isDeleteDialogVisible = false
                onPositionDeleted()
            },
        )
    }
}

private fun FenPositionDetailsData.toDetailsItem(
    continuationSanLines: List<String>,
): FenPositionDetailsItem {
    return FenPositionDetailsItem(
        id = id,
        fen = fen,
        name = name,
        theme = theme,
        description = description,
        continuationSanLines = continuationSanLines,
        catalogIndex = catalogIndex,
        previousPositionId = previousPositionId,
        nextPositionId = nextPositionId,
    )
}
