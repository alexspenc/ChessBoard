package com.example.chessboard.ui.screen.createOpening

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.ui.components.AppIconSizes
import com.example.chessboard.ui.components.BoardActionNavigationBar
import com.example.chessboard.ui.components.BoardActionNavigationItem
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.screen.EditableLineSide
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.BottomBarContentColor

@Composable
internal fun CreateOpeningBoardControlsBar(
    selectedSide: EditableLineSide,
    onSideSelected: (EditableLineSide) -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndoClick: () -> Unit,
    onResetClick: () -> Unit,
    onRedoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val whiteLabel = stringResource(R.string.create_opening_side_white)
    val blackLabel = stringResource(R.string.create_opening_side_black)
    val resetLabel = stringResource(R.string.common_reset)
    val backLabel = stringResource(R.string.common_back)
    val forwardLabel = stringResource(R.string.common_forward)

    BoardActionNavigationBar(
        modifier = modifier,
        items = EditableLineSide.entries.map { side ->
            BoardActionNavigationItem(
                label = if (side == EditableLineSide.AS_WHITE) whiteLabel else blackLabel,
                selected = side == selectedSide,
                onClick = { onSideSelected(side) },
            ) {
                SideSymbolNavigationIcon(
                    contentDescription = if (side == EditableLineSide.AS_WHITE) whiteLabel else blackLabel,
                    selected = side == selectedSide,
                )
            }
        } + listOf(
            BoardActionNavigationItem(
                label = resetLabel,
                onClick = onResetClick,
            ) {
                IconMd(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = resetLabel,
                    tint = BottomBarContentColor,
                )
            },
            BoardActionNavigationItem(
                label = backLabel,
                enabled = canUndo,
                onClick = onUndoClick,
            ) {
                IconMd(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = backLabel,
                    tint = if (canUndo) BottomBarContentColor else BottomBarContentColor.copy(alpha = 0.5f),
                )
            },
            BoardActionNavigationItem(
                label = forwardLabel,
                enabled = canRedo,
                onClick = onRedoClick,
            ) {
                IconMd(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = forwardLabel,
                    tint = if (canRedo) BottomBarContentColor else BottomBarContentColor.copy(alpha = 0.5f),
                )
            },
        ),
    )
}

@Composable
private fun SideSymbolNavigationIcon(
    contentDescription: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(R.drawable.ic_king),
        contentDescription = contentDescription,
        tint = if (selected) TrainingAccentTeal else BottomBarContentColor,
        modifier = modifier.size(AppIconSizes.Lg),
    )
}
