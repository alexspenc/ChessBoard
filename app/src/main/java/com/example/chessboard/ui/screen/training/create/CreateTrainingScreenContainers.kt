package com.example.chessboard.ui.screen.training.create
import com.example.chessboard.ui.screen.training.common.DEFAULT_TRAINING_NAME

/*
 * Container helpers that prepare initial training data for specific create-training flows.
 */

import com.example.chessboard.ui.screen.training.common.toTrainingLineEditorItem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.chessboard.service.OneLineTrainingData
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.screen.ScreenContainerContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CreateTrainingFromAllLinesScreenContainer(
    screenContext: ScreenContainerContext,
    screenTitle: String = "Create Training",
    linesCountLabel: String = "Lines loaded for training",
    modifier: Modifier = Modifier,
) {
    var initialData by remember { mutableStateOf(CreateTrainingInitialData()) }

    LaunchedEffect(Unit) {
        val allLines = withContext(Dispatchers.IO) {
            screenContext.inDbProvider.getAllLines()
        }

        initialData = CreateTrainingInitialData(
            trainingName = DEFAULT_TRAINING_NAME,
            linesForTraining = allLines.map { line ->
                line.toTrainingLineEditorItem()
            }
        )
    }

    CreateTrainingScreenContainer(
        screenContext = screenContext,
        initialData = initialData,
        screenTitle = screenTitle,
        linesCountLabel = linesCountLabel,
        modifier = modifier
    )
}

@Composable
fun CreateTrainingFromTemplateScreenContainer(
    templateId: Long,
    screenContext: ScreenContainerContext,
    screenTitle: String = "Create Training From Template",
    linesCountLabel: String = "Lines loaded from template",
    modifier: Modifier = Modifier,
) {
    var initialData by remember { mutableStateOf(CreateTrainingInitialData()) }
    var loadErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(templateId) {
        val templateInitialData = withContext(Dispatchers.IO) {
            val template = screenContext.inDbProvider.createTrainingTemplateService().getTemplateById(templateId)
                ?: return@withContext null
            val allLinesById = screenContext.inDbProvider.getAllLines().associateBy { it.id }
            val templateLines = OneLineTrainingData.fromJson(template.linesJson).mapNotNull { templateLine ->
                val line = allLinesById[templateLine.lineId] ?: return@mapNotNull null
                line.toTrainingLineEditorItem(weight = templateLine.weight)
            }

            CreateTrainingInitialData(
                trainingName = template.name.ifBlank { DEFAULT_TRAINING_NAME },
                linesForTraining = templateLines
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
        linesCountLabel = linesCountLabel,
        modifier = modifier
    )
}

@Composable
fun CreateTrainingFromLineIdsScreenContainer(
    lineIds: List<Long>,
    screenContext: ScreenContainerContext,
    initialTrainingName: String? = null,
    screenTitle: String = "Create Training From Position",
    linesCountLabel: String = "Lines found for position",
    modifier: Modifier = Modifier,
) {
    var initialData by remember { mutableStateOf(CreateTrainingInitialData()) }

    LaunchedEffect(lineIds, initialTrainingName) {
        val allLinesById = withContext(Dispatchers.IO) {
            screenContext.inDbProvider.getAllLines().associateBy { it.id }
        }

        initialData = CreateTrainingInitialData(
            trainingName = initialTrainingName ?: DEFAULT_TRAINING_NAME,
            linesForTraining = lineIds.mapNotNull { lineId ->
                allLinesById[lineId]?.toTrainingLineEditorItem()
            }
        )
    }

    CreateTrainingScreenContainer(
        screenContext = screenContext,
        initialData = initialData,
        screenTitle = screenTitle,
        linesCountLabel = linesCountLabel,
        modifier = modifier
    )
}
