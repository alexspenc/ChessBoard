package com.example.chessboard.ui.screen.training.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.example.chessboard.service.OneLineTrainingData
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTextField
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.DeleteIconButton
import com.example.chessboard.ui.components.HomeIconButton
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.RepeatStepIconButton
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.SecondaryButton
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.screen.training.TrainingLineRowHeight
import com.example.chessboard.ui.screen.training.TrainingLineRowSpacing
import com.example.chessboard.ui.screen.training.TrainingLinesHeaderHeight
import com.example.chessboard.ui.screen.training.TrainingLinesNavigationHeight
import com.example.chessboard.ui.screen.training.common.CreateTrainingEditorState
import com.example.chessboard.ui.screen.training.common.DEFAULT_TRAINING_NAME
import com.example.chessboard.ui.screen.training.common.TrainingLineEditorItem
import com.example.chessboard.ui.screen.training.common.decreaseTrainingLineWeight
import com.example.chessboard.ui.screen.training.common.increaseTrainingLineWeight
import com.example.chessboard.ui.screen.training.common.removeTrainingLine
import com.example.chessboard.ui.screen.training.loadsave.TrainingSaveSuccess
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TrainingAccentTeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class CreateTrainingInitialData(
    val trainingName: String = DEFAULT_TRAINING_NAME,
    val linesForTraining: List<TrainingLineEditorItem> = emptyList()
)

@Composable
internal fun CreateTrainingScreenContainer(
    screenContext: ScreenContainerContext,
    initialData: CreateTrainingInitialData,
    screenTitle: String = "Create Training",
    linesCountLabel: String = "Lines loaded for training",
    modifier: Modifier = Modifier,
) {
    var trainingSaveSuccess by remember { mutableStateOf<TrainingSaveSuccess?>(null) }
    val scope = rememberCoroutineScope()

    trainingSaveSuccess?.let { success ->
        TrainingSaveSuccessDialog(
            success = success,
            onDismiss = {
                trainingSaveSuccess = null
                screenContext.onNavigate(ScreenType.Home)
            }
        )
    }

    CreateTrainingScreen(
        editorState = CreateTrainingEditorState(
            trainingName = initialData.trainingName,
            editableLinesForTraining = initialData.linesForTraining
        ),
        screenTitle = screenTitle,
        linesCountLabel = linesCountLabel,
        onBackClick = screenContext.onBackClick,
        onNavigate = screenContext.onNavigate,
        onSaveTraining = { trainingName, editableLines ->
            scope.launch {
                val normalizedName = trainingName.ifBlank { DEFAULT_TRAINING_NAME }
                val trainingLines = editableLines.map { line ->
                    OneLineTrainingData(
                        lineId = line.lineId,
                        weight = line.weight
                    )
                }

                val savedTrainingId = withContext(Dispatchers.IO) {
                    val trainingService = screenContext.inDbProvider.createTrainingService()
                    trainingService.createTrainingFromLines(
                        name = normalizedName,
                        lines = trainingLines
                    )
                }

                trainingSaveSuccess = TrainingSaveSuccess(
                    trainingId = savedTrainingId ?: return@launch,
                    trainingName = normalizedName,
                    linesCount = editableLines.size
                )
            }
        },
        modifier = modifier
    )
}

