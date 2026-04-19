package com.example.chessboard.ui.screen.training.template

/*
 * Screen-level UI for editing a training template.
 *
 * Keep only the template editor scaffold, local screen state, and wiring for
 * shared training editor components here. Do not add database loading, save
 * orchestration, or route-level navigation setup to this file.
 */

import com.example.chessboard.ui.screen.training.common.CreateTrainingEditorState
import com.example.chessboard.ui.screen.training.common.TrainingCollectionEditorScreen
import com.example.chessboard.ui.screen.training.common.TrainingCollectionRemoveAction
import com.example.chessboard.ui.screen.training.common.TrainingCollectionEditorStrings
import com.example.chessboard.ui.screen.training.common.TrainingEditorGameSection
import com.example.chessboard.ui.screen.training.common.TrainingEditorGameSectionActions
import com.example.chessboard.ui.screen.training.common.TrainingEditorGameSectionState
import com.example.chessboard.ui.screen.training.common.TrainingGameEditorItem
import com.example.chessboard.ui.screen.training.common.decreaseTrainingGameWeight
import com.example.chessboard.ui.screen.training.common.increaseTrainingGameWeight
import com.example.chessboard.ui.screen.training.common.removeTrainingGame
import com.example.chessboard.ui.screen.training.common.resolveNextSelectedTrainingGameId
import com.example.chessboard.ui.screen.training.common.rememberTrainingEditorBoardSession

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.screen.training.loadsave.RenderUnsavedTrainingChangesDialog
import com.example.chessboard.ui.screen.training.loadsave.hasUnsavedTrainingEditorChanges
import com.example.chessboard.ui.screen.training.loadsave.normalizeTrainingEditorName
import kotlinx.coroutines.launch


@Composable
private fun RenderMissingTemplateDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    if (!visible) {
        return
    }

    AppMessageDialog(
        title = "Template Not Found",
        message = "The selected template is unavailable.",
        onDismiss = onDismiss,
    )
}

@Composable
private fun RenderEditTrainingTemplateSaveSuccessDialog(
    success: TrainingTemplateSaveSuccess?,
    onDismiss: () -> Unit,
) {
    val currentSuccess = success ?: return

    AppMessageDialog(
        title = "Template Updated",
        message = buildString {
            appendLine("ID: ${currentSuccess.templateId}")
            appendLine("Name: ${currentSuccess.templateName}")
            append("Games in template: ")
            append(currentSuccess.gamesCount)
        },
        onDismiss = onDismiss,
    )
}

private fun createOpenEditTrainingTemplateGameEditorAction(
    allGamesById: Map<Long, GameEntity>,
    onOpenGameEditorClick: (GameEntity) -> Unit,
): (Long) -> Unit {
    return openGameEditor@{ gameId ->
        val game = allGamesById[gameId] ?: return@openGameEditor
        onOpenGameEditorClick(game)
    }
}

