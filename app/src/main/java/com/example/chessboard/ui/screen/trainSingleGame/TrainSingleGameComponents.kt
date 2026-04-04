package com.example.chessboard.ui.screen.trainSingleGame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.ui.ChessBoardWithCoordinates
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor

// Renders the wrong-move dialog only when the session has a message.
@Composable
internal fun RenderWrongMoveDialog(
    message: String?,
    onDismiss: () -> Unit
) {
    if (message == null) {
        return
    }

    AppMessageDialog(
        title = "Wrong Move",
        message = message,
        onDismiss = onDismiss
    )
}

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

// Renders the central training content for the selected game and side.
@Composable
internal fun TrainSingleGameContent(
    state: TrainSingleGameContentState,
    actions: TrainSingleGameContentActions,
) {
    ScreenSection {
        Column {
            TrainingGameHeader(title = state.trainingGameData.game.event)
            Spacer(modifier = Modifier.height(AppDimens.spaceSm))
            TrainingBoardSection(gameController = state.gameController)
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            TrainingSingleGameActions(
                actions = actions,
                isShowingLine = state.phase == TrainSingleGamePhase.ShowingLine,
                isTrainingActive = state.phase == TrainSingleGamePhase.Training || state.phase == TrainSingleGamePhase.Mistake,
                showCorrectMove = state.phase == TrainSingleGamePhase.Mistake,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            TrainingSessionInfo(state = state)
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            TrainingMovesLegend(
                moveLabels = state.moveLabels,
                currentPly = state.currentPly
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
internal fun TrainingMovesLegend(
    moveLabels: List<String>,
    currentPly: Int,
    modifier: Modifier = Modifier
) {
    CardSurface(modifier = modifier.fillMaxWidth()) {
        Column {
            SectionTitleText(text = "Moves")
            Spacer(modifier = Modifier.height(AppDimens.spaceSm))
            BodySecondaryText(
                text = resolveTrainingMoveLegendText(
                    moveLabels = moveLabels,
                    currentPly = currentPly
                )
            )
        }
    }
}

internal fun resolveTrainingMoveLegendText(
    moveLabels: List<String>,
    currentPly: Int
): String {
    val visibleMoves = moveLabels.take(currentPly)
    if (visibleMoves.isEmpty()) {
        return "No moves yet"
    }

    return visibleMoves.mapIndexed { index, label ->
        val moveNumber = index / 2 + 1
        val prefix = if (index % 2 == 0) "$moveNumber." else "$moveNumber..."
        "$prefix$label"
    }.joinToString(separator = " ")
}

// Displays the session action buttons and the corrective move action after mistakes.
@Composable
internal fun TrainingSingleGameActions(
    actions: TrainSingleGameContentActions,
    isShowingLine: Boolean,
    isTrainingActive: Boolean,
    showCorrectMove: Boolean,
    modifier: Modifier = Modifier
) {
    @Composable
    fun IdleTrainingActions(
        onShowLineClick: () -> Unit,
        onStartTrainingClick: () -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
        ) {
            PrimaryButton(
                text = "Show line",
                onClick = onShowLineClick
            )
            IconButton(onClick = onStartTrainingClick) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start training",
                    tint = TextColor.Primary
                )
            }
        }
    }

    @Composable
    fun PrimaryTrainingAction() {
        if (isTrainingActive) {
            PrimaryButton(
                text = "Stop training",
                onClick = actions.onStopTrainingClick,
                modifier = Modifier.fillMaxWidth()
            )
            return
        }

        IdleTrainingActions(
            onShowLineClick = actions.onShowLineClick,
            onStartTrainingClick = actions.onStartTrainingClick
        )
    }

    Column(modifier = modifier) {
        if (isShowingLine) {
            PrimaryButton(
                text = "Stop show line",
                onClick = actions.onStopShowLineClick,
                modifier = Modifier.fillMaxWidth()
            )
            return@Column
        }

        PrimaryTrainingAction()

        if (!showCorrectMove) {
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

// Renders the interactive chess board used by the training session.
@Composable
internal fun TrainingBoardSection(
    gameController: GameController,
    modifier: Modifier = Modifier
) {
    CardSurface(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(AppDimens.spaceMd)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppDimens.radiusLg))
        ) {
            ChessBoardWithCoordinates(
                gameController = gameController,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Shows the completion dialog for repeating the variation or finishing the side/session.
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
