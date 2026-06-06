package com.example.chessboard.ui.screen.training.template

/*
 * Load/save helpers for the edit-template screen.
 *
 * Keep screen-specific loading state and persistence-backed load/save helpers
 * here so EditTrainingTemplateScreen can stay focused on UI and local screen
 * orchestration. Do not add dialog composables or broader screen layout code.
 */

import com.example.chessboard.ui.screen.training.loadsave.buildTrainingEditorItems
import com.example.chessboard.ui.screen.training.loadsave.normalizeTrainingEditorName
import com.example.chessboard.ui.screen.training.common.TrainingLineEditorItem

import com.example.chessboard.entity.LineEntity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.OneLineTrainingData
import com.example.chessboard.service.TrainingTemplateService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class TrainingTemplateLoadState(
    val templateName: String = "",
    val linesForTemplate: List<TrainingLineEditorItem> = emptyList(),
    val allLinesById: Map<Long, LineEntity> = emptyMap(),
    val templateLoadFailed: Boolean = false,
)

internal data class TrainingTemplateSaveSuccess(
    val templateId: Long,
    val templateName: String,
    val linesCount: Int,
)

internal enum class TrainingTemplateSaveResult {
    UPDATED,
    DELETED,
}

internal suspend fun loadEditTrainingTemplateState(
    inDbProvider: DatabaseProvider,
    trainingTemplateService: TrainingTemplateService,
    templateId: Long,
    defaultTemplateName: String,
    unnamedOpeningName: String,
): TrainingTemplateLoadState {
    val allLines = withContext(Dispatchers.IO) {
        inDbProvider.getAllLines()
    }

    val template = withContext(Dispatchers.IO) {
        trainingTemplateService.getTemplateById(templateId)
    } ?: return TrainingTemplateLoadState(
        templateName = defaultTemplateName,
        linesForTemplate = emptyList(),
        templateLoadFailed = true,
    )

    return TrainingTemplateLoadState(
        templateName = normalizeTrainingEditorName(
            trainingName = template.name,
            defaultName = defaultTemplateName,
        ),
        linesForTemplate = buildTrainingEditorItems(
            allLines = allLines,
            trainingLines = OneLineTrainingData.fromJson(template.linesJson),
            unnamedOpeningName = unnamedOpeningName,
        ),
        allLinesById = allLines.associateBy { line -> line.id },
    )
}

internal suspend fun saveEditedTrainingTemplate(
    trainingTemplateService: TrainingTemplateService,
    templateId: Long,
    templateName: String,
    editableLines: List<TrainingLineEditorItem>,
    defaultTemplateName: String,
): Pair<TrainingTemplateSaveResult, TrainingTemplateSaveSuccess?>? {
    val normalizedName = normalizeTrainingEditorName(
        trainingName = templateName,
        defaultName = defaultTemplateName,
    )
    val templateLines = editableLines.map { line ->
        OneLineTrainingData(
            lineId = line.lineId,
            weight = line.weight,
        )
    }

    val updateResult = withContext(Dispatchers.IO) {
        trainingTemplateService.updateTemplateFromLines(
            templateId = templateId,
            lines = templateLines,
            name = normalizedName,
        )
    }

    when (updateResult) {
        TrainingTemplateService.UpdateTemplateFromLinesResult.NOT_FOUND -> return null
        TrainingTemplateService.UpdateTemplateFromLinesResult.DELETED -> {
            return TrainingTemplateSaveResult.DELETED to null
        }
        TrainingTemplateService.UpdateTemplateFromLinesResult.UPDATED -> {
            return TrainingTemplateSaveResult.UPDATED to TrainingTemplateSaveSuccess(
                templateId = templateId,
                templateName = normalizedName,
                linesCount = editableLines.size,
            )
        }
    }
}
