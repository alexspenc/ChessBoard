package com.example.chessboard.ui.screen.training

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.chessboard.service.OneGameTrainingData
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DEFAULT_STATISTICS_TRAINING_NAME = "StatisticsTraining"

private data class StatisticsTrainingLoadState(
    val isLoading: Boolean = true,
    val trainingName: String = DEFAULT_STATISTICS_TRAINING_NAME,
    val gamesForTraining: List<TrainingGameEditorItem> = emptyList(),
)

private data class StatisticsTrainingSaveSuccess(
    val trainingId: Long,
    val trainingName: String,
    val gamesCount: Int,
)

@Composable
fun CreateTrainingByStatisticsScreenContainer(
    screenContext: ScreenContainerContext,
    modifier: Modifier = Modifier,
) {
    var loadState by remember { mutableStateOf(StatisticsTrainingLoadState()) }
    var trainingSaveSuccess by remember { mutableStateOf<StatisticsTrainingSaveSuccess?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val recommendations = withContext(Dispatchers.IO) {
            screenContext.inDbProvider.createStatisticsTrainingService().getRecommendation(limit = 50)
        }

        loadState = StatisticsTrainingLoadState(
            isLoading = false,
            trainingName = DEFAULT_STATISTICS_TRAINING_NAME,
            gamesForTraining = recommendations.map { recommendation ->
                recommendation.game.toTrainingGameEditorItem(weight = recommendation.weight)
            }
        )
    }

    trainingSaveSuccess?.let { success ->
        AppMessageDialog(
            title = "Training Created",
            message = buildString {
                appendLine("ID: ${success.trainingId}")
                appendLine("Name: ${success.trainingName}")
                append("Games added: ")
                append(success.gamesCount)
            },
            onDismiss = {
                trainingSaveSuccess = null
                screenContext.onNavigate(ScreenType.Home)
            }
        )
    }

    if (loadState.isLoading) {
        AppScreenScaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                AppTopBar(
                    title = "Create Training by Statistics",
                    onBackClick = screenContext.onBackClick
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TrainingAccentTeal)
            }
        }
        return
    }

    if (loadState.gamesForTraining.isEmpty()) {
        AppScreenScaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                AppTopBar(
                    title = "Create Training by Statistics",
                    onBackClick = screenContext.onBackClick
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                BodySecondaryText(
                    text = "No games available for statistics-based training.",
                    color = TextColor.Secondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        return
    }

    CreateTrainingScreen(
        editorState = CreateTrainingEditorState(
            trainingName = loadState.trainingName,
            editableGamesForTraining = loadState.gamesForTraining,
        ),
        screenTitle = "Create Training by Statistics",
        gamesCountLabel = "Games selected by statistics",
        onBackClick = screenContext.onBackClick,
        onNavigate = screenContext.onNavigate,
        onSaveTraining = { trainingName, editableGames ->
            scope.launch {
                val normalizedName = trainingName.ifBlank { DEFAULT_STATISTICS_TRAINING_NAME }
                val trainingGames = editableGames.map { game ->
                    OneGameTrainingData(
                        gameId = game.gameId,
                        weight = game.weight,
                    )
                }

                val savedTrainingId = withContext(Dispatchers.IO) {
                    screenContext.inDbProvider.createTrainingFromGames(
                        name = normalizedName,
                        games = trainingGames,
                    )
                }

                trainingSaveSuccess = StatisticsTrainingSaveSuccess(
                    trainingId = savedTrainingId ?: return@launch,
                    trainingName = normalizedName,
                    gamesCount = editableGames.size,
                )
            }
        },
        modifier = modifier,
    )
}
