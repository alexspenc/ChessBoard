package com.example.chessboard.ui.screen.training.loadsave

/*
 * Load/save helpers for the edit-template screen.
 *
 * Keep screen-specific loading state and persistence-backed load/save helpers
 * here so EditTrainingTemplateScreen can stay focused on UI and local screen
 * orchestration. Do not add dialog composables or broader screen layout code.
 */

import com.example.chessboard.ui.screen.training.common.TrainingGameEditorItem

import com.example.chessboard.entity.GameEntity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.OneGameTrainingData
import com.example.chessboard.service.TrainingTemplateService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val DEFAULT_TEMPLATE_NAME = "Unnamed Template"

internal data class TrainingTemplateLoadState(
    val templateName: String = DEFAULT_TEMPLATE_NAME,
    val gamesForTemplate: List<TrainingGameEditorItem> = emptyList(),
    val allGamesById: Map<Long, GameEntity> = emptyMap(),
    val templateLoadFailed: Boolean = false,
)

internal data class TrainingTemplateSaveSuccess(
    val templateId: Long,
    val templateName: String,
    val gamesCount: Int,
)

internal suspend fun loadEditTrainingTemplateState(
    inDbProvider: DatabaseProvider,
    trainingTemplateService: TrainingTemplateService,
    templateId: Long,
): TrainingTemplateLoadState {
    val allGames = withContext(Dispatchers.IO) {
        inDbProvider.getAllGames()
    }

    val template = withContext(Dispatchers.IO) {
        trainingTemplateService.getTemplateById(templateId)
    } ?: return TrainingTemplateLoadState(
        templateName = DEFAULT_TEMPLATE_NAME,
        gamesForTemplate = emptyList(),
        templateLoadFailed = true,
    )

    return TrainingTemplateLoadState(
        templateName = normalizeTrainingEditorName(
            trainingName = template.name,
            defaultName = DEFAULT_TEMPLATE_NAME,
        ),
        gamesForTemplate = buildTrainingEditorItems(
            allGames = allGames,
            trainingGames = OneGameTrainingData.fromJson(template.gamesJson),
        ),
        allGamesById = allGames.associateBy { game -> game.id },
    )
}

internal suspend fun saveEditedTrainingTemplate(
    trainingTemplateService: TrainingTemplateService,
    templateId: Long,
    templateName: String,
    editableGames: List<TrainingGameEditorItem>,
): TrainingTemplateSaveSuccess? {
    val normalizedName = normalizeTrainingEditorName(
        trainingName = templateName,
        defaultName = DEFAULT_TEMPLATE_NAME,
    )
    val templateGames = editableGames.map { game ->
        OneGameTrainingData(
            gameId = game.gameId,
            weight = game.weight,
        )
    }

    val wasUpdated = withContext(Dispatchers.IO) {
        trainingTemplateService.updateTemplateFromGames(
            templateId = templateId,
            games = templateGames,
            name = normalizedName,
        )
    }

    if (!wasUpdated) {
        return null
    }

    return TrainingTemplateSaveSuccess(
        templateId = templateId,
        templateName = normalizedName,
        gamesCount = editableGames.size,
    )
}
