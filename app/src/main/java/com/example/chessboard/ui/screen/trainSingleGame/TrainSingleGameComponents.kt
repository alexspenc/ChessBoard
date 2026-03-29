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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.ui.BoardOrientation
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

// Renders the completion dialog only when the session has a completion state.
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
    gameId: Long,
    trainingId: Long,
    trainingGameData: TrainSingleGameData,
    gameController: GameController,
    currentOrientation: BoardOrientation,
    currentSideIndex: Int,
    sidesCount: Int,
    phase: TrainSingleGamePhase,
    mistakesCount: Int,
    onShowLineClick: () -> Unit,
    onStopShowLineClick: () -> Unit,
    onStartTrainingClick: () -> Unit,
    onMakeCorrectMoveClick: () -> Unit,
) {
    ScreenSection {
        Column {
            TrainingGameHeader(title = trainingGameData.game.event)
            Spacer(modifier = Modifier.height(AppDimens.spaceSm))
            TrainingBoardSection(gameController = gameController)
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            TrainingSingleGameActions(
                onShowLineClick = onShowLineClick,
                onStopShowLineClick = onStopShowLineClick,
                onStartTrainingClick = onStartTrainingClick,
                onMakeCorrectMoveClick = onMakeCorrectMoveClick,
                isShowingLine = phase == TrainSingleGamePhase.ShowingLine,
                isTraining = phase == TrainSingleGamePhase.Training,
                showCorrectMove = phase == TrainSingleGamePhase.Mistake,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            TrainingSessionInfo(
                trainingId = trainingId,
                gameId = gameId,
                movesCount = trainingGameData.uciMoves.size,
                currentOrientation = currentOrientation,
                currentSideIndex = currentSideIndex,
                sidesCount = sidesCount,
                phase = phase,
                mistakesCount = mistakesCount
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
    trainingId: Long,
    gameId: Long,
    movesCount: Int,
    currentOrientation: BoardOrientation,
    currentSideIndex: Int,
    sidesCount: Int,
    phase: TrainSingleGamePhase,
    mistakesCount: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        BodySecondaryText(
            text = "Training ID: $trainingId"
        )
        BodySecondaryText(
            text = "Game ID: $gameId"
        )
        BodySecondaryText(
            text = "Moves loaded: $movesCount"
        )
        BodySecondaryText(
            text = "Training side: ${orientationLabel(currentOrientation)}"
        )
        if (sidesCount > 1) {
            CardMetaText(text = "Side ${currentSideIndex + 1} of $sidesCount")
        }
        BodySecondaryText(
            text = "Session state: ${phase.name}"
        )
        BodySecondaryText(
            text = "Mistakes: $mistakesCount"
        )
    }
}

// Displays the session action buttons and the corrective move action after mistakes.
@Composable
internal fun TrainingSingleGameActions(
    onShowLineClick: () -> Unit,
    onStopShowLineClick: () -> Unit,
    onStartTrainingClick: () -> Unit,
    onMakeCorrectMoveClick: () -> Unit,
    isShowingLine: Boolean,
    isTraining: Boolean,
    showCorrectMove: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (isShowingLine) {
            PrimaryButton(
                text = "Stop show line",
                onClick = onStopShowLineClick,
                modifier = Modifier.fillMaxWidth()
            )
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
        ) {
            PrimaryButton(
                text = "Show line",
                onClick = onShowLineClick,
                enabled = !isTraining
            )
            PrimaryButton(
                text = "Start training",
                onClick = onStartTrainingClick,
                enabled = !isTraining
            )
        }

        if (!showCorrectMove) {
            return
        }

        Spacer(modifier = Modifier.height(AppDimens.spaceMd))
        PrimaryButton(
            text = "Make correct move",
            onClick = onMakeCorrectMoveClick,
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