@Composable
internal fun CreateTrainingScreen(
    editorState: CreateTrainingEditorState = CreateTrainingEditorState(),
    screenTitle: String = "Create Training",
    linesCountLabel: String = "Lines loaded for training",
    headerContent: (@Composable () -> Unit)? = null,
    topBarActions: @Composable () -> Unit = {},
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onEditorStateChange: (CreateTrainingEditorState) -> Unit = {},
    onSaveTraining: (String, List<TrainingLineEditorItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedNavItem by remember { mutableStateOf<ScreenType>(ScreenType.Training) }
    var currentEditorState by remember(editorState) {
        mutableStateOf(editorState)
    }

    LaunchedEffect(editorState) {
        currentEditorState = editorState
    }

    fun updateEditorState(newEditorState: CreateTrainingEditorState) {
        currentEditorState = newEditorState
        onEditorStateChange(newEditorState)
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = screenTitle,
                onBackClick = onBackClick,
                actions = {
                    topBarActions()
                    HomeIconButton(onClick = { onNavigate(ScreenType.Home) })
                    IconButton(
                        onClick = { onSaveTraining(currentEditorState.trainingName, currentEditorState.editableLinesForTraining) }
                    ) {
                        IconMd(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save",
                            tint = TrainingAccentTeal,
                        )
                    }
                }
            )
        },
        bottomBar = {
            CreateTrainingBottomNavigation(
                selectedItem = selectedNavItem,
                onItemSelected = {
                    selectedNavItem = it
                    onNavigate(it)
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))

            if (headerContent != null) {
                headerContent()
                Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            }

                ScreenSection {
                    AppTextField(
                        value = currentEditorState.trainingName,
                    onValueChange = { updateEditorState(currentEditorState.copy(trainingName = it)) },
                        label = "Training Name",
                        placeholder = DEFAULT_TRAINING_NAME
                    )
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceLg))

            ScreenSection {
                BodySecondaryText(text = "$linesCountLabel: ${currentEditorState.editableLinesForTraining.size}")
            }

            TrainingLinesEditorSection(
                editorState = currentEditorState,
                onEditorStateChange = ::updateEditorState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TrainingLinesEditorSection(
    editorState: CreateTrainingEditorState,
    onEditorStateChange: (CreateTrainingEditorState) -> Unit,
    modifier: Modifier = Modifier
) {
    data class TrainingLinesPageState(
        val currentPageItems: List<TrainingLineEditorItem>,
        val currentPage: Int,
        val totalPages: Int,
        val canGoPrevious: Boolean,
        val canGoNext: Boolean,
    )

    fun resolveTrainingLinesPageState(maxHeight: Dp): TrainingLinesPageState {
        val availableHeightForRows =
            (maxHeight - TrainingLinesHeaderHeight - TrainingLinesNavigationHeight)
                .coerceAtLeast(TrainingLineRowHeight)
        val trainingLineSlotHeight = TrainingLineRowHeight + TrainingLineRowSpacing
        val pageSize = (availableHeightForRows / trainingLineSlotHeight)
            .toInt()
            .coerceAtLeast(1)
        val totalPages = ((editorState.editableLinesForTraining.size + pageSize - 1) / pageSize)
            .coerceAtLeast(1)
        val safeCurrentPage = editorState.currentPage.coerceIn(0, totalPages - 1)

        return TrainingLinesPageState(
            currentPageItems = editorState.editableLinesForTraining
                .drop(safeCurrentPage * pageSize)
                .take(pageSize),
            currentPage = safeCurrentPage,
            totalPages = totalPages,
            canGoPrevious = safeCurrentPage > 0,
            canGoNext = safeCurrentPage + 1 < totalPages,
        )
    }

    BoxWithConstraints(modifier = modifier) {
        val pageState = resolveTrainingLinesPageState(maxHeight)
        ScreenSection(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppDimens.spaceLg)
        ) {
            TrainingLinesPage(
                lines = pageState.currentPageItems,
                currentPage = pageState.currentPage,
                totalPages = pageState.totalPages,
                canGoPrevious = pageState.canGoPrevious,
                canGoNext = pageState.canGoNext,
                onDecreaseWeightClick = { lineId ->
                    onEditorStateChange(
                        editorState.copy(
                            editableLinesForTraining = decreaseTrainingLineWeight(
                                lines = editorState.editableLinesForTraining,
                                lineId = lineId
                            )
                        )
                    )
                },
                onIncreaseWeightClick = { lineId ->
                    onEditorStateChange(
                        editorState.copy(
                            editableLinesForTraining = increaseTrainingLineWeight(
                                lines = editorState.editableLinesForTraining,
                                lineId = lineId
                            )
                        )
                    )
                },
                onRemoveLineClick = { lineId ->
                    onEditorStateChange(
                        editorState.copy(
                            editableLinesForTraining = removeTrainingLine(
                                lines = editorState.editableLinesForTraining,
                                lineId = lineId
                            )
                        )
                    )
                },
                onPreviousPageClick = {
                    if (!pageState.canGoPrevious) {
                        return@TrainingLinesPage
                    }

                    onEditorStateChange(editorState.copy(currentPage = pageState.currentPage - 1))
                },
                onNextPageClick = {
                    if (!pageState.canGoNext) {
                        return@TrainingLinesPage
                    }

                    onEditorStateChange(editorState.copy(currentPage = pageState.currentPage + 1))
                }
            )
        }
    }
}

@Composable
private fun TrainingLinesPage(
    lines: List<TrainingLineEditorItem>,
    currentPage: Int,
    totalPages: Int,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onDecreaseWeightClick: (Long) -> Unit,
    onIncreaseWeightClick: (Long) -> Unit,
    onRemoveLineClick: (Long) -> Unit,
    onPreviousPageClick: () -> Unit,
    onNextPageClick: () -> Unit
) {
    CardSurface(modifier = Modifier.fillMaxWidth()) {
        SectionTitleText(
            text = "Lines in Training"
        )

        Spacer(modifier = Modifier.height(AppDimens.spaceSm))

        CardMetaText(
            text = "Page ${currentPage + 1} of $totalPages",
        )

        Spacer(modifier = Modifier.height(AppDimens.spaceLg))

        if (lines.isEmpty()) {
            BodySecondaryText(text = "No lines available.")
        } else {
            lines.forEachIndexed { index, line ->
                TrainingLinePageRow(
                    line = line,
                    onDecreaseWeightClick = { onDecreaseWeightClick(line.lineId) },
                    onIncreaseWeightClick = { onIncreaseWeightClick(line.lineId) },
                    onRemoveLineClick = { onRemoveLineClick(line.lineId) },
                )
                if (index + 1 < lines.size) {
                    Spacer(modifier = Modifier.height(TrainingLineRowSpacing))
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.spaceSm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
        ) {
            SecondaryButton(
                text = "Previous",
                onClick = onPreviousPageClick,
                enabled = canGoPrevious,
                modifier = Modifier.weight(1f)
            )
            SecondaryButton(
                text = "Next",
                onClick = onNextPageClick,
                enabled = canGoNext,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TrainingLinePageRow(
    line: TrainingLineEditorItem,
    onDecreaseWeightClick: () -> Unit,
    onIncreaseWeightClick: () -> Unit,
    onRemoveLineClick: () -> Unit,
) {
    CardSurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(AppDimens.spaceMd)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                SectionTitleText(
                    text = line.title,
                )
                Spacer(modifier = Modifier.height(AppDimens.spaceXs))
                CardMetaText(
                    text = "ID: ${line.lineId}"
                )
                CardMetaText(
                    text = "Weight: ${line.weight}"
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
                ) {
                    RepeatStepIconButton(
                        icon = Icons.Default.Remove,
                        contentDescription = "Decrease line weight",
                        onStep = onDecreaseWeightClick,
                    )
                    RepeatStepIconButton(
                        icon = Icons.Default.Add,
                        contentDescription = "Increase line weight",
                        onStep = onIncreaseWeightClick,
                    )
                }
                DeleteIconButton(
                    onClick = onRemoveLineClick,
                    contentDescription = "Remove line from training",
                )
            }
        }
    }
}

@Composable
private fun TrainingSaveSuccessDialog(
    success: TrainingSaveSuccess,
    onDismiss: () -> Unit
) {
    AppMessageDialog(
        title = "Training Created",
        message = buildString {
            appendLine("ID: ${success.trainingId}")
            appendLine("Name: ${success.trainingName}")
            append("Lines added: ")
            append(success.linesCount)
        },
        onDismiss = onDismiss
    )
}

@Composable
private fun CreateTrainingBottomNavigation(
    selectedItem: ScreenType,
    onItemSelected: (ScreenType) -> Unit,
    modifier: Modifier = Modifier
) {
    AppBottomNavigation(
        items = defaultAppBottomNavigationItems(),
        selectedItem = selectedItem,
        onItemSelected = onItemSelected,
        modifier = modifier
    )
}
