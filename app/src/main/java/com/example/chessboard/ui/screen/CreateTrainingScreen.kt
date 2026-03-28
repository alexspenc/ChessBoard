package com.example.chessboard.ui.screen

import android.app.Activity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.OneGameTrainingData
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
import com.example.chessboard.ui.theme.TrainingErrorRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DEFAULT_TRAINING_NAME = "FullTraining"
private val TrainingGameRowHeight = 92.dp
private val TrainingGamesHeaderHeight = 88.dp
private val TrainingGamesNavigationHeight = 64.dp
private val TrainingGameRowSpacing = AppDimens.spaceMd

data class TrainingGameEditorItem(
    val gameId: Long,
    val title: String,
    val weight: Int = 1
)

private data class CreateTrainingLoadState(
    val trainingName: String = DEFAULT_TRAINING_NAME,
    val gamesForTraining: List<TrainingGameEditorItem> = emptyList(),
    val trainingLoadFailed: Boolean = false
)

private data class CreateTrainingEditorState(
    val trainingName: String = DEFAULT_TRAINING_NAME,
    val currentPage: Int = 0,
    val editableGamesForTraining: List<TrainingGameEditorItem> = emptyList()
)

private data class TrainingSaveSuccess(
    val trainingId: Long,
    val trainingName: String,
    val gamesCount: Int,
    val wasUpdated: Boolean
)

private fun resolveCreateTrainingTitle(isEditMode: Boolean): String {
    if (isEditMode) {
        return "Edit Training"
    }

    return "Create Training"
}

private fun resolveRandomTrainingGameId(
    games: List<TrainingGameEditorItem>
): Long? {
    if (games.isEmpty()) {
        return null
    }

    return games.random().gameId
}

private fun decreaseTrainingGameWeight(
    editorState: CreateTrainingEditorState,
    gameId: Long
): CreateTrainingEditorState {
    if (editorState.editableGamesForTraining.none { it.gameId == gameId }) {
        return editorState
    }

    return editorState.copy(
        editableGamesForTraining = editorState.editableGamesForTraining.map { game ->
            if (game.gameId != gameId) {
                return@map game
            }

            return@map game.copy(weight = (game.weight - 1).coerceAtLeast(1))
        }
    )
}

private fun increaseTrainingGameWeight(
    editorState: CreateTrainingEditorState,
    gameId: Long
): CreateTrainingEditorState {
    if (editorState.editableGamesForTraining.none { it.gameId == gameId }) {
        return editorState
    }

    return editorState.copy(
        editableGamesForTraining = editorState.editableGamesForTraining.map { game ->
            if (game.gameId != gameId) {
                return@map game
            }

            return@map game.copy(weight = game.weight + 1)
        }
    )
}

private suspend fun saveTraining(
    dbProvider: DatabaseProvider,
    trainingId: Long?,
    normalizedName: String,
    trainingGames: List<OneGameTrainingData>
): Long? {
    if (trainingId == null) {
        return dbProvider.createTrainingFromGames(
            name = normalizedName,
            games = trainingGames
        )
    }

    val wasUpdated = dbProvider.updateTrainingFromGames(
        trainingId = trainingId,
        name = normalizedName,
        games = trainingGames
    )
    if (!wasUpdated) {
        return null
    }

    return trainingId
}


