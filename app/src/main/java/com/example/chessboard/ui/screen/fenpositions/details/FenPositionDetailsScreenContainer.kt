package com.example.chessboard.ui.screen.fenpositions.details

/*
 * File role: loads one FEN position and connects it to the details screen.
 * Allowed here:
 * - loading details by database id and mapping service data to screen state
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
import com.example.chessboard.service.FenPositionService
import com.example.chessboard.ui.screen.fenpositions.FenPositionDeletionFlow
import com.example.chessboard.ui.screen.fenpositions.fenPositionDeletionStrings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FenPositionDetailsScreenContainer(
    positionId: Long,
    fenPositionService: FenPositionService,
    onBackClick: () -> Unit,
    onOpenPosition: (Long, Int) -> Unit,
    onPositionDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var uiState by remember(positionId, fenPositionService) {
        mutableStateOf<FenPositionDetailsUiState>(FenPositionDetailsUiState.Loading)
    }
    var reloadRevision by remember(positionId, fenPositionService) { mutableIntStateOf(0) }
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

    LaunchedEffect(positionId, fenPositionService, reloadRevision) {
        uiState = FenPositionDetailsUiState.Loading
        val details = try {
            withContext(Dispatchers.IO) {
                fenPositionService.getDetailsById(positionId)
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

        uiState = FenPositionDetailsUiState.Content(details.toDetailsItem())
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

private fun FenPositionDetailsData.toDetailsItem(): FenPositionDetailsItem {
    return FenPositionDetailsItem(
        id = id,
        fen = fen,
        name = name,
        theme = theme,
        description = description,
        catalogIndex = catalogIndex,
        previousPositionId = previousPositionId,
        nextPositionId = nextPositionId,
    )
}
