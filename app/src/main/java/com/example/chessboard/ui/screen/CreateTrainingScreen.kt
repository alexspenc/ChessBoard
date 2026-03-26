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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.OneGameTrainingData
import com.example.chessboard.ui.components.AppBottomNavigation
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
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingBackgroundDark
import com.example.chessboard.ui.theme.TrainingTextPrimary
import com.example.chessboard.ui.theme.TrainingTextSecondary
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
    val gamesCount: Int
)

@Composable
fun CreateTrainingScreenContainer(
    activity: Activity,
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    modifier: Modifier = Modifier,
    inDbProvider: DatabaseProvider,
) {
    var gamesForTraining by remember { mutableStateOf<List<TrainingGameEditorItem>>(emptyList()) }
    var showNoGamesError by remember { mutableStateOf(false) }
    var trainingSaveSuccess by remember { mutableStateOf<TrainingSaveSuccess?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        gamesForTraining = withContext(Dispatchers.IO) {
            inDbProvider.getAllGames().map { game ->
                game.toTrainingGameEditorItem()
            }
        }
    }

    if (showNoGamesError) {
        TrainingSaveErrorDialog(
            onDismiss = { showNoGamesError = false }
        )
    }

    trainingSaveSuccess?.let { success ->
        TrainingSaveSuccessDialog(
            success = success,
            onDismiss = { trainingSaveSuccess = null }
        )
    }

    CreateTrainingScreen(
        gamesForTraining = gamesForTraining,
        onBackClick = onBackClick,
        onNavigate = onNavigate,
        onSaveTraining = { trainingName, editableGames ->
            scope.launch {
                val normalizedName = trainingName.ifBlank { DEFAULT_TRAINING_NAME }
                val trainingId = withContext(Dispatchers.IO) {
                    inDbProvider.createTrainingFromGames(
                        name = normalizedName,
                        games = editableGames.map { game ->
                            OneGameTrainingData(
                                gameId = game.gameId,
                                weight = game.weight
                            )
                        }
                    )
                }

                if (trainingId == null) {
                    showNoGamesError = true
                } else {
                    trainingSaveSuccess = TrainingSaveSuccess(
                        trainingId = trainingId,
                        trainingName = normalizedName,
                        gamesCount = editableGames.size
                    )
                }
            }
        },
        modifier = modifier
    )
}

@Composable
fun CreateTrainingScreen(
    gamesForTraining: List<TrainingGameEditorItem> = emptyList(),
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onSaveTraining: (String, List<TrainingGameEditorItem>) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedNavItem by remember { mutableStateOf<ScreenType>(ScreenType.Home) }
    var trainingName by remember { mutableStateOf(DEFAULT_TRAINING_NAME) }
    var currentPage by remember { mutableStateOf(0) }
    var editableGamesForTraining by remember { mutableStateOf(gamesForTraining) }

    LaunchedEffect(gamesForTraining) {
        editableGamesForTraining = gamesForTraining
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = TrainingBackgroundDark,
        topBar = {
            AppTopBar(
                title = "Create Training",
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
                BodySecondaryText(
                    text = "Games loaded for training: ${editableGamesForTraining.size}",
                    color = TrainingTextSecondary
                )
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

private fun GameEntity.toTrainingGameEditorItem(): TrainingGameEditorItem {
    return TrainingGameEditorItem(
        gameId = id,
        title = event ?: "Unnamed Opening",
        weight = 1
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
    onPreviousPageClick: () -> Unit,
    onNextPageClick: () -> Unit
) {
    CardSurface(modifier = Modifier.fillMaxWidth()) {
        SectionTitleText(
            text = "Games in Training",
            color = TrainingTextPrimary
        )

        Spacer(modifier = Modifier.height(AppDimens.spaceSm))

        CardMetaText(
            text = "Page ${currentPage + 1} of $totalPages",
            color = TrainingTextSecondary
        )

        Spacer(modifier = Modifier.height(AppDimens.spaceLg))

        if (games.isEmpty()) {
            BodySecondaryText(
                text = "No games available.",
                color = TrainingTextSecondary
            )
        } else {
            games.forEachIndexed { index, game ->
                TrainingGamePageRow(
                    game = game,
                    onDecreaseWeightClick = { onDecreaseWeightClick(game.gameId) },
                    onIncreaseWeightClick = { onIncreaseWeightClick(game.gameId) }
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
    onIncreaseWeightClick: () -> Unit
) {
    CardSurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(AppDimens.spaceMd)
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
                    color = TrainingTextPrimary
                )
                Spacer(modifier = Modifier.height(AppDimens.spaceXs))
                CardMetaText(
                    text = "ID: ${game.gameId}",
                    color = TrainingTextSecondary
                )
                CardMetaText(
                    text = "Weight: ${game.weight}",
                    color = TrainingTextSecondary
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
            ) {
                SecondaryButton(
                    text = "-",
                    onClick = onDecreaseWeightClick,
                    modifier = Modifier.width(56.dp)
                )
                SecondaryButton(
                    text = "+",
                    onClick = onIncreaseWeightClick,
                    modifier = Modifier.width(56.dp)
                )
            }
        }
    }
}

@Composable
private fun TrainingSaveErrorDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TrainingBackgroundDark,
        title = {
            SectionTitleText(
                text = "Training Creation Failed",
                color = TrainingTextPrimary
            )
        },
        text = {
            BodySecondaryText(
                text = "There are no games available to create a training.",
                color = TrainingTextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                CardMetaText(
                    text = "OK",
                    color = TrainingAccentTeal
                )
            }
        }
    )
}

@Composable
private fun TrainingSaveSuccessDialog(
    success: TrainingSaveSuccess,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TrainingBackgroundDark,
        title = {
            SectionTitleText(
                text = "Training Created",
                color = TrainingTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)) {
                BodySecondaryText(
                    text = "ID: ${success.trainingId}",
                    color = TrainingTextSecondary
                )
                BodySecondaryText(
                    text = "Name: ${success.trainingName}",
                    color = TrainingTextSecondary
                )
                BodySecondaryText(
                    text = "Games added: ${success.gamesCount}",
                    color = TrainingTextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                CardMetaText(
                    text = "OK",
                    color = TrainingAccentTeal
                )
            }
        }
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
