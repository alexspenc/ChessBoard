package com.example.chessboard.ui.screen.trainSingleGame

// Entry/loading layer for single-game training.
// This file stays separate so screen startup, error routing, and data preloading
// do not leak into the actual training screen rendering.

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.chessboard.boardmodel.GameDraft
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.service.parsePgnMoves
import com.example.chessboard.service.uciMovesToMoves
import com.example.chessboard.ui.components.AppMessageDialog
import com.github.bhlangonijr.chesslib.Board
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface TrainSingleGameLaunchState {
    data object Loading : TrainSingleGameLaunchState
    data class Ready(val trainingGameData: TrainSingleGameData) : TrainSingleGameLaunchState
    data object TrainingNotFound : TrainSingleGameLaunchState
    data object GameNotFound : TrainSingleGameLaunchState
}

@Composable
// Loads the training game data or shows a launch error before opening the training screen.
fun TrainSingleGameLauncherScreenContainer(
    trainingId: Long,
    gameId: Long,
    moveFrom: Int = 1,
    moveTo: Int = 0,
    keepLineIfZero: Boolean = false,
    hasNextTrainingGame: Boolean = false,
    sessionCurrent: Int = 0,
    sessionTotal: Int = 0,
    onTrainingFinished: (TrainSingleGameResult) -> Unit = {},
    onNextTrainingClick: (TrainSingleGameResult) -> Unit = {},
    onOpenGameEditorClick: (GameEntity) -> Unit = {},
    onCloneGameClick: (GameDraft) -> Unit = {},
    onSearchByPositionClick: (String) -> Unit = {},
    onAnalyzeGameClick: (List<String>, Int) -> Unit = { _, _ -> },
    screenContext: ScreenContainerContext,
    modifier: Modifier = Modifier,
) {
    val inDbProvider = screenContext.inDbProvider
    val trainingService = remember(inDbProvider) { inDbProvider.createTrainingService() }
    var launchState by remember { mutableStateOf<TrainSingleGameLaunchState>(TrainSingleGameLaunchState.Loading) }

    LaunchedEffect(trainingId, gameId) {
        launchState = TrainSingleGameLaunchState.Loading

        val training = withContext(Dispatchers.IO) {
            trainingService.getTrainingById(trainingId)
        }
        if (training == null) {
            launchState = TrainSingleGameLaunchState.TrainingNotFound
            return@LaunchedEffect
        }

        launchState = withContext(Dispatchers.IO) {
            val game = inDbProvider.loadTrainingGame(gameId)
            if (game == null) {
                return@withContext TrainSingleGameLaunchState.GameNotFound
            }

            val allMoves = parsePgnMoves(game.pgn)
            val startPly = ((moveFrom - 1) * 2).coerceAtMost(allMoves.size)
            val endPly = if (moveTo > 0) (moveTo * 2).coerceAtMost(allMoves.size) else allMoves.size
            val moves = allMoves.subList(startPly, endPly)
            val startFen = if (startPly == 0) null else {
                val board = Board()
                uciMovesToMoves(allMoves.take(startPly)).forEach { board.doMove(it) }
                board.fen
            }

            TrainSingleGameLaunchState.Ready(
                trainingGameData = TrainSingleGameData(
                    game = game,
                    uciMoves = moves,
                    startFen = startFen,
                    hasMoveCap = moveTo > 0,
                    analysisUciMoves = allMoves,
                    analysisStartPly = startPly,
                )
            )
        }
    }

    if (launchState is TrainSingleGameLaunchState.TrainingNotFound) {
        TrainingLaunchErrorDialog(
            title = "Training not found",
            message = "The selected training is unavailable to start.",
            onDismiss = { screenContext.onNavigate(ScreenType.Training) },
        )
        return
    }

    if (launchState is TrainSingleGameLaunchState.GameNotFound) {
        TrainingLaunchErrorDialog(
            title = "Game not found",
            message = "The selected game is unavailable to start.",
            onDismiss = { screenContext.onNavigate(ScreenType.EditTraining(trainingId)) },
        )
        return
    }

    val readyState = launchState as? TrainSingleGameLaunchState.Ready ?: return

    TrainSingleGameScreenContainer(
        gameId = gameId,
        trainingId = trainingId,
        trainingGameData = readyState.trainingGameData,
        keepLineIfZero = keepLineIfZero,
        hasNextTrainingGame = hasNextTrainingGame,
        sessionCurrent = sessionCurrent,
        sessionTotal = sessionTotal,
        onTrainingFinished = onTrainingFinished,
        onNextTrainingClick = onNextTrainingClick,
        onOpenGameEditorClick = { onOpenGameEditorClick(readyState.trainingGameData.game) },
        onCloneGameClick = { onCloneGameClick(it) },
        onSearchByPositionClick = onSearchByPositionClick,
        onAnalyzeGameClick = onAnalyzeGameClick,
        screenContext = screenContext,
        modifier = modifier,
    )
}

@Composable
private fun TrainingLaunchErrorDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
) {
    AppMessageDialog(
        title = title,
        message = message,
        onDismiss = onDismiss,
    )
}
