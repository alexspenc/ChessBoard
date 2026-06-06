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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.example.chessboard.R
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
    val trainingName: String = "",
    val linesForTraining: List<TrainingLineEditorItem> = emptyList()
)

@Composable
internal fun CreateTrainingScreenContainer(
    screenContext: ScreenContainerContext,
    initialData: CreateTrainingInitialData,
    screenTitle: String? = null,
    linesCountLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    val defaultTrainingName = stringResource(R.string.create_training_default_name)
    val resolvedScreenTitle = screenTitle ?: stringResource(R.string.create_training_title)
    val resolvedLinesCountLabel = linesCountLabel ?: stringResource(
        R.string.create_training_lines_loaded_label,
    )
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
            trainingName = initialData.trainingName.ifBlank { defaultTrainingName },
            editableLinesForTraining = initialData.linesForTraining
        ),
        screenTitle = resolvedScreenTitle,
        linesCountLabel = resolvedLinesCountLabel,
        defaultTrainingName = defaultTrainingName,
        onBackClick = screenContext.onBackClick,
        onNavigate = screenContext.onNavigate,
        onSaveTraining = { trainingName, editableLines ->
            scope.launch {
                val normalizedName = trainingName.ifBlank { defaultTrainingName }
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
    editorState: CreateTrainingEditorState = CreateTrainingEditorState(trainingName = ""),
    screenTitle: String? = null,
    linesCountLabel: String? = null,
    defaultTrainingName: String,
    headerContent: (@Composable () -> Unit)? = null,
    topBarActions: @Composable () -> Unit = {},
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onEditorStateChange: (CreateTrainingEditorState) -> Unit = {},
    onSaveTraining: (String, List<TrainingLineEditorItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    val resolvedScreenTitle = screenTitle ?: stringResource(R.string.create_training_title)
    val resolvedLinesCountLabel = linesCountLabel ?: stringResource(
        R.string.create_training_lines_loaded_label,
    )
    val resolvedEditorState = editorState.copy(
        trainingName = editorState.trainingName.ifBlank { defaultTrainingName },
    )
    var selectedNavItem by remember { mutableStateOf<ScreenType>(ScreenType.Training) }
    var currentEditorState by remember(resolvedEditorState) {
        mutableStateOf(resolvedEditorState)
    }

    LaunchedEffect(resolvedEditorState) {
        currentEditorState = resolvedEditorState
    }

    fun updateEditorState(newEditorState: CreateTrainingEditorState) {
        currentEditorState = newEditorState
        onEditorStateChange(newEditorState)
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = resolvedScreenTitle,
                onBackClick = onBackClick,
                actions = {
                    topBarActions()
                    HomeIconButton(onClick = { onNavigate(ScreenType.Home) })
                    IconButton(
                        onClick = { onSaveTraining(currentEditorState.trainingName, currentEditorState.editableLinesForTraining) }
                    ) {
                        IconMd(
                            imageVector = Icons.Default.Save,
                            contentDescription = stringResource(R.string.common_save),
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
                    label = stringResource(R.string.create_training_name_label),
                    placeholder = defaultTrainingName
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceLg))

            ScreenSection {
                BodySecondaryText(text = "$resolvedLinesCountLabel: ${currentEditorState.editableLinesForTraining.size}")
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
            text = stringResource(R.string.create_training_lines_section_title)
        )

        Spacer(modifier = Modifier.height(AppDimens.spaceSm))

        CardMetaText(
            text = stringResource(R.string.create_training_page_label, currentPage + 1, totalPages),
        )

        Spacer(modifier = Modifier.height(AppDimens.spaceLg))

        if (lines.isEmpty()) {
            BodySecondaryText(text = stringResource(R.string.create_training_empty_lines))
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
                text = stringResource(R.string.common_previous),
                onClick = onPreviousPageClick,
                enabled = canGoPrevious,
                modifier = Modifier.weight(1f)
            )
            SecondaryButton(
                text = stringResource(R.string.common_next),
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
                    text = stringResource(R.string.common_id, line.lineId)
                )
                CardMetaText(
                    text = stringResource(R.string.create_training_weight_label, line.weight)
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
                        contentDescription = stringResource(R.string.create_training_decrease_line_weight),
                        onStep = onDecreaseWeightClick,
                    )
                    RepeatStepIconButton(
                        icon = Icons.Default.Add,
                        contentDescription = stringResource(R.string.create_training_increase_line_weight),
                        onStep = onIncreaseWeightClick,
                    )
                }
                DeleteIconButton(
                    onClick = onRemoveLineClick,
                    contentDescription = stringResource(R.string.create_training_remove_line_content_description),
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
        title = stringResource(R.string.create_training_created_title),
        message = stringResource(
            R.string.create_training_created_message,
            success.trainingId,
            success.trainingName,
            success.linesCount,
        ),
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
