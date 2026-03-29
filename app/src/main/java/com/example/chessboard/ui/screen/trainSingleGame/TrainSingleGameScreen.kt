package com.example.chessboard.ui.screen.trainSingleGame

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.screen.buildMoveLabels
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.theme.AppDimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Loads the selected game, runs the single-game training flow and updates the training entry
 * before returning the session result.
 *
 * @param gameId Identifier of the game being trained.
 * @param trainingId Identifier of the training that owns the game.
 * @param onTrainingFinished Called after the training is updated in the database.
 * @param onBackClick Called when the user presses the top bar back button.
 * @param onNavigate Called when the user selects an item in the bottom navigation.
 * @param modifier Modifier for the root container.
 * @param inDbProvider Database access used to load the game and update the training.
 */
@Composable
fun TrainSingleGameScreenContainer(
    gameId: Long,
    trainingId: Long,
    trainingGameData: TrainSingleGameData,
    onTrainingFinished: (TrainSingleGameResult) -> Unit = {},
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    modifier: Modifier = Modifier,
    inDbProvider: DatabaseProvider,
) {
    val scope = rememberCoroutineScope()

    TrainSingleGameScreen(
        gameId = gameId,
        trainingId = trainingId,
        trainingGameData = trainingGameData,
        onTrainingFinished = { result ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    inDbProvider.finishTrainingGame(
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
// Hosts the single-game training session state and coordinates training flow.
private fun TrainSingleGameScreen(
    gameId: Long,
    trainingId: Long,
    trainingGameData: TrainSingleGameData,
    onTrainingFinished: (TrainSingleGameResult) -> Unit = {},
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Keeps the parent navigation section highlighted while the training screen is open.
    var selectedNavItem by remember { mutableStateOf<ScreenType>(ScreenType.Home) }
    val loadedGame = trainingGameData.game
    val uciMoves = trainingGameData.uciMoves
    val moveLabels = remember(uciMoves) { buildMoveLabels(uciMoves) }
    // Stores the full mutable training session state for the current screen.
    var uiState by remember(loadedGame.id) { mutableStateOf(TrainSingleGameUiState()) }
    var showLineJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    // Resolves the ordered list of sides that must be trained for the current game.
    val trainingSides = remember(loadedGame.sideMask) {
        resolveTrainingOrientations(loadedGame.sideMask)
    }
    // Selects the active board orientation for the current training side.
    val currentOrientation = trainingSides.getOrNull(uiState.currentSideIndex) ?: BoardOrientation.WHITE
    // Owns the interactive board state for the currently trained side.
    val gameController = remember(currentOrientation) { GameController(currentOrientation) }

    LaunchedEffect(gameController, loadedGame.id) {
        showLineJob?.cancel()
        showLineJob = null
        gameController.resetToStartPosition()
        uiState = resetSessionState(uiState)
    }

    SideEffect {
        gameController.setUserMovesEnabled(resolveBoardInteractionEnabled(uiState))
        gameController.setAllowedMoveUci(
            resolveAllowedUserMoveUci(
                uiState = uiState,
                currentOrientation = currentOrientation,
                uciMoves = uciMoves
            )
        )
    }

    LaunchedEffect(
        uiState.phase,
        gameController.boardState,
        uiState.expectedPly,
        currentOrientation,
        uciMoves
    ) {
        uiState = handleTrainingProgress(
            uiState = uiState,
            gameController = gameController,
            uciMoves = uciMoves,
            currentOrientation = currentOrientation,
            sidesCount = trainingSides.size
        )
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
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
        RenderCompletionDialog(
            dialogState = uiState.completionDialog,
            onRepeatClick = {
                gameController.resetToStartPosition()
                uiState = buildRepeatVariationState(uiState)
            },
            onFinishClick = {
                uiState = handleCompletionFinish(
                    uiState = uiState,
                    gameId = gameId,
                    trainingId = trainingId,
                    onTrainingFinished = onTrainingFinished
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            TrainSingleGameContent(
                gameId = gameId,
                trainingId = trainingId,
                trainingGameData = trainingGameData,
                gameController = gameController,
                currentPly = gameController.currentMoveIndex,
                moveLabels = moveLabels,
                currentOrientation = currentOrientation,
                currentSideIndex = uiState.currentSideIndex,
                sidesCount = trainingSides.size,
                phase = uiState.phase,
                mistakesCount = uiState.mistakesCount,
                onShowLineClick = {
                    Log.d(
                        TrainSingleGameLogTag,
                        "Show line clicked. gameId= trainingId= boardState= moves="
                    )

                    showLineJob?.cancel()
                    showLineJob = null
                    gameController.resetToStartPosition()
                    uiState = buildShowLineState(uiState)
                    showLineJob = scope.launch {
                        try {
                            Log.d(
                                TrainSingleGameLogTag,
                                "Starting runShowLine coroutine. boardState= movesCount="
                            )
                            uiState = runShowLine(
                                uiState = uiState,
                                gameController = gameController,
                                uciMoves = uciMoves
                            )
                        } finally {
                            showLineJob = null
                        }
                    }
                },
                onStopShowLineClick = {
                    showLineJob?.cancel()
                    showLineJob = null
                    uiState = uiState.copy(phase = TrainSingleGamePhase.Idle)
                },
                onStartTrainingClick = {
                    gameController.resetToStartPosition()
                    uiState = advanceProgramMoves(
                        uiState = buildStartTrainingState(uiState),
                        gameController = gameController,
                        uciMoves = uciMoves,
                        currentOrientation = currentOrientation,
                        sidesCount = trainingSides.size
                    )
                },
                onStopTrainingClick = {
                    showLineJob?.cancel()
                    showLineJob = null
                    gameController.resetToStartPosition()
                    uiState = resetSessionState(uiState)
                },
                onMakeCorrectMoveClick = {
                    uiState = handleCorrectMove(
                        uiState = uiState,
                        gameController = gameController,
                        uciMoves = uciMoves,
                        currentOrientation = currentOrientation,
                        sidesCount = trainingSides.size
                    )
                }
            )
        }
    }
}
