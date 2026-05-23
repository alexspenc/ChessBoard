package com.example.chessboard.ui.screen.training.template

/*
 * Screen-level UI for editing a training template.
 *
 * Keep only the template editor scaffold, local screen state, and wiring for
 * shared training editor components here. Do not add database loading, save
 * orchestration, or route-level navigation setup to this file.
 */

import com.example.chessboard.ui.screen.training.common.CreateTrainingEditorState
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.chessboard.entity.LineEntity
import com.example.chessboard.runtimecontext.LineCollectionRuntimeContext
import com.example.chessboard.ui.components.AppConfirmDialog
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
            append("Lines in template: ")
            append(currentSuccess.linesCount)
        },
        onDismiss = onDismiss,
    )
}

private fun createOpenEditTrainingTemplateLineEditorAction(
    allLinesById: Map<Long, LineEntity>,
    onOpenLineEditorClick: (LineEntity) -> Unit,
): (Long) -> Unit {
    return openLineEditor@{ lineId ->
        val line = allLinesById[lineId] ?: return@openLineEditor
        onOpenLineEditorClick(line)
    }
}

@Composable
fun EditTrainingTemplateScreenContainer(
    templateId: Long,
    screenContext: ScreenContainerContext,
    onOpenLineEditorClick: (LineEntity) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val onBackClick = screenContext.onBackClick
    val onNavigate = screenContext.onNavigate
    val inDbProvider = screenContext.inDbProvider
    val lineCollectionRuntimeContext = screenContext.runtimeContext.lineCollections
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
        linesForTemplate = loadState.linesForTemplate,
        selectionCollectionId = templateId,
        lineCollectionRuntimeContext = lineCollectionRuntimeContext,
        onBackClick = onBackClick,
        onNavigate = onNavigate,
        onOpenLineEditorClick = createOpenEditTrainingTemplateLineEditorAction(
            allLinesById = loadState.allLinesById,
            onOpenLineEditorClick = onOpenLineEditorClick,
        ),
        onSaveTemplate = { templateName, editableLines, onSaved ->
            scope.launch {
                val saveResult = saveEditedTrainingTemplate(
                    trainingTemplateService = trainingTemplateService,
                    templateId = templateId,
                    templateName = templateName,
                    editableLines = editableLines,
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
    linesCountLabel = "Lines in template",
)

@Composable
fun EditTrainingTemplateScreen(
    initialTemplateName: String = DEFAULT_TEMPLATE_NAME,
    linesForTemplate: List<TrainingLineEditorItem> = emptyList(),
    selectionCollectionId: Long = 0L,
    lineCollectionRuntimeContext: LineCollectionRuntimeContext? = null,
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onOpenLineEditorClick: (Long) -> Unit = {},
    onSaveTemplate: (String, List<TrainingLineEditorItem>, (() -> Unit)?) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
) {
    var selectedNavItem by remember { mutableStateOf<ScreenType>(ScreenType.Training) }
    var hasUserSelectedLine by remember { mutableStateOf(false) }
    val resolvedLineCollectionRuntimeContext = lineCollectionRuntimeContext ?: remember {
        LineCollectionRuntimeContext()
    }
    var editorState by remember(initialTemplateName, linesForTemplate) {
        mutableStateOf(
            CreateTrainingEditorState(
                trainingName = initialTemplateName,
                editableLinesForTraining = linesForTemplate,
            )
        )
    }
    var savedTemplateName by remember(initialTemplateName) { mutableStateOf(initialTemplateName) }
    var savedLinesForTemplate by remember(linesForTemplate) { mutableStateOf(linesForTemplate) }
    var pendingLeaveAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingRemoveLine by remember { mutableStateOf<TrainingLineEditorItem?>(null) }
    var selectedPly by remember(selectionCollectionId) { mutableIntStateOf(0) }
    var selectionRevision by remember(selectionCollectionId) { mutableIntStateOf(0) }
    fun resolveSelectedLineId(): Long? {
        return resolvedLineCollectionRuntimeContext.resolveSelectedLineId(selectionCollectionId)
    }

    val boardSession = rememberTrainingEditorBoardSession(
        lines = editorState.editableLinesForTraining,
        selectedLineId = resolveSelectedLineId(),
        selectedPly = selectedPly,
        selectionRevision = selectionRevision,
    )
    val selectedLine = editorState.editableLinesForTraining.firstOrNull { line ->
        line.lineId == resolveSelectedLineId()
    }
    val canUndo = selectedLine != null && boardSession.lineController.canUndo
    val canRedo = selectedLine != null && boardSession.lineController.canRedo

    fun hasUnsavedChanges(): Boolean {
        return hasUnsavedTrainingEditorChanges(
            editorState = editorState,
            initialTrainingName = savedTemplateName,
            initialLinesForTraining = savedLinesForTemplate,
            defaultName = DEFAULT_TEMPLATE_NAME,
        )
    }

    fun updateSavedState() {
        savedTemplateName = normalizeTrainingEditorName(
            trainingName = editorState.trainingName,
            defaultName = DEFAULT_TEMPLATE_NAME,
        )
        savedLinesForTemplate = editorState.editableLinesForTraining
    }

    fun saveTemplate(afterSave: (() -> Unit)? = null) {
        onSaveTemplate(
            editorState.trainingName,
            editorState.editableLinesForTraining,
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

    fun removeLineFromTemplate(lineId: Long) {
        val nextSelectedLineId = resolveNextSelectedTrainingLineId(
            lines = editorState.editableLinesForTraining,
            removedLineId = lineId,
        )
        editorState = editorState.copy(
            editableLinesForTraining = removeTrainingLine(
                lines = editorState.editableLinesForTraining,
                lineId = lineId,
            )
        )
        resolvedLineCollectionRuntimeContext.setOrderedLineIds(
            collectionId = selectionCollectionId,
            lineIds = editorState.editableLinesForTraining.map { line -> line.lineId },
        )

        if (nextSelectedLineId == null) {
            hasUserSelectedLine = false
            resolvedLineCollectionRuntimeContext.setSelectedLineId(selectionCollectionId, null)
            selectedPly = 0
            selectionRevision++
            return
        }

        hasUserSelectedLine = true
        resolvedLineCollectionRuntimeContext.setSelectedLineId(selectionCollectionId, nextSelectedLineId)
        selectedPly = 0
        selectionRevision++
    }

    fun withSelectedLine(action: (TrainingLineEditorItem) -> Unit) {
        val lineId = resolveSelectedLineId() ?: return
        val line = editorState.editableLinesForTraining.firstOrNull { it.lineId == lineId } ?: return
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

    LaunchedEffect(initialTemplateName, linesForTemplate) {
        editorState = editorState.copy(
            trainingName = initialTemplateName,
            editableLinesForTraining = linesForTemplate,
        )
        savedTemplateName = initialTemplateName
        savedLinesForTemplate = linesForTemplate
        pendingLeaveAction = null
        resolvedLineCollectionRuntimeContext.setOrderedLineIds(
            collectionId = selectionCollectionId,
            lineIds = linesForTemplate.map { line -> line.lineId },
        )
        selectedPly = 0
        selectionRevision++
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

    pendingRemoveLine?.let { lineToRemove ->
        AppConfirmDialog(
            title = "Remove Line",
            message = "Remove \"${lineToRemove.title}\" from template?",
            onDismiss = { pendingRemoveLine = null },
            onConfirm = {
                pendingRemoveLine = null
                removeLineFromTemplate(lineToRemove.lineId)
            },
            confirmText = "Remove",
            isDestructive = true,
        )
    }

    BackHandler {
        requestLeave(onBackClick)
    }

    val autoScrollToLineIndex = if (hasUserSelectedLine) {
        editorState.editableLinesForTraining.indexOfFirst { line ->
            line.lineId == resolveSelectedLineId()
        }.takeIf { it >= 0 }
    } else {
        null
    }

    val editorBars = TrainingCollectionEditorBarsFactory(
        onHomeClick = { requestLeave { onNavigate(ScreenType.Home) } },
        hasSelection = selectedLine != null,
        onEditClick = ::openSelectedLineEditor,
        onDeleteClick = ::removeSelectedLine,
        deleteContentDescription = "Remove line from template",
        canUndo = canUndo,
        onPrevClick = { boardSession.lineController.undoMove() },
        canRedo = canRedo,
        onNextClick = { boardSession.lineController.redoMove() },
    )

    TrainingCollectionEditorScreen(
        strings = EditTrainingTemplateScreenStrings,
        collectionName = editorState.trainingName,
        onCollectionNameChange = { editorState = editorState.copy(trainingName = it) },
        lines = editorState.editableLinesForTraining,
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
        autoScrollToLineIndex = autoScrollToLineIndex,
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
            ),
            actions = TrainingEditorLineSectionActions(
                onDecreaseWeightClick = {
                    editorState = editorState.copy(
                        editableLinesForTraining = decreaseTrainingLineWeight(
                            lines = editorState.editableLinesForTraining,
                            lineId = line.lineId,
                        )
                    )
                },
                onIncreaseWeightClick = {
                    editorState = editorState.copy(
                        editableLinesForTraining = increaseTrainingLineWeight(
                            lines = editorState.editableLinesForTraining,
                            lineId = line.lineId,
                        )
                    )
                },
                onSelect = {
                    hasUserSelectedLine = true
                    resolvedLineCollectionRuntimeContext.setSelectedLineId(selectionCollectionId, line.lineId)
                    selectedPly = 0
                    selectionRevision++
                },
                onMovePlyClick = { ply ->
                    hasUserSelectedLine = true
                    resolvedLineCollectionRuntimeContext.setSelectedLineId(selectionCollectionId, line.lineId)
                    selectedPly = ply
                    selectionRevision++
                },
            ),
        )
    }
}
