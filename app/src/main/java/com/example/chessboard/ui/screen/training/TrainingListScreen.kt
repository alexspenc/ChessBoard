package com.example.chessboard.ui.screen.training

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.chessboard.service.OneGameTrainingData
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppConfirmDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingErrorRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class TrainingListState(
    val isLoading: Boolean = true,
    val trainings: List<TrainingListItem> = emptyList(),
    val trainingToDelete: TrainingListItem? = null,
)

private data class TrainingListItem(
    val trainingId: Long,
    val name: String,
    val gamesCount: Int,
)

@Composable
fun TrainingListScreenContainer(
    modifier: Modifier = Modifier,
    screenContext: ScreenContainerContext,
    onOpenTraining: (Long) -> Unit = {},
) {
    val inDbProvider = screenContext.inDbProvider
    val trainingService = remember(inDbProvider) { inDbProvider.createTrainingService() }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(TrainingListState()) }

    LaunchedEffect(Unit) {
        val trainings = withContext(Dispatchers.IO) {
            trainingService.getAllTrainings().map { training ->
                TrainingListItem(
                    trainingId = training.id,
                    name = training.name.ifBlank { "Unnamed Training" },
                    gamesCount = OneGameTrainingData.fromJson(training.gamesJson).size,
                )
            }
        }
        state = state.copy(
            trainings = trainings,
            isLoading = false
        )
    }

    TrainingListScreen(
        state = state,
        modifier = modifier,
        onBackClick = screenContext.onBackClick,
        onNavigate = screenContext.onNavigate,
        onOpenTraining = onOpenTraining,
        onDeleteTraining = createDeleteTrainingAction(
            scope = scope,
            trainingService = trainingService,
            trainings = { state.trainings },
            onTrainingsChange = { trainings -> state = state.copy(trainings = trainings) }
        ),
        onTrainingToDeleteChange = { training -> state = state.copy(trainingToDelete = training) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainingListScreen(
    state: TrainingListState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onOpenTraining: (Long) -> Unit = {},
    onDeleteTraining: (Long) -> Unit = {},
    onTrainingToDeleteChange: (TrainingListItem?) -> Unit = {},
) {

    if (state.trainingToDelete != null) {
        AppConfirmDialog(
            title = "Delete Training",
            message = resolveDeleteTrainingMessage(state.trainingToDelete),
            onDismiss = { onTrainingToDeleteChange(null) },
            onConfirm = {
                onDeleteTraining(state.trainingToDelete.trainingId)
                onTrainingToDeleteChange(null)
            },
            confirmText = "Delete",
            isDestructive = true
        )
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Trainings",
                onBackClick = onBackClick,
                filledBackButton = true,
            )
        },
        bottomBar = {
            AppBottomNavigation(
                items = defaultAppBottomNavigationItems(),
                selectedItem = ScreenType.Training,
                onItemSelected = onNavigate,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                horizontal = AppDimens.spaceLg,
                vertical = AppDimens.spaceLg,
            ),
        ) {
            when {
                state.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = TrainingAccentTeal)
                        }
                    }
                }

                state.trainings.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            BodySecondaryText(
                                text = "No trainings available.",
                                color = TextColor.Secondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                else -> {
                    items(state.trainings, key = { it.trainingId }) { training ->
                        TrainingListCard(
                            training = training,
                            onClick = { onOpenTraining(training.trainingId) },
                            onDeleteClick = { onTrainingToDeleteChange(training) },
                        )
                        Spacer(modifier = Modifier.height(AppDimens.spaceMd))
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainingListCard(
    training: TrainingListItem,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CardSurface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                ScreenTitleText(text = training.name)
                Spacer(modifier = Modifier.height(AppDimens.spaceXs))
                CardMetaText(text = "Training ID: ${training.trainingId}")
                CardMetaText(text = "Games: ${training.gamesCount}")
            }
            IconButton(onClick = onDeleteClick) {
                IconMd(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete training",
                    tint = TrainingErrorRed,
                )
            }
        }
    }
}

private fun createDeleteTrainingAction(
    scope: kotlinx.coroutines.CoroutineScope,
    trainingService: com.example.chessboard.service.TrainingService,
    trainings: () -> List<TrainingListItem>,
    onTrainingsChange: (List<TrainingListItem>) -> Unit
): (Long) -> Unit {
    return { trainingId ->
        scope.launch {
            withContext(Dispatchers.IO) {
                trainingService.deleteTraining(trainingId)
            }
            onTrainingsChange(trainings().filterNot { it.trainingId == trainingId })
        }
    }
}

private fun resolveDeleteTrainingMessage(training: TrainingListItem): String {
    return "Delete \"${training.name}\"?\nTraining ID: ${training.trainingId}"
}
