package com.example.chessboard.ui.screen.positions

/**
 * Top bar rendering for the saved positions screen.
 *
 * Keep top-bar actions and their visual state here. Do not add persistence logic or list rendering.
 */
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.example.chessboard.ui.SavedPositionsNextPageTestTag
import com.example.chessboard.ui.SavedPositionsPreviousPageTestTag
import com.example.chessboard.ui.SavedPositionsSearchActionTestTag
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingIconInactive

internal data class SavedPositionsTopBarPaginationState(
    val totalPositionsCount: Int,
    val currentPage: Int,
    val totalPages: Int,
    val canOpenPreviousPage: Boolean,
    val canOpenNextPage: Boolean,
)

@Composable
internal fun SavedPositionsTopBar(
    paginationState: SavedPositionsTopBarPaginationState,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onOpenPreviousPageClick: () -> Unit,
    onOpenNextPageClick: () -> Unit,
) {
    fun resolvePageArrowTint(isEnabled: Boolean): Color {
        if (!isEnabled) {
            return TrainingIconInactive
        }

        return TextColor.Primary
    }

    AppTopBar(
        title = "Saved Positions",
        subtitle = resolveSavedPositionsTopBarSubtitle(paginationState),
        onBackClick = onBackClick,
        filledBackButton = true,
        actions = {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.testTag(SavedPositionsSearchActionTestTag),
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search saved positions",
                    tint = TextColor.Primary,
                )
            }
            IconButton(
                onClick = onOpenPreviousPageClick,
                enabled = paginationState.canOpenPreviousPage,
                modifier = Modifier.testTag(SavedPositionsPreviousPageTestTag),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous saved positions page",
                    tint = resolvePageArrowTint(paginationState.canOpenPreviousPage),
                )
            }
            IconButton(
                onClick = onOpenNextPageClick,
                enabled = paginationState.canOpenNextPage,
                modifier = Modifier.testTag(SavedPositionsNextPageTestTag),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next saved positions page",
                    tint = resolvePageArrowTint(paginationState.canOpenNextPage),
                )
            }
        },
    )
}

private fun resolveSavedPositionsTopBarSubtitle(
    paginationState: SavedPositionsTopBarPaginationState,
): String {
    return "Positions: ${paginationState.totalPositionsCount} • " +
        "Page ${paginationState.currentPage}/${paginationState.totalPages}"
}
