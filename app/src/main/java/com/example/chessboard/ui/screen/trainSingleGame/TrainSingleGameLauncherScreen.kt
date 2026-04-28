package com.example.chessboard.ui.screen.trainSingleGame

/**
 * File role: groups the launch/loading layer for a single-game training screen.
 * Allowed here:
 * - training-game data loading, launch-time validation, and error routing
 * - wiring from loaded launch data into the actual training screen container
 * Not allowed here:
 * - long-lived session state that belongs in runtimecontext
 * - detailed training-screen interaction logic that belongs in TrainSingleGameScreen
 * Validation date: 2026-04-25
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.chessboard.boardmodel.GameDraft
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.runtimecontext.TrainingRuntimeContext
import com.example.chessboard.service.TrainingGameLaunchBrokenTrainingDeleted
import com.example.chessboard.service.TrainingGameLaunchGameNotFound
import com.example.chessboard.service.TrainingGameLaunchGameRemovedFromTraining
import com.example.chessboard.service.TrainingGameLaunchNoTrainings
import com.example.chessboard.service.TrainingGameLaunchReady
import com.example.chessboard.service.TrainingGameLaunchTrainingNotFound
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
    data object GameRemovedFromTraining : TrainSingleGameLaunchState
}

@Composable
// Loads the training game data or shows a launch error before opening the training screen.
fun TrainSingleGameLauncherScreenContainer(
    trainingId: Long,
    gameId: Long,
    moveFrom: Int = 1,
    moveTo: Int = 0,
    trainingRuntimeContext: TrainingRuntimeContext,
    keepLineIfZero: Boolean = false,
    hasNextTrainingGame: Boolean = false,
    sessionCurrent: Int = 0,
    sessionTotal: Int = 0,
    onTrainingFinished: (TrainSingleGameResult) -> Unit = {},
    onNextTrainingClick: (TrainSingleGameResult) -> Unit = {},
    onInterruptTrainingClick: () -> Unit,
    onOpenGameEditorClick: (GameEntity) -> Unit,
    onCloneGameClick: (GameDraft) -> Unit,
    onSearchByPositionClick: (String) -> Unit,
    onAnalyzeGameClick: (List<String>, Int) -> Unit,
    simpleViewEnabled: Boolean = false,
    screenContext: ScreenContainerContext,
    modifier: Modifier = Modifier,
) {
    val inDbProvider = screenContext.inDbProvider
    val trainSingleGameService = remember(inDbProvider) {
        inDbProvider.createTrainSingleGameService()
    }
    var launchState by remember { mutableStateOf<TrainSingleGameLaunchState>(TrainSingleGameLaunchState.Loading) }

    fun clearInvalidTrainingRuntimeState() {
        trainingRuntimeContext.clearGameProgress(trainingId, gameId)
        if (trainingRuntimeContext.activeGameId(trainingId) == gameId) {
            trainingRuntimeContext.setCurrentGameId(trainingId, null)
        }
    }

    LaunchedEffect(trainingId, gameId) {
        launchState = TrainSingleGameLaunchState.Loading

        launchState = withContext(Dispatchers.IO) {
            when (val launchData = trainSingleGameService.getTrainingGameLaunchData(trainingId, gameId)) {
                is TrainingGameLaunchTrainingNotFound,
                is TrainingGameLaunchBrokenTrainingDeleted,
                TrainingGameLaunchNoTrainings -> {
                    return@withContext TrainSingleGameLaunchState.TrainingNotFound
                }

                is TrainingGameLaunchGameNotFound -> {
                    return@withContext TrainSingleGameLaunchState.GameNotFound
                }

                is TrainingGameLaunchGameRemovedFromTraining -> {
                    return@withContext TrainSingleGameLaunchState.GameRemovedFromTraining
                }

                is TrainingGameLaunchReady -> {
                    val game = trainSingleGameService.loadGame(launchData.launchData.gameId)
                        ?: return@withContext TrainSingleGameLaunchState.GameNotFound

                    val allMoves = parsePgnMoves(game.pgn)
                    val startPly = ((moveFrom - 1) * 2).coerceAtMost(allMoves.size)
                    val endPly = if (moveTo > 0) (moveTo * 2).coerceAtMost(allMoves.size) else allMoves.size
                    val moves = allMoves.subList(startPly, endPly)
                    val startFen = if (startPly == 0) null else {
                        val board = Board()
                        uciMovesToMoves(allMoves.take(startPly)).forEach { board.doMove(it) }
                        board.fen
                    }

                    return@withContext TrainSingleGameLaunchState.Ready(
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
        }
    }

    if (launchState is TrainSingleGameLaunchState.TrainingNotFound) {
        LaunchedEffect(launchState) {
            clearInvalidTrainingRuntimeState()
        }
        TrainingLaunchErrorDialog(
            title = "Training not found",
            message = "The selected training is unavailable to start.",
            onDismiss = { screenContext.onNavigate(ScreenType.Training) },
        )
        return
    }

    if (launchState is TrainSingleGameLaunchState.GameNotFound) {
        LaunchedEffect(launchState) {
            clearInvalidTrainingRuntimeState()
        }
        TrainingLaunchErrorDialog(
            title = "Game not found",
            message = "The selected game is unavailable to start.",
            onDismiss = { screenContext.onNavigate(ScreenType.EditTraining(trainingId)) },
        )
        return
    }

    if (launchState is TrainSingleGameLaunchState.GameRemovedFromTraining) {
        LaunchedEffect(launchState) {
            clearInvalidTrainingRuntimeState()
        }
        TrainingLaunchErrorDialog(
            title = "Game removed from training",
            message = "The selected game no longer belongs to this training.",
            onDismiss = { screenContext.onNavigate(ScreenType.EditTraining(trainingId)) },
        )
        return
    }

    val readyState = launchState as? TrainSingleGameLaunchState.Ready ?: return

    TrainSingleGameScreenContainer(
        gameId = gameId,
        trainingId = trainingId,
        trainingGameData = readyState.trainingGameData,
        trainingRuntimeContext = trainingRuntimeContext,
        keepLineIfZero = keepLineIfZero,
        hasNextTrainingGame = hasNextTrainingGame,
        sessionCurrent = sessionCurrent,
        sessionTotal = sessionTotal,
        onTrainingFinished = onTrainingFinished,
        onNextTrainingClick = onNextTrainingClick,
        onInterruptTrainingClick = onInterruptTrainingClick,
        onOpenGameEditorClick = { onOpenGameEditorClick(readyState.trainingGameData.game) },
        onCloneGameClick = { onCloneGameClick(it) },
        onSearchByPositionClick = onSearchByPositionClick,
        onAnalyzeGameClick = onAnalyzeGameClick,
        simpleViewEnabled = simpleViewEnabled,
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
