package com.example.chessboard.ui.screen.fenpositions.catalog

/*
 * File role: connects the FEN position catalog UI to its service and runtime pagination state.
 * Allowed here:
 * - loading one catalog page, correcting stale offsets, and mapping entities to UI items
 * - forwarding screen actions to injected callbacks and runtime context
 * Not allowed here:
 * - app-wide navigation registration, Room queries, or card presentation details
 * Validation date: 2026-08-30
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.chessboard.entity.FenPositionEntity
import com.example.chessboard.runtimecontext.FenPositionCatalogRuntimeContext
import com.example.chessboard.service.FenPositionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FenPositionCatalogScreenContainer(
    fenPositionService: FenPositionService,
    runtimeContext: FenPositionCatalogRuntimeContext,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onPositionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var uiState by remember(fenPositionService, runtimeContext) {
        mutableStateOf(FenPositionCatalogUiState())
    }
    var totalPositionsCount by remember(fenPositionService, runtimeContext) {
        mutableStateOf(0)
    }
    val pageOffset = runtimeContext.offset

    LaunchedEffect(fenPositionService, runtimeContext, pageOffset) {
        uiState = uiState.copy(isLoading = true)

        val page = withContext(Dispatchers.IO) {
            fenPositionService.getCatalogPage(
                limit = runtimeContext.pageLimit,
                offset = pageOffset,
            )
        }

        runtimeContext.ensureValidOffset(page.totalCount)
        if (runtimeContext.offset != pageOffset) {
            return@LaunchedEffect
        }

        totalPositionsCount = page.totalCount
        uiState = FenPositionCatalogUiState(
            isLoading = false,
            positions = page.positions.map { position -> position.toCatalogItem() },
        )
    }

    FenPositionCatalogScreen(
        uiState = uiState,
        paginationState = resolveFenPositionCatalogPaginationState(
            runtimeContext = runtimeContext,
            totalPositionsCount = totalPositionsCount,
        ),
        onBackClick = onBackClick,
        onHomeClick = onHomeClick,
        onPositionClick = onPositionClick,
        onOpenPreviousPageClick = runtimeContext::openPreviousPage,
        onOpenNextPageClick = {
            runtimeContext.openNextPage(totalCount = totalPositionsCount)
        },
        modifier = modifier,
    )
}

private fun FenPositionEntity.toCatalogItem(): FenPositionCatalogItem {
    return FenPositionCatalogItem(
        id = id,
        fen = fen,
        name = name,
        theme = theme,
    )
}

private fun resolveFenPositionCatalogPaginationState(
    runtimeContext: FenPositionCatalogRuntimeContext,
    totalPositionsCount: Int,
): FenPositionCatalogPaginationState {
    val totalPages = if (totalPositionsCount == 0) {
        1
    } else {
        (totalPositionsCount - 1) / runtimeContext.pageLimit + 1
    }
    val currentPage = if (totalPositionsCount == 0) {
        1
    } else {
        runtimeContext.offset / runtimeContext.pageLimit + 1
    }

    return FenPositionCatalogPaginationState(
        totalPositionsCount = totalPositionsCount,
        currentPage = currentPage,
        totalPages = totalPages,
        canOpenPreviousPage = runtimeContext.canOpenPreviousPage(),
        canOpenNextPage = runtimeContext.canOpenNextPage(totalPositionsCount),
    )
}
