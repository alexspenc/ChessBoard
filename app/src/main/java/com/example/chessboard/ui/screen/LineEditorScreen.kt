package com.example.chessboard.ui.screen

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.chessboard.R
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.entity.LineEntity
import com.example.chessboard.service.DubiousLineService
import com.example.chessboard.service.parsePgnMoves
import com.example.chessboard.ui.LineEditorMoveSequenceSectionTestTag
import com.example.chessboard.ui.LineEditorNextTestTag
import com.example.chessboard.ui.LineEditorPreviousTestTag
import com.example.chessboard.ui.LineEditorScrollContainerTestTag
import com.example.chessboard.ui.components.AppConfirmDialog
import com.example.chessboard.ui.components.AppDivider
import com.example.chessboard.ui.components.AppIconSizes
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTextField
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BoardActionNavigationBar
import com.example.chessboard.ui.components.BoardActionNavigationItem
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.ChessBoardSection
import com.example.chessboard.ui.components.DeleteIconButton
import com.example.chessboard.ui.components.HomeIconButton
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.LineMoveTreeSection
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.BottomBarContentColor
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingSuccessGreen
import com.example.chessboard.ui.theme.TrainingWarningOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private suspend fun toggleDubiousLine(
    dubiousLineService: DubiousLineService,
    lineId: Long,
    isDubiousLine: Boolean,
) {
    if (isDubiousLine) {
        dubiousLineService.delete(lineId)
        return
    }

    dubiousLineService.markDubious(lineId)
}

@Composable
fun LineEditorScreenContainer(
    activity: Activity,
    line: LineEntity,
    screenContext: ScreenContainerContext,
    modifier: Modifier = Modifier,
) {
    val dbProvider = screenContext.inDbProvider
    val dubiousLineService = remember { dbProvider.createDubiousLineService() }
    val lineController = remember { LineController() }
    var isLoading by remember { mutableStateOf(true) }
    var isDubiousLine by remember(line.id) { mutableStateOf(false) }
    val defaultOpeningName = stringResource(R.string.line_editor_default_opening)

    LaunchedEffect(line.id) {
        val parsed = withContext(Dispatchers.Default) { parsePgnMoves(line.pgn) }
        lineController.resetToStartPosition()
        lineController.setOrientation(EditableLineSide.fromSideMask(line.sideMask).orientation)
        lineController.loadFromUciMoves(parsed, targetPly = parsed.size)
        isLoading = false
    }

    LaunchedEffect(line.id) {
        isDubiousLine =
            withContext(Dispatchers.IO) {
                dubiousLineService.isDubious(line.id)
            }
    }

    LineEditorScreen(
        line = line,
        lineController = lineController,
        isLoading = isLoading,
        isDubiousLine = isDubiousLine,
        onBackClick = screenContext.onBackClick,
        onHomeClick = { screenContext.onNavigate(ScreenType.Home) },
        onNavigate = screenContext.onNavigate,
        onToggleDubiousClick = {
            val currentIsDubiousLine = isDubiousLine
            (activity as? LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.IO) {
                toggleDubiousLine(
                    dubiousLineService = dubiousLineService,
                    lineId = line.id,
                    isDubiousLine = currentIsDubiousLine,
                )

                withContext(Dispatchers.Main) { isDubiousLine = !currentIsDubiousLine }
            }
        },
        onSave = { name, eco, selectedSide ->
            val idx = lineController.currentMoveIndex
            val pgn = lineController.generatePgn(
                event = name.ifBlank { defaultOpeningName },
                upToIndex = idx
            )
            val updatedLine = line.copy(
                event = name.ifBlank { null },
                eco = eco.ifBlank { null },
                pgn = pgn,
                sideMask = selectedSide.sideMask
            )
            val updatedMoves = lineController.getMovesCopy().take(idx)

            (activity as? LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.IO) {
                val updated = dbProvider.updateLine(
                    line = updatedLine,
                    moves = updatedMoves
                )

                if (!updated) {
                    return@launch
                }

                withContext(Dispatchers.Main) { screenContext.onBackClick() }
            }
        },
        onDelete = {
            (activity as? LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.IO) {
                dbProvider.createLineDeleter().deleteLine(line.id)
                withContext(Dispatchers.Main) { screenContext.onBackClick() }
            }
        },
        modifier = modifier
    )
}

