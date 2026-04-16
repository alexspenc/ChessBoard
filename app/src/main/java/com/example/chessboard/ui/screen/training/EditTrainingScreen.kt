package com.example.chessboard.ui.screen.training

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.chessboard.RuntimeContext
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.screen.training.loadsave.RenderUnsavedTrainingChangesDialog
import com.example.chessboard.ui.screen.training.loadsave.TrainingLoadState
import com.example.chessboard.ui.screen.training.loadsave.TrainingSaveSuccess
import com.example.chessboard.ui.screen.training.loadsave.hasUnsavedTrainingEditorChanges
import com.example.chessboard.ui.screen.training.loadsave.loadEditTrainingState
import com.example.chessboard.ui.screen.training.loadsave.normalizeTrainingEditorName
import com.example.chessboard.ui.screen.training.loadsave.saveEditedTraining
import com.example.chessboard.ui.theme.AppDimens
import kotlinx.coroutines.launch

@Composable
private fun RenderMissingTrainingDialog(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    if (!visible) {
        return
    }

    AppMessageDialog(
        title = "Training Not Found",
        message = "The selected training is unavailable.",
        onDismiss = onDismiss
    )
}

@Composable
private fun RenderEditTrainingSaveSuccessDialog(
    success: TrainingSaveSuccess?,
    onDismiss: () -> Unit
) {
    val currentSuccess = success ?: return

    AppMessageDialog(
        title = "Training Updated",
        message = buildString {
            appendLine("ID: ${currentSuccess.trainingId}")
            appendLine("Name: ${currentSuccess.trainingName}")
            append("Games in training: ")
            append(currentSuccess.gamesCount)
        },
        onDismiss = onDismiss
    )
}

private fun createOpenEditTrainingGameEditorAction(
    allGamesById: Map<Long, GameEntity>,
    onOpenGameEditorClick: (GameEntity) -> Unit
): (Long) -> Unit {
    return openGameEditor@{ gameId ->
        val game = allGamesById[gameId] ?: return@openGameEditor
        onOpenGameEditorClick(game)
    }
}

