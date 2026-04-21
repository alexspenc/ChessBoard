package com.example.chessboard.ui.screen.positions

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.chessboard.boardmodel.BoardPiece
import com.example.chessboard.boardmodel.BoardPosition
import com.example.chessboard.boardmodel.ChesslibMapper
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.boardmodel.InitialBoardFenWithoutMoveNumbers
import com.example.chessboard.service.SaveSavedSearchPositionResult
import com.example.chessboard.service.calculateFenHashWithoutMoveNumbers
import com.example.chessboard.ui.PositionEditorBoardWithCoordinates
import com.example.chessboard.ui.PositionEditorClearBoardTestTag
import com.example.chessboard.ui.PositionEditorInitialPositionTestTag
import com.example.chessboard.ui.PositionEditorListTestTag
import com.example.chessboard.ui.resolvePieceGlyph
import com.example.chessboard.ui.resolvePieceTint
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.SecondaryButton
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.screen.EditableGameSide
import com.example.chessboard.ui.screen.GameSideSelector
import com.example.chessboard.ui.screen.PositionEditorCastlingState
import com.example.chessboard.ui.screen.PositionEditorCastlesSection
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.screen.SideButtonSelectedBg
import com.example.chessboard.ui.screen.replacePositionEditorFenCastlingPart
import com.example.chessboard.ui.screen.resolvePositionEditorCastlingState
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingTextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val EmptyBoardBoardPart = "8/8/8/8/8/8/8/8"
private const val EmptyBoardFen = "$EmptyBoardBoardPart w - -"
private const val PositionEditorLogTag = "PositionEditor"

internal data class PositionEditorPieceOption(
    val letter: Char,
    val label: String
)

private val PositionEditorPieceOptions = listOf(
    PositionEditorPieceOption('K', "White King"),
    PositionEditorPieceOption('Q', "White Queen"),
    PositionEditorPieceOption('R', "White Rook"),
    PositionEditorPieceOption('B', "White Bishop"),
    PositionEditorPieceOption('N', "White Knight"),
    PositionEditorPieceOption('P', "White Pawn"),
    PositionEditorPieceOption('k', "Black King"),
    PositionEditorPieceOption('q', "Black Queen"),
    PositionEditorPieceOption('r', "Black Rook"),
    PositionEditorPieceOption('b', "Black Bishop"),
    PositionEditorPieceOption('n', "Black Knight"),
    PositionEditorPieceOption('p', "Black Pawn")
)

private data class PositionEditorUiState(
    val fenText: String = EmptyBoardFen,
    val selectedSide: EditableGameSide = EditableGameSide.AS_WHITE,
    val selectedPiece: PositionEditorPieceOption = PositionEditorPieceOptions.first(),
    val fenError: String? = null,
    val foundGameIds: List<Long>? = null,
    val infoDialog: PositionEditorInfoDialog? = null
)

private data class PositionEditorInfoDialog(
    val title: String,
    val message: String
)

internal data class PositionEditorSaveDialogState(
    val positionName: String = "",
    val errorMessage: String? = null
)

private data class PositionEditorScreenState(
    val editor: PositionEditorUiState,
    val saveDialog: PositionEditorSaveDialogState?
)

internal data class PositionEditorScreenActions(
    val position: Position,
    val board: Board,
    val topBar: TopBar,
    val saveDialog: SaveDialog,
    val foundGamesDialog: PositionSearchResultDialogActions,
    val feedback: Feedback
) {
    data class Position(
        val onFenTextChange: (String) -> Unit,
        val onSideSelected: (EditableGameSide) -> Unit,
        val onCastlingStateChange: (PositionEditorCastlingState) -> Unit,
        val onClearBoardClick: () -> Unit,
        val onSetInitialPositionClick: () -> Unit
    )

    data class Board(
        val onPieceSelected: (PositionEditorPieceOption) -> Unit,
        val onBoardSquareClick: (String) -> Unit,
        val onBoardPieceMove: (String, String) -> Unit
    )

    data class TopBar(
        val onApplyFenClick: () -> Unit,
        val onSavePositionClick: () -> Unit,
        val onFindGamesClick: () -> Unit
    )

    data class SaveDialog(
        val onDismiss: () -> Unit,
        val onPositionNameChange: (String) -> Unit,
        val onConfirm: () -> Unit
    )

    data class Feedback(
        val onFenErrorDismiss: () -> Unit,
        val onInfoDialogDismiss: () -> Unit
    )
}

