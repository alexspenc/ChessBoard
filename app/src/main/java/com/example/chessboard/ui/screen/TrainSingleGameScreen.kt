package com.example.chessboard.ui.screen

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.entity.SideMask
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.ChessBoardWithCoordinates
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TrainingBackgroundDark
import com.example.chessboard.ui.theme.TrainingTextPrimary
import com.example.chessboard.ui.theme.TrainingTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class TrainSingleGameData(
    val game: GameEntity,
    val uciMoves: List<String>
)

data class TrainSingleGameResult(
    val gameId: Long,
    val trainingId: Long,
    val mistakesCount: Int
)

private const val ShowLineMoveDelayMs = 500L

private enum class TrainSingleGamePhase {
    Idle,
    ShowingLine,
    Training,
    Mistake
}

@Composable
fun TrainSingleGameScreenContainer(
    activity: Activity,
    gameId: Long,
    trainingId: Long,
    onTrainingFinished: (TrainSingleGameResult) -> Unit = {},
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    modifier: Modifier = Modifier,
    inDbProvider: DatabaseProvider,
) {
    var trainingGameData by remember { mutableStateOf<TrainSingleGameData?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(gameId) {
        trainingGameData = withContext(Dispatchers.IO) {
            val game = inDbProvider.getGameById(gameId) ?: return@withContext null
            TrainSingleGameData(
                game = game,
                uciMoves = parsePgnMoves(game.pgn)
            )
        }
    }

    TrainSingleGameScreen(
        gameId = gameId,
        trainingId = trainingId,
        trainingGameData = trainingGameData,
        onTrainingFinished = { result ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    inDbProvider.decreaseLineWeight(
                        trainingId = result.trainingId,
                        gameId = result.gameId
                    )
                }
                onTrainingFinished(result)
            }
        },
        onBackClick = onBackClick,
        onNavigate = onNavigate,
        modifier = modifier
    )
}

