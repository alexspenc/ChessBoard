package com.example.chessboard.ui.screen.fenpositions.details

/*
 * File role: loads one FEN position and connects it to the read-only details screen.
 * Allowed here:
 * - loading details by database id and mapping service data to screen state
 * - forwarding the required back action
 * Not allowed here:
 * - persistence mutations, app-wide routing, or details presentation
 * Validation date: 2026-09-01
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.chessboard.service.FenPositionDetailsData
import com.example.chessboard.service.FenPositionService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FenPositionDetailsScreenContainer(
    positionId: Long,
    fenPositionService: FenPositionService,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var uiState by remember(positionId, fenPositionService) {
        mutableStateOf<FenPositionDetailsUiState>(FenPositionDetailsUiState.Loading)
    }

    LaunchedEffect(positionId, fenPositionService) {
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
        modifier = modifier,
    )
}

private fun FenPositionDetailsData.toDetailsItem(): FenPositionDetailsItem {
    return FenPositionDetailsItem(
        id = id,
        fen = fen,
        name = name,
        theme = theme,
        description = description,
    )
}