private data class PositionEditorNavigationActions(
    val onBackClick: () -> Unit = {},
    val onNavigate: (ScreenType) -> Unit = {}
)

@Composable
fun PositionEditorScreenContainer(
    initialFen: String = InitialBoardFenWithoutMoveNumbers,
    screenContext: ScreenContainerContext,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val savedSearchPositionService = remember(screenContext.inDbProvider) {
        screenContext.inDbProvider.createSavedSearchPositionService()
    }
    val gameController = remember {
        GameController().also { controller ->
            controller.loadPreviewFen(toLoadableFen(EmptyBoardFen))
        }
    }
    var uiState by remember { mutableStateOf(PositionEditorUiState()) }
    var saveDialogState by remember { mutableStateOf<PositionEditorSaveDialogState?>(null) }

    fun resolveSelectedSide(fen: String): EditableGameSide {
        val sideToken = fen.trim().split(Regex("\\s+")).getOrNull(1)
        if (sideToken == "b") {
            return EditableGameSide.AS_BLACK
        }

        return EditableGameSide.AS_WHITE
    }

    fun updatePositionEditorPreview(
        fen: String,
        selectedSide: EditableGameSide = uiState.selectedSide,
        foundGameIds: List<Long>? = uiState.foundGameIds
    ) {
        val normalizedFen = normalizePositionEditorFen(
            fen = fen,
            selectedSide = selectedSide
        )
        gameController.loadPreviewFen(toLoadableFen(normalizedFen))
        uiState = uiState.copy(
            fenText = normalizedFen,
            selectedSide = selectedSide,
            fenError = null,
            foundGameIds = foundGameIds
        )
    }

    fun applyPositionEditorFen(
        fen: String,
        selectedSide: EditableGameSide = uiState.selectedSide,
        foundGameIds: List<Long>? = null
    ): Boolean {
        val normalizedFen = normalizePositionEditorFen(
            fen = fen,
            selectedSide = selectedSide
        )
        val wasLoaded = gameController.loadFromFen(toLoadableFen(normalizedFen))
        if (!wasLoaded) {
            uiState = uiState.copy(fenError = "Failed to apply FEN")
            return false
        }

        uiState = uiState.copy(
            fenText = normalizePositionEditorFen(
                fen = gameController.getFen(),
                selectedSide = selectedSide
            ),
            selectedSide = selectedSide,
            fenError = null,
            foundGameIds = foundGameIds
        )
        return true
    }

    fun createTemplateFromFoundGames() {
        val foundGameIds = uiState.foundGameIds ?: return
        uiState = uiState.copy(foundGameIds = null)

        scope.launch {
            val templateId = withContext(Dispatchers.IO) {
                createPositionTemplateFromGameIds(
                    dbProvider = screenContext.inDbProvider,
                    gameIds = foundGameIds
                )
            }

            if (templateId != null) {
                uiState = uiState.copy(
                    infoDialog = PositionEditorInfoDialog(
                        title = "Template Created",
                        message = "Template ID: $templateId\nGames added: ${foundGameIds.size}"
                    ),
                    fenError = null
                )
                return@launch
            }

            uiState = uiState.copy(
                infoDialog = PositionEditorInfoDialog(
                    title = "Template Error",
                    message = "Found games could not be saved as a template."
                ),
                fenError = null
            )
        }
    }

    fun updateSaveDialogError(errorMessage: String?) {
        saveDialogState = saveDialogState?.copy(errorMessage = errorMessage)
    }

    fun resolveSavePositionErrorMessage(
        result: SaveSavedSearchPositionResult
    ): String? {
        if (result is SaveSavedSearchPositionResult.Success) {
            return null
        }

        if (result is SaveSavedSearchPositionResult.DuplicateName) {
            return "Position name already exists."
        }

        if (result is SaveSavedSearchPositionResult.DuplicateSearchFen) {
            return "This search position has already been saved."
        }

        if (result is SaveSavedSearchPositionResult.DuplicateFullFen) {
            return "This exact position has already been saved."
        }

        return "Failed to save position."
    }

    LaunchedEffect(uiState.selectedSide) {
        gameController.setOrientation(uiState.selectedSide.orientation)
    }

    LaunchedEffect(initialFen) {
        val selectedSide = resolveSelectedSide(initialFen)
        updatePositionEditorPreview(
            fen = initialFen,
            selectedSide = selectedSide,
            foundGameIds = null
        )
    }

    PositionEditorScreen(
        gameController = gameController,
        state = PositionEditorScreenState(
            editor = uiState,
            saveDialog = saveDialogState
        ),
        actions = PositionEditorScreenActions(
            position = PositionEditorScreenActions.Position(
                onFenTextChange = { uiState = uiState.copy(fenText = it) },
                onSideSelected = { selectedSide ->
                    val updatedFen = replaceFenSide(
                        fen = uiState.fenText,
                        selectedSide = selectedSide
                    )
                    updatePositionEditorPreview(
                        fen = updatedFen,
                        selectedSide = selectedSide,
                        foundGameIds = uiState.foundGameIds
                    )
                },
                onCastlingStateChange = { castlingState ->
                    val normalizedFen = normalizePositionEditorFen(
                        fen = uiState.fenText,
                        selectedSide = uiState.selectedSide
                    )
                    updatePositionEditorPreview(
                        fen = replacePositionEditorFenCastlingPart(
                            fen = normalizedFen,
                            castlingState = castlingState
                        ),
                        foundGameIds = uiState.foundGameIds
                    )
                },
                onClearBoardClick = {
                    val updatedFen = resolveEmptyBoardFen(uiState.selectedSide)
                    updatePositionEditorPreview(updatedFen)
                },
                onSetInitialPositionClick = {
                    val updatedFen = replaceFenSide(
                        fen = InitialBoardFenWithoutMoveNumbers,
                        selectedSide = uiState.selectedSide
                    )
                    updatePositionEditorPreview(updatedFen)
                }
            ),
            board = PositionEditorScreenActions.Board(
                onPieceSelected = { uiState = uiState.copy(selectedPiece = it) },
                onBoardSquareClick = { square ->
                    val updatedFen = placePieceOnFen(
                        fen = gameController.getFen(),
                        square = square,
                        pieceLetter = uiState.selectedPiece.letter
                    )
                    updatePositionEditorPreview(updatedFen)
                },
                onBoardPieceMove = { fromSquare, toSquare ->
                    val updatedFen = movePieceOnFen(
                        fen = gameController.getFen(),
                        fromSquare = fromSquare,
                        toSquare = toSquare
                    )
                    updatePositionEditorPreview(updatedFen)
                }
            ),
            topBar = PositionEditorScreenActions.TopBar(
                onApplyFenClick = {
                    applyPositionEditorFen(uiState.fenText)
                },
                onSavePositionClick = {
                    if (applyPositionEditorFen(uiState.fenText)) {
                        saveDialogState = PositionEditorSaveDialogState()
                    }
                },
                onFindGamesClick = {
                    scope.launch {
                        if (!applyPositionEditorFen(uiState.fenText)) {
                            return@launch
                        }

                        Log.d(
                            PositionEditorLogTag,
                            "findGames fen=${gameController.getFen()} hash=${calculateFenHashWithoutMoveNumbers(gameController.getFen())}"
                        )
                        val foundGameIds = withContext(Dispatchers.IO) {
                            screenContext.inDbProvider.findGameIdsByFenWithoutMoveNumber(
                                gameController.getFen()
                            )
                        }

                        uiState = uiState.copy(
                            foundGameIds = foundGameIds,
                            fenError = null
                        )
                    }
                }
            ),
            saveDialog = PositionEditorScreenActions.SaveDialog(
                onDismiss = { saveDialogState = null },
                onPositionNameChange = { updatedName ->
                    saveDialogState = saveDialogState?.copy(
                        positionName = updatedName,
                        errorMessage = null
                    )
                },
                onConfirm = {
                    val currentSaveDialogState = saveDialogState
                    if (currentSaveDialogState != null) {
                        val trimmedName = currentSaveDialogState.positionName.trim()
                        if (trimmedName.isBlank()) {
                            updateSaveDialogError("Position name is required.")
                        } else {
                            scope.launch {
                                if (!applyPositionEditorFen(uiState.fenText)) {
                                    return@launch
                                }

                                val currentFen = gameController.getFen()
                                val saveResult = withContext(Dispatchers.IO) {
                                    savedSearchPositionService.create(
                                        name = trimmedName,
                                        fenForSearch = currentFen,
                                        fenFull = currentFen
                                    )
                                }
                                val saveErrorMessage = resolveSavePositionErrorMessage(saveResult)
                                if (saveErrorMessage != null) {
                                    updateSaveDialogError(saveErrorMessage)
                                    return@launch
                                }

                                val savedPositionId = (saveResult as SaveSavedSearchPositionResult.Success).id
                                saveDialogState = null
                                uiState = uiState.copy(
                                    infoDialog = PositionEditorInfoDialog(
                                        title = "Position Saved",
                                        message = "Position \"$trimmedName\" saved.\nID: $savedPositionId"
                                    ),
                                    fenError = null
                                )
                            }
                        }
                    }
                }
            ),
            foundGamesDialog = PositionSearchResultDialogActions(
                onDismiss = { uiState = uiState.copy(foundGameIds = null) },
                onCreateTrainingClick = createTrainingFromFoundGames@{
                    val foundGameIds = uiState.foundGameIds ?: return@createTrainingFromFoundGames
                    screenContext.onNavigate(ScreenType.CreateTrainingFromGameIds(foundGameIds))
                    uiState = uiState.copy(foundGameIds = null)
                },
                onCreateTemplateClick = ::createTemplateFromFoundGames
            ),
            feedback = PositionEditorScreenActions.Feedback(
                onFenErrorDismiss = { uiState = uiState.copy(fenError = null) },
                onInfoDialogDismiss = { uiState = uiState.copy(infoDialog = null) }
            )
        ),
        navigation = PositionEditorNavigationActions(
            onBackClick = screenContext.onBackClick,
            onNavigate = screenContext.onNavigate
        ),
        modifier = modifier
    )
}

