package com.example.chessboard.ui.screen.trainSingleLine

/**
 * File role: groups the launch/loading layer for a single-line training screen.
 * Allowed here:
 * - training-line data loading, launch-time validation, and error routing
 * - wiring from loaded launch data into the actual training screen container
 * Not allowed here:
 * - long-lived session state that belongs in runtimecontext
 * - detailed training-screen interaction logic that belongs in TrainSingleLineScreen
 * Validation date: 2026-04-25
 */

import android.content.ClipData
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.chessboard.R
import com.example.chessboard.runtimecontext.TrainingRuntimeContext
import com.example.chessboard.service.TrainingLineLaunchBrokenTrainingDeleted
import com.example.chessboard.service.TrainingLineLaunchLineNotFound
import com.example.chessboard.service.TrainingLineLaunchLineRemovedFromTraining
import com.example.chessboard.service.TrainingLineLaunchNoTrainings
import com.example.chessboard.service.TrainingLineLaunchReady
import com.example.chessboard.service.TrainingLineLaunchTrainingNotFound
import com.example.chessboard.service.parsePgnMoves
import com.example.chessboard.service.uciMovesToMoves
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppMessageDialogAction
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import com.github.bhlangonijr.chesslib.Board
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface TrainSingleLineLaunchState {
    data object Loading : TrainSingleLineLaunchState
    data class Ready(
        val trainingLineData: TrainSingleLineData,
        val autoNextLine: Boolean,
    ) : TrainSingleLineLaunchState
    data class Failed(val message: String) : TrainSingleLineLaunchState
    data object TrainingNotFound : TrainSingleLineLaunchState
    data object LineNotFound : TrainSingleLineLaunchState
    data object LineRemovedFromTraining : TrainSingleLineLaunchState
}

