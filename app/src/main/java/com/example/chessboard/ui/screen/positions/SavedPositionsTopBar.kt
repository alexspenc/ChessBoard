package com.example.chessboard.ui.screen.positions

/**
 * Top bar rendering for the saved positions screen.
 *
 * Keep top-bar actions and their visual state here. Do not add persistence logic or list rendering.
 */
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.example.chessboard.ui.SavedPositionsOpenSelectedTestTag
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal

@Composable
internal fun SavedPositionsTopBar(
    selectedPosition: SavedPositionListItem?,
    onBackClick: () -> Unit,
    onOpenSelectedPosition: (SavedPositionListItem) -> Unit,
) {
    fun resolveOpenSelectedPositionTint(): Color {
        if (selectedPosition == null) {
            return TextColor.Secondary
        }

        return TrainingAccentTeal
    }

    AppTopBar(
        title = "Saved Positions",
        onBackClick = onBackClick,
        filledBackButton = true,
        actions = {
            IconButton(
                onClick = {
                    val position = selectedPosition ?: return@IconButton
                    onOpenSelectedPosition(position)
                },
                enabled = selectedPosition != null,
                modifier = Modifier.testTag(SavedPositionsOpenSelectedTestTag),
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Open selected position",
                    tint = resolveOpenSelectedPositionTint(),
                )
            }
        },
    )
}
