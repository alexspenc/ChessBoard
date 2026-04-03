package com.example.chessboard.ui.screen.training

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.SecondaryButton
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingIconInactive
import com.example.chessboard.ui.theme.TrainingTextPrimary
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

@Composable
fun EditTrainingScreenContainer(
    trainingId: Long,
    screenContext: ScreenContainerContext,
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
        loadState = loadState.copy(trainingLoadFailed = false)

        val allGames = withContext(Dispatchers.IO) {
            inDbProvider.getAllGames()
        }

        val training = withContext(Dispatchers.IO) {
            inDbProvider.getTrainingById(trainingId)
        }

        if (training == null) {
            loadState = EditTrainingLoadState(
                trainingName = DEFAULT_TRAINING_NAME,
                gamesForTraining = emptyList(),
                trainingLoadFailed = true
            )
            return@LaunchedEffect
        }

        loadState = EditTrainingLoadState(
            trainingName = training.name.ifBlank { DEFAULT_TRAINING_NAME },
            gamesForTraining = buildTrainingEditorItems(
                allGames = allGames,
                trainingGames = OneGameTrainingData.fromJson(training.gamesJson)
            ),
            allGamesById = allGames.associateBy { game -> game.id }
        )
    }

    if (loadState.trainingLoadFailed) {
        AppMessageDialog(
            title = "Training Not Found",
            message = "The selected training is unavailable.",
            onDismiss = {
                loadState = loadState.copy(trainingLoadFailed = false)
                onNavigate(ScreenType.Training)
            }
        )
    }

    trainingSaveSuccess?.let { success ->
        AppMessageDialog(
            title = "Training Updated",
            message = buildString {
                appendLine("ID: ${success.trainingId}")
                appendLine("Name: ${success.trainingName}")
                append("Games in training: ")
                append(success.gamesCount)
            },
            onDismiss = {
                trainingSaveSuccess = null
                onNavigate(ScreenType.Home)
            }
        )
    }

    EditTrainingScreen(
        trainingId = trainingId,
        initialTrainingName = loadState.trainingName,
        gamesForTraining = loadState.gamesForTraining,
        onBackClick = onBackClick,
        onNavigate = onNavigate,
        onStartGameTrainingClick = onStartGameTrainingClick,
        onOpenGameEditorClick = { gameId ->
            val game = loadState.allGamesById[gameId] ?: return@EditTrainingScreen
            onOpenGameEditorClick(game)
        },
        onSaveTraining = { trainingName, editableGames ->
            scope.launch {
                val normalizedName = trainingName.ifBlank { DEFAULT_TRAINING_NAME }
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
                    return@launch
                }

                trainingSaveSuccess = EditTrainingSaveSuccess(
                    trainingId = trainingId,
                    trainingName = normalizedName,
                    gamesCount = editableGames.size
                )
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
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onStartGameTrainingClick: (Long) -> Unit = {},
    onOpenGameEditorClick: (Long) -> Unit = {},
    onSaveTraining: (String, List<TrainingGameEditorItem>) -> Unit = { _, _ -> },
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

    LaunchedEffect(initialTrainingName, gamesForTraining) {
        editorState = editorState.copy(
            trainingName = initialTrainingName,
            editableGamesForTraining = gamesForTraining
        )
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Edit Training",
                onBackClick = onBackClick,
                actions = {
                    PrimaryButton(
                        text = "Random",
                        onClick = {
                            val randomGameId = resolveRandomTrainingGameId(editorState.editableGamesForTraining)
                            if (randomGameId != null) {
                                onStartGameTrainingClick(randomGameId)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(AppDimens.spaceSm))
                    PrimaryButton(
                        text = "Save",
                        onClick = { onSaveTraining(editorState.trainingName, editorState.editableGamesForTraining) }
                    )
                }
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
                items = editorState.editableGamesForTraining,
                key = { game -> game.gameId }
            ) { game ->
                GameTrainingBlock(
                    game = game,
                    onEditGameClick = { onOpenGameEditorClick(game.gameId) },
                    onDecreaseWeightClick = {
                        editorState = decreaseTrainingGameWeight(editorState, game.gameId)
                    },
                    onIncreaseWeightClick = {
                        editorState = increaseTrainingGameWeight(editorState, game.gameId)
                    },
                    onStartTrainingClick = { onStartGameTrainingClick(game.gameId) }
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
            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)
            ) {
                SecondaryButton(
                    text = "-",
                    onClick = onDecreaseWeightClick,
                )
                SecondaryButton(
                    text = "+",
                    onClick = onIncreaseWeightClick,
                )
            }
            PrimaryButton(
                text = "GO",
                onClick = onStartTrainingClick,
            )
        }
    }
}

@Composable
private fun GameTrainingBlock(
    game: TrainingGameEditorItem,
    onEditGameClick: () -> Unit,
    onDecreaseWeightClick: () -> Unit,
    onIncreaseWeightClick: () -> Unit,
    onStartTrainingClick: () -> Unit,
) {
    val gameController = remember(game.gameId) { GameController() }
    var moveLabels by remember(game.gameId) { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingBoard by remember(game.gameId) { mutableStateOf(true) }

    // Read boardState to trigger recomposition when the controller moves
    @Suppress("UNUSED_VARIABLE")
    val boardState = gameController.boardState

    LaunchedEffect(game.pgn) {
        isLoadingBoard = true
        val uciMoves = withContext(Dispatchers.Default) {
            parsePgnMoves(game.pgn)
        }
        val labels = withContext(Dispatchers.Default) {
            buildMoveLabels(uciMoves)
        }
        withContext(Dispatchers.Main) {
            moveLabels = labels
            gameController.loadFromUciMoves(uciMoves, targetPly = 0)
            isLoadingBoard = false
        }
    }

    CardSurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(AppDimens.spaceMd)
    ) {
        GameTrainingBlockHeader(
            game = game,
            onEditGameClick = onEditGameClick,
            onDecreaseWeightClick = onDecreaseWeightClick,
            onIncreaseWeightClick = onIncreaseWeightClick,
            onStartTrainingClick = onStartTrainingClick
        )

        Spacer(modifier = Modifier.height(AppDimens.spaceMd))

        // Board
        if (isLoadingBoard) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TrainingAccentTeal)
            }
        } else {
            ChessBoardSection(
                gameController = gameController,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(AppDimens.spaceMd))

        // Move sequence label row
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "\u25AB", color = TrainingAccentTeal)
            Spacer(modifier = Modifier.width(AppDimens.spaceXs))
            SectionTitleText(text = "Move Sequence", color = TextColor.Secondary)
        }

        Spacer(modifier = Modifier.height(AppDimens.spaceSm))

        // Move chip row
        if (moveLabels.isEmpty()) {
            BodySecondaryText(text = "No moves.")
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
            ) {
                moveLabels.forEachIndexed { index, label ->
                    val moveNumber = index / 2 + 1
                    val isBlackMove = index % 2 == 1
                    val chipLabel = if (isBlackMove) "$moveNumber... $label" else "$moveNumber. $label"
                    MoveChip(
                        label = chipLabel,
                        isSelected = (index + 1 == gameController.currentMoveIndex),
                        onClick = {
                            val uciMoves = parsePgnMoves(game.pgn)
                            gameController.loadFromUciMoves(uciMoves, targetPly = index + 1)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.spaceSm))

        // Navigation row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val atStart = gameController.currentMoveIndex == 0
            TextButton(
                onClick = {
                    val uciMoves = parsePgnMoves(game.pgn)
                    gameController.loadFromUciMoves(uciMoves, targetPly = 0)
                },
                enabled = !atStart
            ) {
                Text(
                    text = "Reset",
                    color = if (atStart) TrainingIconInactive else TrainingAccentTeal
                )
            }
            Row {
                IconButton(
                    onClick = { gameController.undoMove() },
                    enabled = gameController.canUndo
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous move",
                        tint = if (gameController.canUndo) TrainingTextPrimary else TrainingIconInactive,
                        modifier = Modifier.size(AppDimens.iconButtonSize)
                    )
                }
                IconButton(
                    onClick = { gameController.redoMove() },
                    enabled = gameController.canRedo
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next move",
                        tint = if (gameController.canRedo) TrainingTextPrimary else TrainingIconInactive,
                        modifier = Modifier.size(AppDimens.iconButtonSize)
                    )
                }
            }
        }
    }
}