@Composable
fun EditTrainingScreenContainer(
    trainingId: Long,
    screenContext: ScreenContainerContext,
    orderGamesInTraining: RuntimeContext.OrderGamesInTraining,
    hideLinesWithWeightZero: Boolean = false,
    onStartGameTrainingClick: (Long, Int, Int) -> Unit = { _, _, _ -> },
    onOpenGameEditorClick: (GameEntity) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val onBackClick = screenContext.onBackClick
    val onNavigate = screenContext.onNavigate
    val inDbProvider = screenContext.inDbProvider
    val trainingService = remember(inDbProvider) { inDbProvider.createTrainingService() }
    var loadState by remember { mutableStateOf(TrainingLoadState()) }
    var trainingSaveSuccess by remember { mutableStateOf<TrainingSaveSuccess?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(trainingId) {
        loadState = loadEditTrainingState(
            inDbProvider = inDbProvider,
            trainingService = trainingService,
            trainingId = trainingId
        )
    }

    RenderMissingTrainingDialog(
        visible = loadState.trainingLoadFailed,
        onDismiss = {
            loadState = loadState.copy(trainingLoadFailed = false)
            onNavigate(ScreenType.Training)
        }
    )

    RenderEditTrainingSaveSuccessDialog(
        success = trainingSaveSuccess,
        onDismiss = {
            trainingSaveSuccess = null
            onNavigate(ScreenType.Home)
        }
    )

    val visibleGamesForTraining = if (hideLinesWithWeightZero) {
        loadState.gamesForTraining.filter { it.weight > 0 }
    } else {
        loadState.gamesForTraining
    }

    EditTrainingScreen(
        initialTrainingName = loadState.trainingName,
        gamesForTraining = visibleGamesForTraining,
        orderGamesInTraining = orderGamesInTraining,
        onBackClick = onBackClick,
        onNavigate = onNavigate,
        onStartGameTrainingClick = onStartGameTrainingClick,
        onOpenGameEditorClick = createOpenEditTrainingGameEditorAction(
            allGamesById = loadState.allGamesById,
            onOpenGameEditorClick = onOpenGameEditorClick
        ),
        onSaveTraining = { trainingName, editableGames, showSuccessMessage, onSaved ->
            scope.launch {
                val saveSuccess = saveEditedTraining(
                    trainingService = trainingService,
                    trainingId = trainingId,
                    trainingName = trainingName,
                    editableGames = editableGames
                ) ?: return@launch

                onSaved?.invoke()
                if (!showSuccessMessage) {
                    return@launch
                }

                trainingSaveSuccess = saveSuccess
            }
        },
        modifier = modifier
    )
}

private val EditTrainingScreenStrings = TrainingCollectionEditorStrings(
    screenTitle = "Edit Training",
    collectionNameLabel = "Training Name",
    collectionNamePlaceholder = DEFAULT_TRAINING_NAME,
    gamesCountLabel = "Games in training",
)


@Composable
fun EditTrainingScreen(
    initialTrainingName: String = DEFAULT_TRAINING_NAME,
    gamesForTraining: List<TrainingGameEditorItem> = emptyList(),
    orderGamesInTraining: RuntimeContext.OrderGamesInTraining,
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onStartGameTrainingClick: (Long, Int, Int) -> Unit = { _, _, _ -> },
    onOpenGameEditorClick: (Long) -> Unit = {},
    onSaveTraining: (String, List<TrainingGameEditorItem>, Boolean, (() -> Unit)?) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedNavItem by remember { mutableStateOf<ScreenType>(ScreenType.Home) }
    var moveRange by remember { mutableStateOf(TrainingMoveRange()) }
    var hasUserSelectedGame by remember { mutableStateOf(false) }
    var editorState by remember(initialTrainingName, gamesForTraining) {
        mutableStateOf(
            CreateTrainingEditorState(
                trainingName = initialTrainingName,
                editableGamesForTraining = gamesForTraining
            )
        )
    }
    var savedTrainingName by remember(initialTrainingName) { mutableStateOf(initialTrainingName) }
    var savedGamesForTraining by remember(gamesForTraining) { mutableStateOf(gamesForTraining) }
    var pendingLeaveAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val orderedGameIds = remember(gamesForTraining) {
        orderGamesInTraining.orderGames(
            games = gamesForTraining,
            getGameId = { game -> game.gameId },
            getWeight = { game -> game.weight }
        ).map { it.gameId }
    }
    val currentGamesById = editorState.editableGamesForTraining.associateBy { it.gameId }
    val orderedGamesForTraining = orderedGameIds.mapNotNull { currentGamesById[it] }
    val boardSession = rememberTrainingEditorBoardSession(orderedGamesForTraining)

    fun hasUnsavedChanges(): Boolean {
        return hasUnsavedTrainingEditorChanges(
            editorState = editorState,
            initialTrainingName = savedTrainingName,
            initialGamesForTraining = savedGamesForTraining
        )
    }

    fun updateSavedState() {
        savedTrainingName = normalizeTrainingEditorName(editorState.trainingName)
        savedGamesForTraining = editorState.editableGamesForTraining
    }

    fun saveTraining(
        showSuccessMessage: Boolean = false,
        afterSave: (() -> Unit)? = null
    ) {
        onSaveTraining(
            editorState.trainingName,
            editorState.editableGamesForTraining,
            showSuccessMessage
        ) {
            updateSavedState()
            afterSave?.invoke()
        }
    }

    fun requestLeave(action: () -> Unit) {
        if (!hasUnsavedChanges()) {
            action()
            return
        }

        pendingLeaveAction = action
    }

    LaunchedEffect(initialTrainingName, gamesForTraining) {
        editorState = editorState.copy(
            trainingName = initialTrainingName,
            editableGamesForTraining = gamesForTraining
        )
        savedTrainingName = initialTrainingName
        savedGamesForTraining = gamesForTraining
        pendingLeaveAction = null
    }

    RenderUnsavedTrainingChangesDialog(
        pendingLeaveAction = pendingLeaveAction,
        onDismiss = { pendingLeaveAction = null },
        onSaveClick = {
            val leaveAction = pendingLeaveAction ?: return@RenderUnsavedTrainingChangesDialog
            saveTraining {
                pendingLeaveAction = null
                leaveAction()
            }
        },
        onDiscardClick = {
            val leaveAction = pendingLeaveAction ?: return@RenderUnsavedTrainingChangesDialog
            pendingLeaveAction = null
            leaveAction()
        }
    )

    BackHandler {
        requestLeave(onBackClick)
    }

    var autoScrollToGameIndex : Int? = null
    if (hasUserSelectedGame) {
        autoScrollToGameIndex = orderedGamesForTraining.indexOfFirst { it.gameId == boardSession.selectedGameId }
            .takeIf { it >= 0 }
    }

    TrainingCollectionEditorScreen(
        strings = EditTrainingScreenStrings,
        collectionName = editorState.trainingName,
        onCollectionNameChange = { editorState = editorState.copy(trainingName = it) },
        games = orderedGamesForTraining,
        selectedNavItem = selectedNavItem,
        onBackClick = {
            requestLeave(onBackClick)
        },
        onSaveClick = {
            saveTraining(showSuccessMessage = true)
        },
        onNavigate = { screenType ->
            requestLeave {
                selectedNavItem = screenType
                onNavigate(screenType)
            }
        },
        modifier = modifier,
        autoScrollToGameIndex = autoScrollToGameIndex,
        headerContent = {
            EditTrainingMoveRangeSection(
                moveRange = moveRange,
                onMoveRangeChange = { moveRange = it }
            )
        },
        topBarActions = {
            RenderEditTrainingRandomAction(
                games = editorState.editableGamesForTraining,
                moveRange = moveRange,
                requestLeave = ::requestLeave,
                onStartGameTrainingClick = onStartGameTrainingClick
            )
            Spacer(modifier = Modifier.width(AppDimens.spaceSm))
        }
    ) { game ->
        val parsedGame = boardSession.parsedGamesById[game.gameId]
        val isSelected = boardSession.selectedGameId == game.gameId

        TrainingEditorGameSection(
            state = TrainingEditorGameSectionState(
                game = game,
                parsedGame = parsedGame,
                isSelected = isSelected,
                gameController = boardSession.gameController,
                currentPly = if (isSelected) boardSession.gameController.currentMoveIndex else 0,
            ),
            actions = TrainingEditorGameSectionActions(
                onDecreaseWeightClick = {
                    editorState = editorState.copy(
                        editableGamesForTraining = decreaseTrainingGameWeight(
                            games = editorState.editableGamesForTraining,
                            gameId = game.gameId
                        )
                    )
                },
                onIncreaseWeightClick = {
                    editorState = editorState.copy(
                        editableGamesForTraining = increaseTrainingGameWeight(
                            games = editorState.editableGamesForTraining,
                            gameId = game.gameId
                        )
                    )
                },
                onSelect = {
                    hasUserSelectedGame = true
                    boardSession.onSelectGame(game.gameId)
                },
                onPrevClick = { boardSession.gameController.undoMove() },
                onNextClick = { boardSession.gameController.redoMove() },
                onResetClick = { boardSession.onResetSelectedGame(game.gameId) },
                onEditGameClick = {
                    requestLeave {
                        onOpenGameEditorClick(game.gameId)
                    }
                },
                onMovePlyClick = { ply -> boardSession.onMoveToPly(game.gameId, ply) },
            ),
            primaryAction = createEditTrainingPrimaryAction(
                gameId = game.gameId,
                moveRange = moveRange,
                requestLeave = ::requestLeave,
                onStartGameTrainingClick = onStartGameTrainingClick
            )
        )
    }
}
