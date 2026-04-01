package com.example.chessboard.ui.screen.trainSingleGame

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.screen.parsePgnMoves
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TrainSingleGameLauncherScreenContainer(
    trainingId: Long,
    gameId: Long,
    onTrainingFinished: (TrainSingleGameResult) -> Unit = {},
    screenContext: ScreenContainerContext,
    modifier: Modifier = Modifier,
) {
    val inDbProvider = screenContext.inDbProvider
    var trainingExists by remember { mutableStateOf(true) }
    var gameExists by remember { mutableStateOf(true) }
    var trainingGameData by remember { mutableStateOf<TrainSingleGameData?>(null) }

    LaunchedEffect(trainingId, gameId) {
        val training = withContext(Dispatchers.IO) {
            inDbProvider.getTrainingById(trainingId)
        }
        if (training == null) {
            trainingExists = false
            return@LaunchedEffect
        }

        trainingGameData = withContext(Dispatchers.IO) {
            val game = inDbProvider.loadTrainingGame(gameId)
            if (game == null) {
                gameExists = false
                return@withContext null
            }
            TrainSingleGameData(
                game = game,
                uciMoves = parsePgnMoves(game.pgn),
            )
        }
    }

    if (!trainingExists) {
        TrainingLaunchErrorDialog(
            title = "Training not found",
            message = "The selected training is unavailable to start.",
            onDismiss = { screenContext.onNavigate(ScreenType.Training) },
        )
        return
    }

    if (!gameExists) {
        TrainingLaunchErrorDialog(
            title = "Game not found",
            message = "The selected game is unavailable to start.",
            onDismiss = { screenContext.onNavigate(ScreenType.CreateTraining(trainingId)) },
        )
        return
    }

    val loadedTrainingGameData = trainingGameData ?: return

    TrainSingleGameScreenContainer(
        gameId = gameId,
        trainingId = trainingId,
        trainingGameData = loadedTrainingGameData,
        onTrainingFinished = onTrainingFinished,
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
