package com.example.chessboard.ui.screen.positions

/**
 * Card rendering for saved positions.
 *
 * Keep card layout, selection styling, and card-local buttons here. Do not add screen loading or service logic.
 */
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.chessboard.ui.savedPositionCardTestTag
import com.example.chessboard.ui.savedPositionCreateButtonTestTag
import com.example.chessboard.ui.savedPositionDeleteButtonTestTag
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingErrorRed

@Composable
internal fun SavedPositionCard(
    position: SavedPositionListItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onCreateClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CardSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(savedPositionCardTestTag(position.id))
            .semantics { selected = isSelected },
        color = if (isSelected) Background.CardDark else Background.SurfaceDark,
        border = if (isSelected) BorderStroke(1.dp, TrainingAccentTeal) else null,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
            ) {
                ScreenTitleText(text = position.name)
                if (isSelected) {
                    CardMetaText(
                        text = "Selected",
                        color = TrainingAccentTeal,
                    )
                }
                CardMetaText(text = "Position ID: ${position.id}")
                CardMetaText(text = "FEN: ${resolveDisplayedFen(position)}")
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onCreateClick,
                    modifier = Modifier.testTag(savedPositionCreateButtonTestTag(position.id)),
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Create from saved position",
                        tint = TrainingAccentTeal,
                    )
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.testTag(savedPositionDeleteButtonTestTag(position.id)),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete saved position",
                        tint = TrainingErrorRed,
                    )
                }
            }
        }
    }
}
