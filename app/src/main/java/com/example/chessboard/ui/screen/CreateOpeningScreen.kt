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
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.buildStoredPgnFromUci
import com.example.chessboard.service.extractPgnHeaders
import com.example.chessboard.service.parsePgnToUciLines
import com.example.chessboard.service.uciMovesToMoves
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.SectionTitleText
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
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    inDbProvider: DatabaseProvider,
) {
    val dbProvider = inDbProvider
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
        onBackClick = onBackClick,
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
                val savedCount = if (importedLinesSnapshot.isEmpty()) {
                    val entity = GameEntity(
                        event = openingName.ifBlank { null },
                        eco = ecoCode.ifBlank { null },
                        pgn = gameController.generatePgn(event = openingName.ifBlank { "Opening" }),
                        initialFen = "",
                        sideMask = selectedSide.sideMask
                    )
                    if (dbProvider.addGame(entity, gameController.getMovesCopy())) 1 else 0
                } else {
                    var successCount = 0
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
                        if (dbProvider.addGame(entity, uciMovesToMoves(uciMoves))) {
                            successCount++
                        }
                    }
                    successCount
                }

                withContext(Dispatchers.Main) {
                    if (savedCount > 0) {
                        onBackClick()
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
                    PrimaryButton("Save", onClick = onSave)
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
            // Side selector: ♔ ♚
            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf(
                    EditableGameSide.AS_WHITE to "♔",
                    EditableGameSide.AS_BLACK to "♚"
                ).forEach { (side, symbol) ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (selectedSide == side) SideButtonSelectedBg
                                else Color.Transparent,
                                RoundedCornerShape(50)
                            )
                            .clickable { onSideSelected(side) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = symbol,
                            fontSize = 20.sp,
                            color = if (selectedSide == side) TrainingTextPrimary else TrainingIconInactive
                        )
                    }
                }
            }

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
