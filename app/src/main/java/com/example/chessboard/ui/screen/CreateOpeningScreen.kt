package com.example.chessboard.ui.screen

import android.app.Activity
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.service.buildStoredPgnFromUci
import com.example.chessboard.service.extractPgnHeaders
import com.example.chessboard.service.OneGameTrainingData
import com.example.chessboard.service.parsePgnToUciLines
import com.example.chessboard.service.uciMovesToMoves
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.screen.training.ChessBoardSection
import com.example.chessboard.ui.screen.training.DarkInputField
import com.example.chessboard.service.computeLabel
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingIconInactive
import com.example.chessboard.ui.theme.TrainingTextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CreateOpeningScreenContainer(
    activity: Activity,
    screenContext: ScreenContainerContext,
    modifier: Modifier = Modifier,
) {
    val dbProvider = screenContext.inDbProvider
    val gameController = remember { GameController() }
    var selectedSide by remember { mutableStateOf(EditableGameSide.AS_WHITE) }
    var openingName by remember { mutableStateOf("") }
    var ecoCode by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var pgnText by remember { mutableStateOf("") }
    var pgnImportError by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var importedPgnHeaders by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var importedUciLines by remember { mutableStateOf<List<List<String>>>(emptyList()) }

    LaunchedEffect(selectedSide) {
        gameController.setOrientation(selectedSide.orientation)
    }

    CreateOpeningScreen(
        gameController = gameController,
        selectedSide = selectedSide,
        onSideSelected = { selectedSide = it },
        onBackClick = screenContext.onBackClick,
        openingName = openingName,
        onOpeningNameChange = { openingName = it; nameError = false },
        ecoCode = ecoCode,
        onEcoCodeChange = { ecoCode = it },
        nameError = nameError,
        pgnText = pgnText,
        onPgnTextChange = {
            pgnText = it
            importedPgnHeaders = emptyMap()
            importedUciLines = emptyList()
        },
        importedUciLines = importedUciLines,
        pgnImportError = pgnImportError,
        onPgnImportErrorDismiss = { pgnImportError = null },
        saveError = saveError,
        onSaveErrorDismiss = { saveError = null },
        onImportPgnClick = {
            (activity as? LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.Default) {
                try {
                    val headers = extractPgnHeaders(pgnText)
                    val uciLines = parsePgnToUciLines(pgnText)
                    withContext(Dispatchers.Main) {
                        if (uciLines.isEmpty()) {
                            pgnImportError = "No valid moves found in PGN text"
                        } else {
                            headers["Event"]
                                ?.takeIf { it.isNotBlank() && it != "?" }
                                ?.let { openingName = it }
                            headers["ECO"]
                                ?.takeIf { it.isNotBlank() && it != "?" }
                                ?.let { ecoCode = it }
                            importedPgnHeaders = headers
                            importedUciLines = uciLines
                            gameController.loadFromUciMoves(uciLines.first())
                            pgnImportError = null
                        }
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) { pgnImportError = "Failed to parse PGN" }
                }
            }
        },
        onSave = {
            if (openingName.isBlank()) {
                nameError = true
                return@CreateOpeningScreen
            }

            val importedHeadersSnapshot = importedPgnHeaders
            val importedLinesSnapshot = importedUciLines
            (activity as? LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.IO) {
                val savedGameIds = if (importedLinesSnapshot.isEmpty()) {
                    val entity = GameEntity(
                        event = openingName.ifBlank { null },
                        eco = ecoCode.ifBlank { null },
                        pgn = gameController.generatePgn(event = openingName.ifBlank { "Opening" }),
                        initialFen = "",
                        sideMask = selectedSide.sideMask
                    )
                    listOfNotNull(dbProvider.addGameAndGetId(entity, gameController.getMovesCopy()))
                } else {
                    buildList {
                        importedLinesSnapshot.forEachIndexed { index, uciMoves ->
                        val eventName = buildImportedLineEventName(
                            baseName = openingName,
                            index = index,
                            total = importedLinesSnapshot.size
                        )
                        val entity = GameEntity(
                            white = importedHeadersSnapshot["White"]?.takeIf { it.isNotBlank() && it != "?" },
                            black = importedHeadersSnapshot["Black"]?.takeIf { it.isNotBlank() && it != "?" },
                            result = importedHeadersSnapshot["Result"]?.takeIf { it.isNotBlank() && it != "?" },
                            site = importedHeadersSnapshot["Site"]?.takeIf { it.isNotBlank() && it != "?" },
                            round = importedHeadersSnapshot["Round"]?.takeIf { it.isNotBlank() && it != "?" },
                            event = eventName,
                            eco = ecoCode.ifBlank { null },
                            pgn = buildStoredPgnFromUci(
                                uciMoves = uciMoves,
                                event = eventName ?: "Opening",
                                whiteName = importedHeadersSnapshot["White"]?.takeIf { it.isNotBlank() && it != "?" } ?: "White",
                                blackName = importedHeadersSnapshot["Black"]?.takeIf { it.isNotBlank() && it != "?" } ?: "Black"
                            ),
                            initialFen = "",
                            sideMask = selectedSide.sideMask
                        )
                            dbProvider.addGameAndGetId(entity, uciMovesToMoves(uciMoves))?.let { gameId ->
                                add(gameId)
                            }
                        }
                    }
                }
                val savedCount = savedGameIds.size
                val trainingId = if (savedGameIds.isNotEmpty()) {
                    dbProvider.createTrainingFromGames(
                        name = openingName.ifBlank { "Opening" },
                        games = savedGameIds.map { gameId ->
                            OneGameTrainingData(gameId = gameId, weight = 1)
                        }
                    )
                } else {
                    null
                }

                withContext(Dispatchers.Main) {
                    if (savedCount > 0 && trainingId != null) {
                        screenContext.onBackClick()
                    } else if (savedCount > 0) {
                        saveError = "Games were saved, but training could not be created"
                    } else {
                        saveError = if (importedLinesSnapshot.isEmpty()) {
                            "Failed to save opening"
                        } else {
                            "None of the imported lines could be saved"
                        }
                    }
                }
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateOpeningScreen(
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
    pgnImportError: String?,
    onPgnImportErrorDismiss: () -> Unit,
    saveError: String?,
    onSaveErrorDismiss: () -> Unit,
    onImportPgnClick: () -> Unit,
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
                    onImportClick = onImportPgnClick
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceLg))

            ScreenSection {
                ChessBoardSection(gameController = gameController)
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceXs))

            Box(modifier = Modifier.padding(horizontal = AppDimens.spaceLg)) {
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

            if (importedUciLines.isNotEmpty()) {
                Spacer(modifier = Modifier.height(AppDimens.spaceLg))
                ScreenSection {
                    ImportedMovesTreeSection(
                        importedUciLines = importedUciLines,
                        gameController = gameController
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
        }
    }
}

private fun buildImportedLineEventName(
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
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
        color = Background.SurfaceDark
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.spaceSm, vertical = AppDimens.spaceSm),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GameSideSelector(
                selectedSide = selectedSide,
                onSideSelected = onSideSelected,
                modifier = Modifier.weight(1f)
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

            PillDivider()

            // More
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = {}),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More",
                    tint = TrainingTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
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

// ── Move tree ──────────────────────────────────────────────────────────────────

private class MoveTrieNode(
    val uciMove: String,
    val label: String,
    val children: MutableList<MoveTrieNode> = mutableListOf()
)

private data class MoveItem(val label: String, val uciPath: List<String>, val ply: Int)

private sealed class TreeSegment {
    data class MainMoves(val moves: List<MoveItem>) : TreeSegment()
    data class Variation(val moves: List<MoveItem>) : TreeSegment()
}

private fun buildMoveTrie(uciLines: List<List<String>>): MoveTrieNode {
    val root = MoveTrieNode("", "")
    for (line in uciLines) {
        val board = Board()
        var current = root
        for (uci in line) {
            val from = uci.take(2)
            val to = uci.drop(2).take(2)
            val move = try {
                Move(Square.fromValue(from.uppercase()), Square.fromValue(to.uppercase()))
            } catch (_: Exception) { break }
            var child = current.children.find { it.uciMove == uci }
            if (child == null) {
                val label = try { computeLabel(move, board.fen) } catch (_: Exception) { to }
                child = MoveTrieNode(uciMove = uci, label = label)
                current.children.add(child)
            }
            try { board.doMove(move) } catch (_: Exception) { break }
            current = child
        }
    }
    return root
}

private fun buildMoveTreeData(uciLines: List<List<String>>): List<TreeSegment> {
    if (uciLines.isEmpty()) return emptyList()
    val root = buildMoveTrie(uciLines)
    val segments = mutableListOf<TreeSegment>()
    var currentMainMoves = mutableListOf<MoveItem>()
    var current = root
    val mainPath = mutableListOf<String>()
    var ply = 0
    while (current.children.isNotEmpty()) {
        val mainChild = current.children[0]
        mainPath.add(mainChild.uciMove)
        currentMainMoves.add(MoveItem(mainChild.label, mainPath.toList(), ply))
        if (current.children.size > 1) {
            // Flush main moves up to and including the branch move, then insert variations
            segments.add(TreeSegment.MainMoves(currentMainMoves.toList()))
            currentMainMoves = mutableListOf()
            for (varChild in current.children.drop(1)) {
                val varBase = mainPath.dropLast(1).toMutableList()
                val varMoves = mutableListOf<MoveItem>()
                val subVariations = collectVariationMoves(varChild, ply, varBase, varMoves)
                if (varMoves.isNotEmpty()) segments.add(TreeSegment.Variation(varMoves))
                for (subVar in subVariations) {
                    if (subVar.isNotEmpty()) segments.add(TreeSegment.Variation(subVar))
                }
            }
        }
        current = mainChild
        ply++
    }
    if (currentMainMoves.isNotEmpty()) segments.add(TreeSegment.MainMoves(currentMainMoves.toList()))
    return segments
}

// Returns any sub-variations found while traversing this variation node.
private fun collectVariationMoves(
    node: MoveTrieNode,
    ply: Int,
    pathSoFar: MutableList<String>,
    moves: MutableList<MoveItem>
): List<List<MoveItem>> {
    pathSoFar.add(node.uciMove)
    moves.add(MoveItem(node.label, pathSoFar.toList(), ply))
    val subVariations = mutableListOf<List<MoveItem>>()
    if (node.children.isNotEmpty()) {
        val pathBeforeBranch = pathSoFar.toList()
        subVariations.addAll(collectVariationMoves(node.children[0], ply + 1, pathSoFar, moves))
        for (varChild in node.children.drop(1)) {
            val subVarBase = pathBeforeBranch.toMutableList()
            val subVarMoves = mutableListOf<MoveItem>()
            val deeperSubs = collectVariationMoves(varChild, ply + 1, subVarBase, subVarMoves)
            if (subVarMoves.isNotEmpty()) subVariations.add(subVarMoves)
            subVariations.addAll(deeperSubs)
        }
    }
    return subVariations
}

@Composable
internal fun ImportedMovesTreeSection(
    importedUciLines: List<List<String>>,
    gameController: GameController,
    modifier: Modifier = Modifier
) {
    val segments = remember(importedUciLines) { buildMoveTreeData(importedUciLines) }
    var selectedPath by remember(importedUciLines) { mutableStateOf<List<String>?>(null) }

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
                                        isSelected = selectedPath == move.uciPath,
                                        onClick = {
                                            selectedPath = move.uciPath
                                            gameController.loadFromUciMoves(move.uciPath, move.uciPath.size)
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
                                        isSelected = selectedPath == move.uciPath,
                                        onClick = {
                                            selectedPath = move.uciPath
                                            gameController.loadFromUciMoves(move.uciPath, move.uciPath.size)
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
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(AppDimens.radiusSm))
            .background(if (isSelected) TrainingAccentTeal else Background.CardDark)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) Color.White else TextColor.Primary
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun ImportFromPgnBlock(
    pgnText: String,
    onPgnTextChange: (String) -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
    ) {
        SectionTitleText(text = "Import from PGN", color = TrainingAccentTeal)

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

        PrimaryButton(
            text = "Import PGN",
            onClick = onImportClick,
            modifier = Modifier.fillMaxWidth()
        )

    }
}
