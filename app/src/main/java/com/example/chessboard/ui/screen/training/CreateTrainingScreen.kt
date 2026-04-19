package com.example.chessboard.ui.screen.training
import com.example.chessboard.ui.screen.training.common.CreateTrainingEditorState
import com.example.chessboard.ui.screen.training.common.DEFAULT_TRAINING_NAME
import com.example.chessboard.ui.screen.training.common.TrainingGameEditorItem
import com.example.chessboard.ui.screen.training.common.decreaseTrainingGameWeight
import com.example.chessboard.ui.screen.training.common.increaseTrainingGameWeight
import com.example.chessboard.ui.screen.training.common.removeTrainingGame

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.Dp
import com.example.chessboard.service.OneGameTrainingData
import com.example.chessboard.ui.screen.training.loadsave.TrainingSaveSuccess
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
import com.example.chessboard.ui.components.RepeatStepIconButton
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

internal data class CreateTrainingInitialData(
    val trainingName: String = DEFAULT_TRAINING_NAME,
    val gamesForTraining: List<TrainingGameEditorItem> = emptyList()
)

@Composable
internal fun CreateTrainingScreenContainer(
    screenContext: ScreenContainerContext,
    initialData: CreateTrainingInitialData,
    screenTitle: String = "Create Training",
    gamesCountLabel: String = "Games loaded for training",
    modifier: Modifier = Modifier,
) {
    var trainingSaveSuccess by remember { mutableStateOf<TrainingSaveSuccess?>(null) }
    val scope = rememberCoroutineScope()

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
            trainingName = initialData.trainingName,
            editableGamesForTraining = initialData.gamesForTraining
        ),
        screenTitle = screenTitle,
        gamesCountLabel = gamesCountLabel,
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
                    val trainingService = screenContext.inDbProvider.createTrainingService()
                    trainingService.createTrainingFromGames(
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
    headerContent: (@Composable () -> Unit)? = null,
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

            if (headerContent != null) {
                headerContent()
                Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            }

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
                    onEditorStateChange(
                        editorState.copy(
                            editableGamesForTraining = decreaseTrainingGameWeight(
                                games = editorState.editableGamesForTraining,
                                gameId = gameId
                            )
                        )
                    )
                },
                onIncreaseWeightClick = { gameId ->
                    onEditorStateChange(
                        editorState.copy(
                            editableGamesForTraining = increaseTrainingGameWeight(
                                games = editorState.editableGamesForTraining,
                                gameId = gameId
                            )
                        )
                    )
                },
                onRemoveGameClick = { gameId ->
                    onEditorStateChange(
                        editorState.copy(
                            editableGamesForTraining = removeTrainingGame(
                                games = editorState.editableGamesForTraining,
                                gameId = gameId
                            )
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
                    RepeatStepIconButton(
                        icon = Icons.Default.Remove,
                        contentDescription = "Decrease game weight",
                        onStep = onDecreaseWeightClick,
                    )
                    RepeatStepIconButton(
                        icon = Icons.Default.Add,
                        contentDescription = "Increase game weight",
                        onStep = onIncreaseWeightClick,
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
