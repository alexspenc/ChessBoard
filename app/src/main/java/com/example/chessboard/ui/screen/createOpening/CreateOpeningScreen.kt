package com.example.chessboard.ui.screen.createOpening

/**
 * Pure UI for the create-opening screen.
 *
 * Keep in this file:
 * - composable layout and visual subcomponents for the create-opening screen
 * - small UI-only helpers used only by this screen
 * - rendering logic that depends only on parameters already prepared by the container
 *
 * It is acceptable to add here:
 * - new visual blocks of this screen
 * - small private UI helper composables
 * - UI-only formatting helpers
 *
 * Do not add here:
 * - database calls, service orchestration, or coroutine-based save flows
 * - navigation decisions beyond invoking callbacks passed from the container
 * - post-save business flow for creating trainings or templates
 */
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.github.bhlangonijr.chesslib.Piece
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.screen.EditableGameSide
import com.example.chessboard.ui.screen.GameSideSelector
import com.example.chessboard.ui.screen.training.ChessBoardSection
import com.example.chessboard.ui.screen.training.DarkInputField
import com.example.chessboard.ui.components.MoveChip
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingIconInactive
import com.example.chessboard.ui.theme.TrainingTextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateOpeningScreen(
    gameController: GameController,
    selectedSide: EditableGameSide,
    onSideSelected: (EditableGameSide) -> Unit,
    onBackClick: () -> Unit = {},
    openingName: String,
    onOpeningNameChange: (String) -> Unit,
    ecoCode: String,
    onEcoCodeChange: (String) -> Unit,
    nameError: Boolean,
    pgnText: String,
    onPgnTextChange: (String) -> Unit,
    importedUciLines: List<List<String>>,
    importedChapterCount: Int,
    pgnImportError: String?,
    onPgnImportErrorDismiss: () -> Unit,
    saveError: String?,
    onSaveErrorDismiss: () -> Unit,
    onImportPgnClick: () -> Unit,
    onImportFromFileClick: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (pgnImportError != null) {
        AppMessageDialog(
            title = "Import Failed",
            message = pgnImportError,
            onDismiss = onPgnImportErrorDismiss
        )
    }
    if (saveError != null) {
        AppMessageDialog(
            title = "Save Failed",
            message = saveError,
            onDismiss = onSaveErrorDismiss
        )
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Create Opening",
                subtitle = "Build your custom opening",
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = onSave) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save",
                            tint = TrainingAccentTeal
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))

            ScreenSection {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                    verticalAlignment = Alignment.Bottom
                ) {
                    DarkInputField(
                        value = openingName,
                        onValueChange = onOpeningNameChange,
                        placeholder = "e.g., Sicilian Defense",
                        label = "Opening Name *",
                        isError = nameError,
                        modifier = Modifier.weight(1f)
                    )
                    DarkInputField(
                        value = ecoCode,
                        onValueChange = onEcoCodeChange,
                        placeholder = "e.g., B20",
                        label = "ECO Code",
                        modifier = Modifier.width(96.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceLg))

            ScreenSection {
                ImportFromPgnBlock(
                    pgnText = pgnText,
                    onPgnTextChange = onPgnTextChange,
                    onImportClick = onImportPgnClick,
                    onImportFromFileClick = onImportFromFileClick,
                    importedChapterCount = importedChapterCount
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceLg))

            ScreenSection {
                ChessBoardSection(gameController = gameController)
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceXs))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.spaceLg),
                contentAlignment = Alignment.Center
            ) {
                BoardControlRow(
                    selectedSide = selectedSide,
                    onSideSelected = onSideSelected,
                    canUndo = gameController.canUndo,
                    canRedo = gameController.canRedo,
                    onUndoClick = { gameController.undoMove() },
                    onResetClick = { gameController.resetToStartPosition() },
                    onRedoClick = { gameController.redoMove() }
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            ScreenSection {
                ImportedMovesTreeSection(
                    importedUciLines = importedUciLines,
                    gameController = gameController
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
        }
    }
}

internal fun buildImportedLineEventName(
    baseName: String,
    index: Int,
    total: Int
): String? {
    val resolvedBaseName = baseName.ifBlank { "Opening" }
    if (total <= 1 || index == 0) {
        return resolvedBaseName
    }

    return "$resolvedBaseName (Line ${index + 1})"
}

@Composable
private fun BoardControlRow(
    selectedSide: EditableGameSide,
    onSideSelected: (EditableGameSide) -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndoClick: () -> Unit,
    onResetClick: () -> Unit,
    onRedoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Background.SurfaceDark
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = AppDimens.spaceSm, vertical = AppDimens.spaceSm),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GameSideSelector(
                selectedSide = selectedSide,
                onSideSelected = onSideSelected,
                showTitle = false,
                modifier = Modifier.width(88.dp)
            )

            PillDivider()

            // Reset
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onResetClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Reset",
                    tint = TrainingTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            PillDivider()

            // Undo / Redo
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(enabled = canUndo, onClick = onUndoClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Undo",
                        tint = if (canUndo) TrainingTextPrimary else TrainingIconInactive,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(enabled = canRedo, onClick = onRedoClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Redo",
                        tint = if (canRedo) TrainingTextPrimary else TrainingIconInactive,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PillDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(TrainingIconInactive.copy(alpha = 0.4f))
    )
}

private fun resolveVisibleMoveLines(
    importedUciLines: List<List<String>>,
    authoredUciLine: List<String>
): List<List<String>> {
    if (authoredUciLine.isEmpty()) {
        return importedUciLines
    }

    if (importedUciLines.any { it == authoredUciLine }) {
        return importedUciLines
    }

    return importedUciLines + listOf(authoredUciLine)
}

private fun resolveUciLine(
    gameController: GameController,
    upToPly: Int = gameController.getMovesCopy().size
): List<String> {
    return gameController.getMovesCopy().take(upToPly).map { move ->
        buildString {
            append(move.from.value().lowercase())
            append(move.to.value().lowercase())
            if (move.promotion != Piece.NONE) {
                append(move.promotion.pieceType.name.first().lowercaseChar())
            }
        }
    }
}

private fun resolveBackingLine(
    visibleLines: List<List<String>>,
    selectedPath: List<String>
): List<String> {
    return visibleLines.firstOrNull { line ->
        line.size >= selectedPath.size && line.take(selectedPath.size) == selectedPath
    } ?: selectedPath
}

@Composable
internal fun ImportedMovesTreeSection(
    importedUciLines: List<List<String>>,
    gameController: GameController,
    modifier: Modifier = Modifier
) {
    val boardState = gameController.boardState
    val authoredUciLine = remember(boardState) { resolveUciLine(gameController) }
    val currentPositionPath = remember(boardState, gameController.currentMoveIndex) {
        resolveUciLine(gameController, upToPly = gameController.currentMoveIndex)
    }
    val visibleLines = remember(importedUciLines, boardState) {
        resolveVisibleMoveLines(
            importedUciLines = importedUciLines,
            authoredUciLine = authoredUciLine
        )
    }
    val segments = remember(visibleLines) { buildMoveTreeData(visibleLines) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
        SectionTitleText(text = "Move Tree", color = TrainingAccentTeal)
        Surface(
            shape = RoundedCornerShape(AppDimens.radiusMd),
            color = Background.SurfaceDark,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("move-tree-box")
        ) {
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .testTag("move-tree-content"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (segments.isEmpty()) {
                    BodySecondaryText(
                        text = "No moves yet. Import a PGN or add moves on the board.",
                        color = TrainingIconInactive
                    )
                }
                segments.forEachIndexed { segIndex, segment ->
                    when (segment) {
                        is TreeSegment.MainMoves -> {
                            // After a variation, resume with move number even on black's move
                            val isContinuation = segIndex > 0 && segments[segIndex - 1] is TreeSegment.Variation
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.testTag("move-tree-row-$segIndex")
                            ) {
                                segment.moves.forEachIndexed { moveIndex, move ->
                                    val isWhite = move.ply % 2 == 0
                                    val showNumber = isWhite || (moveIndex == 0 && isContinuation)
                                    if (showNumber) {
                                        Text(
                                            text = if (isWhite) "${move.ply / 2 + 1}." else "${move.ply / 2 + 1}...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextColor.Secondary,
                                            modifier = Modifier.align(Alignment.CenterVertically)
                                        )
                                    }
                                    TreeMoveChip(
                                        label = move.label,
                                        isSelected = currentPositionPath == move.uciPath,
                                        onClick = {
                                            val backingLine = resolveBackingLine(visibleLines, move.uciPath)
                                            gameController.loadFromUciMoves(backingLine, move.uciPath.size)
                                        }
                                    )
                                }
                            }
                        }
                        is TreeSegment.Variation -> {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.testTag("move-tree-row-$segIndex")
                            ) {
                                Text(
                                    text = "—",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextColor.Secondary,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                                segment.moves.forEach { move ->
                                    if (move.ply % 2 == 0) {
                                        Text(
                                            text = "${move.ply / 2 + 1}.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextColor.Secondary,
                                            modifier = Modifier.align(Alignment.CenterVertically)
                                        )
                                    }
                                    TreeMoveChip(
                                        label = move.label,
                                        isSelected = currentPositionPath == move.uciPath,
                                        onClick = {
                                            val backingLine = resolveBackingLine(visibleLines, move.uciPath)
                                            gameController.loadFromUciMoves(backingLine, move.uciPath.size)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TreeMoveChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    MoveChip(
        label = label,
        isSelected = isSelected,
        onClick = onClick,
        unselectedBackground = Background.CardDark,
        unselectedTextColor = TextColor.Primary,
        textStyle = MaterialTheme.typography.bodyMedium,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
    )
}

// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun ImportFromPgnBlock(
    pgnText: String,
    onPgnTextChange: (String) -> Unit,
    onImportClick: () -> Unit,
    onImportFromFileClick: () -> Unit,
    importedChapterCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
        ) {
            SectionTitleText(text = "Import from PGN", color = TrainingAccentTeal)
            if (importedChapterCount > 1) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = TrainingAccentTeal.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "$importedChapterCount chapters",
                        style = MaterialTheme.typography.labelSmall,
                        color = TrainingAccentTeal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(AppDimens.radiusMd),
            color = Background.SurfaceDark,
            border = BorderStroke(1.dp, TrainingAccentTeal),
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicTextField(
                value = pgnText,
                onValueChange = onPgnTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = AppDimens.spaceMd),
                textStyle = MaterialTheme.typography.bodyMedium.merge(
                    TextStyle(color = TextColor.Primary)
                ),
                cursorBrush = SolidColor(TrainingAccentTeal),
                minLines = 4,
                decorationBox = { innerTextField ->
                    if (pgnText.isEmpty()) {
                        BodySecondaryText(
                            text = "Paste PGN here... e.g., 1. e4 e5 2. Nf3 Nc6 3. Bb5",
                            color = TrainingIconInactive
                        )
                    }
                    innerTextField()
                }
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                onClick = onImportFromFileClick,
                shape = RoundedCornerShape(AppDimens.radiusMd),
                color = Background.SurfaceDark,
                border = BorderStroke(1.dp, TrainingAccentTeal),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = AppDimens.spaceMd, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = TrainingAccentTeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(AppDimens.spaceXs))
                    Text(
                        text = "From File",
                        style = MaterialTheme.typography.labelLarge,
                        color = TrainingAccentTeal
                    )
                }
            }

            PrimaryButton(
                text = "Import PGN",
                onClick = onImportClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
