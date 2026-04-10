package com.example.chessboard.ui.screen.training

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chessboard.RuntimeContext
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.service.OneGameTrainingData
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTextField
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingTextPrimary
import com.example.chessboard.service.buildMoveLabels
import com.example.chessboard.service.parsePgnMoves
import com.example.chessboard.ui.components.BodySecondaryText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class EditTrainingLoadState(
    val trainingName: String = DEFAULT_TRAINING_NAME,
    val gamesForTraining: List<TrainingGameEditorItem> = emptyList(),
    val allGamesById: Map<Long, GameEntity> = emptyMap(),
    val trainingLoadFailed: Boolean = false
)

private data class EditTrainingSaveSuccess(
    val trainingId: Long,
    val trainingName: String,
    val gamesCount: Int
)

private data class ParsedTrainingGameEditorItem(
    val game: TrainingGameEditorItem,
    val uciMoves: List<String>,
    val moveLabels: List<String>
)

private data class EditTrainingBoardSession(
    val gameController: GameController,
    val parsedGamesById: Map<Long, ParsedTrainingGameEditorItem>,
    val selectedGameId: Long?,
    val onSelectGame: (Long) -> Unit,
    val onMoveToPly: (Long, Int) -> Unit,
    val onResetSelectedGame: (Long) -> Unit,
)

private fun normalizeTrainingName(trainingName: String): String {
    if (trainingName.isBlank()) {
        return DEFAULT_TRAINING_NAME
    }

    return trainingName
}

private fun hasUnsavedTrainingChanges(
    editorState: CreateTrainingEditorState,
    initialTrainingName: String,
    initialGamesForTraining: List<TrainingGameEditorItem>
): Boolean {
    if (normalizeTrainingName(editorState.trainingName) != normalizeTrainingName(initialTrainingName)) {
        return true
    }

    return editorState.editableGamesForTraining != initialGamesForTraining
}

private fun buildTrainingEditorItems(
    allGames: List<GameEntity>,
    trainingGames: List<OneGameTrainingData>
): List<TrainingGameEditorItem> {
    if (trainingGames.isEmpty()) {
        return emptyList()
    }

    val weightsByGameId = trainingGames.associate { trainingGame ->
        trainingGame.gameId to trainingGame.weight
    }

    return allGames.mapNotNull { game ->
        val weight = weightsByGameId[game.id] ?: return@mapNotNull null
        game.toTrainingGameEditorItem(weight = weight)
    }
}

private fun resolveRandomTrainingGameId(
    games: List<TrainingGameEditorItem>
): Long? {
    if (games.isEmpty()) {
        return null
    }

    return games.random().gameId
}

private suspend fun loadEditTrainingState(
    inDbProvider: com.example.chessboard.repository.DatabaseProvider,
    trainingId: Long
): EditTrainingLoadState {
    val allGames = withContext(Dispatchers.IO) {
        inDbProvider.getAllGames()
    }

    val training = withContext(Dispatchers.IO) {
        inDbProvider.getTrainingById(trainingId)
    } ?: return EditTrainingLoadState(
        trainingName = DEFAULT_TRAINING_NAME,
        gamesForTraining = emptyList(),
        trainingLoadFailed = true
    )

    return EditTrainingLoadState(
        trainingName = training.name.ifBlank { DEFAULT_TRAINING_NAME },
        gamesForTraining = buildTrainingEditorItems(
            allGames = allGames,
            trainingGames = OneGameTrainingData.fromJson(training.gamesJson)
        ),
        allGamesById = allGames.associateBy { game -> game.id }
    )
}

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
    success: EditTrainingSaveSuccess?,
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

