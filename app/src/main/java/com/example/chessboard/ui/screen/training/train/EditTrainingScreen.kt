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
import com.example.chessboard.ui.screen.training.common.DEFAULT_TRAINING_NAME
import com.example.chessboard.ui.screen.training.common.TrainingCollectionEditorBarsFactory
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.chessboard.runtimecontext.RuntimeContext
import com.example.chessboard.runtimecontext.TrainingRuntimeContext
import com.example.chessboard.entity.LineEntity
import com.example.chessboard.ui.components.AppConfirmDialog
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
        title = "Training Not Found",
        message = "The selected training is unavailable.",
        onDismiss = onDismiss
    )
}

@Composable
private fun RenderEditTrainingSaveSuccessDialog(
    success: TrainingSaveSuccess?,
    onDismiss: () -> Unit
) {
    val currentSuccess = success ?: return

    AppMessageDialog(
        title = "Training Updated",
        message = buildString {
            appendLine("ID: ${currentSuccess.trainingId}")
            appendLine("Name: ${currentSuccess.trainingName}")
            append("Lines in training: ")
            append(currentSuccess.linesCount)
        },
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
    var loadState by remember { mutableStateOf(TrainingLoadState()) }
    var trainingSaveSuccess by remember { mutableStateOf<TrainingSaveSuccess?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(trainingId) {
        loadState = loadEditTrainingState(
            inDbProvider = inDbProvider,
            trainingService = trainingService,
            trainingId = trainingId
        )
    }

    RenderMissingTrainingDialog(
        visible = loadState.trainingLoadFailed,
        onDismiss = {
            loadState = loadState.copy(trainingLoadFailed = false)
            onNavigate(ScreenType.Training)
        }
    )

    RenderEditTrainingSaveSuccessDialog(
        success = trainingSaveSuccess,
        onDismiss = {
            trainingSaveSuccess = null
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
        onSaveTraining = { trainingName, editableLines, showSuccessMessage, onSaved ->
            scope.launch {
                val saveSuccess = saveEditedTraining(
                    trainingService = trainingService,
                    trainingId = trainingId,
                    trainingName = trainingName,
                    editableLines = editableLines
                ) ?: return@launch

                onSaved?.invoke()
                if (!showSuccessMessage) {
                    return@launch
                }

                trainingSaveSuccess = saveSuccess
            }
        },
        modifier = modifier
    )
}

private val EditTrainingScreenStrings = TrainingCollectionEditorStrings(
    screenTitle = "Edit Training",
    collectionNameLabel = "Training Name",
    collectionNamePlaceholder = DEFAULT_TRAINING_NAME,
    linesCountLabel = "Lines in training",
)


@Composable
fun EditTrainingScreen(
    trainingId: Long,
    initialTrainingName: String = DEFAULT_TRAINING_NAME,
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
    var selectedNavItem by remember { mutableStateOf<ScreenType>(ScreenType.Training) }
    var editorState by remember(initialTrainingName, linesForTraining) {
        mutableStateOf(
            CreateTrainingEditorState(
                trainingName = initialTrainingName,
                editableLinesForTraining = linesForTraining
            )
        )
    }
    var savedTrainingName by remember(initialTrainingName) { mutableStateOf(initialTrainingName) }
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
        return trainingRuntimeContext.selectedLineId(trainingId)
            ?: orderedLinesForTraining.firstOrNull()?.lineId
    }

    val boardSession = rememberTrainingEditorBoardSession(
        lines = orderedLinesForTraining,
        initialSelectedLineId = resolveSelectedLineId(),
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
            initialLinesForTraining = savedLinesForTraining
        )
    }

    fun updateSavedState() {
        savedTrainingName = normalizeTrainingEditorName(editorState.trainingName)
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
            return
        }

        trainingRuntimeContext.setSelectedLineId(trainingId, nextSelectedLineId)
        boardSession.onSelectLine(nextSelectedLineId)
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

    LaunchedEffect(initialTrainingName, linesForTraining) {
        editorState = editorState.copy(
            trainingName = initialTrainingName,
            editableLinesForTraining = linesForTraining
        )
        savedTrainingName = initialTrainingName
        savedLinesForTraining = linesForTraining
        pendingLeaveAction = null
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
    }

    // Mirror runtime selection into the preview-board session so the
    // visible board and move controls always follow the selected line.
    LaunchedEffect(resolveSelectedLineId()) {
        val lineId = resolveSelectedLineId() ?: return@LaunchedEffect
        if (boardSession.selectedLineId == lineId) {
            return@LaunchedEffect
        }

        boardSession.onSelectLine(lineId)
    }

    RenderUnsavedTrainingChangesDialog(
        pendingLeaveAction = pendingLeaveAction,
        onDismiss = { pendingLeaveAction = null },
        onSaveClick = {
            val leaveAction = pendingLeaveAction ?: return@RenderUnsavedTrainingChangesDialog
            saveTraining {
                pendingLeaveAction = null
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
            title = "Remove Line",
            message = "Remove \"${lineToRemove.title}\" from training?",
            onDismiss = { pendingRemoveLine = null },
            onConfirm = {
                pendingRemoveLine = null
                removeLineFromTraining(lineToRemove.lineId)
            },
            confirmText = "Remove",
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
        deleteContentDescription = "Remove line from training",
        canUndo = canUndo,
        onPrevClick = { boardSession.lineController.undoMove() },
        canRedo = canRedo,
        onNextClick = { boardSession.lineController.redoMove() },
    )
        .addTopBarAction {
            SettingsIconButton(
                onClick = onOpenSettingsClick,
                contentDescription = "Training settings",
            )
        }
        .addBottomBarAction(
            label = "Start",
            enabled = selectedLine != null,
            onClick = ::startSelectedLineTraining,
            index = 2,
        ) { isEnabled ->
            IconMd(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Start training",
                tint = if (isEnabled) {
                    BottomBarContentColor
                } else {
                    BottomBarContentColor.copy(alpha = 0.5f)
                },
            )
        }

    TrainingCollectionEditorScreen(
        strings = EditTrainingScreenStrings,
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
                },
                onMovePlyClick = { ply ->
                    trainingRuntimeContext.setSelectedLineId(trainingId, line.lineId)
                    boardSession.onMoveToPly(line.lineId, ply)
                },
            ),
        )
    }
}
