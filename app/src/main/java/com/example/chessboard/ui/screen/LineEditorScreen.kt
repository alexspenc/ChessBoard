package com.example.chessboard.ui.screen

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.chessboard.R
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.entity.LineEntity
import com.example.chessboard.service.parsePgnMoves
import com.example.chessboard.ui.LineEditorMoveSequenceSectionTestTag
import com.example.chessboard.ui.LineEditorNextTestTag
import com.example.chessboard.ui.LineEditorPreviousTestTag
import com.example.chessboard.ui.LineEditorScrollContainerTestTag
import com.example.chessboard.ui.components.AppConfirmDialog
import com.example.chessboard.ui.components.AppDivider
import com.example.chessboard.ui.components.AppIconSizes
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BoardActionNavigationBar
import com.example.chessboard.ui.components.BoardActionNavigationItem
import com.example.chessboard.ui.components.ChessBoardSection
import com.example.chessboard.ui.components.DeleteIconButton
import com.example.chessboard.ui.components.HomeIconButton
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.LineMoveTreeSection
import com.example.chessboard.ui.screen.training.DarkInputField
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.BottomBarContentColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LineEditorScreenContainer(
    activity: Activity,
    line: LineEntity,
    screenContext: ScreenContainerContext,
    modifier: Modifier = Modifier,
) {
    val dbProvider = screenContext.inDbProvider
    val lineController = remember { LineController() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(line.id) {
        val parsed = withContext(Dispatchers.Default) { parsePgnMoves(line.pgn) }
        lineController.resetToStartPosition()
        lineController.setOrientation(EditableLineSide.fromSideMask(line.sideMask).orientation)
        lineController.loadFromUciMoves(parsed, targetPly = parsed.size)
        isLoading = false
    }

    LineEditorScreen(
        line = line,
        lineController = lineController,
        isLoading = isLoading,
        onBackClick = screenContext.onBackClick,
        onHomeClick = { screenContext.onNavigate(ScreenType.Home) },
        onNavigate = screenContext.onNavigate,
        onSave = { name, eco, selectedSide ->
            val idx = lineController.currentMoveIndex
            val pgn = lineController.generatePgn(
                event = name.ifBlank { "Opening" },
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
                label = if (side == EditableLineSide.AS_WHITE) "White" else "Black",
                selected = side == selectedSide,
                onClick = { onSideSelected(side) },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_king),
                    contentDescription = side.toDisplayText(),
                    tint = if (side == selectedSide) TrainingAccentTeal else BottomBarContentColor,
                    modifier = Modifier.size(AppIconSizes.Lg),
                )
            }
        } + listOf(
            BoardActionNavigationItem(
                label = "Reset",
                enabled = canUndo,
                onClick = onResetClick,
            ) {
                IconMd(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = if (canUndo) BottomBarContentColor else BottomBarContentColor.copy(alpha = 0.5f),
                )
            },
            BoardActionNavigationItem(
                label = "Back",
                enabled = canUndo,
                modifier = Modifier.testTag(LineEditorPreviousTestTag),
                onClick = onBackClick,
            ) {
                IconMd(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous move",
                    tint = if (canUndo) BottomBarContentColor else BottomBarContentColor.copy(alpha = 0.5f),
                )
            },
            BoardActionNavigationItem(
                label = "Forward",
                enabled = canRedo,
                modifier = Modifier.testTag(LineEditorNextTestTag),
                onClick = onForwardClick,
            ) {
                IconMd(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next move",
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
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onSave: (name: String, eco: String, selectedSide: EditableLineSide) -> Unit,
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    @Suppress("UNUSED_VARIABLE")
    val boardState = lineController.boardState
    val currentPly = lineController.currentMoveIndex
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editedName by remember(line.id) { mutableStateOf(line.event ?: "") }
    var editedEco by remember(line.id) { mutableStateOf(line.eco ?: "") }
    var selectedSide by remember(line.id) { mutableStateOf(EditableLineSide.fromSideMask(line.sideMask)) }

    if (showDeleteDialog) {
        AppConfirmDialog(
            title = "Delete Opening",
            message = "Delete \"${line.event ?: "this opening"}\"? This cannot be undone.",
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            confirmText = "Delete",
            isDestructive = true
        )
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = editedName.ifBlank { "Opening" },
                subtitle = editedEco.ifBlank { null },
                onBackClick = onBackClick,
                actions = {
                    HomeIconButton(onClick = onHomeClick)
                    DeleteIconButton(onClick = { showDeleteDialog = true })
                    IconButton(onClick = { onSave(editedName, editedEco, selectedSide) }) {
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

                DarkInputField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = "Opening Name",
                    placeholder = "e.g., Sicilian Defense",
                    modifier = Modifier.padding(horizontal = AppDimens.spaceLg)
                )

                Spacer(modifier = Modifier.height(AppDimens.spaceMd))

                DarkInputField(
                    value = editedEco,
                    onValueChange = { editedEco = it },
                    label = "ECO Code",
                    placeholder = "e.g., B20",
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .padding(start = AppDimens.spaceLg)
                )

                Spacer(modifier = Modifier.height(AppDimens.spaceXl))
            }
        }
    }
}
