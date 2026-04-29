package com.example.chessboard.ui.screen.createOpening

/**
 * Post-save flow for actions offered after opening games are saved.
 *
 * Keep in this file:
 * - state models for the post-save flow
 * - dialog sequencing for optional training/template creation
 * - helper functions that execute training/template creation after save
 *
 * It is acceptable to add here:
 * - new post-save steps related to saved opening games
 * - flow-specific validation or warning aggregation
 * - small helper functions used only by this flow
 *
 * Do not add here:
 * - the main create-opening screen layout
 * - unrelated screen container state
 * - generic app-wide dialog abstractions or unrelated database helpers
 */
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.OneGameTrainingData
import com.example.chessboard.ui.components.AppConfirmDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SavedOpeningGames(
    val name: String,
    val gameIds: List<Long>,
)

enum class PostSaveDialogStep {
    CreateTraining,
    CreateTemplate,
}

data class CreateOpeningPostSaveState(
    val pendingSavedGames: SavedOpeningGames? = null,
    val dialogStep: PostSaveDialogStep? = null,
    val warningMessage: String? = null,
)

fun startCreateOpeningPostSaveFlow(
    openingName: String,
    savedGameIds: List<Long>,
): CreateOpeningPostSaveState {
    return CreateOpeningPostSaveState(
        pendingSavedGames = SavedOpeningGames(
            name = openingName.ifBlank { "Opening" },
            gameIds = savedGameIds,
        ),
        dialogStep = PostSaveDialogStep.CreateTraining,
    )
}

private fun advanceCreateOpeningPostSaveFlowToTemplate(
    state: CreateOpeningPostSaveState,
): CreateOpeningPostSaveState {
    if (state.pendingSavedGames == null) {
        return state.copy(dialogStep = null)
    }
    return state.copy(dialogStep = PostSaveDialogStep.CreateTemplate)
}

private fun appendCreateOpeningPostSaveWarning(
    state: CreateOpeningPostSaveState,
    message: String,
): CreateOpeningPostSaveState {
    val updatedWarning = if (state.warningMessage.isNullOrBlank()) {
        message
    } else {
        state.warningMessage + "\n" + message
    }
    return state.copy(warningMessage = updatedWarning)
}

internal suspend fun createOpeningTraining(
    dbProvider: DatabaseProvider,
    savedGames: SavedOpeningGames,
): Boolean {
    val trainingId = dbProvider.createTrainingService().createTrainingFromGames(
        name = savedGames.name,
        games = savedGames.gameIds.map { gameId ->
            OneGameTrainingData(gameId = gameId, weight = 1)
        },
    )
    return trainingId != null
}

private suspend fun createOpeningTemplate(
    dbProvider: DatabaseProvider,
    savedGames: SavedOpeningGames,
): Boolean {
    val templateService = dbProvider.createTrainingTemplateService()
    val templateId = templateService.createTemplate(savedGames.name)
    if (templateId <= 0L) {
        return false
    }

    return savedGames.gameIds.all { gameId ->
        templateService.addGame(templateId = templateId, gameId = gameId, weight = 1)
    }
}

@Composable
fun CreateOpeningPostSaveDialogs(
    activity: Activity,
    dbProvider: DatabaseProvider,
    state: CreateOpeningPostSaveState,
    onStateChange: (CreateOpeningPostSaveState) -> Unit,
    onFinished: () -> Unit,
    onError: (String) -> Unit,
) {
    val savedGames = state.pendingSavedGames ?: return
    val savedGamesCount = savedGames.gameIds.size
    val trainingMessage = if (savedGamesCount == 1) {
        "Do you want to create a training from the saved game?"
    } else {
        "Do you want to create a training from the $savedGamesCount saved games?"
    }
    val templateMessage = if (savedGamesCount == 1) {
        "Do you want to create a template from the saved game?"
    } else {
        "Do you want to create a template from the $savedGamesCount saved games?"
    }

    if (state.dialogStep == PostSaveDialogStep.CreateTraining) {
        AppConfirmDialog(
            title = "Create Training",
            message = trainingMessage,
            onDismiss = {
                onStateChange(advanceCreateOpeningPostSaveFlowToTemplate(state))
            },
            onConfirm = {
                onStateChange(state.copy(dialogStep = null))
                (activity as? LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.IO) {
                    var nextState = state
                    if (!createOpeningTraining(dbProvider, savedGames)) {
                        nextState = appendCreateOpeningPostSaveWarning(
                            state = nextState,
                            message = "Games were saved, but training could not be created",
                        )
                    }
                    nextState = advanceCreateOpeningPostSaveFlowToTemplate(nextState)

                    withContext(Dispatchers.Main) {
                        onStateChange(nextState)
                    }
                }
            },
            confirmText = "Create",
            dismissText = "Skip",
        )
    }

    if (state.dialogStep == PostSaveDialogStep.CreateTemplate) {
        AppConfirmDialog(
            title = "Create Template",
            message = templateMessage,
            onDismiss = {
                if (state.warningMessage.isNullOrBlank()) {
                    onFinished()
                } else {
                    onError(state.warningMessage)
                }
            },
            onConfirm = {
                onStateChange(state.copy(dialogStep = null))
                (activity as? LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.IO) {
                    var nextState = state
                    if (!createOpeningTemplate(dbProvider, savedGames)) {
                        nextState = appendCreateOpeningPostSaveWarning(
                            state = nextState,
                            message = "Games were saved, but template could not be created",
                        )
                    }

                    withContext(Dispatchers.Main) {
                        if (nextState.warningMessage.isNullOrBlank()) {
                            onFinished()
                        } else {
                            onError(nextState.warningMessage)
                        }
                    }
                }
            },
            confirmText = "Create",
            dismissText = "Skip",
        )
    }
}
