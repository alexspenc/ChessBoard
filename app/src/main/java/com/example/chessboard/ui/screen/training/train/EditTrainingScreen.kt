package com.example.chessboard.ui.screen.training.train

/**
 * File role: groups the training-specific editor screen and its launch orchestration.
 * Allowed here:
 * - screen state, unsaved-changes flow, and training launch wiring for one training
 * - screen-specific callbacks that connect the editor to navigation and runtime context
 * Not allowed here:
 * - reusable generic editor UI that belongs in shared training/common files
 * - persistence helpers unrelated to this concrete screen flow
 * Validation date: 2026-04-25
 */
import com.example.chessboard.ui.screen.training.common.CreateTrainingEditorState
import com.example.chessboard.ui.screen.training.common.TrainingCollectionEditorBarsFactory
import com.example.chessboard.ui.screen.training.common.trainingCollectionEditorBarStrings
import com.example.chessboard.ui.screen.training.common.TrainingCollectionEditorScreen
import com.example.chessboard.ui.screen.training.common.TrainingCollectionEditorStrings
import com.example.chessboard.ui.screen.training.common.TrainingEditorLineSection
import com.example.chessboard.ui.screen.training.common.TrainingEditorLineSectionActions
import com.example.chessboard.ui.screen.training.common.TrainingEditorLineSectionState
import com.example.chessboard.ui.screen.training.common.TrainingLineEditorItem
import com.example.chessboard.ui.screen.training.common.decreaseTrainingLineWeight
import com.example.chessboard.ui.screen.training.common.increaseTrainingLineWeight
import com.example.chessboard.ui.screen.training.common.removeTrainingLine
import com.example.chessboard.ui.screen.training.common.resolveNextSelectedTrainingLineId
import com.example.chessboard.ui.screen.training.common.rememberTrainingEditorBoardSession

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.runtimecontext.RuntimeContext
import com.example.chessboard.runtimecontext.TrainingRuntimeContext
import com.example.chessboard.entity.LineEntity
import com.example.chessboard.ui.components.AppConfirmDialog
import com.example.chessboard.ui.components.AppLoadingDialog
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.SettingsIconButton
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.screen.training.loadsave.RenderUnsavedTrainingChangesDialog
import com.example.chessboard.ui.screen.training.loadsave.TrainingLoadState
import com.example.chessboard.ui.screen.training.loadsave.TrainingSaveSuccess
import com.example.chessboard.ui.screen.training.loadsave.hasUnsavedTrainingEditorChanges
import com.example.chessboard.ui.screen.training.loadsave.loadEditTrainingState
import com.example.chessboard.ui.screen.training.loadsave.normalizeTrainingEditorName
import com.example.chessboard.ui.screen.training.loadsave.saveEditedTraining
import com.example.chessboard.ui.theme.BottomBarContentColor
import kotlinx.coroutines.launch

@Composable
private fun RenderMissingTrainingDialog(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    if (!visible) {
        return
    }

    AppMessageDialog(
        title = stringResource(R.string.edit_training_not_found_title),
        message = stringResource(R.string.edit_training_not_found_message),
        onDismiss = onDismiss
    )
}

private sealed interface EditTrainingSaveDialogState {
    data object Saving : EditTrainingSaveDialogState

    data class Success(
        val success: TrainingSaveSuccess
    ) : EditTrainingSaveDialogState
}

@Composable
private fun RenderEditTrainingSaveDialog(
    state: EditTrainingSaveDialogState?,
    onDismiss: () -> Unit
) {
    val currentState = state ?: return
    if (currentState is EditTrainingSaveDialogState.Saving) {
        AppLoadingDialog(
            title = stringResource(R.string.edit_training_saving_title),
            message = stringResource(R.string.edit_training_saving_message)
        )
        return
    }

    val currentSuccess = (currentState as EditTrainingSaveDialogState.Success).success
    AppMessageDialog(
        title = stringResource(R.string.edit_training_updated_title),
        message = stringResource(
            R.string.edit_training_updated_message,
            currentSuccess.trainingId,
            currentSuccess.trainingName,
            currentSuccess.linesCount,
        ),
        onDismiss = onDismiss
    )
}