private fun goToPly(
    lineController: LineController,
    currentPly: Int,
    targetPly: Int
) {
    if (targetPly == currentPly) {
        return
    }

    val safeTargetPly = targetPly.coerceAtLeast(0)
    val diff = safeTargetPly - currentPly
    if (diff > 0) {
        repeat(diff) { lineController.redoMove() }
        return
    }

    repeat(-diff) { lineController.undoMove() }
}

private fun goToStart(
    lineController: LineController,
    currentPly: Int
) {
    if (currentPly == 0) {
        return
    }

    goToPly(
        lineController = lineController,
        currentPly = currentPly,
        targetPly = 0
    )
}

@Composable
private fun LineEditorBoardControlsBar(
    selectedSide: EditableLineSide,
    onSideSelected: (EditableLineSide) -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onResetClick: () -> Unit,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoardActionNavigationBar(
        modifier = modifier,
        items = EditableLineSide.entries.map { side ->
            BoardActionNavigationItem(
                label = if (side == EditableLineSide.AS_WHITE) {
                    stringResource(R.string.line_analysis_side_white)
                } else {
                    stringResource(R.string.line_analysis_side_black)
                },
                selected = side == selectedSide,
                onClick = { onSideSelected(side) },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_king),
                    contentDescription = resolveEditableLineSideContentDescription(side),
                    tint = if (side == selectedSide) TrainingAccentTeal else BottomBarContentColor,
                    modifier = Modifier.size(AppIconSizes.Lg),
                )
            }
        } + listOf(
            BoardActionNavigationItem(
                label = stringResource(R.string.common_reset),
                enabled = canUndo,
                onClick = onResetClick,
            ) {
                IconMd(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.common_reset),
                    tint = if (canUndo) BottomBarContentColor else BottomBarContentColor.copy(alpha = 0.5f),
                )
            },
            BoardActionNavigationItem(
                label = stringResource(R.string.common_back),
                enabled = canUndo,
                modifier = Modifier.testTag(LineEditorPreviousTestTag),
                onClick = onBackClick,
            ) {
                IconMd(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.line_analysis_previous_move_content_description),
                    tint = if (canUndo) BottomBarContentColor else BottomBarContentColor.copy(alpha = 0.5f),
                )
            },
            BoardActionNavigationItem(
                label = stringResource(R.string.common_forward),
                enabled = canRedo,
                modifier = Modifier.testTag(LineEditorNextTestTag),
                onClick = onForwardClick,
            ) {
                IconMd(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.line_analysis_next_move_content_description),
                    tint = if (canRedo) BottomBarContentColor else BottomBarContentColor.copy(alpha = 0.5f),
                )
            },
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineEditorScreen(
    line: LineEntity,
    lineController: LineController,
    isLoading: Boolean,
    isDubiousLine: Boolean = false,
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onToggleDubiousClick: () -> Unit = {},
    onSave: (name: String, eco: String, selectedSide: EditableLineSide) -> Unit,
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    @Suppress("UNUSED_VARIABLE")
    val boardState = lineController.boardState
    val currentPly = lineController.currentMoveIndex
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAdditionalMenu by remember { mutableStateOf(false) }
    var editedName by remember(line.id) { mutableStateOf(line.event ?: "") }
    var editedEco by remember(line.id) { mutableStateOf(line.eco ?: "") }
    var selectedSide by remember(line.id) { mutableStateOf(EditableLineSide.fromSideMask(line.sideMask)) }

    renderLineEditorAdditionalMenu(
        visible = showAdditionalMenu,
        isDubiousLine = isDubiousLine,
        onDismiss = { showAdditionalMenu = false },
        onToggleDubiousClick = {
            showAdditionalMenu = false
            onToggleDubiousClick()
        },
    )

    if (showDeleteDialog) {
        val deleteLineTitle = line.event ?: stringResource(R.string.line_editor_delete_fallback_opening)
        AppConfirmDialog(
            title = stringResource(R.string.line_editor_delete_title),
            message = stringResource(R.string.line_editor_delete_message, deleteLineTitle),
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            confirmText = stringResource(R.string.common_delete),
            isDestructive = true
        )
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = editedName.ifBlank { stringResource(R.string.line_editor_default_opening) },
                subtitle = editedEco.ifBlank { null },
                onBackClick = onBackClick,
                actions = {
                    HomeIconButton(onClick = onHomeClick)
                    IconButton(onClick = { showAdditionalMenu = true }) {
                        IconMd(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.line_editor_line_actions_content_description),
                        )
                    }
                    DeleteIconButton(onClick = { showDeleteDialog = true })
                    IconButton(onClick = { onSave(editedName, editedEco, selectedSide) }) {
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
            LineEditorBoardControlsBar(
                selectedSide = selectedSide,
                onSideSelected = {
                    selectedSide = it
                    lineController.setOrientation(it.orientation)
                },
                canUndo = lineController.canUndo,
                canRedo = lineController.canRedo,
                onResetClick = { goToStart(lineController, currentPly) },
                onBackClick = { lineController.undoMove() },
                onForwardClick = { lineController.redoMove() },
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TrainingAccentTeal)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag(LineEditorScrollContainerTestTag)
                    .verticalScroll(rememberScrollState())
            ) {
                ChessBoardSection(lineController = lineController, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(AppDimens.spaceMd))

                LineMoveTreeSection(
                    importedUciLines = emptyList(),
                    lineController = lineController,
                    modifier = Modifier
                        .padding(horizontal = AppDimens.spaceLg)
                        .testTag(LineEditorMoveSequenceSectionTestTag),
                )

                AppDivider(
                    modifier = Modifier.padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceMd)
                )

                AppTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = stringResource(R.string.line_editor_opening_name_label),
                    placeholder = stringResource(R.string.create_opening_name_placeholder),
                    modifier = Modifier.padding(horizontal = AppDimens.spaceLg)
                )

                Spacer(modifier = Modifier.height(AppDimens.spaceMd))

                AppTextField(
                    value = editedEco,
                    onValueChange = { editedEco = it },
                    label = stringResource(R.string.create_opening_eco_label),
                    placeholder = stringResource(R.string.create_opening_eco_placeholder),
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .padding(start = AppDimens.spaceLg)
                )

                Spacer(modifier = Modifier.height(AppDimens.spaceXl))
            }
        }
    }
}

