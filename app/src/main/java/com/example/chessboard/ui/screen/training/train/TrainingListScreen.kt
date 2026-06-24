package com.example.chessboard.ui.screen.training.train

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.chessboard.R
import com.example.chessboard.service.OneLineTrainingData
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppConfirmDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.DeleteIconButton
import com.example.chessboard.ui.components.HomeIconButton
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
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
    val linesCount: Int,
)

@Composable
fun TrainingListScreenContainer(
    modifier: Modifier = Modifier,
    screenContext: ScreenContainerContext,
    onOpenTraining: (Long) -> Unit,
) {
    val inDbProvider = screenContext.inDbProvider
    val trainingService = remember(inDbProvider) { inDbProvider.createTrainingService() }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(TrainingListState()) }
    val unnamedTrainingName = stringResource(R.string.training_list_unnamed_training)

    LaunchedEffect(unnamedTrainingName) {
        val trainings = withContext(Dispatchers.IO) {
            trainingService.getAllTrainings().map { training ->
                TrainingListItem(
                    trainingId = training.id,
                    name = training.name.ifBlank { unnamedTrainingName },
                    linesCount = OneLineTrainingData.fromJson(training.linesJson).size,
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
            title = stringResource(R.string.training_list_delete_title),
            message = stringResource(
                R.string.training_list_delete_message,
                state.trainingToDelete.name,
                state.trainingToDelete.trainingId,
            ),
            onDismiss = { onTrainingToDeleteChange(null) },
            onConfirm = {
                onDeleteTraining(state.trainingToDelete.trainingId)
                onTrainingToDeleteChange(null)
            },
            confirmText = stringResource(R.string.training_list_delete_confirm),
            isDestructive = true
        )
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.training_list_title),
                onBackClick = onBackClick,
                handleSystemBack = true,
                filledBackButton = true,
                actions = {
                    HomeIconButton(onClick = { onNavigate(ScreenType.Home) })
                },
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
                                text = stringResource(R.string.training_list_empty),
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
                CardMetaText(
                    text = stringResource(
                        R.string.training_list_training_id,
                        training.trainingId,
                    )
                )
                CardMetaText(
                    text = stringResource(
                        R.string.training_list_lines_count,
                        training.linesCount,
                    )
                )
            }
            DeleteIconButton(
                onClick = onDeleteClick,
                contentDescription = stringResource(R.string.training_list_delete_content_description),
            )
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
