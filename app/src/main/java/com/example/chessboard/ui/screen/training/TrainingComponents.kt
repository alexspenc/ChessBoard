package com.example.chessboard.ui.screen.training

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.ui.ChessBoardWithCoordinates
import com.example.chessboard.ui.MoveLegendNextTestTag
import com.example.chessboard.ui.MoveLegendPreviousTestTag
import com.example.chessboard.ui.components.AppTextField
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.MoveChip
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.ButtonColor
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingIconInactive

// ──────────────────────────────────────────────────────────────────────────────
// Shared composables
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun DarkInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    minLines: Int = 1,
) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        modifier = modifier,
        isError = isError,
        minLines = minLines
    )
}


@Composable
fun MoveLegendSection(
    moveLabels: List<String>,
    currentPly: Int,
    isSelectionEnabled: Boolean,
    onMovePlyClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Moves",
    emptyText: String = "No moves yet",
    showNavControls: Boolean = isSelectionEnabled,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onPrevMoveClick: () -> Unit = {},
    onNextMoveClick: () -> Unit = {},
    onResetMovesClick: () -> Unit = {},
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(moveLabels) { index, label ->
                    val ply = index + 1
                    val moveNumber = index / 2 + 1
                    val prefix = if (index % 2 == 0) "$moveNumber." else "$moveNumber..."
                    MoveChip(
                        label = "$prefix$label",
                        isSelected = ply == currentPly,
                        onClick = {
                            if (!isSelectionEnabled) {
                                return@MoveChip
                            }
                            onMovePlyClick(ply)
                        }
                    )
                }
            }

            if (!showNavControls) {
                return@Column
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceMd))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevMoveClick,
                    enabled = canUndo,
                    modifier = Modifier
                        .testTag(MoveLegendPreviousTestTag)
                        .semantics { contentDescription = "Previous move" }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = if (canUndo) TextColor.Primary else TrainingIconInactive,
                        modifier = Modifier.size(AppDimens.iconButtonSize)
                    )
                }
                TextButton(onClick = onResetMovesClick, enabled = canUndo) {
                    Text(
                        text = "Reset",
                        color = if (canUndo) TextColor.Primary else TextColor.Secondary
                    )
                }
                IconButton(
                    onClick = onNextMoveClick,
                    enabled = canRedo,
                    modifier = Modifier
                        .testTag(MoveLegendNextTestTag)
                        .semantics { contentDescription = "Next move" }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = if (canRedo) TextColor.Primary else TrainingIconInactive,
                        modifier = Modifier.size(AppDimens.iconButtonSize)
                    )
                }
            }
        }
    }
}

@Composable
fun ChessBoardSection(
    gameController: GameController,
    modifier: Modifier = Modifier
) {
    val boardState = gameController.boardState

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(AppDimens.radiusXl))
    ) {
        key(boardState) {
            ChessBoardWithCoordinates(
                gameController = gameController,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun TrainingActionButtons(
    onSaveGame: () -> Unit,
    onDatabaseClear: () -> Unit,
    gameController: GameController,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Primary actions: Save game, Clear database
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryButton("Save game", onClick = onSaveGame)
            PrimaryButton("Clear database", onClick = onDatabaseClear)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Secondary actions: Back, Forward, Reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryButton("Back", onClick = { gameController.undoMove() })
            PrimaryButton("Forward", onClick = { gameController.redoMove() })
            PrimaryButton("Reset", onClick = { gameController.resetToStartPosition() })
        }
    }
}

@Composable
fun ResetTrainingButton(
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onResetClick,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(AppDimens.radiusLg),
        colors = ButtonDefaults.buttonColors(
            containerColor = Background.SurfaceDark,
            contentColor = ButtonColor.Content
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        )
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Reset",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        SectionTitleText(text = "Reset Training", color = TextColor.Primary)
    }
}