@Composable
fun EditTrainingTemplateScreenContainer(
    templateId: Long,
    screenContext: ScreenContainerContext,
    onOpenGameEditorClick: (GameEntity) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val onBackClick = screenContext.onBackClick
    val onNavigate = screenContext.onNavigate
    val inDbProvider = screenContext.inDbProvider
    val trainingTemplateService = remember(inDbProvider) { inDbProvider.createTrainingTemplateService() }
    var loadState by remember { mutableStateOf(TrainingTemplateLoadState()) }
    var templateSaveSuccess by remember { mutableStateOf<TrainingTemplateSaveSuccess?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(templateId) {
        loadState = loadEditTrainingTemplateState(
            inDbProvider = inDbProvider,
            trainingTemplateService = trainingTemplateService,
            templateId = templateId,
        )
    }

    RenderMissingTemplateDialog(
        visible = loadState.templateLoadFailed,
        onDismiss = {
            loadState = loadState.copy(templateLoadFailed = false)
            onNavigate(ScreenType.TrainingTemplates)
        },
    )

    RenderEditTrainingTemplateSaveSuccessDialog(
        success = templateSaveSuccess,
        onDismiss = {
            templateSaveSuccess = null
            onNavigate(ScreenType.TrainingTemplates)
        },
    )

    EditTrainingTemplateScreen(
        initialTemplateName = loadState.templateName,
        gamesForTemplate = loadState.gamesForTemplate,
        onBackClick = onBackClick,
        onNavigate = onNavigate,
        onOpenGameEditorClick = createOpenEditTrainingTemplateGameEditorAction(
            allGamesById = loadState.allGamesById,
            onOpenGameEditorClick = onOpenGameEditorClick,
        ),
        onSaveTemplate = { templateName, editableGames, onSaved ->
            scope.launch {
                val saveResult = saveEditedTrainingTemplate(
                    trainingTemplateService = trainingTemplateService,
                    templateId = templateId,
                    templateName = templateName,
                    editableGames = editableGames,
                ) ?: return@launch

                onSaved?.invoke()
                if (saveResult.first == TrainingTemplateSaveResult.DELETED) {
                    onNavigate(ScreenType.TrainingTemplates)
                    return@launch
                }

                templateSaveSuccess = saveResult.second
            }
        },
        modifier = modifier,
    )
}

private val EditTrainingTemplateScreenStrings = TrainingCollectionEditorStrings(
    screenTitle = "Edit Template",
    collectionNameLabel = "Template Name",
    collectionNamePlaceholder = DEFAULT_TEMPLATE_NAME,
    gamesCountLabel = "Games in template",
)

@Composable
fun EditTrainingTemplateScreen(
    initialTemplateName: String = DEFAULT_TEMPLATE_NAME,
    gamesForTemplate: List<TrainingGameEditorItem> = emptyList(),
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onOpenGameEditorClick: (Long) -> Unit = {},
    onSaveTemplate: (String, List<TrainingGameEditorItem>, (() -> Unit)?) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
) {
    var selectedNavItem by remember { mutableStateOf<ScreenType>(ScreenType.Home) }
    var hasUserSelectedGame by remember { mutableStateOf(false) }
    var editorState by remember(initialTemplateName, gamesForTemplate) {
        mutableStateOf(
            CreateTrainingEditorState(
                trainingName = initialTemplateName,
                editableGamesForTraining = gamesForTemplate,
            )
        )
    }
    var savedTemplateName by remember(initialTemplateName) { mutableStateOf(initialTemplateName) }
    var savedGamesForTemplate by remember(gamesForTemplate) { mutableStateOf(gamesForTemplate) }
    var pendingLeaveAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val boardSession = rememberTrainingEditorBoardSession(editorState.editableGamesForTraining)
    val selectedGame = editorState.editableGamesForTraining.firstOrNull { game ->
        game.gameId == boardSession.selectedGameId
    }

    fun hasUnsavedChanges(): Boolean {
        return hasUnsavedTrainingEditorChanges(
            editorState = editorState,
            initialTrainingName = savedTemplateName,
            initialGamesForTraining = savedGamesForTemplate,
            defaultName = DEFAULT_TEMPLATE_NAME,
        )
    }

    fun updateSavedState() {
        savedTemplateName = normalizeTrainingEditorName(
            trainingName = editorState.trainingName,
            defaultName = DEFAULT_TEMPLATE_NAME,
        )
        savedGamesForTemplate = editorState.editableGamesForTraining
    }

    fun saveTemplate(afterSave: (() -> Unit)? = null) {
        onSaveTemplate(
            editorState.trainingName,
            editorState.editableGamesForTraining,
        ) {
            updateSavedState()
            afterSave?.invoke()
        }
    }

    fun requestLeave(action: () -> Unit) {
        if (!hasUnsavedChanges()) {
            action()
            return
        }

        pendingLeaveAction = action
    }

    fun removeGameFromTemplate(gameId: Long) {
        val nextSelectedGameId = resolveNextSelectedTrainingGameId(
            games = editorState.editableGamesForTraining,
            removedGameId = gameId,
        )
        editorState = editorState.copy(
            editableGamesForTraining = removeTrainingGame(
                games = editorState.editableGamesForTraining,
                gameId = gameId,
            )
        )

        if (nextSelectedGameId == null) {
            hasUserSelectedGame = false
            return
        }

        hasUserSelectedGame = true
        boardSession.onSelectGame(nextSelectedGameId)
    }

    LaunchedEffect(initialTemplateName, gamesForTemplate) {
        editorState = editorState.copy(
            trainingName = initialTemplateName,
            editableGamesForTraining = gamesForTemplate,
        )
        savedTemplateName = initialTemplateName
        savedGamesForTemplate = gamesForTemplate
        pendingLeaveAction = null
    }

    RenderUnsavedTrainingChangesDialog(
        pendingLeaveAction = pendingLeaveAction,
        onDismiss = { pendingLeaveAction = null },
        onSaveClick = {
            val leaveAction = pendingLeaveAction ?: return@RenderUnsavedTrainingChangesDialog
            saveTemplate {
                pendingLeaveAction = null
                leaveAction()
            }
        },
        onDiscardClick = {
            val leaveAction = pendingLeaveAction ?: return@RenderUnsavedTrainingChangesDialog
            pendingLeaveAction = null
            leaveAction()
        },
    )

    BackHandler {
        requestLeave(onBackClick)
    }

    val autoScrollToGameIndex = if (hasUserSelectedGame) {
        editorState.editableGamesForTraining.indexOfFirst { game ->
            game.gameId == boardSession.selectedGameId
        }.takeIf { it >= 0 }
    } else {
        null
    }

    TrainingCollectionEditorScreen(
        strings = EditTrainingTemplateScreenStrings,
        collectionName = editorState.trainingName,
        onCollectionNameChange = { editorState = editorState.copy(trainingName = it) },
        games = editorState.editableGamesForTraining,
        selectedNavItem = selectedNavItem,
        onBackClick = {
            requestLeave(onBackClick)
        },
        onSaveClick = {
            saveTemplate()
        },
        onNavigate = { screenType ->
            requestLeave {
                selectedNavItem = screenType
                onNavigate(screenType)
            }
        },
        modifier = modifier,
        autoScrollToGameIndex = autoScrollToGameIndex,
        topBarActions = {
            TrainingCollectionRemoveAction(
                selectedGame = selectedGame,
                collectionLabel = "template",
                onConfirmRemove = ::removeGameFromTemplate,
            )
        },
    ) { game ->
        val parsedGame = boardSession.parsedGamesById[game.gameId]
        val isSelected = boardSession.selectedGameId == game.gameId

        TrainingEditorGameSection(
            state = TrainingEditorGameSectionState(
                game = game,
                parsedGame = parsedGame,
                isSelected = isSelected,
                gameController = boardSession.gameController,
                currentPly = if (isSelected) boardSession.gameController.currentMoveIndex else 0,
            ),
            actions = TrainingEditorGameSectionActions(
                onDecreaseWeightClick = {
                    editorState = editorState.copy(
                        editableGamesForTraining = decreaseTrainingGameWeight(
                            games = editorState.editableGamesForTraining,
                            gameId = game.gameId,
                        )
                    )
                },
                onIncreaseWeightClick = {
                    editorState = editorState.copy(
                        editableGamesForTraining = increaseTrainingGameWeight(
                            games = editorState.editableGamesForTraining,
                            gameId = game.gameId,
                        )
                    )
                },
                onSelect = {
                    hasUserSelectedGame = true
                    boardSession.onSelectGame(game.gameId)
                },
                onPrevClick = { boardSession.gameController.undoMove() },
                onNextClick = { boardSession.gameController.redoMove() },
                onResetClick = { boardSession.onResetSelectedGame(game.gameId) },
                onEditGameClick = {
                    requestLeave {
                        onOpenGameEditorClick(game.gameId)
                    }
                },
                onMovePlyClick = { ply -> boardSession.onMoveToPly(game.gameId, ply) },
            ),
        )
    }
}
