package com.example.chessboard.ui.screen.training

/*
 * Container helpers that prepare initial training data for specific create-training flows.
 */

import com.example.chessboard.ui.screen.training.common.toTrainingGameEditorItem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.chessboard.service.OneGameTrainingData
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.screen.ScreenContainerContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CreateTrainingFromAllGamesScreenContainer(
    screenContext: ScreenContainerContext,
    screenTitle: String = "Create Training",
    gamesCountLabel: String = "Games loaded for training",
    modifier: Modifier = Modifier,
) {
    var initialData by remember { mutableStateOf(CreateTrainingInitialData()) }

    LaunchedEffect(Unit) {
        val allGames = withContext(Dispatchers.IO) {
            screenContext.inDbProvider.getAllGames()
        }

        initialData = CreateTrainingInitialData(
            trainingName = DEFAULT_TRAINING_NAME,
            gamesForTraining = allGames.map { game ->
                game.toTrainingGameEditorItem()
            }
        )
    }

    CreateTrainingScreenContainer(
        screenContext = screenContext,
        initialData = initialData,
        screenTitle = screenTitle,
        gamesCountLabel = gamesCountLabel,
        modifier = modifier
    )
}

@Composable
fun CreateTrainingFromTemplateScreenContainer(
    templateId: Long,
    screenContext: ScreenContainerContext,
    screenTitle: String = "Create Training From Template",
    gamesCountLabel: String = "Games loaded from template",
    modifier: Modifier = Modifier,
) {
    var initialData by remember { mutableStateOf(CreateTrainingInitialData()) }
    var loadErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(templateId) {
        val templateInitialData = withContext(Dispatchers.IO) {
            val template = screenContext.inDbProvider.createTrainingTemplateService().getTemplateById(templateId)
                ?: return@withContext null
            val allGamesById = screenContext.inDbProvider.getAllGames().associateBy { it.id }
            val templateGames = OneGameTrainingData.fromJson(template.gamesJson).mapNotNull { templateGame ->
                val game = allGamesById[templateGame.gameId] ?: return@mapNotNull null
                game.toTrainingGameEditorItem(weight = templateGame.weight)
            }

            CreateTrainingInitialData(
                trainingName = template.name.ifBlank { DEFAULT_TRAINING_NAME },
                gamesForTraining = templateGames
            )
        }

        if (templateInitialData == null) {
            loadErrorMessage = "Template ID: $templateId"
            return@LaunchedEffect
        }

        initialData = templateInitialData
    }

    loadErrorMessage?.let { message ->
        AppMessageDialog(
            title = "Template Not Found",
            message = message,
            onDismiss = {
                loadErrorMessage = null
                screenContext.onBackClick()
            }
        )
    }

    CreateTrainingScreenContainer(
        screenContext = screenContext,
        initialData = initialData,
        screenTitle = screenTitle,
        gamesCountLabel = gamesCountLabel,
        modifier = modifier
    )
}

@Composable
fun CreateTrainingFromGameIdsScreenContainer(
    gameIds: List<Long>,
    screenContext: ScreenContainerContext,
    screenTitle: String = "Create Training From Position",
    gamesCountLabel: String = "Games found for position",
    modifier: Modifier = Modifier,
) {
    var initialData by remember { mutableStateOf(CreateTrainingInitialData()) }

    LaunchedEffect(gameIds) {
        val allGamesById = withContext(Dispatchers.IO) {
            screenContext.inDbProvider.getAllGames().associateBy { it.id }
        }

        initialData = CreateTrainingInitialData(
            trainingName = DEFAULT_TRAINING_NAME,
            gamesForTraining = gameIds.mapNotNull { gameId ->
                allGamesById[gameId]?.toTrainingGameEditorItem()
            }
        )
    }

    CreateTrainingScreenContainer(
        screenContext = screenContext,
        initialData = initialData,
        screenTitle = screenTitle,
        gamesCountLabel = gamesCountLabel,
        modifier = modifier
    )
}