@Composable
// Loads the training line data or shows a launch error before opening the training screen.
fun TrainSingleLineLauncherScreenContainer(
    launchRequest: TrainSingleLineLaunchRequest,
    trainingRuntimeContext: TrainingRuntimeContext,
    keepLineIfZero: Boolean = false,
    sessionProgress: TrainSingleLineSessionProgress = TrainSingleLineSessionProgress(),
    launchActions: TrainSingleLineLaunchActions,
    simpleViewEnabled: Boolean = false,
    screenContext: ScreenContainerContext,
    modifier: Modifier = Modifier,
) {
    val inDbProvider = screenContext.inDbProvider
    val trainSingleLineService = remember(inDbProvider) {
        inDbProvider.createTrainSingleLineService()
    }
    val unknownExceptionName = stringResource(R.string.train_single_line_unknown_exception)
    var launchState by remember { mutableStateOf<TrainSingleLineLaunchState>(TrainSingleLineLaunchState.Loading) }

    fun clearInvalidTrainingRuntimeState() {
        val target = launchRequest.target

        trainingRuntimeContext.clearLineProgress(target.trainingId, target.lineId)
        if (trainingRuntimeContext.lineIdInTraining(target.trainingId) == target.lineId) {
            trainingRuntimeContext.setLineIdInTraining(target.trainingId, null)
        }
    }

    LaunchedEffect(
        launchRequest.target.trainingId,
        launchRequest.target.lineId,
        unknownExceptionName,
    ) {
        launchState = try {
            withContext(Dispatchers.IO) {
                when (val launchData = trainSingleLineService.getTrainingLineLaunchData(
                    launchRequest.target.trainingId,
                    launchRequest.target.lineId,
                )) {
                    is TrainingLineLaunchTrainingNotFound,
                    is TrainingLineLaunchBrokenTrainingDeleted,
                    TrainingLineLaunchNoTrainings -> {
                        return@withContext TrainSingleLineLaunchState.TrainingNotFound
                    }

                    is TrainingLineLaunchLineNotFound -> {
                        return@withContext TrainSingleLineLaunchState.LineNotFound
                    }

                    is TrainingLineLaunchLineRemovedFromTraining -> {
                        return@withContext TrainSingleLineLaunchState.LineRemovedFromTraining
                    }

                    is TrainingLineLaunchReady -> {
                        val line = trainSingleLineService.loadLine(launchData.launchData.lineId)
                            ?: return@withContext TrainSingleLineLaunchState.LineNotFound

                        val allMoves = parsePgnMoves(line.pgn)
                        val startPly = ((launchRequest.moveFrom - 1) * 2).coerceAtMost(allMoves.size)
                        var endPly = allMoves.size
                        if (launchRequest.moveTo > 0) {
                            endPly = (launchRequest.moveTo * 2).coerceAtMost(allMoves.size)
                        }
                        val moves = allMoves.subList(startPly, endPly)
                        val startFen = createTrainingStartFen(startPly, allMoves)
                        val autoNextLine = screenContext.inDbProvider
                            .createUserProfileService()
                            .getProfile()
                            .autoNextLine

                        return@withContext TrainSingleLineLaunchState.Ready(
                            trainingLineData = TrainSingleLineData(
                                line = line,
                                uciMoves = moves,
                                startFen = startFen,
                                hasMoveCap = launchRequest.moveTo > 0,
                                analysisUciMoves = allMoves,
                                analysisStartPly = startPly,
                            ),
                            autoNextLine = autoNextLine,
                        )
                    }
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            TrainSingleLineLaunchState.Failed(
                formatTrainingLaunchException(
                    error = error,
                    unknownExceptionName = unknownExceptionName,
                )
            )
        }
    }

    if (launchState is TrainSingleLineLaunchState.TrainingNotFound) {
        LaunchedEffect(launchState) {
            clearInvalidTrainingRuntimeState()
        }
        TrainingLaunchErrorDialog(
            title = stringResource(R.string.train_single_line_training_not_found_title),
            message = stringResource(R.string.train_single_line_training_not_found_message),
            onDismiss = { screenContext.onNavigate(ScreenType.Training) },
        )
        return
    }

    if (launchState is TrainSingleLineLaunchState.LineNotFound) {
        LaunchedEffect(launchState) {
            clearInvalidTrainingRuntimeState()
        }
        TrainingLaunchErrorDialog(
            title = stringResource(R.string.train_single_line_line_not_found_title),
            message = stringResource(R.string.train_single_line_line_not_found_message),
            onDismiss = {
                screenContext.onNavigate(ScreenType.EditTraining(launchRequest.target.trainingId))
            },
        )
        return
    }

    if (launchState is TrainSingleLineLaunchState.LineRemovedFromTraining) {
        LaunchedEffect(launchState) {
            clearInvalidTrainingRuntimeState()
        }
        TrainingLaunchErrorDialog(
            title = stringResource(R.string.train_single_line_line_removed_title),
            message = stringResource(R.string.train_single_line_line_removed_message),
            onDismiss = {
                screenContext.onNavigate(ScreenType.EditTraining(launchRequest.target.trainingId))
            },
        )
        return
    }

    val failedState = launchState as? TrainSingleLineLaunchState.Failed
    if (failedState != null) {
        TrainingLaunchErrorDialog(
            title = stringResource(R.string.train_single_line_launch_failed_title),
            message = failedState.message,
            onDismiss = { screenContext.onNavigate(ScreenType.Training) },
            copyable = true,
        )
        return
    }

    val readyState = launchState as? TrainSingleLineLaunchState.Ready ?: return

    TrainSingleLineScreenContainer(
        target = launchRequest.target,
        trainingLineData = readyState.trainingLineData,
        trainingRuntimeContext = trainingRuntimeContext,
        keepLineIfZero = keepLineIfZero,
        sessionProgress = sessionProgress,
        onTrainingFinished = launchActions.onTrainingFinished,
        onNextTrainingClick = launchActions.onNextTrainingClick,
        onInterruptTrainingClick = launchActions.onInterruptTrainingClick,
        onOpenLineEditorClick = {
            launchActions.onOpenLineEditorClick(readyState.trainingLineData.line)
        },
        onCloneLineClick = launchActions.onCloneLineClick,
        onSearchByPositionClick = launchActions.onSearchByPositionClick,
        onAnalyzeLineClick = launchActions.onAnalyzeLineClick,
        onSimpleViewUpgradePromptRequested = launchActions.onSimpleViewUpgradePromptRequested,
        autoNextLine = readyState.autoNextLine,
        simpleViewEnabled = simpleViewEnabled,
        screenContext = screenContext,
        modifier = modifier,
    )
}

private fun createTrainingStartFen(
    startPly: Int,
    allMoves: List<String>,
): String? {
    if (startPly == 0) {
        return null
    }

    val board = Board()
    uciMovesToMoves(allMoves.take(startPly)).forEach { board.doMove(it) }
    return board.fen
}

private fun formatTrainingLaunchException(
    error: Exception,
    unknownExceptionName: String,
): String {
    val exceptionName = error::class.qualifiedName
        ?: error::class.simpleName
        ?: unknownExceptionName
    if (error.message.isNullOrBlank()) {
        return "$exceptionName\n\n${error.stackTraceToString()}"
    }

    return "$exceptionName: ${error.message}\n\n${error.stackTraceToString()}"
}

@Composable
private fun TrainingLaunchErrorDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    copyable: Boolean = false,
) {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val maxMessageHeight = LocalConfiguration.current.screenHeightDp.dp * 0.6f
    var messageModifier: Modifier = Modifier
    var actions: List<AppMessageDialogAction>? = null
    if (copyable) {
        messageModifier = Modifier
            .heightIn(max = maxMessageHeight)
            .verticalScroll(scrollState)
        actions = listOf(
            AppMessageDialogAction(
                text = stringResource(R.string.common_copy),
                onClick = {
                    coroutineScope.launch {
                        clipboard.setClipEntry(
                            ClipEntry(
                                ClipData.newPlainText(
                                    title,
                                    message,
                                )
                            )
                        )
                    }
                },
            ),
            AppMessageDialogAction(
                text = stringResource(R.string.common_ok),
                onClick = onDismiss,
            ),
        )
    }

    AppMessageDialog(
        title = title,
        message = message,
        onDismiss = onDismiss,
        actions = actions,
        messageModifier = messageModifier,
    )
}