@Composable
private fun PositionEditorScreen(
    gameController: GameController,
    state: PositionEditorScreenState,
    actions: PositionEditorScreenActions,
    navigation: PositionEditorNavigationActions = PositionEditorNavigationActions(),
    modifier: Modifier = Modifier
) {
    RenderPositionEditorFenError(
        fenError = state.editor.fenError,
        onDismiss = actions.feedback.onFenErrorDismiss
    )
    RenderPositionSearchResultDialog(
        foundGameIds = state.editor.foundGameIds,
        actions = actions.foundGamesDialog
    )
    RenderPositionEditorInfoDialog(
        infoDialog = state.editor.infoDialog,
        onDismiss = actions.feedback.onInfoDialogDismiss
    )
    RenderPositionEditorSaveDialog(
        saveDialogState = state.saveDialog,
        actions = actions.saveDialog
    )

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Position Editor",
                onBackClick = navigation.onBackClick,
                actions = {
                    SecondaryButton(
                        text = "Apply FEN",
                        onClick = actions.topBar.onApplyFenClick
                    )
                    IconButton(
                        onClick = actions.topBar.onSavePositionClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save",
                            tint = TrainingAccentTeal
                        )
                    }
                    IconButton(
                        onClick = actions.topBar.onFindGamesClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Find Games",
                            tint = TrainingTextPrimary
                        )
                    }
                }
            )
        },
        bottomBar = {
            AppBottomNavigation(
                items = defaultAppBottomNavigationItems(),
                selectedItem = ScreenType.Home,
                onItemSelected = navigation.onNavigate
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag(PositionEditorListTestTag),
            contentPadding = PaddingValues(horizontal = AppDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            item {
                Spacer(modifier = Modifier.height(AppDimens.spaceXs))
            }

            item {
                PositionEditorFenSection(
                    fenText = state.editor.fenText,
                    onFenTextChange = actions.position.onFenTextChange
                )
            }

            item {
                PositionEditorCastlesSection(
                    castlingState = resolvePositionEditorCastlingState(state.editor.fenText),
                    onCastlingStateChange = actions.position.onCastlingStateChange
                )
            }

            item {
                PositionEditorBoardSection(
                    gameController = gameController,
                    onBoardSquareClick = actions.board.onBoardSquareClick,
                    onBoardPieceMove = actions.board.onBoardPieceMove
                )
            }

            item {
                PositionEditorControlsSection(
                    selectedSide = state.editor.selectedSide,
                    actions = actions.position
                )
            }

            item {
                PositionEditorPaletteSection(
                    selectedPiece = state.editor.selectedPiece,
                    onPieceSelected = actions.board.onPieceSelected
                )
            }

            item {
                Spacer(modifier = Modifier.height(AppDimens.spaceXs))
            }
        }
    }
}

