package com.example.chessboard.ui.screen.fenpositions.catalog

/*
 * File role: renders the FEN position catalog top bar and page controls.
 * Allowed here:
 * - title, catalog page summary, and previous/next page actions
 * Not allowed here:
 * - page loading, card rendering, navigation routing, or persistence operations
 * Validation date: 2026-08-30
 */

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.HomeIconButton
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.theme.MutedContentColor
import com.example.chessboard.ui.theme.TextColor

@Composable
internal fun FenPositionCatalogTopBar(
    strings: FenPositionCatalogTopBarStrings,
    paginationState: FenPositionCatalogPaginationState,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onOpenPreviousPageClick: () -> Unit,
    onOpenNextPageClick: () -> Unit,
) {
    fun resolvePageArrowTint(isEnabled: Boolean): Color {
        if (!isEnabled) {
            return MutedContentColor
        }

        return TextColor.Primary
    }

    AppTopBar(
        title = strings.screenTitle,
        subtitleLines = listOf(
            strings.subtitle(
                totalPositionsCount = paginationState.totalPositionsCount,
                currentPage = paginationState.currentPage,
                totalPages = paginationState.totalPages,
            ),
        ),
        onBackClick = onBackClick,
        handleSystemBack = true,
        filledBackButton = true,
        actions = {
            HomeIconButton(onClick = onHomeClick)
            IconButton(
                onClick = onOpenPreviousPageClick,
                enabled = paginationState.canOpenPreviousPage,
            ) {
                IconMd(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = strings.previousPageContentDescription,
                    tint = resolvePageArrowTint(paginationState.canOpenPreviousPage),
                )
            }
            IconButton(
                onClick = onOpenNextPageClick,
                enabled = paginationState.canOpenNextPage,
            ) {
                IconMd(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = strings.nextPageContentDescription,
                    tint = resolvePageArrowTint(paginationState.canOpenNextPage),
                )
            }
        },
    )
}
