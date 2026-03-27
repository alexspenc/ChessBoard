package com.example.chessboard.ui.screen

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.OneGameTrainingData
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class TrainingListItem(
    val trainingId: Long,
    val name: String,
    val gamesCount: Int,
)

@Composable
fun TrainingListScreenContainer(
    activity: Activity,
    modifier: Modifier = Modifier,
    inDbProvider: DatabaseProvider,
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onOpenTraining: (Long) -> Unit = {},
) {
    var isLoading by remember { mutableStateOf(true) }
    var trainings by remember { mutableStateOf<List<TrainingListItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        trainings = withContext(Dispatchers.IO) {
            inDbProvider.getAllTrainings().map { training ->
                TrainingListItem(
                    trainingId = training.id,
                    name = training.name.ifBlank { "Unnamed Training" },
                    gamesCount = OneGameTrainingData.fromJson(training.gamesJson).size,
                )
            }
        }
        isLoading = false
    }

    TrainingListScreen(
        trainings = trainings,
        isLoading = isLoading,
        modifier = modifier,
        onBackClick = onBackClick,
        onNavigate = onNavigate,
        onOpenTraining = onOpenTraining,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainingListScreen(
    trainings: List<TrainingListItem>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onOpenTraining: (Long) -> Unit = {},
) {
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
                isLoading -> {
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

                trainings.isEmpty() -> {
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
                    items(trainings, key = { it.trainingId }) { training ->
                        TrainingListCard(
                            training = training,
                            onClick = { onOpenTraining(training.trainingId) },
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
    modifier: Modifier = Modifier,
) {
    CardSurface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ScreenTitleText(text = training.name)
            Spacer(modifier = Modifier.height(AppDimens.spaceXs))
            CardMetaText(text = "Training ID: ${training.trainingId}")
            CardMetaText(text = "Games: ${training.gamesCount}")
        }
    }
}