@Composable
private fun TrainSingleGameScreen(
    gameId: Long,
    trainingId: Long,
    trainingGameData: TrainSingleGameData? = null,
    onTrainingFinished: (TrainSingleGameResult) -> Unit = {},
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedNavItem by remember { mutableStateOf<ScreenType>(ScreenType.Home) }
    var currentSideIndex by remember(trainingGameData?.game?.id) { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(TrainSingleGamePhase.Idle) }
    val trainingSides = remember(trainingGameData?.game?.sideMask) {
        trainingGameData?.game?.sideMask?.let(::resolveTrainingOrientations).orEmpty()
    }
    val currentOrientation = trainingSides.getOrNull(currentSideIndex) ?: BoardOrientation.WHITE
    var expectedPly by remember(trainingGameData?.game?.id, currentOrientation) { mutableIntStateOf(0) }
    var mistakesCount by remember(trainingGameData?.game?.id) { mutableIntStateOf(0) }
    var completionDialog by remember { mutableStateOf<TrainSingleGameCompletionState?>(null) }
    val gameController = remember(currentOrientation) { GameController(currentOrientation) }

    LaunchedEffect(gameController, trainingGameData?.game?.id) {
        gameController.resetToStartPosition()
        phase = TrainSingleGamePhase.Idle
        expectedPly = 0
        completionDialog = null
    }

    LaunchedEffect(trainingGameData?.game?.id) {
        mistakesCount = 0
    }

    LaunchedEffect(phase, trainingGameData?.uciMoves, gameController) {
        if (phase != TrainSingleGamePhase.ShowingLine) {
            return@LaunchedEffect
        }

        val uciMoves = trainingGameData?.uciMoves.orEmpty()
        gameController.resetToStartPosition()

        for (ply in 1..uciMoves.size) {
            delay(ShowLineMoveDelayMs)
            gameController.loadFromUciMoves(uciMoves, ply)
        }

        phase = TrainSingleGamePhase.Idle
    }

    LaunchedEffect(phase, gameController.boardState, expectedPly, currentOrientation, trainingGameData?.uciMoves) {
        if (phase != TrainSingleGamePhase.Training) {
            return@LaunchedEffect
        }

        val uciMoves = trainingGameData?.uciMoves.orEmpty()
        if (uciMoves.isEmpty()) {
            return@LaunchedEffect
        }

        if (expectedPly >= uciMoves.size) {
            phase = TrainSingleGamePhase.Idle
            completionDialog = buildCompletionDialog(
                currentSideIndex = currentSideIndex,
                sidesCount = trainingSides.size,
                currentOrientation = currentOrientation
            )
            return@LaunchedEffect
        }

        if (!isUserTurn(expectedPly, currentOrientation)) {
            gameController.loadFromUciMoves(uciMoves, expectedPly + 1)
            expectedPly += 1
            return@LaunchedEffect
        }

        if (gameController.currentMoveIndex <= expectedPly) {
            return@LaunchedEffect
        }

        val lastMoveUci = gameController.getMovesCopy()
            .getOrNull(gameController.currentMoveIndex - 1)
            ?.let(::moveToUci)
            ?: return@LaunchedEffect

        if (lastMoveUci == uciMoves[expectedPly]) {
            expectedPly += 1
            return@LaunchedEffect
        }

        mistakesCount += 1
        gameController.loadFromUciMoves(uciMoves, expectedPly)
        phase = TrainSingleGamePhase.Mistake
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = TrainingBackgroundDark,
        topBar = {
            AppTopBar(
                title = "Train Game",
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            AppBottomNavigation(
                items = defaultAppBottomNavigationItems(),
                selectedItem = selectedNavItem,
                onItemSelected = {
                    selectedNavItem = it
                    onNavigate(it)
                }
            )
        }
    ) { paddingValues ->
        completionDialog?.let { dialogState ->
            TrainSingleGameCompletionDialog(
                dialogState = dialogState,
                onRepeatClick = {
                    completionDialog = null
                    gameController.resetToStartPosition()
                    expectedPly = 0
                    phase = TrainSingleGamePhase.Training
                },
                onFinishClick = {
                    completionDialog = null

                    if (dialogState.hasNextSide) {
                        currentSideIndex += 1
                    } else {
                        onTrainingFinished(
                            TrainSingleGameResult(
                                gameId = gameId,
                                trainingId = trainingId,
                                mistakesCount = mistakesCount
                            )
                        )
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            TrainSingleGameContent(
                gameId = gameId,
                trainingId = trainingId,
                trainingGameData = trainingGameData,
                gameController = gameController,
                currentOrientation = currentOrientation,
                currentSideIndex = currentSideIndex,
                sidesCount = trainingSides.size,
                phase = phase,
                mistakesCount = mistakesCount,
                onShowLineClick = {
                    completionDialog = null
                    gameController.resetToStartPosition()
                    expectedPly = 0
                    phase = TrainSingleGamePhase.ShowingLine
                },
                onStartTrainingClick = {
                    completionDialog = null
                    gameController.resetToStartPosition()
                    expectedPly = 0
                    phase = TrainSingleGamePhase.Training
                },
                onMakeCorrectMoveClick = {
                    val uciMoves = trainingGameData?.uciMoves.orEmpty()
                    if (expectedPly >= uciMoves.size) {
                        phase = TrainSingleGamePhase.Idle
                    } else {
                        gameController.loadFromUciMoves(uciMoves, expectedPly + 1)
                        expectedPly += 1
                        phase = TrainSingleGamePhase.Training
                    }
                }
            )
        }
    }
}

@Composable
private fun TrainSingleGameContent(
    gameId: Long,
    trainingId: Long,
    trainingGameData: TrainSingleGameData?,
    gameController: GameController,
    currentOrientation: BoardOrientation,
    currentSideIndex: Int,
    sidesCount: Int,
    phase: TrainSingleGamePhase,
    mistakesCount: Int,
    onShowLineClick: () -> Unit,
    onStartTrainingClick: () -> Unit,
    onMakeCorrectMoveClick: () -> Unit,
) {
    ScreenSection {
        if (trainingGameData == null) {
            BodySecondaryText(
                text = "Loading training session for gameId=$gameId, trainingId=$trainingId",
                color = TrainingTextSecondary
            )
            return@ScreenSection
        }

        Column {
            SectionTitleText(
                text = trainingGameData.game.event ?: "Unnamed Opening",
                color = TrainingTextPrimary
            )
            Spacer(modifier = Modifier.height(AppDimens.spaceSm))
            TrainingBoardSection(gameController = gameController)
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            TrainingSingleGameActions(
                onShowLineClick = onShowLineClick,
                onStartTrainingClick = onStartTrainingClick,
                onMakeCorrectMoveClick = onMakeCorrectMoveClick,
                isShowingLine = phase == TrainSingleGamePhase.ShowingLine,
                isTraining = phase == TrainSingleGamePhase.Training,
                showCorrectMove = phase == TrainSingleGamePhase.Mistake,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            BodySecondaryText(
                text = "Training ID: $trainingId",
                color = TrainingTextSecondary
            )
            BodySecondaryText(
                text = "Game ID: $gameId",
                color = TrainingTextSecondary
            )
            BodySecondaryText(
                text = "Moves loaded: ${trainingGameData.uciMoves.size}",
                color = TrainingTextSecondary
            )
            BodySecondaryText(
                text = "Training side: ${orientationLabel(currentOrientation)}",
                color = TrainingTextSecondary
            )
            if (sidesCount > 1) {
                CardMetaText(
                    text = "Side ${currentSideIndex + 1} of $sidesCount",
                    color = TrainingTextSecondary
                )
            }
            BodySecondaryText(
                text = "Session state: ${phase.name}",
                color = TrainingTextSecondary
            )
            BodySecondaryText(
                text = "Mistakes: $mistakesCount",
                color = TrainingTextSecondary
            )
        }
    }
}

@Composable
private fun TrainingSingleGameActions(
    onShowLineClick: () -> Unit,
    onStartTrainingClick: () -> Unit,
    onMakeCorrectMoveClick: () -> Unit,
    isShowingLine: Boolean,
    isTraining: Boolean,
    showCorrectMove: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
        ) {
            PrimaryButton(
                text = "Show line",
                onClick = onShowLineClick,
                enabled = !isShowingLine && !isTraining
            )
            PrimaryButton(
                text = "Start training",
                onClick = onStartTrainingClick,
                enabled = !isShowingLine && !isTraining
            )
        }

        if (showCorrectMove) {
            Spacer(modifier = Modifier.height(AppDimens.spaceMd))
            PrimaryButton(
                text = "Make correct move",
                onClick = onMakeCorrectMoveClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TrainingBoardSection(
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

@Composable
private fun TrainSingleGameCompletionDialog(
    dialogState: TrainSingleGameCompletionState,
    onRepeatClick: () -> Unit,
    onFinishClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onFinishClick,
        title = {
            SectionTitleText(text = dialogState.title, color = TrainingTextPrimary)
        },
        text = {
            BodySecondaryText(text = dialogState.message, color = TrainingTextSecondary)
        },
        confirmButton = {
            TextButton(onClick = onRepeatClick) {
                BodySecondaryText(text = "Repeat variation", color = TrainingTextPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onFinishClick) {
                BodySecondaryText(text = dialogState.finishLabel, color = TrainingTextPrimary)
            }
        }
    )
}

private fun resolveTrainingOrientations(sideMask: Int): List<BoardOrientation> {
    if (sideMask == SideMask.WHITE) {
        return listOf(BoardOrientation.WHITE)
    }

    if (sideMask == SideMask.BLACK) {
        return listOf(BoardOrientation.BLACK)
    }

    if (sideMask == SideMask.BOTH) {
        return listOf(BoardOrientation.WHITE, BoardOrientation.BLACK)
    }

    return listOf(BoardOrientation.WHITE)
}

private fun orientationLabel(orientation: BoardOrientation): String =
    when (orientation) {
        BoardOrientation.WHITE -> "White"
        BoardOrientation.BLACK -> "Black"
    }

private fun isUserTurn(expectedPly: Int, orientation: BoardOrientation): Boolean =
    when (orientation) {
        BoardOrientation.WHITE -> expectedPly % 2 == 0
        BoardOrientation.BLACK -> expectedPly % 2 == 1
    }

private fun moveToUci(move: com.github.bhlangonijr.chesslib.move.Move): String =
    "${move.from.value().lowercase()}${move.to.value().lowercase()}"

private data class TrainSingleGameCompletionState(
    val title: String,
    val message: String,
    val finishLabel: String,
    val hasNextSide: Boolean
)

private fun buildCompletionDialog(
    currentSideIndex: Int,
    sidesCount: Int,
    currentOrientation: BoardOrientation
): TrainSingleGameCompletionState {
    if (currentSideIndex + 1 < sidesCount) {
        return TrainSingleGameCompletionState(
            title = "Variation completed",
            message = "The ${orientationLabel(currentOrientation).lowercase()} side is completed. Continue with the next side or repeat this variation.",
            finishLabel = "Next side",
            hasNextSide = true
        )
    }

    return TrainSingleGameCompletionState(
        title = "Variation completed",
        message = "You reached the end of the game. Repeat the variation or finish this training.",
        finishLabel = "Finish variation",
        hasNextSide = false
    )
}
