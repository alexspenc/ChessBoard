package com.example.chessboard.ui.screen.training.create

/*
 * Container helpers that prepare initial training data for specific create-training flows.
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.service.OneLineTrainingData
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.training.common.toTrainingLineEditorItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CreateTrainingFromAllLinesScreenContainer(
    screenContext: ScreenContainerContext,
    screenTitle: String? = null,
    linesCountLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    val defaultTrainingName = stringResource(R.string.create_training_default_name)
    var initialData by remember(defaultTrainingName) {
        mutableStateOf(CreateTrainingInitialData(trainingName = defaultTrainingName))
    }

    LaunchedEffect(defaultTrainingName) {
        val allLines = withContext(Dispatchers.IO) {
            screenContext.inDbProvider.getAllLines()
        }

        initialData = CreateTrainingInitialData(
            trainingName = defaultTrainingName,
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
    screenTitle: String? = null,
    linesCountLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    val defaultTrainingName = stringResource(R.string.create_training_default_name)
    val resolvedScreenTitle = screenTitle ?: stringResource(
        R.string.create_training_from_template_title,
    )
    val resolvedLinesCountLabel = linesCountLabel ?: stringResource(
        R.string.create_training_lines_loaded_from_template_label,
    )
    var initialData by remember(defaultTrainingName) {
        mutableStateOf(CreateTrainingInitialData(trainingName = defaultTrainingName))
    }
    var templateLoadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(templateId, defaultTrainingName) {
        val templateInitialData = withContext(Dispatchers.IO) {
            val template = screenContext.inDbProvider.createTrainingTemplateService().getTemplateById(templateId)
                ?: return@withContext null
            val allLinesById = screenContext.inDbProvider.getAllLines().associateBy { it.id }
            val templateLines = OneLineTrainingData.fromJson(template.linesJson).mapNotNull { templateLine ->
                val line = allLinesById[templateLine.lineId] ?: return@mapNotNull null
                line.toTrainingLineEditorItem(weight = templateLine.weight)
            }

            CreateTrainingInitialData(
                trainingName = template.name.ifBlank { defaultTrainingName },
                linesForTraining = templateLines
            )
        }

        if (templateInitialData == null) {
            templateLoadFailed = true
            return@LaunchedEffect
        }

        initialData = templateInitialData
    }

    if (templateLoadFailed) {
        AppMessageDialog(
            title = stringResource(R.string.training_template_not_found_title),
            message = stringResource(R.string.training_template_id, templateId),
            onDismiss = {
                templateLoadFailed = false
                screenContext.onBackClick()
            }
        )
    }

    CreateTrainingScreenContainer(
        screenContext = screenContext,
        initialData = initialData,
        screenTitle = resolvedScreenTitle,
        linesCountLabel = resolvedLinesCountLabel,
        modifier = modifier
    )
}

@Composable
fun CreateTrainingFromLineIdsScreenContainer(
    lineIds: List<Long>,
    screenContext: ScreenContainerContext,
    initialTrainingName: String? = null,
    screenTitle: String? = null,
    linesCountLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    val defaultTrainingName = stringResource(R.string.create_training_default_name)
    val resolvedScreenTitle = screenTitle ?: stringResource(
        R.string.create_training_from_position_title,
    )
    val resolvedLinesCountLabel = linesCountLabel ?: stringResource(
        R.string.create_training_lines_found_for_position_label,
    )
    var initialData by remember(defaultTrainingName) {
        mutableStateOf(CreateTrainingInitialData(trainingName = defaultTrainingName))
    }

    LaunchedEffect(lineIds, initialTrainingName, defaultTrainingName) {
        val allLinesById = withContext(Dispatchers.IO) {
            screenContext.inDbProvider.getAllLines().associateBy { it.id }
        }

        initialData = CreateTrainingInitialData(
            trainingName = initialTrainingName ?: defaultTrainingName,
            linesForTraining = lineIds.mapNotNull { lineId ->
                allLinesById[lineId]?.toTrainingLineEditorItem()
            }
        )
    }

    CreateTrainingScreenContainer(
        screenContext = screenContext,
        initialData = initialData,
        screenTitle = resolvedScreenTitle,
        linesCountLabel = resolvedLinesCountLabel,
        modifier = modifier
    )
}
