package com.example.chessboard.ui.screen.gamesExplorer

/**
 * Visual blocks and search helpers for the games-explorer package.
 *
 * Keep in this file:
 * - render-only components used by the games explorer screen
 * - search dialog UI and filter matching helpers for games explorer
 * - small package-local models that support the explorer UI
 *
 * It is acceptable to add here:
 * - new reusable UI blocks for the games explorer package
 * - search-related helpers used only by files in this package
 *
 * Do not add here:
 * - database calls, coroutine orchestration, or navigation decisions
 * - logic for unrelated screens
 * - broad app-wide UI utilities
 */
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chessboard.service.ParsedGame
import com.example.chessboard.ui.components.AppTextField
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.MoveChip
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingIconInactive
import com.example.chessboard.ui.theme.TrainingErrorRed
import com.example.chessboard.ui.theme.TrainingTextPrimary

internal data class GamesExplorerFilterState(
    val query: String = "",
    val isCaseSensitive: Boolean = false
)

@Composable
internal fun GameBlock(
    parsedGame: ParsedGame,
    isSelected: Boolean,
    currentPly: Int,
    onSelectClick: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onMovePlyClick: (ply: Int) -> Unit,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onResetClick: () -> Unit,
    onCloneClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CardSurface(
        modifier = modifier.fillMaxWidth(),
        color = if (isSelected) Background.CardDark else Background.SurfaceDark,
        border = if (isSelected) BorderStroke(1.dp, TrainingAccentTeal) else null,
        onClick = onSelectClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SectionTitleText(
                    text = parsedGame.game.event ?: "Opening"
                )
                GameBlockMetaRow(
                    eco = parsedGame.game.eco,
                    gameId = parsedGame.game.id
                )
            }
            CardMetaText(text = "${parsedGame.moveLabels.size} moves")
        }

        Spacer(modifier = Modifier.size(AppDimens.spaceSm))

        if (isSelected) {
            GamesExplorerActionRow(
                canUndo = canUndo,
                canRedo = canRedo,
                onPrevClick = onPrevClick,
                onResetClick = onResetClick,
                onNextClick = onNextClick,
                onCloneClick = onCloneClick,
                onEditClick = onEditClick,
                onDeleteClick = onDeleteClick
            )

            Spacer(modifier = Modifier.size(AppDimens.spaceSm))
        }

        GameMoveChips(
            moveLabels = parsedGame.moveLabels,
            isSelected = isSelected,
            currentPly = currentPly,
            onMovePlyClick = onMovePlyClick
        )
    }
}

@Composable
private fun GamesExplorerActionRow(
    canUndo: Boolean,
    canRedo: Boolean,
    onPrevClick: () -> Unit,
    onResetClick: () -> Unit,
    onNextClick: () -> Unit,
    onCloneClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppDimens.radiusXs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GameBlockActionButton(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous",
                onClick = onPrevClick,
                tint = if (canUndo) TrainingTextPrimary else TrainingIconInactive,
                isEnabled = canUndo
            )
            GameBlockActionButton(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reset",
                onClick = onResetClick
            )
            GameBlockActionButton(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next",
                onClick = onNextClick,
                tint = if (canRedo) TrainingTextPrimary else TrainingIconInactive,
                isEnabled = canRedo
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(AppDimens.radiusXs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GameBlockActionButton(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Clone game",
                onClick = onCloneClick
            )
            GameBlockActionButton(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit game",
                onClick = onEditClick
            )
            GameBlockActionButton(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete game",
                tint = TrainingErrorRed,
                onClick = onDeleteClick
            )
        }
    }
}

@Composable
private fun GameBlockActionButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = TrainingTextPrimary,
    isEnabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun GameBlockMetaRow(
    eco: String?,
    gameId: Long
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!eco.isNullOrBlank()) {
            CardMetaText(text = eco)
        }

        CardMetaText(text = "ID: $gameId")
    }
}

@Composable
private fun GameMoveChips(
    moveLabels: List<String>,
    isSelected: Boolean,
    currentPly: Int,
    onMovePlyClick: (Int) -> Unit
) {
    if (moveLabels.isEmpty()) {
        BodySecondaryText(text = "No moves recorded")
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.radiusXs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        moveLabels.forEachIndexed { index, label ->
            val ply = index + 1
            val moveNumber = index / 2 + 1
            val prefix = if (index % 2 == 0) "$moveNumber." else "$moveNumber..."
            MoveChip(
                label = "$prefix$label",
                isSelected = isSelected && ply == currentPly,
                onClick = { onMovePlyClick(ply) }
            )
        }
    }
}

internal fun matchesGamesExplorerFilter(
    parsedGame: ParsedGame,
    filterState: GamesExplorerFilterState
): Boolean {
    if (filterState.query.isBlank()) {
        return true
    }

    val gameName = parsedGame.game.event.orEmpty()
    if (filterState.isCaseSensitive) {
        return gameName.contains(filterState.query)
    }

    return gameName.contains(filterState.query, ignoreCase = true)
}

@Composable
internal fun RenderGamesExplorerSearchDialog(
    visible: Boolean,
    filterState: GamesExplorerFilterState,
    onDismiss: () -> Unit,
    onFilterStateChange: (GamesExplorerFilterState) -> Unit,
    onApplyClick: () -> Unit
) {
    if (!visible) {
        return
    }

    fun updateQuery(query: String) {
        onFilterStateChange(
            filterState.copy(query = query)
        )
    }

    fun updateCaseSensitive(isCaseSensitive: Boolean) {
        onFilterStateChange(
            filterState.copy(isCaseSensitive = isCaseSensitive)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Background.ScreenDark,
        title = {
            SectionTitleText(text = "Search Games")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
            ) {
                AppTextField(
                    value = filterState.query,
                    onValueChange = ::updateQuery,
                    label = "Game name",
                    placeholder = "Enter part of the title"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Case sensitive",
                            color = TextColor.Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        CardMetaText(
                            text = "Match uppercase and lowercase exactly"
                        )
                    }
                    Checkbox(
                        checked = filterState.isCaseSensitive,
                        onCheckedChange = ::updateCaseSensitive
                    )
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Apply",
                onClick = onApplyClick
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                CardMetaText(text = "Cancel")
            }
        }
    )
}
