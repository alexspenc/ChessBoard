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
    var gamesForTraining by remember { mutableStateOf<List<TrainingGameEditorItem>>(emptyList()) }
    var trainingName by remember { mutableStateOf(DEFAULT_TRAINING_NAME) }
    var trainingLoadFailed by remember { mutableStateOf(false) }
    var trainingSaveSuccess by remember { mutableStateOf<TrainingSaveSuccess?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(trainingId) {
        trainingLoadFailed = false
        val allGames = withContext(Dispatchers.IO) {
            inDbProvider.getAllGames()
        }

        if (trainingId == null) {
            trainingName = DEFAULT_TRAINING_NAME
            gamesForTraining = allGames.map { game ->
                game.toTrainingGameEditorItem()
            }
            return@LaunchedEffect
        }

        val training = withContext(Dispatchers.IO) {
            inDbProvider.getTrainingById(trainingId)
        }

        if (training == null) {
            trainingName = DEFAULT_TRAINING_NAME
            gamesForTraining = emptyList()
            trainingLoadFailed = true
            return@LaunchedEffect
        }

        trainingName = training.name.ifBlank { DEFAULT_TRAINING_NAME }
        gamesForTraining = buildTrainingEditorItems(
            allGames = allGames,
            trainingGames = OneGameTrainingData.fromJson(training.gamesJson)
        )
    }

    if (trainingLoadFailed) {
        MissingTrainingDialog(
            onDismiss = {
                trainingLoadFailed = false
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
        initialTrainingName = trainingName,
        gamesForTraining = gamesForTraining,
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
    var trainingName by remember(initialTrainingName) { mutableStateOf(initialTrainingName) }
    var currentPage by remember { mutableStateOf(0) }
    var editableGamesForTraining by remember { mutableStateOf(gamesForTraining) }
    val isEditMode = trainingId != null

    LaunchedEffect(initialTrainingName, gamesForTraining) {
        trainingName = initialTrainingName
        editableGamesForTraining = gamesForTraining
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = resolveCreateTrainingTitle(isEditMode),
                onBackClick = onBackClick,
                actions = {
                    PrimaryButton(
                        text = "Save",
                        onClick = { onSaveTraining(trainingName, editableGamesForTraining) }
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
                    value = trainingName,
                    onValueChange = { trainingName = it },
                    label = "Training Name",
                    placeholder = DEFAULT_TRAINING_NAME
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceLg))

            ScreenSection {
                BodySecondaryText(text = "Games loaded for training: ${editableGamesForTraining.size}")
            }

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val availableHeightForRows =
                    (maxHeight - TrainingGamesHeaderHeight - TrainingGamesNavigationHeight)
                        .coerceAtLeast(TrainingGameRowHeight)
                val trainingGameSlotHeight = TrainingGameRowHeight + TrainingGameRowSpacing
                val pageSize = (availableHeightForRows / trainingGameSlotHeight)
                    .toInt()
                    .coerceAtLeast(1)
                val totalPages = ((editableGamesForTraining.size + pageSize - 1) / pageSize).coerceAtLeast(1)
                val safeCurrentPage = currentPage.coerceIn(0, totalPages - 1)
                val canGoPrevious = safeCurrentPage > 0
                val canGoNext = safeCurrentPage + 1 < totalPages
                val currentPageItems = editableGamesForTraining
                    .drop(safeCurrentPage * pageSize)
                    .take(pageSize)

                ScreenSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppDimens.spaceLg)
                ) {
                    TrainingGamesPage(
                        games = currentPageItems,
                        currentPage = safeCurrentPage,
                        totalPages = totalPages,
                        canGoPrevious = canGoPrevious,
                        canGoNext = canGoNext,
                        showStartButton = isEditMode,
                        onDecreaseWeightClick = { gameId ->
                            editableGamesForTraining = editableGamesForTraining.map { game ->
                                if (game.gameId == gameId) {
                                    game.copy(weight = (game.weight - 1).coerceAtLeast(1))
                                } else {
                                    game
                                }
                            }
                        },
                        onIncreaseWeightClick = { gameId ->
                            editableGamesForTraining = editableGamesForTraining.map { game ->
                                if (game.gameId == gameId) {
                                    game.copy(weight = game.weight + 1)
                                } else {
                                    game
                                }
                            }
                        },
                        onRemoveGameClick = { gameId ->
                            editableGamesForTraining = editableGamesForTraining.filterNot { game ->
                                game.gameId == gameId
                            }
                        },
                        onStartTrainingClick = onStartGameTrainingClick,
                        onPreviousPageClick = {
                            if (canGoPrevious) {
                                currentPage = safeCurrentPage - 1
                            }
                        },
                        onNextPageClick = {
                            if (canGoNext) {
                                currentPage = safeCurrentPage + 1
                            }
                        }
                    )
                }
            }
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