@Composable
private fun renderLineEditorAdditionalMenu(
    visible: Boolean,
    isDubiousLine: Boolean,
    onDismiss: () -> Unit,
    onToggleDubiousClick: () -> Unit,
) {
    if (!visible) {
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Background.ScreenDark,
        title = {
            SectionTitleText(text = stringResource(R.string.line_editor_line_actions_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
            ) {
                lineEditorDialogAction(
                    label = resolveDubiousLineActionLabel(isDubiousLine),
                    contentColor = resolveDubiousLineActionColor(isDubiousLine),
                    onClick = onToggleDubiousClick,
                ) { actionTint ->
                    IconMd(
                        imageVector = Icons.Default.ReportProblem,
                        contentDescription = resolveDubiousLineActionContentDescription(isDubiousLine),
                        tint = actionTint,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                CardMetaText(text = stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun lineEditorDialogAction(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    contentColor: Color? = null,
    icon: @Composable (Color) -> Unit,
) {
    val actionTint = contentColor ?: resolveLineEditorDialogActionTint(enabled)

    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon(actionTint)
            Text(
                text = label,
                color = actionTint,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun resolveEditableLineSideContentDescription(side: EditableLineSide): String {
    if (side == EditableLineSide.AS_WHITE) {
        return stringResource(R.string.line_editor_side_as_white)
    }

    return stringResource(R.string.line_editor_side_as_black)
}

@Composable
private fun resolveDubiousLineActionLabel(isDubiousLine: Boolean): String {
    if (isDubiousLine) {
        return stringResource(R.string.line_editor_remove_doubt_action)
    }

    return stringResource(R.string.line_editor_doubt_action)
}

@Composable
private fun resolveDubiousLineActionContentDescription(isDubiousLine: Boolean): String {
    if (isDubiousLine) {
        return stringResource(R.string.line_editor_remove_dubious_content_description)
    }

    return stringResource(R.string.line_editor_mark_dubious_content_description)
}

private fun resolveDubiousLineActionColor(isDubiousLine: Boolean): Color {
    if (isDubiousLine) {
        return TrainingSuccessGreen
    }

    return TrainingWarningOrange
}

private fun resolveLineEditorDialogActionTint(isEnabled: Boolean): Color {
    if (isEnabled) {
        return TextColor.Primary
    }

    return TextColor.Primary.copy(alpha = 0.5f)
}
