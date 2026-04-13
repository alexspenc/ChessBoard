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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingIconInactive
import androidx.compose.ui.text.style.TextAlign
import com.example.chessboard.service.buildMoveLabels
import com.example.chessboard.service.parsePgnMoves
import com.example.chessboard.ui.EditTrainingListTestTag
import com.example.chessboard.ui.EditTrainingMoveLegendSectionTestTag
import com.example.chessboard.ui.MoveLegendNextTestTag
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
    trainingService: com.example.chessboard.service.TrainingService,
    trainingId: Long
): EditTrainingLoadState {
    val allGames = withContext(Dispatchers.IO) {
        inDbProvider.getAllGames()
    }

    val training = withContext(Dispatchers.IO) {
        trainingService.getTrainingById(trainingId)
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
    trainingService: com.example.chessboard.service.TrainingService,
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
        trainingService.updateTrainingFromGames(
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
    hideLinesWithWeightZero: Boolean = false,
    onStartGameTrainingClick: (Long, Int) -> Unit = { _, _ -> },
    onOpenGameEditorClick: (GameEntity) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val onBackClick = screenContext.onBackClick
    val onNavigate = screenContext.onNavigate
    val inDbProvider = screenContext.inDbProvider
    val trainingService = remember(inDbProvider) { inDbProvider.createTrainingService() }
    var loadState by remember { mutableStateOf(EditTrainingLoadState()) }
    var trainingSaveSuccess by remember { mutableStateOf<EditTrainingSaveSuccess?>(null) }
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

@Composable
fun EditTrainingScreen(
    initialTrainingName: String = DEFAULT_TRAINING_NAME,
    gamesForTraining: List<TrainingGameEditorItem> = emptyList(),
    orderGamesInTraining: RuntimeContext.OrderGamesInTraining,
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onStartGameTrainingClick: (Long, Int) -> Unit = { _, _ -> },
    onOpenGameEditorClick: (Long) -> Unit = {},
    onSaveTraining: (String, List<TrainingGameEditorItem>, Boolean, (() -> Unit)?) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedNavItem by remember { mutableStateOf<ScreenType>(ScreenType.Home) }
    var movesDepth by remember { mutableIntStateOf(0) }
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
                                onStartGameTrainingClick(randomGameId, movesDepth)
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
        val listState = rememberLazyListState()
        var hasUserSelectedGame by remember { mutableStateOf(false) }

        LaunchedEffect(boardSession.selectedGameId) {
            if (!hasUserSelectedGame) return@LaunchedEffect
            val selectedIndex = orderedGamesForTraining
                .indexOfFirst { it.gameId == boardSession.selectedGameId }
            if (selectedIndex >= 0) {
                // +3 for the three header items (training name, depth control, games count)
                listState.animateScrollToItem(selectedIndex + 3)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag(EditTrainingListTestTag),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
                ) {
                    BodySecondaryText(text = "Move depth:")
                    IconButton(
                        onClick = { if (movesDepth > 0) movesDepth-- },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease depth",
                            tint = TrainingAccentTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = if (movesDepth == 0) "All" else "$movesDepth",
                        color = TextColor.Primary,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.widthIn(min = 32.dp),
                        textAlign = TextAlign.Center
                    )
                    IconButton(
                        onClick = { movesDepth++ },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase depth",
                            tint = TrainingAccentTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
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

                // Game title header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = game.title,
                            color = TextColor.Primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (!game.eco.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = TrainingAccentTeal.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = game.eco,
                                    color = TrainingAccentTeal,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = AppDimens.spaceSm, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)
                    ) {
                        IconButton(
                            onClick = { editorState = decreaseTrainingGameWeight(editorState, game.gameId) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease",
                                tint = TrainingAccentTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${game.weight}",
                                color = TextColor.Primary,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "reps",
                                color = TextColor.Secondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        IconButton(
                            onClick = { editorState = increaseTrainingGameWeight(editorState, game.gameId) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase",
                                tint = TrainingAccentTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimens.spaceSm))

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
                    onSelect = { hasUserSelectedGame = true; boardSession.onSelectGame(game.gameId) },
                    onPrevClick = { boardSession.gameController.undoMove() },
                    onNextClick = { boardSession.gameController.redoMove() },
                    onResetClick = { boardSession.onResetSelectedGame(game.gameId) },
                    onEditGameClick = {
                        requestLeave {
                            onOpenGameEditorClick(game.gameId)
                        }
                    },
                    onMovePlyClick = { ply -> boardSession.onMoveToPly(game.gameId, ply) },
                    onStartTrainingClick = {
                        requestLeave {
                            onStartGameTrainingClick(game.gameId, movesDepth)
                        }
                    }
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
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onResetClick: () -> Unit,
    onEditGameClick: () -> Unit,
    onMovePlyClick: (Int) -> Unit,
    onStartTrainingClick: () -> Unit,
) {
    val moveLabels = parsedGame?.moveLabels ?: emptyList()
    val currentMoveLabel = if (currentPly > 0 && currentPly <= moveLabels.size) {
        moveLabels[currentPly - 1]
    } else {
        "Start"
    }

    CardSurface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isSelected) Background.CardDark else Background.SurfaceDark,
        border = if (isSelected) BorderStroke(1.dp, TrainingAccentTeal) else null,
        contentPadding = PaddingValues(AppDimens.spaceMd),
        onClick = if (isSelected) null else onSelect
    ) {
        // Control bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Edit pill
            Surface(
                onClick = onEditGameClick,
                shape = RoundedCornerShape(50),
                color = Background.ScreenDark
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = AppDimens.spaceMd, vertical = AppDimens.spaceSm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = TextColor.Primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Edit",
                        color = TextColor.Primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Nav group pill
            Surface(
                shape = RoundedCornerShape(50),
                color = Background.ScreenDark
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onResetClick,
                        enabled = canUndo,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = if (canUndo) TextColor.Primary else TrainingIconInactive,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                    IconButton(
                        onClick = onPrevClick,
                        enabled = canUndo,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous move",
                            tint = if (canUndo) TextColor.Primary else TrainingIconInactive,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Text(
                        text = currentMoveLabel,
                        color = TextColor.Primary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.widthIn(min = 66.dp),
                        textAlign = TextAlign.Center
                    )
                    IconButton(
                        onClick = onNextClick,
                        enabled = canRedo,
                        modifier = Modifier
                            .size(54.dp)
                            .testTag(MoveLegendNextTestTag)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next move",
                            tint = if (canRedo) TextColor.Primary else TrainingIconInactive,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Run button
            IconButton(
                onClick = onStartTrainingClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Start training",
                    tint = TrainingAccentTeal,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        if (parsedGame != null) {
            Spacer(modifier = Modifier.height(AppDimens.spaceMd))
            HorizontalDivider(color = Background.ScreenDark, thickness = 1.dp)
            Spacer(modifier = Modifier.height(AppDimens.spaceMd))
            MoveLegendSection(
                moveLabels = parsedGame.moveLabels,
                currentPly = currentPly,
                isSelectionEnabled = true,
                showNavControls = false,
                canUndo = false,
                canRedo = false,
                onMovePlyClick = { ply ->
                    onSelect()
                    onMovePlyClick(ply)
                },
                onPrevMoveClick = {},
                onNextMoveClick = {},
                onResetMovesClick = {},
                modifier = Modifier.testTag(EditTrainingMoveLegendSectionTestTag),
                title = "Move Sequence",
                emptyText = "No moves."
            )
        }
    }
}
