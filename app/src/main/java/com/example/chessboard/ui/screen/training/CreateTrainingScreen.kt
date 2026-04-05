package com.example.chessboard.ui.screen.training

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
import androidx.compose.material.icons.filled.Save
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
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.SecondaryButton
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingErrorRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal const val DEFAULT_TRAINING_NAME = "FullTraining"
private val TrainingGameRowHeight = 92.dp
private val TrainingGamesHeaderHeight = 88.dp
private val TrainingGamesNavigationHeight = 64.dp
private val TrainingGameRowSpacing = AppDimens.spaceMd

data class TrainingGameEditorItem(
    val gameId: Long,
    val title: String,
    val weight: Int = 1,
    val eco: String? = null,
    val pgn: String = ""
)

private data class CreateTrainingLoadState(
    val trainingName: String = DEFAULT_TRAINING_NAME,
    val gamesForTraining: List<TrainingGameEditorItem> = emptyList()
)

internal data class CreateTrainingEditorState(
    val trainingName: String = DEFAULT_TRAINING_NAME,
    val currentPage: Int = 0,
    val editableGamesForTraining: List<TrainingGameEditorItem> = emptyList()
)

private data class TrainingSaveSuccess(
    val trainingId: Long,
    val trainingName: String,
    val gamesCount: Int
)

internal fun decreaseTrainingGameWeight(
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

internal fun increaseTrainingGameWeight(
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

@Composable
fun CreateTrainingScreenContainer(
    screenContext: ScreenContainerContext,
    modifier: Modifier = Modifier,
) {
    var loadState by remember { mutableStateOf(CreateTrainingLoadState()) }
    var trainingSaveSuccess by remember { mutableStateOf<TrainingSaveSuccess?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val allGames = withContext(Dispatchers.IO) {
            screenContext.inDbProvider.getAllGames()
        }

        loadState = CreateTrainingLoadState(
            trainingName = DEFAULT_TRAINING_NAME,
            gamesForTraining = allGames.map { game ->
                game.toTrainingGameEditorItem()
            }
        )
    }

    trainingSaveSuccess?.let { success ->
        TrainingSaveSuccessDialog(
            success = success,
            onDismiss = {
                trainingSaveSuccess = null
                screenContext.onNavigate(ScreenType.Home)
            }
        )
    }

    CreateTrainingScreen(
        editorState = CreateTrainingEditorState(
            trainingName = loadState.trainingName,
            editableGamesForTraining = loadState.gamesForTraining
        ),
        onBackClick = screenContext.onBackClick,
        onNavigate = screenContext.onNavigate,
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
                    screenContext.inDbProvider.createTrainingFromGames(
                        name = normalizedName,
                        games = trainingGames
                    )
                }

                trainingSaveSuccess = TrainingSaveSuccess(
                    trainingId = savedTrainingId ?: return@launch,
                    trainingName = normalizedName,
                    gamesCount = editableGames.size
                )
            }
        },
        modifier = modifier
    )
}

@Composable
internal fun CreateTrainingScreen(
    editorState: CreateTrainingEditorState = CreateTrainingEditorState(),
    screenTitle: String = "Create Training",
    gamesCountLabel: String = "Games loaded for training",
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onSaveTraining: (String, List<TrainingGameEditorItem>) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedNavItem by remember { mutableStateOf<ScreenType>(ScreenType.Home) }
    var currentEditorState by remember(editorState) {
        mutableStateOf(editorState)
    }

    LaunchedEffect(editorState) {
        currentEditorState = editorState
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = screenTitle,
                onBackClick = onBackClick,
                actions = {
                    Spacer(modifier = Modifier.width(AppDimens.spaceSm))
                    IconButton(
                        onClick = { onSaveTraining(currentEditorState.trainingName, currentEditorState.editableGamesForTraining) }
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
                    value = currentEditorState.trainingName,
                    onValueChange = { currentEditorState = currentEditorState.copy(trainingName = it) },
                    label = "Training Name",
                    placeholder = DEFAULT_TRAINING_NAME
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceLg))

            ScreenSection {
                BodySecondaryText(text = "$gamesCountLabel: ${currentEditorState.editableGamesForTraining.size}")
            }

            TrainingGamesEditorSection(
                editorState = currentEditorState,
                onEditorStateChange = { currentEditorState = it },
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
    onEditorStateChange: (CreateTrainingEditorState) -> Unit,
    modifier: Modifier = Modifier
) {
    data class TrainingGamesPageState(
        val currentPageItems: List<TrainingGameEditorItem>,
        val currentPage: Int,
        val totalPages: Int,
        val canGoPrevious: Boolean,
        val canGoNext: Boolean,
    )

    fun resolveTrainingGamesPageState(maxHeight: Dp): TrainingGamesPageState {
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

internal fun GameEntity.toTrainingGameEditorItem(weight: Int = 1): TrainingGameEditorItem {
    return TrainingGameEditorItem(
        gameId = id,
        title = event ?: "Unnamed Opening",
        weight = weight,
        eco = eco,
        pgn = pgn
    )
}

@Composable
private fun TrainingGamesPage(
    games: List<TrainingGameEditorItem>,
    currentPage: Int,
    totalPages: Int,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onDecreaseWeightClick: (Long) -> Unit,
    onIncreaseWeightClick: (Long) -> Unit,
    onRemoveGameClick: (Long) -> Unit,
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
                    onDecreaseWeightClick = { onDecreaseWeightClick(game.gameId) },
                    onIncreaseWeightClick = { onIncreaseWeightClick(game.gameId) },
                    onRemoveGameClick = { onRemoveGameClick(game.gameId) },
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
    onDecreaseWeightClick: () -> Unit,
    onIncreaseWeightClick: () -> Unit,
    onRemoveGameClick: () -> Unit,
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
private fun TrainingSaveSuccessDialog(
    success: TrainingSaveSuccess,
    onDismiss: () -> Unit
) {
    AppMessageDialog(
        title = "Training Created",
        message = buildString {
            appendLine("ID: ${success.trainingId}")
            appendLine("Name: ${success.trainingName}")
            append("Games added: ")
            append(success.gamesCount)
        },
        onDismiss = onDismiss
    )
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
