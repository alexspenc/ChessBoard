package com.example.chessboard.ui.screen.trainSingleLine

// Render-only composables for the single-line training flow.
// This file stays focused on UI structure so the screen and logic files do not mix
// domain decisions with presentation details.

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.ui.ChessBoardWithCoordinates
import com.example.chessboard.ui.components.AppIconSizes
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppMessageDialogAction
import com.example.chessboard.ui.components.AppNumberSlider
import com.example.chessboard.ui.components.AppProgressCard
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.LineMoveTreeSection
import com.example.chessboard.ui.components.HintIconButton
import com.example.chessboard.ui.components.IconLg
import com.example.chessboard.ui.components.IconSm
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.MutedContentColor

@Composable
internal fun RenderCompletionDialog(
    dialogState: TrainSingleLineCompletionState?,
    onRepeatClick: () -> Unit,
    onFinishClick: () -> Unit,
    onNextTrainingClick: (() -> Unit)? = null,
    onMarkDubiousAndNextTrainingClick: (() -> Unit)? = null,
) {
    if (dialogState == null) {
        return
    }

    TrainSingleLineCompletionDialog(
        dialogState = dialogState,
        onRepeatClick = onRepeatClick,
        onFinishClick = onFinishClick,
        onNextTrainingClick = onNextTrainingClick,
        onMarkDubiousAndNextTrainingClick = onMarkDubiousAndNextTrainingClick,
    )
}

