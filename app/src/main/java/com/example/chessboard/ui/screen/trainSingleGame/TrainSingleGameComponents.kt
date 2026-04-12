package com.example.chessboard.ui.screen.trainSingleGame

// Render-only composables for the single-game training flow.
// This file stays focused on UI structure so the screen and logic files do not mix
// domain decisions with presentation details.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.ui.ChessBoardWithCoordinates
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppTextField
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.screen.training.MoveLegendSection
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor

@Composable
internal fun RenderCompletionDialog(
    dialogState: TrainSingleGameCompletionState?,
    onRepeatClick: () -> Unit,
    onFinishClick: () -> Unit
) {
    if (dialogState == null) {
        return
    }

    TrainSingleGameCompletionDialog(
        dialogState = dialogState,
        onRepeatClick = onRepeatClick,
        onFinishClick = onFinishClick
    )
}

@Composable
internal fun TrainSingleGameContent(
    state: TrainSingleGameContentState,
    gameController: GameController,
    actions: TrainSingleGameContentActions,
) {
    ScreenSection {
        Column {
            TrainingGameHeader(title = state.trainingGameData.game.event)
            Spacer(modifier = Modifier.height(AppDimens.spaceSm))
            TrainingBoardSection(gameController = gameController, wrongMoveSquare = state.wrongMoveSquare)
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            TrainingSingleGameActions(
                state = resolveTrainingSingleGameActionsState(state.phase),
                contentState = state,
                actions = actions,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            TrainingSessionInfo(state = state)
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            val visibleMoveLabels = resolveVisibleTrainingMoveLabels(state)
            if (visibleMoveLabels.isNotEmpty()) {
                TrainingMovesLegend(
                    moveLabels = visibleMoveLabels,
                    currentPly = state.currentPly,
                    isSelectionEnabled = state.showLineCompleted,
                    canUndo = state.showLineCompleted && state.currentPly > 0,
                    canRedo = state.showLineCompleted && state.currentPly < state.moveLabels.size,
                    onMovePlyClick = actions.onMovePlyClick,
                    onPrevMoveClick = actions.onPrevMoveClick,
                    onNextMoveClick = actions.onNextMoveClick,
                    onResetMovesClick = actions.onResetMovesClick
                )
            }
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

    return emptyList()
}

@Composable
internal fun TrainingMovesLegend(
    moveLabels: List<String>,
    currentPly: Int,
    isSelectionEnabled: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onMovePlyClick: (Int) -> Unit,
    onPrevMoveClick: () -> Unit,
    onNextMoveClick: () -> Unit,
    onResetMovesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MoveLegendSection(
        moveLabels = moveLabels,
        currentPly = currentPly,
        isSelectionEnabled = isSelectionEnabled,
        canUndo = canUndo,
        canRedo = canRedo,
        onMovePlyClick = onMovePlyClick,
        onPrevMoveClick = onPrevMoveClick,
        onNextMoveClick = onNextMoveClick,
        onResetMovesClick = onResetMovesClick,
        modifier = modifier,
        title = "Moves",
        emptyText = "No moves yet"
    )
}

@Composable
internal fun TrainingSingleGameActions(
    state: TrainingSingleGameActionsState,
    contentState: TrainSingleGameContentState,
    actions: TrainSingleGameContentActions,
    modifier: Modifier = Modifier
) {
    @Composable
    fun TrainingActionButton() {
        if (state == TrainingSingleGameActionsState.Idle) {
            IconButton(onClick = actions.onStartTrainingClick) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start training",
                    tint = TextColor.Primary
                )
            }
            return
        }

        IconButton(onClick = actions.onStopTrainingClick) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "Stop training",
                tint = TextColor.Primary
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
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
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
            IconButton(onClick = actions.onDecreaseShowLineMoveDelayClick) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease move delay",
                    tint = TextColor.Primary
                )
            }
            IconButton(onClick = actions.onIncreaseShowLineMoveDelayClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase move delay",
                    tint = TextColor.Primary
                )
            }
            TrainingActionButton()
        }
    }

    Column(modifier = modifier) {
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
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
internal fun TrainSingleGameCompletionDialog(
    dialogState: TrainSingleGameCompletionState,
    onRepeatClick: () -> Unit,
    onFinishClick: () -> Unit
) {
    AppMessageDialog(
        title = dialogState.title,
        message = dialogState.message,
        onDismiss = onFinishClick,
        confirmText = "Repeat variation",
        onConfirm = onRepeatClick,
        dismissText = dialogState.finishLabel,
        onDismissClick = onFinishClick
    )
}