@Composable
private fun RenderPositionEditorInfoDialog(
    infoDialog: PositionEditorInfoDialog?,
    onDismiss: () -> Unit
) {
    val currentDialog = infoDialog ?: return

    AppMessageDialog(
        title = currentDialog.title,
        message = currentDialog.message,
        onDismiss = onDismiss
    )
}

@Composable
private fun RenderPositionEditorFenError(
    fenError: String?,
    onDismiss: () -> Unit
) {
    if (fenError == null) {
        return
    }

    AppMessageDialog(
        title = "Invalid FEN",
        message = fenError,
        onDismiss = onDismiss
    )
}

@Composable
private fun PositionEditorFenSection(
    fenText: String,
    onFenTextChange: (String) -> Unit
) {
    ScreenSection {
        Column(modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = fenText,
                onValueChange = onFenTextChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = TextColor.Primary
                ),
                cursorBrush = SolidColor(TrainingAccentTeal),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = AppDimens.spaceMd,
                                vertical = AppDimens.spaceMd
                            )
                    ) {
                        if (fenText.isBlank()) {
                            BodySecondaryText(text = "Enter a FEN string")
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
private fun PositionEditorBoardSection(
    gameController: GameController,
    onBoardSquareClick: (String) -> Unit,
    onBoardPieceMove: (String, String) -> Unit
) {
    val boardState = gameController.boardState

    ScreenSection {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            key(boardState) {
                PositionEditorBoardWithCoordinates(
                    gameController = gameController,
                    onSquareClick = onBoardSquareClick,
                    onPieceMove = onBoardPieceMove,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun PositionEditorControlsSection(
    selectedSide: EditableGameSide,
    actions: PositionEditorScreenActions.Position
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
        verticalAlignment = Alignment.Top
    ) {
        GameSideSelector(
            selectedSide = selectedSide,
            onSideSelected = actions.onSideSelected,
            modifier = Modifier.weight(1f)
        )

        PositionEditorActionButtons(
            onClearBoardClick = actions.onClearBoardClick,
            onSetInitialPositionClick = actions.onSetInitialPositionClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PositionEditorActionButtons(
    onClearBoardClick: () -> Unit,
    onSetInitialPositionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
    ) {
        SecondaryButton(
            text = "Clear board",
            onClick = onClearBoardClick,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(PositionEditorClearBoardTestTag)
        )
        SecondaryButton(
            text = "Initial position",
            onClick = onSetInitialPositionClick,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(PositionEditorInitialPositionTestTag)
        )
    }
}

@Composable
private fun PositionEditorPaletteSection(
    selectedPiece: PositionEditorPieceOption,
    onPieceSelected: (PositionEditorPieceOption) -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenSection(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(AppDimens.spaceSm))
            BodySecondaryText(text = selectedPiece.label)
            Spacer(modifier = Modifier.height(AppDimens.spaceMd))

            PositionEditorPieceGrid(
                selectedPiece = selectedPiece,
                onPieceSelected = onPieceSelected
            )
        }
    }
}

@Composable
private fun PositionEditorPieceGrid(
    selectedPiece: PositionEditorPieceOption,
    onPieceSelected: (PositionEditorPieceOption) -> Unit
) {
    PositionEditorPieceOptions.chunked(6).forEach { rowOptions ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            rowOptions.forEach { pieceOption ->
                PositionEditorPieceIcon(
                    pieceOption = pieceOption,
                    isSelected = pieceOption == selectedPiece,
                    onClick = { onPieceSelected(pieceOption) }
                )
            }
        }
        Spacer(modifier = Modifier.height(AppDimens.spaceMd))
    }
}

@Composable
private fun PositionEditorPieceIcon(
    pieceOption: PositionEditorPieceOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                color = if (isSelected) SideButtonSelectedBg else Color.Transparent,
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = resolvePieceGlyph(pieceOption.letter) ?: pieceOption.letter.toString(),
            color = resolvePieceTint(pieceOption.letter),
            style = MaterialTheme.typography.titleLarge
        )
    }
}

private fun normalizePositionEditorFen(
    fen: String,
    selectedSide: EditableGameSide
): String {
    val trimmedFen = fen.trim()
    if (trimmedFen.isBlank()) {
        return resolveEmptyBoardFen(selectedSide)
    }

    val fenParts = trimmedFen.split(Regex("\\s+"))
    if (fenParts.size == 1) {
        return buildPositionEditorFen(
            boardPart = fenParts.first(),
            sidePart = resolveFenSideToken(selectedSide),
            castlingPart = "-",
            enPassantPart = "-"
        )
    }

    return buildPositionEditorFen(
        boardPart = fenParts[0],
        sidePart = fenParts.getOrNull(1)?.takeIf { it == "w" || it == "b" }
            ?: resolveFenSideToken(selectedSide),
        castlingPart = fenParts.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "-",
        enPassantPart = fenParts.getOrNull(3)?.takeIf { it.isNotBlank() } ?: "-"
    )
}

private fun replaceFenSide(
    fen: String,
    selectedSide: EditableGameSide
): String {
    val normalizedFen = normalizePositionEditorFen(
        fen = fen,
        selectedSide = selectedSide
    )
    val fenParts = normalizedFen.split(Regex("\\s+"))

    return buildPositionEditorFen(
        boardPart = fenParts[0],
        sidePart = resolveFenSideToken(selectedSide),
        castlingPart = fenParts[2],
        enPassantPart = fenParts[3]
    )
}

private fun resolveEmptyBoardFen(selectedSide: EditableGameSide): String {
    return buildPositionEditorFen(
        boardPart = EmptyBoardBoardPart,
        sidePart = resolveFenSideToken(selectedSide),
        castlingPart = "-",
        enPassantPart = "-"
    )
}

private fun resolveFenSideToken(selectedSide: EditableGameSide): String {
    if (selectedSide == EditableGameSide.AS_BLACK) {
        return "b"
    }

    return "w"
}

private fun buildPositionEditorFen(
    boardPart: String,
    sidePart: String,
    castlingPart: String,
    enPassantPart: String
): String {
    return listOf(
        boardPart,
        sidePart,
        castlingPart,
        enPassantPart
    ).joinToString(separator = " ")
}

private fun toLoadableFen(editorFen: String): String {
    val normalizedFen = editorFen.trim()
    val fenParts = normalizedFen.split(Regex("\\s+"))

    if (fenParts.size >= 6) {
        return normalizedFen
    }

    return "$normalizedFen 0 1"
}

private fun placePieceOnFen(
    fen: String,
    square: String,
    pieceLetter: Char
): String {
    val currentPosition = ChesslibMapper.fromFen(fen)
    val currentPiece = currentPosition.pieces.find { it.field == square }
    val updatedPieces = currentPosition.pieces.filterNot { it.field == square }.toMutableList()

    if (currentPiece?.letter != pieceLetter) {
        updatedPieces += BoardPiece(pieceLetter, square)
    }

    val updatedPosition = BoardPosition(pieces = updatedPieces)
    val metadata = fen.substringAfter(' ', "w KQkq -")
    return buildFenFromBoardPosition(updatedPosition, metadata)
}

private fun movePieceOnFen(
    fen: String,
    fromSquare: String,
    toSquare: String
): String {
    if (fromSquare == toSquare) {
        return fen
    }

    val currentPosition = ChesslibMapper.fromFen(fen)
    val movingPiece = currentPosition.pieces.find { piece ->
        piece.field == fromSquare
    } ?: return fen

    val updatedPieces = currentPosition.pieces
        .filterNot { piece -> piece.field == fromSquare || piece.field == toSquare }
        .toMutableList()
    updatedPieces += movingPiece.copy(field = toSquare)

    val updatedPosition = BoardPosition(pieces = updatedPieces)
    val metadata = fen.substringAfter(' ', "w KQkq -")
    return buildFenFromBoardPosition(updatedPosition, metadata)
}

private fun buildFenFromBoardPosition(
    position: BoardPosition,
    metadata: String
): String {
    val board = Array(8) { CharArray(8) { ' ' } }

    position.pieces.forEach { piece ->
        if (piece.field.length != 2) {
            return@forEach
        }

        val file = piece.field[0]
        val rank = piece.field[1]
        if (file !in 'a'..'h' || rank !in '1'..'8') {
            return@forEach
        }

        val row = 8 - rank.digitToInt()
        val col = file - 'a'
        board[row][col] = piece.letter
    }

    val boardPart = buildString {
        for (row in 0 until 8) {
            var emptySquares = 0
            for (col in 0 until 8) {
                val pieceLetter = board[row][col]
                if (pieceLetter == ' ') {
                    emptySquares++
                    continue
                }

                if (emptySquares > 0) {
                    append(emptySquares)
                    emptySquares = 0
                }
                append(pieceLetter)
            }

            if (emptySquares > 0) {
                append(emptySquares)
            }

            if (row < 7) {
                append('/')
            }
        }
    }

    return "$boardPart $metadata"
}