@Composable
internal fun TrainSingleLineContent(
    state: TrainSingleLineContentState,
    lineController: LineController,
    actions: TrainSingleLineContentActions,
    showShowLineDialog: Boolean = false,
    simpleViewEnabled: Boolean = false,
) {
    RenderShowLineDialog(
        visible = showShowLineDialog,
        moveDelayInput = state.showLineMoveDelayInput,
        onMoveDelayChange = actions.onShowLineMoveDelayInputChange,
        onDismiss = actions.onDismissShowLineDialogClick,
        onStartClick = actions.onConfirmShowLineClick,
    )

    ScreenSection {
        Column {
            TrainingLineHeader(title = state.trainingLineData.line.event)
            Spacer(modifier = Modifier.height(AppDimens.spaceSm))
            TrainingBoardSection(
                lineController = lineController,
                wrongMoveSquare = state.wrongMoveSquare,
                hintSquare = state.hintSquare,
            )
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            TrainingSingleLineActions(
                state = resolveTrainingSingleLineActionsState(state.phase),
                contentState = state,
                actions = actions,
                simpleViewEnabled = simpleViewEnabled,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            TrainingSessionInfoRow(state = state, simpleViewEnabled = simpleViewEnabled)
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            val maxVisiblePly = resolveTrainingMaxVisiblePly(state)
            val uciMoves = state.trainingLineData.uciMoves
            val importedUciLines = remember(uciMoves) { listOf(uciMoves) }
            if (maxVisiblePly == null || maxVisiblePly > 0) {
                LineMoveTreeSection(
                    importedUciLines = importedUciLines,
                    lineController = lineController,
                    startFen = state.trainingLineData.startFen,
                    maxVisiblePly = maxVisiblePly,
                    onMoveSelected = { _, targetPly ->
                        if (state.showLineCompleted) actions.onMovePlyClick(targetPly)
                    },
                )
            }
            if (state.showLineCompleted) {
                val canUndo = state.currentPly > 0
                val canRedo = state.currentPly < state.trainingLineData.uciMoves.size
                Spacer(modifier = Modifier.height(AppDimens.spaceMd))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    IconButton(onClick = actions.onPrevMoveClick, enabled = canUndo) {
                        IconLg(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous move",
                            tint = if (canUndo) TextColor.Primary else MutedContentColor,
                        )
                    }
                    TextButton(onClick = actions.onResetMovesClick, enabled = canUndo) {
                        Text(
                            text = "Reset",
                            color = if (canUndo) TextColor.Primary else TextColor.Secondary,
                        )
                    }
                    IconButton(onClick = actions.onNextMoveClick, enabled = canRedo) {
                        IconLg(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next move",
                            tint = if (canRedo) TextColor.Primary else MutedContentColor,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun TrainingLineHeader(
    title: String?,
    modifier: Modifier = Modifier
) {
    SectionTitleText(
        text = title ?: "Unnamed Opening",
    )
}

@Composable
private fun TrainingSessionInfoRow(
    state: TrainSingleLineContentState,
    simpleViewEnabled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (simpleViewEnabled) {
        AppProgressCard(
            label = "Lines completed",
            progress = (state.sessionProgress.sessionCurrent - 1).coerceAtLeast(0),
            total = state.sessionProgress.sessionTotal,
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
    state: TrainSingleLineContentState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        BodySecondaryText(
            text = "Training ID: ${state.target.trainingId}"
        )
        BodySecondaryText(
            text = "Line ID: ${state.target.lineId}"
        )
        BodySecondaryText(
            text = "Mistakes: ${state.mistakesCount}"
        )
    }
}

@Composable
private fun RenderTrainingSessionProgressBar(
    state: TrainSingleLineContentState,
    modifier: Modifier = Modifier,
) {
    if (state.sessionProgress.sessionTotal <= 1) {
        return
    }

    TrainingSessionProgressBar(
        current = state.sessionProgress.sessionCurrent,
        total = state.sessionProgress.sessionTotal,
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

internal fun resolveTrainingMaxVisiblePly(
    state: TrainSingleLineContentState
): Int? {
    if (state.phase == TrainSingleLinePhase.ShowingLine || state.showLineCompleted) {
        return null
    }
    return state.currentPly
}

@Composable
internal fun TrainingSingleLineActions(
    state: TrainingSingleLineActionsState,
    contentState: TrainSingleLineContentState,
    actions: TrainSingleLineContentActions,
    simpleViewEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val compactIconButtonSize = 40.dp
    val compactActionSpacing = AppDimens.spaceSm
    val compactIconSize = AppIconSizes.Sm

    @Composable
    fun TrainingActionButton() {
        if (state == TrainingSingleLineActionsState.Idle) {
            IconButton(
                onClick = actions.onAnalyzeLineClick,
                modifier = Modifier.size(compactIconButtonSize)
            ) {
                IconSm(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Analyze line",
                    modifier = Modifier.size(compactIconSize)
                )
            }
            IconButton(
                onClick = actions.onStartTrainingClick,
                modifier = Modifier.size(compactIconButtonSize)
            ) {
                IconSm(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start training",
                    modifier = Modifier.size(compactIconSize)
                )
            }
            return
        }

        if (state == TrainingSingleLineActionsState.Training) {
            HintIconButton(
                onClick = actions.onHintClick,
                iconSize = compactIconSize,
                buttonSize = compactIconButtonSize,
            )
        }
        IconButton(
            onClick = actions.onAnalyzeLineClick,
            modifier = Modifier.size(compactIconButtonSize)
        ) {
            IconSm(
                imageVector = Icons.Default.Analytics,
                contentDescription = "Analyze line",
                modifier = Modifier.size(compactIconSize)
            )
        }
        IconButton(
            onClick = actions.onStopTrainingClick,
            modifier = Modifier.size(compactIconButtonSize)
        ) {
            IconSm(
                imageVector = Icons.Default.Stop,
                contentDescription = "Stop training",
                modifier = Modifier.size(compactIconSize)
            )
        }
    }

    @Composable
    fun ShowLineActionsRow(
        actions: TrainSingleLineContentActions
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
            TrainingActionButton()
        }
    }

    Column(modifier = modifier) {
        if (simpleViewEnabled) {
            if (state != TrainingSingleLineActionsState.ShowingLine) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(compactActionSpacing),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    if (state == TrainingSingleLineActionsState.Training) {
                        HintIconButton(
                            onClick = actions.onHintClick,
                            iconSize = compactIconSize,
                            buttonSize = compactIconButtonSize,
                        )
                    }
                    IconButton(
                        onClick = actions.onAnalyzeLineClick,
                        modifier = Modifier.size(compactIconButtonSize)
                    ) {
                        IconSm(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Analyze line",
                            modifier = Modifier.size(compactIconSize)
                        )
                    }
                }
            }
            if (state == TrainingSingleLineActionsState.Mistake) {
                Spacer(modifier = Modifier.height(AppDimens.spaceMd))
                PrimaryButton(
                    text = "Make correct move",
                    onClick = actions.onMakeCorrectMoveClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            return@Column
        }

        if (state == TrainingSingleLineActionsState.ShowingLine) {
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
            actions = actions
        )

        if (state != TrainingSingleLineActionsState.Mistake) {
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
private fun RenderShowLineDialog(
    visible: Boolean,
    moveDelayInput: String,
    onMoveDelayChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onStartClick: () -> Unit,
) {
    if (!visible) {
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Background.ScreenDark,
        title = {
            SectionTitleText(text = "Show line")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                BodySecondaryText(text = "Move delay (ms)")
                AppNumberSlider(
                    value = resolveShowLineMoveDelayMs(moveDelayInput).toInt(),
                    min = MinShowLineMoveDelayMs.toInt(),
                    max = MaxShowLineMoveDelayMs.toInt(),
                    onValueChange = { onMoveDelayChange(it.toString()) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onStartClick) {
                BodySecondaryText(
                    text = "Start",
                    color = TextColor.Primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                BodySecondaryText(
                    text = "Cancel",
                    color = TextColor.Primary,
                )
            }
        }
    )
}

@Composable
internal fun TrainingBoardSection(
    lineController: LineController,
    wrongMoveSquare: String? = null,
    hintSquare: String? = null,
    modifier: Modifier = Modifier
) {
    val boardState = lineController.boardState

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(AppDimens.radiusXl))
    ) {
        key(boardState) {
            ChessBoardWithCoordinates(
                lineController = lineController,
                wrongMoveSquare = wrongMoveSquare,
                hintSquare = hintSquare,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
internal fun TrainSingleLineCompletionDialog(
    dialogState: TrainSingleLineCompletionState,
    onRepeatClick: () -> Unit,
    onFinishClick: () -> Unit,
    onNextTrainingClick: (() -> Unit)? = null,
    onMarkDubiousAndNextTrainingClick: (() -> Unit)? = null,
) {
    val dialogActions = buildList {
        add(AppMessageDialogAction(text = "Repeat", onClick = onRepeatClick))

        if (onNextTrainingClick != null) {
            add(AppMessageDialogAction(text = "Next", onClick = onNextTrainingClick))
        }

        if (onMarkDubiousAndNextTrainingClick != null) {
            add(
                AppMessageDialogAction(
                    text = "Doubt & Next",
                    onClick = onMarkDubiousAndNextTrainingClick,
                ),
            )
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