@Composable
fun CreateTrainingScreenContainer(
    activity: Activity,
    trainingId: Long? = null,
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onStartGameTrainingClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    inDbProvider: DatabaseProvider,
) {
    var loadState by remember { mutableStateOf(CreateTrainingLoadState()) }
    var trainingSaveSuccess by remember { mutableStateOf<TrainingSaveSuccess?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(trainingId) {
        loadState = loadState.copy(trainingLoadFailed = false)
        val allGames = withContext(Dispatchers.IO) {
            inDbProvider.getAllGames()
        }

        if (trainingId == null) {
            loadState = CreateTrainingLoadState(
                trainingName = DEFAULT_TRAINING_NAME,
                gamesForTraining = allGames.map { game ->
                    game.toTrainingGameEditorItem()
                }
            )
            return@LaunchedEffect
        }

        val training = withContext(Dispatchers.IO) {
            inDbProvider.getTrainingById(trainingId)
        }

        if (training == null) {
            loadState = CreateTrainingLoadState(
                trainingName = DEFAULT_TRAINING_NAME,
                gamesForTraining = emptyList(),
                trainingLoadFailed = true
            )
            return@LaunchedEffect
        }

        loadState = CreateTrainingLoadState(
            trainingName = training.name.ifBlank { DEFAULT_TRAINING_NAME },
            gamesForTraining = buildTrainingEditorItems(
                allGames = allGames,
                trainingGames = OneGameTrainingData.fromJson(training.gamesJson)
            )
        )
    }

    if (loadState.trainingLoadFailed) {
        MissingTrainingDialog(
            onDismiss = {
                loadState = loadState.copy(trainingLoadFailed = false)
                onNavigate(ScreenType.Training)
            }
        )
    }

    trainingSaveSuccess?.let { success ->
        TrainingSaveSuccessDialog(
            success = success,
            onDismiss = {
                trainingSaveSuccess = null
                onNavigate(ScreenType.Home)
            }
        )
    }

    CreateTrainingScreen(
        trainingId = trainingId,
        initialTrainingName = loadState.trainingName,
        gamesForTraining = loadState.gamesForTraining,
        onBackClick = onBackClick,
        onNavigate = onNavigate,
        onStartGameTrainingClick = onStartGameTrainingClick,
        onSaveTraining = { trainingName, editableGames ->
            scope.launch {
                val normalizedName = trainingName.ifBlank { DEFAULT_TRAINING_NAME }
                val trainingGames = editableGames.map { game ->
                    OneGameTrainingData(
                        gameId = game.gameId,
                        weight = game.weight
                    )
                }

                val savedTrainingId = withContext(Dispatchers.IO) {
                    saveTraining(
                        dbProvider = inDbProvider,
                        trainingId = trainingId,
                        normalizedName = normalizedName,
                        trainingGames = trainingGames
                    )
                }

                trainingSaveSuccess = TrainingSaveSuccess(
                    trainingId = savedTrainingId ?: return@launch,
                    trainingName = normalizedName,
                    gamesCount = editableGames.size,
                    wasUpdated = trainingId != null
                )
            }
        },
        modifier = modifier
    )
}

@Composable
fun CreateTrainingScreen(
    trainingId: Long? = null,
    initialTrainingName: String = DEFAULT_TRAINING_NAME,
    gamesForTraining: List<TrainingGameEditorItem> = emptyList(),
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onStartGameTrainingClick: (Long) -> Unit = {},
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
    val isEditMode = trainingId != null

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
                title = resolveCreateTrainingTitle(isEditMode),
                onBackClick = onBackClick,
                actions = {
                    if (isEditMode) {
                        PrimaryButton(
                            text = "Random",
                            onClick = {
                                val randomGameId = resolveRandomTrainingGameId(editorState.editableGamesForTraining)
                                if (randomGameId != null) {
                                    onStartGameTrainingClick(randomGameId)
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(AppDimens.spaceSm))
                    PrimaryButton(
                        text = "Save",
                        onClick = { onSaveTraining(editorState.trainingName, editorState.editableGamesForTraining) }
                    )
                }
            )
        },
        bottomBar = {
            CreateTrainingBottomNavigation(
                selectedItem = selectedNavItem,
                onItemSelected = {
                    selectedNavItem = it
                    onNavigate(it)
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            ScreenSection {
                AppTextField(
                    value = editorState.trainingName,
                    onValueChange = { editorState = editorState.copy(trainingName = it) },
                    label = "Training Name",
                    placeholder = DEFAULT_TRAINING_NAME
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceLg))

            ScreenSection {
                BodySecondaryText(text = "Games loaded for training: ${editorState.editableGamesForTraining.size}")
            }

            TrainingGamesEditorSection(
                editorState = editorState,
                isEditMode = isEditMode,
                onEditorStateChange = { editorState = it },
                onStartTrainingClick = onStartGameTrainingClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TrainingGamesEditorSection(
    editorState: CreateTrainingEditorState,
    isEditMode: Boolean,
    onEditorStateChange: (CreateTrainingEditorState) -> Unit,
    onStartTrainingClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    data class TrainingGamesPageState(
        val currentPageItems: List<TrainingGameEditorItem>,
        val currentPage: Int,
        val totalPages: Int,
        val canGoPrevious: Boolean,
        val canGoNext: Boolean,
    )

    fun resolveTrainingGamesPageState(maxHeight : Dp): TrainingGamesPageState {
        val availableHeightForRows =
            (maxHeight - TrainingGamesHeaderHeight - TrainingGamesNavigationHeight)
                .coerceAtLeast(TrainingGameRowHeight)
        val trainingGameSlotHeight = TrainingGameRowHeight + TrainingGameRowSpacing
        val pageSize = (availableHeightForRows / trainingGameSlotHeight)
            .toInt()
            .coerceAtLeast(1)
        val totalPages = ((editorState.editableGamesForTraining.size + pageSize - 1) / pageSize)
            .coerceAtLeast(1)
        val safeCurrentPage = editorState.currentPage.coerceIn(0, totalPages - 1)

        return TrainingGamesPageState(
            currentPageItems = editorState.editableGamesForTraining
                .drop(safeCurrentPage * pageSize)
                .take(pageSize),
            currentPage = safeCurrentPage,
            totalPages = totalPages,
            canGoPrevious = safeCurrentPage > 0,
            canGoNext = safeCurrentPage + 1 < totalPages,
        )
    }

    BoxWithConstraints(modifier = modifier) {
        val pageState = resolveTrainingGamesPageState(maxHeight)
        ScreenSection(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppDimens.spaceLg)
        ) {
            TrainingGamesPage(
                games = pageState.currentPageItems,
                currentPage = pageState.currentPage,
                totalPages = pageState.totalPages,
                canGoPrevious = pageState.canGoPrevious,
                canGoNext = pageState.canGoNext,
                showStartButton = isEditMode,
                onDecreaseWeightClick = { gameId ->
                    onEditorStateChange(decreaseTrainingGameWeight(editorState, gameId))
                },
                onIncreaseWeightClick = { gameId ->
                    onEditorStateChange(increaseTrainingGameWeight(editorState, gameId))
                },
                onRemoveGameClick = { gameId ->
                    onEditorStateChange(
                        editorState.copy(
                            editableGamesForTraining = editorState.editableGamesForTraining.filterNot { game ->
                                game.gameId == gameId
                            }
                        )
                    )
                },
                onStartTrainingClick = onStartTrainingClick,
                onPreviousPageClick = {
                    if (!pageState.canGoPrevious) {
                        return@TrainingGamesPage
                    }

                    onEditorStateChange(editorState.copy(currentPage = pageState.currentPage - 1))
                },
                onNextPageClick = {
                    if (!pageState.canGoNext) {
                        return@TrainingGamesPage
                    }

                    onEditorStateChange(editorState.copy(currentPage = pageState.currentPage + 1))
                }
            )
        }
    }
}
private fun GameEntity.toTrainingGameEditorItem(weight: Int = 1): TrainingGameEditorItem {
    return TrainingGameEditorItem(
        gameId = id,
        title = event ?: "Unnamed Opening",
        weight = weight
    )
}

private fun buildTrainingEditorItems(
    allGames: List<GameEntity>,
    trainingGames: List<OneGameTrainingData>
): List<TrainingGameEditorItem> {
    if (trainingGames.isEmpty()) {
        return allGames.map { game ->
            game.toTrainingGameEditorItem()
        }
    }

    val weightsByGameId = trainingGames.associate { trainingGame ->
        trainingGame.gameId to trainingGame.weight
    }

    return allGames.mapNotNull { game ->
        val weight = weightsByGameId[game.id] ?: return@mapNotNull null
        game.toTrainingGameEditorItem(weight = weight)
    }
}

@Composable
private fun TrainingGamesPage(
    games: List<TrainingGameEditorItem>,
    currentPage: Int,
    totalPages: Int,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    showStartButton: Boolean,
    onDecreaseWeightClick: (Long) -> Unit,
    onIncreaseWeightClick: (Long) -> Unit,
    onRemoveGameClick: (Long) -> Unit,
    onStartTrainingClick: (Long) -> Unit,
    onPreviousPageClick: () -> Unit,
    onNextPageClick: () -> Unit
) {
    CardSurface(modifier = Modifier.fillMaxWidth()) {
        SectionTitleText(
            text = "Games in Training"
        )

        Spacer(modifier = Modifier.height(AppDimens.spaceSm))

        CardMetaText(
            text = "Page ${currentPage + 1} of $totalPages",
        )

        Spacer(modifier = Modifier.height(AppDimens.spaceLg))

        if (games.isEmpty()) {
            BodySecondaryText(text = "No games available.")
        } else {
            games.forEachIndexed { index, game ->
                TrainingGamePageRow(
                    game = game,
                    showStartButton = showStartButton,
                    onDecreaseWeightClick = { onDecreaseWeightClick(game.gameId) },
                    onIncreaseWeightClick = { onIncreaseWeightClick(game.gameId) },
                    onRemoveGameClick = { onRemoveGameClick(game.gameId) },
                    onStartTrainingClick = { onStartTrainingClick(game.gameId) },
                )
                if (index + 1 < games.size) {
                    Spacer(modifier = Modifier.height(TrainingGameRowSpacing))
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.spaceSm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
        ) {
            SecondaryButton(
                text = "Previous",
                onClick = onPreviousPageClick,
                enabled = canGoPrevious,
                modifier = Modifier.weight(1f)
            )
            SecondaryButton(
                text = "Next",
                onClick = onNextPageClick,
                enabled = canGoNext,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TrainingGamePageRow(
    game: TrainingGameEditorItem,
    showStartButton: Boolean,
    onDecreaseWeightClick: () -> Unit,
    onIncreaseWeightClick: () -> Unit,
    onRemoveGameClick: () -> Unit,
    onStartTrainingClick: () -> Unit,
) {
    CardSurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(AppDimens.spaceMd)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                SectionTitleText(
                    text = game.title,
                )
                Spacer(modifier = Modifier.height(AppDimens.spaceXs))
                CardMetaText(
                    text = "ID: ${game.gameId}"
                )
                CardMetaText(
                    text = "Weight: ${game.weight}"
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
                ) {
                    SecondaryButton(
                        text = "-",
                        onClick = onDecreaseWeightClick,
                        modifier = Modifier.width(56.dp)
                    )
                    SecondaryButton(
                        text = "+",
                        onClick = onIncreaseWeightClick,
                        modifier = Modifier.width(56.dp),
                    )
                }
                if (showStartButton) {
                    PrimaryButton(
                        text = "GO",
                        onClick = onStartTrainingClick,
                        modifier = Modifier.width(72.dp),
                    )
                }
                IconButton(onClick = onRemoveGameClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove game from training",
                        tint = TrainingErrorRed
                    )
                }
            }
        }
    }
}

@Composable
private fun MissingTrainingDialog(
    onDismiss: () -> Unit
) {
    AppMessageDialog(
        title = "Training Not Found",
        message = "The selected training is unavailable.",
        onDismiss = onDismiss
    )
}


@Composable
private fun TrainingSaveSuccessDialog(
    success: TrainingSaveSuccess,
    onDismiss: () -> Unit
) {
    AppMessageDialog(
        title = if (success.wasUpdated) "Training Updated" else "Training Created",
        message = buildTrainingSaveSuccessMessage(success),
        onDismiss = onDismiss
    )
}

private fun buildTrainingSaveSuccessMessage(
    success: TrainingSaveSuccess
): String {
    return buildString {
        appendLine("ID: ${success.trainingId}")
        appendLine("Name: ${success.trainingName}")
        append(if (success.wasUpdated) "Games in training: " else "Games added: ")
        append(success.gamesCount)
    }
}

@Composable
private fun CreateTrainingBottomNavigation(
    selectedItem: ScreenType,
    onItemSelected: (ScreenType) -> Unit,
    modifier: Modifier = Modifier
) {
    AppBottomNavigation(
        items = defaultAppBottomNavigationItems(),
        selectedItem = selectedItem,
        onItemSelected = onItemSelected,
        modifier = modifier
    )
}