private fun createOpenEditTrainingLineEditorAction(
    allLinesById: Map<Long, LineEntity>,
    onOpenLineEditorClick: (LineEntity) -> Unit
): (Long) -> Unit {
    return openLineEditor@{ lineId ->
        val line = allLinesById[lineId] ?: return@openLineEditor
        onOpenLineEditorClick(line)
    }
}

@Composable
fun EditTrainingScreenContainer(
    trainingId: Long,
    screenContext: ScreenContainerContext,
    orderLinesInTraining: RuntimeContext.OrderLinesInTraining,
    trainingRuntimeContext: TrainingRuntimeContext,
    hideLinesWithWeightZero: Boolean = false,
    simpleViewEnabled: Boolean = false,
    onStartLineTrainingClick: (Long, List<Long>) -> Unit,
    onOpenLineEditorClick: (LineEntity) -> Unit,
    onOpenSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onBackClick = screenContext.onBackClick
    val onNavigate = screenContext.onNavigate
    val inDbProvider = screenContext.inDbProvider
    val trainingService = remember(inDbProvider) { inDbProvider.createTrainingService() }
    val defaultTrainingName = stringResource(R.string.edit_training_default_name)
    val unnamedOpeningName = stringResource(R.string.training_line_unnamed_opening)
    var loadState by remember(defaultTrainingName) {
        mutableStateOf(TrainingLoadState(trainingName = defaultTrainingName))
    }
    var trainingSaveDialogState by remember {
        mutableStateOf<EditTrainingSaveDialogState?>(null)
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(trainingId, defaultTrainingName, unnamedOpeningName) {
        loadState = loadEditTrainingState(
            inDbProvider = inDbProvider,
            trainingService = trainingService,
            trainingId = trainingId,
            defaultTrainingName = defaultTrainingName,
            unnamedOpeningName = unnamedOpeningName,
        )
    }

    RenderMissingTrainingDialog(
        visible = loadState.trainingLoadFailed,
        onDismiss = {
            loadState = loadState.copy(trainingLoadFailed = false)
            onNavigate(ScreenType.Training)
        }
    )

    RenderEditTrainingSaveDialog(
        state = trainingSaveDialogState,
        onDismiss = {
            trainingSaveDialogState = null
        }
    )

    val visibleLinesForTraining = if (hideLinesWithWeightZero) {
        loadState.linesForTraining.filter { it.weight > 0 }
    } else {
        loadState.linesForTraining
    }

    EditTrainingScreen(
        trainingId = trainingId,
        initialTrainingName = loadState.trainingName,
        linesForTraining = visibleLinesForTraining,
        orderLinesInTraining = orderLinesInTraining,
        trainingRuntimeContext = trainingRuntimeContext,
        simpleViewEnabled = simpleViewEnabled,
        onBackClick = onBackClick,
        onNavigate = onNavigate,
        onStartLineTrainingClick = onStartLineTrainingClick,
        onOpenSettingsClick = onOpenSettingsClick,
        onOpenLineEditorClick = createOpenEditTrainingLineEditorAction(
            allLinesById = loadState.allLinesById,
            onOpenLineEditorClick = onOpenLineEditorClick
        ),
        onSaveTraining = saveTraining@{ trainingName, editableLines, showSuccessMessage, onSaved ->
            if (trainingSaveDialogState is EditTrainingSaveDialogState.Saving) {
                return@saveTraining
            }

            trainingSaveDialogState = EditTrainingSaveDialogState.Saving
            scope.launch {
                val saveSuccess = saveEditedTraining(
                    trainingService = trainingService,
                    trainingId = trainingId,
                    trainingName = trainingName,
                    editableLines = editableLines,
                    defaultTrainingName = defaultTrainingName,
                ) ?: run {
                    trainingSaveDialogState = null
                    return@launch
                }

                if (!showSuccessMessage) {
                    trainingSaveDialogState = null
                    onSaved?.invoke()
                    return@launch
                }

                onSaved?.invoke()
                trainingSaveDialogState = EditTrainingSaveDialogState.Success(saveSuccess)
            }
        },
        modifier = modifier
    )
}

@Composable
fun EditTrainingScreen(
    trainingId: Long,
    initialTrainingName: String = "",
    linesForTraining: List<TrainingLineEditorItem> = emptyList(),
    orderLinesInTraining: RuntimeContext.OrderLinesInTraining,
    trainingRuntimeContext: TrainingRuntimeContext,
    simpleViewEnabled: Boolean = false,
    onBackClick: () -> Unit,
    onNavigate: (ScreenType) -> Unit,
    onStartLineTrainingClick: (Long, List<Long>) -> Unit,
    onOpenLineEditorClick: (Long) -> Unit,
    onOpenSettingsClick: () -> Unit,
    onSaveTraining: (String, List<TrainingLineEditorItem>, Boolean, (() -> Unit)?) -> Unit,
    modifier: Modifier = Modifier
) {
    val defaultTrainingName = stringResource(R.string.edit_training_default_name)
    val resolvedInitialTrainingName = initialTrainingName.ifBlank { defaultTrainingName }
    val editorStrings = TrainingCollectionEditorStrings(
        screenTitle = stringResource(R.string.edit_training_title),
        collectionNameLabel = stringResource(R.string.edit_training_name_label),
        collectionNamePlaceholder = defaultTrainingName,
        linesCountLabel = stringResource(R.string.edit_training_lines_in_training),
    )
    var selectedNavItem by remember { mutableStateOf<ScreenType>(ScreenType.Training) }
    var editorState by remember(resolvedInitialTrainingName, linesForTraining) {
        mutableStateOf(
            CreateTrainingEditorState(
                trainingName = resolvedInitialTrainingName,
                editableLinesForTraining = linesForTraining
            )
        )
    }
    var savedTrainingName by remember(resolvedInitialTrainingName) {
        mutableStateOf(resolvedInitialTrainingName)
    }
    var savedLinesForTraining by remember(linesForTraining) { mutableStateOf(linesForTraining) }
    var pendingLeaveAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingRemoveLine by remember { mutableStateOf<TrainingLineEditorItem?>(null) }
    val orderedLineIds = remember(editorState.editableLinesForTraining) {
        orderLinesInTraining.orderLines(
            lines = editorState.editableLinesForTraining,
            getLineId = { line -> line.lineId },
            getWeight = { line -> line.weight }
        ).map { it.lineId }
    }
    val currentLinesById = editorState.editableLinesForTraining.associateBy { it.lineId }
    val orderedLinesForTraining = orderedLineIds.mapNotNull { currentLinesById[it] }

    fun resolveSelectedLineId(): Long? {
        return trainingRuntimeContext.editorSelectedLineId(trainingId)
            ?: orderedLinesForTraining.firstOrNull()?.lineId
    }

    var selectedPly by remember(trainingId) { mutableIntStateOf(0) }
    var selectionRevision by remember(trainingId) { mutableIntStateOf(0) }

    val boardSession = rememberTrainingEditorBoardSession(
        lines = orderedLinesForTraining,
        selectedLineId = resolveSelectedLineId(),
        selectedPly = selectedPly,
        selectionRevision = selectionRevision,
    )
    val selectedLine = orderedLinesForTraining.firstOrNull { line ->
        line.lineId == resolveSelectedLineId()
    }
    @Suppress("UNUSED_VARIABLE")
    val boardState = boardSession.lineController.boardState
    val canUndo = selectedLine != null && boardSession.lineController.canUndo
    val canRedo = selectedLine != null && boardSession.lineController.canRedo

    fun hasUnsavedChanges(): Boolean {
        return hasUnsavedTrainingEditorChanges(
            editorState = editorState,
            initialTrainingName = savedTrainingName,
            initialLinesForTraining = savedLinesForTraining,
            defaultName = defaultTrainingName,
        )
    }

    fun updateSavedState() {
        savedTrainingName = normalizeTrainingEditorName(
            trainingName = editorState.trainingName,
            defaultName = defaultTrainingName,
        )
        savedLinesForTraining = editorState.editableLinesForTraining
    }

    fun saveTraining(
        showSuccessMessage: Boolean = false,
        afterSave: (() -> Unit)? = null
    ) {
        onSaveTraining(
            editorState.trainingName,
            editorState.editableLinesForTraining,
            showSuccessMessage
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

    fun removeLineFromTraining(lineId: Long) {
        val nextSelectedLineId = resolveNextSelectedTrainingLineId(
            lines = editorState.editableLinesForTraining,
            removedLineId = lineId,
        )
        editorState = editorState.copy(
            editableLinesForTraining = removeTrainingLine(
                lines = editorState.editableLinesForTraining,
                lineId = lineId
            )
        )

        if (nextSelectedLineId == null) {
            trainingRuntimeContext.setSelectedLineId(trainingId, null)
            selectedPly = 0
            selectionRevision++
            return
        }

        trainingRuntimeContext.setSelectedLineId(trainingId, nextSelectedLineId)
        selectedPly = 0
        selectionRevision++
    }

    fun withSelectedLine(action: (TrainingLineEditorItem) -> Unit) {
        val lineId = resolveSelectedLineId() ?: return
        val line = currentLinesById[lineId] ?: return
        action(line)
    }

    fun openSelectedLineEditor() {
        withSelectedLine { line ->
            requestLeave { onOpenLineEditorClick(line.lineId) }
        }
    }

    fun removeSelectedLine() {
        withSelectedLine { line ->
            pendingRemoveLine = line
        }
    }

    fun startSelectedLineTraining() {
        withSelectedLine { line ->
            requestLeave {
                onStartLineTrainingClick(line.lineId, orderedLineIds)
            }
        }
    }

    LaunchedEffect(resolvedInitialTrainingName, linesForTraining) {
        editorState = editorState.copy(
            trainingName = resolvedInitialTrainingName,
            editableLinesForTraining = linesForTraining
        )
        savedTrainingName = resolvedInitialTrainingName
        savedLinesForTraining = linesForTraining
        pendingLeaveAction = null
        selectedPly = 0
        selectionRevision++
    }

    // If the previously selected line disappeared after list changes
    // (for example after removal or reordering), move selection to the
    // first remaining line so editor actions always point to a valid line.
    LaunchedEffect(trainingId, orderedLineIds) {
        if (resolveSelectedLineId() in orderedLineIds) {
            return@LaunchedEffect
        }

        trainingRuntimeContext.setSelectedLineId(
            trainingId = trainingId,
            lineId = orderedLinesForTraining.firstOrNull()?.lineId,
        )
        selectedPly = 0
        selectionRevision++
    }

    RenderUnsavedTrainingChangesDialog(
        pendingLeaveAction = pendingLeaveAction,
        onDismiss = { pendingLeaveAction = null },
        onSaveClick = {
            val leaveAction = pendingLeaveAction ?: return@RenderUnsavedTrainingChangesDialog
            pendingLeaveAction = null
            saveTraining {
                leaveAction()
            }
        },
        onDiscardClick = {
            val leaveAction = pendingLeaveAction ?: return@RenderUnsavedTrainingChangesDialog
            pendingLeaveAction = null
            leaveAction()
        }
    )

    pendingRemoveLine?.let { lineToRemove ->
        AppConfirmDialog(
            title = stringResource(R.string.edit_training_remove_line_title),
            message = stringResource(
                R.string.edit_training_remove_line_message,
                lineToRemove.title,
            ),
            onDismiss = { pendingRemoveLine = null },
            onConfirm = {
                pendingRemoveLine = null
                removeLineFromTraining(lineToRemove.lineId)
            },
            confirmText = stringResource(R.string.common_remove),
            isDestructive = true,
        )
    }

    BackHandler {
        requestLeave(onBackClick)
    }

    fun resolveAutoScrollToLineIndex(): Int? {
        return orderedLinesForTraining.indexOfFirst { it.lineId == resolveSelectedLineId() }
            .takeIf { it >= 0 }
    }

    val editorBars = TrainingCollectionEditorBarsFactory(
        onHomeClick = { requestLeave { onNavigate(ScreenType.Home) } },
        hasSelection = selectedLine != null,
        onEditClick = ::openSelectedLineEditor,
        onDeleteClick = ::removeSelectedLine,
        strings = trainingCollectionEditorBarStrings(
            deleteContentDescription = stringResource(
                R.string.edit_training_remove_line_content_description,
            ),
        ),
        canUndo = canUndo,
        onPrevClick = { boardSession.lineController.undoMove() },
        canRedo = canRedo,
        onNextClick = { boardSession.lineController.redoMove() },
    )
        .addTopBarAction {
            SettingsIconButton(
                onClick = onOpenSettingsClick,
                contentDescription = stringResource(
                    R.string.edit_training_settings_content_description,
                ),
            )
        }
        .addBottomBarAction(
            label = stringResource(R.string.edit_training_start_action),
            enabled = selectedLine != null,
            onClick = ::startSelectedLineTraining,
            index = 2,
        ) { isEnabled ->
            IconMd(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = stringResource(
                    R.string.edit_training_start_content_description,
                ),
                tint = if (isEnabled) {
                    BottomBarContentColor
                } else {
                    BottomBarContentColor.copy(alpha = 0.5f)
                },
            )
        }

    TrainingCollectionEditorScreen(
        strings = editorStrings,
        collectionName = editorState.trainingName,
        onCollectionNameChange = { editorState = editorState.copy(trainingName = it) },
        lines = orderedLinesForTraining,
        selectedNavItem = selectedNavItem,
        onBackClick = {
            requestLeave(onBackClick)
        },
        onSaveClick = {
            saveTraining(showSuccessMessage = true)
        },
        onNavigate = { screenType ->
            requestLeave {
                selectedNavItem = screenType
                onNavigate(screenType)
            }
        },
        modifier = modifier,
        simpleViewEnabled = simpleViewEnabled,
        autoScrollToLineIndex = resolveAutoScrollToLineIndex(),
        bottomBarOverride = editorBars.buildBottomBar(),
        topBarActions = editorBars.buildTopBarActions(),
    ) { line ->
        val parsedLine = boardSession.parsedLinesById[line.lineId]
        val isSelected = resolveSelectedLineId() == line.lineId

        TrainingEditorLineSection(
            state = TrainingEditorLineSectionState(
                line = line,
                parsedLine = parsedLine,
                isSelected = isSelected,
                lineController = boardSession.lineController,
                simpleViewEnabled = simpleViewEnabled,
            ),
            actions = TrainingEditorLineSectionActions(
                onDecreaseWeightClick = {
                    editorState = editorState.copy(
                        editableLinesForTraining = decreaseTrainingLineWeight(
                            lines = editorState.editableLinesForTraining,
                            lineId = line.lineId
                        )
                    )
                },
                onIncreaseWeightClick = {
                    editorState = editorState.copy(
                        editableLinesForTraining = increaseTrainingLineWeight(
                            lines = editorState.editableLinesForTraining,
                            lineId = line.lineId
                        )
                    )
                },
                onSelect = {
                    trainingRuntimeContext.setSelectedLineId(trainingId, line.lineId)
                    selectedPly = 0
                    selectionRevision++
                },
                onMovePlyClick = { ply ->
                    trainingRuntimeContext.setSelectedLineId(trainingId, line.lineId)
                    selectedPly = ply
                    selectionRevision++
                },
            ),
        )
    }
}
