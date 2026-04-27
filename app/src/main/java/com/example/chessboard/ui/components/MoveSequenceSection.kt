package com.example.chessboard.ui.components

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.chessboard.ui.MoveLegendNextTestTag
import com.example.chessboard.ui.MoveLegendPreviousTestTag
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingIconInactive

@Composable
fun MoveSequenceSection(
    moveLabels: List<String>,
    currentPly: Int,
    isSelectionEnabled: Boolean = true,
    showNavControls: Boolean = false,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onMovePlyClick: (Int) -> Unit = {},
    onPrevMoveClick: () -> Unit = {},
    onNextMoveClick: () -> Unit = {},
    onResetMovesClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    title: String = "Move Sequence",
    emptyText: String = "No moves.",
) {
    CardSurface(modifier = modifier.fillMaxWidth()) {
        Column {
            SectionTitleText(text = title)
            Spacer(modifier = Modifier.height(AppDimens.spaceSm))

            if (moveLabels.isEmpty()) {
                BodySecondaryText(text = emptyText)
                return@Column
            }

            val listState = rememberLazyListState()
            LaunchedEffect(currentPly) {
                val targetIndex = maxOf(0, currentPly - 1)
                val layoutInfo = listState.layoutInfo
                val viewportStart = layoutInfo.viewportStartOffset
                val viewportEnd = layoutInfo.viewportEndOffset
                val item = layoutInfo.visibleItemsInfo.find { it.index == targetIndex }
                if (item != null) {
                    val fullyVisible = item.offset >= viewportStart && item.offset + item.size <= viewportEnd
                    if (!fullyVisible) {
                        val delta = if (item.offset < viewportStart) {
                            (item.offset - viewportStart).toFloat()
                        } else {
                            (item.offset + item.size - viewportEnd).toFloat()
                        }
                        listState.animateScrollBy(delta)
                    }
                } else {
                    val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    if (targetIndex > lastVisibleIndex) {
                        val visibleCount = layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
                        listState.animateScrollToItem(maxOf(0, targetIndex - visibleCount + 1))
                    } else {
                        listState.animateScrollToItem(targetIndex)
                    }
                }
            }

            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                itemsIndexed(moveLabels) { index, label ->
                    val ply = index + 1
                    val moveNumber = index / 2 + 1
                    val prefix = if (index % 2 == 0) "$moveNumber." else "$moveNumber..."
                    MoveChip(
                        label = "$prefix$label",
                        isSelected = ply == currentPly,
                        onClick = {
                            if (!isSelectionEnabled) return@MoveChip
                            onMovePlyClick(ply)
                        },
                    )
                }
            }

            if (!showNavControls) return@Column

            Spacer(modifier = Modifier.height(AppDimens.spaceMd))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onPrevMoveClick,
                    enabled = canUndo,
                    modifier = Modifier
                        .testTag(MoveLegendPreviousTestTag)
                        .semantics { contentDescription = "Previous move" },
                ) {
                    IconLg(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = if (canUndo) TextColor.Primary else TrainingIconInactive,
                    )
                }
                TextButton(onClick = onResetMovesClick, enabled = canUndo) {
                    Text(
                        text = "Reset",
                        color = if (canUndo) TextColor.Primary else TextColor.Secondary,
                    )
                }
                IconButton(
                    onClick = onNextMoveClick,
                    enabled = canRedo,
                    modifier = Modifier
                        .testTag(MoveLegendNextTestTag)
                        .semantics { contentDescription = "Next move" },
                ) {
                    IconLg(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = if (canRedo) TextColor.Primary else TrainingIconInactive,
                    )
                }
            }
        }
    }
}
