package com.example.chessboard.ui.screen.trainSingleGame

// Render-only composables for the single-game training flow.
// This file stays focused on UI structure so the screen and logic files do not mix
// domain decisions with presentation details.

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.ui.ChessBoardWithCoordinates
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppMessageDialogAction
import com.example.chessboard.ui.components.AppProgressCard
import com.example.chessboard.ui.components.AppTextField
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.MoveSequenceSection
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.components.HintIconButton
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TextColor

@Composable
internal fun RenderCompletionDialog(
    dialogState: TrainSingleGameCompletionState?,
    onRepeatClick: () -> Unit,
    onFinishClick: () -> Unit,
    onNextTrainingClick: (() -> Unit)? = null,
) {
    if (dialogState == null) {
        return
    }

    TrainSingleGameCompletionDialog(
        dialogState = dialogState,
        onRepeatClick = onRepeatClick,
        onFinishClick = onFinishClick,
        onNextTrainingClick = onNextTrainingClick,
    )
}

@Composable
internal fun TrainSingleGameContent(
    state: TrainSingleGameContentState,
    gameController: GameController,
    actions: TrainSingleGameContentActions,
    simpleViewEnabled: Boolean = false,
) {
    ScreenSection {
        Column {
            TrainingGameHeader(title = state.trainingGameData.game.event)
            Spacer(modifier = Modifier.height(AppDimens.spaceSm))
            TrainingBoardSection(
                gameController = gameController,
                wrongMoveSquare = state.wrongMoveSquare,
                hintSquare = state.hintSquare,
            )
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            TrainingSingleGameActions(
                state = resolveTrainingSingleGameActionsState(state.phase),
                contentState = state,
                actions = actions,
                simpleViewEnabled = simpleViewEnabled,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            TrainingSessionInfoRow(state = state, simpleViewEnabled = simpleViewEnabled)
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            val visibleMoveLabels = resolveVisibleTrainingMoveLabels(state)
            MoveSequenceSection(
                moveLabels = visibleMoveLabels,
                currentPly = state.currentPly,
                isSelectionEnabled = state.showLineCompleted,
                showNavControls = state.showLineCompleted,
                canUndo = state.showLineCompleted && state.currentPly > 0,
                canRedo = state.showLineCompleted && state.currentPly < state.moveLabels.size,
                onMovePlyClick = actions.onMovePlyClick,
                onPrevMoveClick = actions.onPrevMoveClick,
                onNextMoveClick = actions.onNextMoveClick,
                onResetMovesClick = actions.onResetMovesClick,
            )
        }
    }
}

@Composable
internal fun TrainingGameHeader(
    title: String?,
    modifier: Modifier = Modifier
) {
    SectionTitleText(
        text = title ?: "Unnamed Opening",
    )
}

@Composable
private fun TrainingSessionInfoRow(
    state: TrainSingleGameContentState,
    simpleViewEnabled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (simpleViewEnabled) {
        AppProgressCard(
            label = "Lines completed",
            progress = (state.sessionCurrent - 1).coerceAtLeast(0),
            total = state.sessionTotal,
            modifier = modifier,
        )
        return
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        TrainingSessionInfo(
            state = state,
            modifier = Modifier.weight(1f),
        )
        RenderTrainingSessionProgressBar(
            state = state,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun TrainingSessionInfo(
    state: TrainSingleGameContentState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        BodySecondaryText(
            text = "Training ID: ${state.trainingId}"
        )
        BodySecondaryText(
            text = "Game ID: ${state.gameId}"
        )
        BodySecondaryText(
            text = "Mistakes: ${state.mistakesCount}"
        )
    }
}

@Composable
private fun RenderTrainingSessionProgressBar(
    state: TrainSingleGameContentState,
    modifier: Modifier = Modifier,
) {
    if (state.sessionTotal <= 1) {
        return
    }

    TrainingSessionProgressBar(
        current = state.sessionCurrent,
        total = state.sessionTotal,
        modifier = modifier,
    )
}

@Composable
private fun TrainingSessionProgressBar(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val fraction = resolveTrainingSessionProgressFraction(
        current = current,
        total = total,
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Line $current of $total",
                style = MaterialTheme.typography.bodySmall,
                color = TextColor.Secondary,
            )
            Text(
                text = "${current - 1} completed",
                style = MaterialTheme.typography.bodySmall,
                color = TextColor.Secondary,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF2A2A2A)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(TrainingAccentTeal),
            )
        }
    }
}

private fun resolveTrainingSessionProgressFraction(
    current: Int,
    total: Int,
): Float {
    if (total <= 0) {
        return 0f
    }

    return (current.toFloat() / total).coerceIn(0f, 1f)
}

internal fun resolveVisibleTrainingMoveLabels(
    state: TrainSingleGameContentState
): List<String> {
    if (state.phase == TrainSingleGamePhase.ShowingLine) {
        return state.moveLabels
    }

    if (state.showLineCompleted) {
        return state.moveLabels
    }

    if (state.phase == TrainSingleGamePhase.Training || state.phase == TrainSingleGamePhase.Mistake) {
        return state.moveLabels.take(state.currentPly)
    }

    return state.moveLabels.take(state.currentPly)
}