private suspend fun saveEditedTraining(
    inDbProvider: com.example.chessboard.repository.DatabaseProvider,
    trainingId: Long,
    trainingName: String,
    editableGames: List<TrainingGameEditorItem>
): EditTrainingSaveSuccess? {
    val normalizedName = normalizeTrainingName(trainingName)
    val trainingGames = editableGames.map { game ->
        OneGameTrainingData(
            gameId = game.gameId,
            weight = game.weight
        )
    }

    val wasUpdated = withContext(Dispatchers.IO) {
        inDbProvider.updateTrainingFromGames(
            trainingId = trainingId,
            name = normalizedName,
            games = trainingGames
        )
    }

    if (!wasUpdated) {
        return null
    }

    return EditTrainingSaveSuccess(
        trainingId = trainingId,
        trainingName = normalizedName,
        gamesCount = editableGames.size
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
private fun rememberEditTrainingBoardSession(
    games: List<TrainingGameEditorItem>
): EditTrainingBoardSession {
    val gameController = remember { GameController() }
    val gameIds = remember(games) { games.map { it.gameId } }
    val parsedGamesById = remember(gameIds) {
        games.associate { game ->
            val uciMoves = parsePgnMoves(game.pgn)
            val moveLabels = buildMoveLabels(uciMoves)
            game.gameId to ParsedTrainingGameEditorItem(
                game = game,
                uciMoves = uciMoves,
                moveLabels = moveLabels
            )
        }
    }
    var selectedGameId by remember(gameIds) { mutableStateOf(games.firstOrNull()?.gameId) }

    SideEffect {
        gameController.setUserMovesEnabled(false)
    }

    LaunchedEffect(gameIds, parsedGamesById) {
        if (selectedGameId !in parsedGamesById.keys) {
            selectedGameId = games.firstOrNull()?.gameId
        }
    }

    LaunchedEffect(selectedGameId, parsedGamesById) {
        val selectedGame = selectedGameId?.let { parsedGamesById[it] } ?: return@LaunchedEffect
        gameController.loadFromUciMoves(selectedGame.uciMoves, targetPly = 0)
    }

    fun selectGame(gameId: Long) {
        selectedGameId = gameId
        val parsedGame = parsedGamesById[gameId] ?: return
        gameController.loadFromUciMoves(parsedGame.uciMoves, targetPly = 0)
    }

    fun moveToPly(gameId: Long, ply: Int) {
        selectedGameId = gameId
        val parsedGame = parsedGamesById[gameId] ?: return
        gameController.loadFromUciMoves(parsedGame.uciMoves, targetPly = ply)
    }

    fun resetSelectedGame(gameId: Long) {
        val parsedGame = parsedGamesById[gameId] ?: return
        gameController.loadFromUciMoves(parsedGame.uciMoves, targetPly = 0)
    }

    return EditTrainingBoardSession(
        gameController = gameController,
        parsedGamesById = parsedGamesById,
        selectedGameId = selectedGameId,
        onSelectGame = ::selectGame,
        onMoveToPly = ::moveToPly,
        onResetSelectedGame = ::resetSelectedGame
    )
}

@Composable
fun EditTrainingScreenContainer(
    trainingId: Long,
    screenContext: ScreenContainerContext,
    orderGamesInTraining: RuntimeContext.OrderGamesInTraining,
    onStartGameTrainingClick: (Long) -> Unit = {},
    onOpenGameEditorClick: (GameEntity) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val onBackClick = screenContext.onBackClick
    val onNavigate = screenContext.onNavigate
    val inDbProvider = screenContext.inDbProvider
    var loadState by remember { mutableStateOf(EditTrainingLoadState()) }
    var trainingSaveSuccess by remember { mutableStateOf<EditTrainingSaveSuccess?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(trainingId) {
        loadState = loadEditTrainingState(
            inDbProvider = inDbProvider,
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

    EditTrainingScreen(
        trainingId = trainingId,
        initialTrainingName = loadState.trainingName,
        gamesForTraining = loadState.gamesForTraining,
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
                    inDbProvider = inDbProvider,
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

@Composable
fun EditTrainingScreen(
    trainingId: Long,
    initialTrainingName: String = DEFAULT_TRAINING_NAME,
    gamesForTraining: List<TrainingGameEditorItem> = emptyList(),
    orderGamesInTraining: RuntimeContext.OrderGamesInTraining,
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onStartGameTrainingClick: (Long) -> Unit = {},
    onOpenGameEditorClick: (Long) -> Unit = {},
    onSaveTraining: (String, List<TrainingGameEditorItem>, Boolean, (() -> Unit)?) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedNavItem by remember { mutableStateOf<ScreenType>(ScreenType.Home) }
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
    val boardSession = rememberEditTrainingBoardSession(orderedGamesForTraining)

    fun hasUnsavedChanges(): Boolean {
        return hasUnsavedTrainingChanges(
            editorState = editorState,
            initialTrainingName = savedTrainingName,
            initialGamesForTraining = savedGamesForTraining
        )
    }

    fun updateSavedState() {
        savedTrainingName = normalizeTrainingName(editorState.trainingName)
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

    pendingLeaveAction?.let { leaveAction ->
        AppMessageDialog(
            title = "Unsaved Changes",
            message = "Save training changes before leaving this screen?",
            onDismiss = { pendingLeaveAction = null },
            confirmText = "Save",
            onConfirm = {
                saveTraining {
                    pendingLeaveAction = null
                    leaveAction()
                }
            },
            dismissText = "Discard",
            onDismissClick = {
                pendingLeaveAction = null
                leaveAction()
            }
        )
    }

    BackHandler {
        requestLeave(onBackClick)
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Edit Training",
                onBackClick = {
                    requestLeave(onBackClick)
                },
                actions = {
                    PrimaryButton(
                        text = "Random",
                        onClick = {
                            val randomGameId = resolveRandomTrainingGameId(editorState.editableGamesForTraining)
                            if (randomGameId == null) {
                                return@PrimaryButton
                            }

                            requestLeave {
                                onStartGameTrainingClick(randomGameId)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(AppDimens.spaceSm))
                    IconButton(
                        onClick = { saveTraining(showSuccessMessage = true) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save",
                            tint = TrainingAccentTeal
                        )
                    }
                }
            )
        },
        bottomBar = {
            AppBottomNavigation(
                items = defaultAppBottomNavigationItems(),
                selectedItem = selectedNavItem,
                onItemSelected = {
                    requestLeave {
                        selectedNavItem = it
                        onNavigate(it)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = AppDimens.spaceLg,
                end = AppDimens.spaceLg,
                top = AppDimens.spaceLg,
                bottom = AppDimens.spaceLg
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            item {
                AppTextField(
                    value = editorState.trainingName,
                    onValueChange = { editorState = editorState.copy(trainingName = it) },
                    label = "Training Name",
                    placeholder = DEFAULT_TRAINING_NAME
                )
            }

            item {
                BodySecondaryText(text = "Games in training: ${editorState.editableGamesForTraining.size}")
            }

            items(
                items = orderedGamesForTraining,
                key = { game -> game.gameId }
            ) { game ->
                val parsedGame = boardSession.parsedGamesById[game.gameId]
                val isSelected = boardSession.selectedGameId == game.gameId

                if (isSelected && parsedGame != null) {
                    ChessBoardSection(gameController = boardSession.gameController)
                    Spacer(modifier = Modifier.height(AppDimens.spaceLg))
                }

                GameTrainingBlock(
                    game = game,
                    parsedGame = parsedGame,
                    isSelected = isSelected,
                    currentPly = if (isSelected) boardSession.gameController.currentMoveIndex else 0,
                    canUndo = isSelected && boardSession.gameController.canUndo,
                    canRedo = isSelected && boardSession.gameController.canRedo,
                    onSelect = { boardSession.onSelectGame(game.gameId) },
                    onMovePlyClick = { ply -> boardSession.onMoveToPly(game.gameId, ply) },
                    onPrevClick = { boardSession.gameController.undoMove() },
                    onNextClick = { boardSession.gameController.redoMove() },
                    onResetClick = { boardSession.onResetSelectedGame(game.gameId) },
                    onEditGameClick = {
                        requestLeave {
                            onOpenGameEditorClick(game.gameId)
                        }
                    },
                    onDecreaseWeightClick = {
                        editorState = decreaseTrainingGameWeight(editorState, game.gameId)
                    },
                    onIncreaseWeightClick = {
                        editorState = increaseTrainingGameWeight(editorState, game.gameId)
                    },
                    onStartTrainingClick = {
                        requestLeave {
                            onStartGameTrainingClick(game.gameId)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun GameTrainingBlockHeader(
    game: TrainingGameEditorItem,
    onEditGameClick: () -> Unit,
    onDecreaseWeightClick: () -> Unit,
    onIncreaseWeightClick: () -> Unit,
    onStartTrainingClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SectionTitleText(text = game.title)
            if (!game.eco.isNullOrBlank()) {
                CardMetaText(text = game.eco)
            }
            CardMetaText(text = "Weight: ${game.weight}")
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onEditGameClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit game",
                    tint = TrainingTextPrimary
                )
            }
            IconButton(onClick = onDecreaseWeightClick) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease weight",
                    tint = TrainingAccentTeal
                )
            }
            IconButton(onClick = onIncreaseWeightClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase weight",
                    tint = TrainingAccentTeal
                )
            }
            IconButton(onClick = onStartTrainingClick) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start training",
                    tint = TrainingTextPrimary
                )
            }
        }
    }
}

@Composable
private fun GameTrainingBlock(
    game: TrainingGameEditorItem,
    parsedGame: ParsedTrainingGameEditorItem?,
    isSelected: Boolean,
    currentPly: Int,
    canUndo: Boolean,
    canRedo: Boolean,
    onSelect: () -> Unit,
    onMovePlyClick: (Int) -> Unit,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onResetClick: () -> Unit,
    onEditGameClick: () -> Unit,
    onDecreaseWeightClick: () -> Unit,
    onIncreaseWeightClick: () -> Unit,
    onStartTrainingClick: () -> Unit,
) {
    CardSurface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isSelected) Background.CardDark else Background.SurfaceDark,
        border = if (isSelected) BorderStroke(1.dp, TrainingAccentTeal) else null,
        contentPadding = PaddingValues(AppDimens.spaceMd),
        onClick = if (isSelected) null else onSelect
    ) {
        GameTrainingBlockHeader(
            game = game,
            onEditGameClick = onEditGameClick,
            onDecreaseWeightClick = onDecreaseWeightClick,
            onIncreaseWeightClick = onIncreaseWeightClick,
            onStartTrainingClick = onStartTrainingClick
        )

        Spacer(modifier = Modifier.height(AppDimens.spaceMd))

        MoveLegendSection(
            moveLabels = parsedGame?.moveLabels ?: emptyList(),
            currentPly = currentPly,
            isSelectionEnabled = true,
            showNavControls = isSelected,
            canUndo = isSelected && canUndo,
            canRedo = isSelected && canRedo,
            onMovePlyClick = { ply ->
                onSelect()
                onMovePlyClick(ply)
            },
            onPrevMoveClick = onPrevClick,
            onNextMoveClick = onNextClick,
            onResetMovesClick = onResetClick,
            title = "Move Sequence",
            emptyText = "No moves."
        )
    }
}
