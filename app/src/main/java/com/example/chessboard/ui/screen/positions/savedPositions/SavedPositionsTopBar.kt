package com.example.chessboard.ui.screen.positions.savedPositions

/**
 * Top bar rendering for the saved positions screen.
 *
 * Keep top-bar actions and their visual state here. Do not add persistence logic or list rendering.
 */
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.example.chessboard.ui.SavedPositionsNextPageTestTag
import com.example.chessboard.ui.SavedPositionsPreviousPageTestTag
import com.example.chessboard.ui.SavedPositionsSearchActionTestTag
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.HomeIconButton
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.MutedContentColor

internal data class SavedPositionsTopBarPaginationState(
    val totalPositionsCount: Int,
    val currentPage: Int,
    val totalPages: Int,
    val canOpenPreviousPage: Boolean,
    val canOpenNextPage: Boolean,
)

@Composable
internal fun SavedPositionsTopBar(
    strings: SavedPositionsTopBarStrings,
    paginationState: SavedPositionsTopBarPaginationState,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onSearchClick: () -> Unit,
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
        subtitleLines = listOf(resolveSavedPositionsTopBarSubtitle(paginationState, strings)),
        onBackClick = onBackClick,
        handleSystemBack = true,
        filledBackButton = true,
        actions = {
            HomeIconButton(onClick = onHomeClick)
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.testTag(SavedPositionsSearchActionTestTag),
            ) {
                IconMd(
                    imageVector = Icons.Default.Search,
                    contentDescription = strings.searchContentDescription,
                    tint = TextColor.Primary,
                )
            }
            IconButton(
                onClick = onOpenPreviousPageClick,
                enabled = paginationState.canOpenPreviousPage,
                modifier = Modifier.testTag(SavedPositionsPreviousPageTestTag),
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
                modifier = Modifier.testTag(SavedPositionsNextPageTestTag),
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

private fun resolveSavedPositionsTopBarSubtitle(
    paginationState: SavedPositionsTopBarPaginationState,
    strings: SavedPositionsTopBarStrings,
): String {
    return strings.subtitle(
        totalPositionsCount = paginationState.totalPositionsCount,
        currentPage = paginationState.currentPage,
        totalPages = paginationState.totalPages,
    )
}