@Composable
internal fun TrainingSingleGameActions(
    state: TrainingSingleGameActionsState,
    contentState: TrainSingleGameContentState,
    actions: TrainSingleGameContentActions,
    simpleViewEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val compactIconButtonSize = 40.dp
    val compactActionSpacing = AppDimens.spaceSm
    val compactIconSize = 22.dp

    @Composable
    fun TrainingActionButton() {
        if (state == TrainingSingleGameActionsState.Idle) {
            IconButton(
                onClick = actions.onAnalyzeGameClick,
                modifier = Modifier.size(compactIconButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Analyze game",
                    tint = TextColor.Primary,
                    modifier = Modifier.size(compactIconSize)
                )
            }
            IconButton(
                onClick = actions.onStartTrainingClick,
                modifier = Modifier.size(compactIconButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start training",
                    tint = TextColor.Primary,
                    modifier = Modifier.size(compactIconSize)
                )
            }
            return
        }

        if (state == TrainingSingleGameActionsState.Training) {
            HintIconButton(
                onClick = actions.onHintClick,
                iconSize = compactIconSize,
                buttonSize = compactIconButtonSize,
            )
        }
        IconButton(
            onClick = actions.onAnalyzeGameClick,
            modifier = Modifier.size(compactIconButtonSize)
        ) {
            Icon(
                imageVector = Icons.Default.Analytics,
                contentDescription = "Analyze game",
                tint = TextColor.Primary,
                modifier = Modifier.size(compactIconSize)
            )
        }
        IconButton(
            onClick = actions.onStopTrainingClick,
            modifier = Modifier.size(compactIconButtonSize)
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "Stop training",
                tint = TextColor.Primary,
                modifier = Modifier.size(compactIconSize)
            )
        }
    }

    @Composable
    fun ShowLineActionsRow(
        state: TrainSingleGameContentState,
        actions: TrainSingleGameContentActions
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(compactActionSpacing),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            PrimaryButton(
                text = "Show line",
                onClick = actions.onShowLineClick
            )
            AppTextField(
                value = state.showLineMoveDelayInput,
                onValueChange = actions.onShowLineMoveDelayInputChange,
                label = "delay(ms)",
                placeholder = ShowLineMoveDelayMs.toString(),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = actions.onDecreaseShowLineMoveDelayClick,
                modifier = Modifier.size(compactIconButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease move delay",
                    tint = TextColor.Primary,
                    modifier = Modifier.size(compactIconSize)
                )
            }
            IconButton(
                onClick = actions.onIncreaseShowLineMoveDelayClick,
                modifier = Modifier.size(compactIconButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase move delay",
                    tint = TextColor.Primary,
                    modifier = Modifier.size(compactIconSize)
                )
            }
            TrainingActionButton()
        }
    }

    Column(modifier = modifier) {
        if (simpleViewEnabled) {
            if (state == TrainingSingleGameActionsState.Training) {
                HintIconButton(
                    onClick = actions.onHintClick,
                    iconSize = compactIconSize,
                    buttonSize = compactIconButtonSize,
                )
            }
            if (state == TrainingSingleGameActionsState.Mistake) {
                PrimaryButton(
                    text = "Make correct move",
                    onClick = actions.onMakeCorrectMoveClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            return@Column
        }

        if (state == TrainingSingleGameActionsState.ShowingLine) {
            PrimaryButton(
                text = "Stop show line",
                onClick = actions.onStopShowLineClick,
                modifier = Modifier.fillMaxWidth()
            )
            return@Column
        }

        // Keep show-line controls visible in idle/training/mistake states.
        // The start/stop action stays in the same row so the control cluster does not
        // jump vertically when the session moves between idle and active training.
        ShowLineActionsRow(
            state = contentState,
            actions = actions
        )

        if (state != TrainingSingleGameActionsState.Mistake) {
            return
        }

        Spacer(modifier = Modifier.height(AppDimens.spaceMd))
        PrimaryButton(
            text = "Make correct move",
            onClick = actions.onMakeCorrectMoveClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun TrainingBoardSection(
    gameController: GameController,
    wrongMoveSquare: String? = null,
    hintSquare: String? = null,
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
                wrongMoveSquare = wrongMoveSquare,
                hintSquare = hintSquare,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
internal fun TrainSingleGameCompletionDialog(
    dialogState: TrainSingleGameCompletionState,
    onRepeatClick: () -> Unit,
    onFinishClick: () -> Unit,
    onNextTrainingClick: (() -> Unit)? = null,
) {
    val dialogActions = buildList {
        add(AppMessageDialogAction(text = "Repeat", onClick = onRepeatClick))

        if (onNextTrainingClick != null) {
            add(AppMessageDialogAction(text = "Next", onClick = onNextTrainingClick))
        }

        add(AppMessageDialogAction(text = "Finish", onClick = onFinishClick))
    }

    AppMessageDialog(
        title = dialogState.title,
        message = dialogState.message,
        onDismiss = onFinishClick,
        actions = dialogActions,
    )
}
