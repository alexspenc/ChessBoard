package com.example.chessboard.ui.screen.fenpositions.catalog

/*
 * File role: renders the portrait FEN position catalog page.
 * Allowed here:
 * - catalog UI state, vertically scrolling page layout, and card composition
 * - forwarding page, position selection, and FEN-copy actions
 * Not allowed here:
 * - Room/service calls, persisted runtime state, or app navigation routing
 * Validation date: 2026-09-01
 */

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogContentTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogEmptyTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogLoadingTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogScreenTestTag
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TrainingAccentTeal

internal data class FenPositionCatalogItem(
    val id: Long,
    val fen: String,
    val name: String,
    val theme: String,
)

internal data class FenPositionCatalogUiState(
    val isLoading: Boolean = true,
    val positions: List<FenPositionCatalogItem> = emptyList(),
)

internal data class FenPositionCatalogPaginationState(
    val totalPositionsCount: Int,
    val currentPage: Int,
    val totalPages: Int,
    val canOpenPreviousPage: Boolean,
    val canOpenNextPage: Boolean,
)

@Composable
internal fun FenPositionCatalogScreen(
    uiState: FenPositionCatalogUiState,
    paginationState: FenPositionCatalogPaginationState,
    selectedPositionId: Long?,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onPositionSelected: (Long) -> Unit,
    onAddPositionClick: () -> Unit,
    onOpenPositionClick: (Long) -> Unit,
    onDeletePositionClick: () -> Unit,
    onCopyFenClick: () -> Unit,
    onOpenPreviousPageClick: () -> Unit,
    onOpenNextPageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = fenPositionCatalogStrings()
    val scrollState = rememberScrollState()

    fun openSelectedPosition() {
        val positionId = selectedPositionId ?: return
        onOpenPositionClick(positionId)
    }

    LaunchedEffect(paginationState.currentPage) {
        scrollState.scrollTo(0)
    }

    AppScreenScaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(FenPositionCatalogScreenTestTag),
        topBar = {
            FenPositionCatalogTopBar(
                strings = strings.topBar,
                paginationState = paginationState,
                onBackClick = onBackClick,
                onHomeClick = onHomeClick,
                onOpenPreviousPageClick = onOpenPreviousPageClick,
                onOpenNextPageClick = onOpenNextPageClick,
            )
        },
        bottomBar = {
            FenPositionCatalogBottomBar(
                addContentDescription = strings.addPositionContentDescription,
                openContentDescription = strings.openPositionContentDescription,
                deleteContentDescription = strings.deletePositionContentDescription,
                copyFenContentDescription = strings.copyFenContentDescription,
                canOpen = selectedPositionId != null,
                canDelete = selectedPositionId != null,
                canCopyFen = selectedPositionId != null,
                onAddClick = onAddPositionClick,
                onOpenClick = ::openSelectedPosition,
                onDeleteClick = onDeletePositionClick,
                onCopyFenClick = onCopyFenClick,
            )
        },
    ) { paddingValues ->
        if (uiState.isLoading) {
            FenPositionCatalogLoadingContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag(FenPositionCatalogLoadingTestTag),
            )
            return@AppScreenScaffold
        }

        if (uiState.positions.isEmpty()) {
            FenPositionCatalogEmptyContent(
                text = strings.emptyState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag(FenPositionCatalogEmptyTestTag),
            )
            return@AppScreenScaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(
                    horizontal = AppDimens.spaceLg,
                    vertical = AppDimens.spaceLg,
                )
                .testTag(FenPositionCatalogContentTestTag),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
        ) {
            uiState.positions.forEach { position ->
                FenPositionCatalogCard(
                    position = position,
                    nameText = strings.name(position.name),
                    themeText = strings.theme(position.theme),
                    isSelected = position.id == selectedPositionId,
                    onClick = { onPositionSelected(position.id) },
                )
            }
        }
    }
}

@Composable
private fun FenPositionCatalogLoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = TrainingAccentTeal)
    }
}

@Composable
private fun FenPositionCatalogEmptyContent(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(AppDimens.spaceLg),
        contentAlignment = Alignment.Center,
    ) {
        BodySecondaryText(
            text = text,
            textAlign = TextAlign.Center,
        )
    }
}
