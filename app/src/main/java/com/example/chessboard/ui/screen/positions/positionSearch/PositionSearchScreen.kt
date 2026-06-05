package com.example.chessboard.ui.screen.positions.positionSearch

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.chessboard.R
import com.example.chessboard.boardmodel.BoardPiece
import com.example.chessboard.boardmodel.BoardPosition
import com.example.chessboard.boardmodel.ChesslibMapper
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.boardmodel.InitialBoardFenWithoutMoveNumbers
import com.example.chessboard.service.SaveSavedSearchPositionResult
import com.example.chessboard.service.calculateFenHashWithoutMoveNumbers
import com.example.chessboard.ui.PositionSearchBoardWithCoordinates
import com.example.chessboard.ui.PositionSearchClearBoardTestTag
import com.example.chessboard.ui.PositionSearchInitialPositionTestTag
import com.example.chessboard.ui.PositionSearchListTestTag
import com.example.chessboard.ui.components.AppIconSizes
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BoardActionNavigationBar
import com.example.chessboard.ui.components.BoardActionNavigationItem
import com.example.chessboard.ui.components.HomeIconButton
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.PasteInputBlock
import com.example.chessboard.ui.components.SettingsIconButton
import com.example.chessboard.ui.drawPieceGlyph
import com.example.chessboard.ui.screen.EditableLineSide
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.screen.SideButtonSelectedBg
import com.example.chessboard.service.OneLineTrainingData
import com.example.chessboard.ui.screen.positions.PositionSearchResultDialogActions
import com.example.chessboard.ui.screen.positions.PositionTemplateNameDialogState
import com.example.chessboard.ui.screen.positions.RenderPositionSearchResultDialog
import com.example.chessboard.ui.screen.positions.createPositionTemplateFromLineIds
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.BottomBarContentColor
import com.example.chessboard.ui.theme.TrainingTextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val EmptyBoardBoardPart = "8/8/8/8/8/8/8/8"
private const val EmptyBoardFen = "$EmptyBoardBoardPart w - -"
private const val PositionSearchLogTag = "PositionSearch"
private val PositionSearchPalettePieceSize = 44.dp

internal data class PositionSearchPieceOption(
    val letter: Char,
    @StringRes val labelResId: Int,
)

private val PositionSearchPieceOptions = listOf(
    PositionSearchPieceOption('K', R.string.position_search_piece_white_king),
    PositionSearchPieceOption('Q', R.string.position_search_piece_white_queen),
    PositionSearchPieceOption('R', R.string.position_search_piece_white_rook),
    PositionSearchPieceOption('B', R.string.position_search_piece_white_bishop),
    PositionSearchPieceOption('N', R.string.position_search_piece_white_knight),
    PositionSearchPieceOption('P', R.string.position_search_piece_white_pawn),
    PositionSearchPieceOption('k', R.string.position_search_piece_black_king),
    PositionSearchPieceOption('q', R.string.position_search_piece_black_queen),
    PositionSearchPieceOption('r', R.string.position_search_piece_black_rook),
    PositionSearchPieceOption('b', R.string.position_search_piece_black_bishop),
    PositionSearchPieceOption('n', R.string.position_search_piece_black_knight),
    PositionSearchPieceOption('p', R.string.position_search_piece_black_pawn),
)

private data class PositionSearchUiState(
    val fenText: String = EmptyBoardFen,
    val selectedSide: EditableLineSide = EditableLineSide.AS_WHITE,
    val selectedPiece: PositionSearchPieceOption? = null,
    val fenError: String? = null,
    val foundLineIds: List<Long>? = null,
    val infoDialog: PositionSearchInfoDialog? = null
)

private data class PositionSearchInfoDialog(
    val title: String,
    val message: String
)

internal data class PositionSearchSaveDialogState(
    val positionName: String = "",
    val errorMessage: String? = null
)

private data class PositionSearchScreenState(
    val search: PositionSearchUiState,
    val saveDialog: PositionSearchSaveDialogState?,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
)

internal data class PositionSearchScreenActions(
    val position: Position,
    val board: Board,
    val topBar: TopBar,
    val saveDialog: SaveDialog,
    val foundLinesDialog: PositionSearchResultDialogActions,
    val feedback: Feedback
) {
    data class Position(
        val onFenTextChange: (String) -> Unit,
        val onSideSelected: (EditableLineSide) -> Unit,
    )

    data class Board(
        val onPieceSelected: (PositionSearchPieceOption) -> Unit,
        val onBoardSquareClick: (String) -> Unit,
        val onBoardPieceMove: (String, String) -> Unit
    )

    data class TopBar(
        val onSavePositionClick: () -> Unit,
        val onFindLinesClick: () -> Unit,
        val onClearBoardClick: () -> Unit,
        val onSetInitialPositionClick: () -> Unit,
        val onHistoryBackClick: () -> Unit,
        val onHistoryForwardClick: () -> Unit,
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

private data class PositionSearchNavigationActions(
    val onBackClick: () -> Unit = {},
    val onHomeClick: () -> Unit = {},
    val onNavigate: (ScreenType) -> Unit = {},
    val onSettingsClick: () -> Unit = {}
)

private data class PositionSearchSnapshot(
    val fenText: String,
    val selectedSide: EditableLineSide,
)

private data class PositionSearchHistory(
    val past: List<PositionSearchSnapshot> = emptyList(),
    val future: List<PositionSearchSnapshot> = emptyList(),
)

@Composable
fun PositionSearchScreenContainer(
    initialFen: String = InitialBoardFenWithoutMoveNumbers,
    screenContext: ScreenContainerContext,
    simpleViewEnabled: Boolean = false,
    onNavigateToSettings: (currentFen: String) -> Unit = {},
    onShowFoundLinesClick: (lineIds: List<Long>, currentFen: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val savedSearchPositionService = remember(screenContext.inDbProvider) {
        screenContext.inDbProvider.createSavedSearchPositionService()
    }
    val lineController = remember {
        LineController().also { controller ->
            controller.loadPreviewFen(toLoadableFen(EmptyBoardFen))
        }
    }
    val strings = positionSearchStrings()

    var uiState by remember { mutableStateOf(PositionSearchUiState()) }
    var saveDialogState by remember { mutableStateOf<PositionSearchSaveDialogState?>(null) }
    var templateNameDialogState by remember { mutableStateOf<PositionTemplateNameDialogState?>(null) }
    var positionHistory by remember { mutableStateOf(PositionSearchHistory()) }

    fun resolveSelectedSide(fen: String): EditableLineSide {
        val sideToken = fen.trim().split(Regex("\\s+")).getOrNull(1)
        if (sideToken == "b") {
            return EditableLineSide.AS_BLACK
        }

        return EditableLineSide.AS_WHITE
    }

    fun updatePositionSearchPreview(
        fen: String,
        selectedSide: EditableLineSide = uiState.selectedSide,
        foundLineIds: List<Long>? = uiState.foundLineIds,
        recordHistory: Boolean = true,
    ) {
        val normalizedFen = normalizePositionSearchFen(
            fen = fen,
            selectedSide = selectedSide
        )
        if (
            recordHistory &&
            (normalizedFen != uiState.fenText || selectedSide != uiState.selectedSide)
        ) {
            positionHistory = positionHistory.copy(
                past = positionHistory.past + PositionSearchSnapshot(
                    fenText = uiState.fenText,
                    selectedSide = uiState.selectedSide,
                ),
                future = emptyList(),
            )
        }
        lineController.loadPreviewFen(toLoadableFen(normalizedFen))
        uiState = uiState.copy(
            fenText = normalizedFen,
            selectedSide = selectedSide,
            fenError = null,
            foundLineIds = foundLineIds
        )
    }

    fun applyPositionSearchFen(
        fen: String,
        selectedSide: EditableLineSide = uiState.selectedSide,
        foundLineIds: List<Long>? = null
    ): Boolean {
        val normalizedFen = normalizePositionSearchFen(
            fen = fen,
            selectedSide = selectedSide
        )
        val wasLoaded = lineController.loadFromFen(toLoadableFen(normalizedFen))
        if (!wasLoaded) {
            uiState = uiState.copy(fenError = strings.failedApplyFenMessage)
            return false
        }

        uiState = uiState.copy(
            fenText = normalizePositionSearchFen(
                fen = lineController.getFen(),
                selectedSide = selectedSide
            ),
            selectedSide = selectedSide,
            fenError = null,
            foundLineIds = foundLineIds
        )
        return true
    }

    fun applyPositionSearchSnapshot(snapshot: PositionSearchSnapshot) {
        lineController.loadPreviewFen(toLoadableFen(snapshot.fenText))
        uiState = uiState.copy(
            fenText = snapshot.fenText,
            selectedSide = snapshot.selectedSide,
            fenError = null,
            foundLineIds = null,
        )
    }

    fun currentPositionSearchSnapshot(): PositionSearchSnapshot {
        return PositionSearchSnapshot(
            fenText = uiState.fenText,
            selectedSide = uiState.selectedSide,
        )
    }

    fun goBackInPositionHistory() {
        val previousSnapshot = positionHistory.past.lastOrNull() ?: return
        positionHistory = PositionSearchHistory(
            past = positionHistory.past.dropLast(1),
            future = listOf(currentPositionSearchSnapshot()) + positionHistory.future,
        )
        applyPositionSearchSnapshot(previousSnapshot)
    }

    fun goForwardInPositionHistory() {
        val nextSnapshot = positionHistory.future.firstOrNull() ?: return
        positionHistory = PositionSearchHistory(
            past = positionHistory.past + currentPositionSearchSnapshot(),
            future = positionHistory.future.drop(1),
        )
        applyPositionSearchSnapshot(nextSnapshot)
    }

    fun openTemplateNameDialog() {
        val foundLineIds = uiState.foundLineIds ?: return
        uiState = uiState.copy(foundLineIds = null)
        templateNameDialogState = PositionTemplateNameDialogState(
            lineIds = foundLineIds,
            templateName = strings.defaultTemplateName,
        )
    }

    fun createTemplateFromFoundLines() {
        val currentDialogState = templateNameDialogState ?: return
        templateNameDialogState = null

        scope.launch {
            val templateId = withContext(Dispatchers.IO) {
                createPositionTemplateFromLineIds(
                    dbProvider = screenContext.inDbProvider,
                    lineIds = currentDialogState.lineIds,
                    templateName = currentDialogState.templateName,
                    defaultTemplateName = strings.defaultTemplateName,
                )
            }

            if (templateId != null) {
                uiState = uiState.copy(
                    infoDialog = PositionSearchInfoDialog(
                        title = strings.templateCreatedTitle,
                        message = strings.templateCreatedMessage(
                            templateId = templateId,
                            lineCount = currentDialogState.lineIds.size,
                        )
                    ),
                    fenError = null
                )
                return@launch
            }

            uiState = uiState.copy(
                infoDialog = PositionSearchInfoDialog(
                    title = strings.templateErrorTitle,
                    message = strings.templateErrorMessage
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
            return strings.duplicateNameMessage
        }

        if (result is SaveSavedSearchPositionResult.DuplicateSearchFen) {
            return strings.duplicateSearchFenMessage
        }

        if (result is SaveSavedSearchPositionResult.DuplicateFullFen) {
            return strings.duplicateFullFenMessage
        }

        return strings.failedSavePositionMessage
    }

    LaunchedEffect(uiState.selectedSide) {
        lineController.setOrientation(uiState.selectedSide.orientation)
    }

    LaunchedEffect(initialFen) {
        val selectedSide = resolveSelectedSide(initialFen)
        updatePositionSearchPreview(
            fen = initialFen,
            selectedSide = selectedSide,
            foundLineIds = null,
            recordHistory = false,
        )
        positionHistory = PositionSearchHistory()
    }

    PositionSearchScreen(
        lineController = lineController,
        state = PositionSearchScreenState(
            search = uiState,
            saveDialog = saveDialogState,
            canGoBack = positionHistory.past.isNotEmpty(),
            canGoForward = positionHistory.future.isNotEmpty(),
        ),
        actions = PositionSearchScreenActions(
            position = PositionSearchScreenActions.Position(
                onFenTextChange = { newFen ->
                    val normalizedFen = normalizePositionSearchFen(newFen, uiState.selectedSide)
                    lineController.loadPreviewFen(toLoadableFen(normalizedFen))
                    uiState = uiState.copy(fenText = newFen, fenError = null)
                },
                onSideSelected = { selectedSide ->
                    val updatedFen = replaceFenSide(
                        fen = uiState.fenText,
                        selectedSide = selectedSide
                    )
                    updatePositionSearchPreview(
                        fen = updatedFen,
                        selectedSide = selectedSide,
                        foundLineIds = uiState.foundLineIds
                    )
                },
            ),
            board = PositionSearchScreenActions.Board(
                onPieceSelected = { selectedPiece ->
                    uiState = uiState.copy(
                        selectedPiece = if (uiState.selectedPiece == selectedPiece) null else selectedPiece
                    )
                },
                onBoardSquareClick = onBoardSquareClick@{ square ->
                    val selectedPiece = uiState.selectedPiece ?: return@onBoardSquareClick
                    val updatedFen = placePieceOnFen(
                        fen = lineController.getFen(),
                        square = square,
                        pieceLetter = selectedPiece.letter
                    )
                    updatePositionSearchPreview(updatedFen)
                },
                onBoardPieceMove = { fromSquare, toSquare ->
                    val updatedFen = movePieceOnFen(
                        fen = lineController.getFen(),
                        fromSquare = fromSquare,
                        toSquare = toSquare
                    )
                    updatePositionSearchPreview(updatedFen)
                }
            ),
            topBar = PositionSearchScreenActions.TopBar(
                onClearBoardClick = {
                    updatePositionSearchPreview(resolveEmptyBoardFen(uiState.selectedSide), uiState.selectedSide)
                },
                onSetInitialPositionClick = {
                    val parts = InitialBoardFenWithoutMoveNumbers.trim()
                        .split(Regex("\\s+")).toMutableList()
                    if (parts.size >= 2) parts[1] = resolveFenSideToken(uiState.selectedSide)
                    updatePositionSearchPreview(parts.joinToString(" "), uiState.selectedSide)
                },
                onHistoryBackClick = ::goBackInPositionHistory,
                onHistoryForwardClick = ::goForwardInPositionHistory,
                onSavePositionClick = {
                    if (applyPositionSearchFen(uiState.fenText)) {
                        saveDialogState = PositionSearchSaveDialogState()
                    }
                },
                onFindLinesClick = {
                    scope.launch {
                        if (!applyPositionSearchFen(uiState.fenText)) {
                            return@launch
                        }

                        Log.d(
                            PositionSearchLogTag,
                            "findLines fen=${lineController.getFen()} hash=${calculateFenHashWithoutMoveNumbers(lineController.getFen())}"
                        )
                        val foundLineIds = withContext(Dispatchers.IO) {
                            screenContext.inDbProvider.findLineIdsByFenWithoutMoveNumber(
                                lineController.getFen()
                            )
                        }

                        withContext(Dispatchers.Main) {
                            uiState = uiState.copy(
                                foundLineIds = foundLineIds,
                                fenError = null
                            )
                        }
                    }
                }
            ),
            saveDialog = PositionSearchScreenActions.SaveDialog(
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
                            updateSaveDialogError(strings.positionNameRequiredMessage)
                        } else {
                            scope.launch {
                                if (!applyPositionSearchFen(uiState.fenText)) {
                                    return@launch
                                }

                                val currentFen = lineController.getFen()
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
                                    infoDialog = PositionSearchInfoDialog(
                                        title = strings.positionSavedTitle,
                                        message = strings.positionSavedMessage(
                                            positionName = trimmedName,
                                            positionId = savedPositionId,
                                        )
                                    ),
                                    fenError = null
                                )
                            }
                        }
                    }
                }
            ),
            foundLinesDialog = PositionSearchResultDialogActions(
                onDismiss = { uiState = uiState.copy(foundLineIds = null) },
                onCreateTrainingClick = createTrainingFromFoundLines@{
                    val foundLineIds = uiState.foundLineIds ?: return@createTrainingFromFoundLines
                    uiState = uiState.copy(foundLineIds = null)
                    if (simpleViewEnabled) {
                        scope.launch {
                            val trainingId = withContext(Dispatchers.IO) {
                                val allLinesById = screenContext.inDbProvider.getAllLines().associateBy { it.id }
                                val trainingLines = foundLineIds.mapNotNull { lineId ->
                                    allLinesById[lineId]?.let { OneLineTrainingData(lineId = lineId, weight = 1) }
                                }
                                screenContext.inDbProvider.createTrainingService().createTrainingFromLines(
                                    name = strings.simpleTrainingName,
                                    lines = trainingLines,
                                )
                            }
                            uiState = if (trainingId != null) {
                                uiState.copy(
                                    infoDialog = PositionSearchInfoDialog(
                                        title = strings.trainingCreatedTitle,
                                        message = strings.trainingCreatedMessage(foundLineIds.size)
                                    )
                                )
                            } else {
                                uiState.copy(
                                    infoDialog = PositionSearchInfoDialog(
                                        title = strings.trainingErrorTitle,
                                        message = strings.trainingErrorMessage
                                    )
                                )
                            }
                        }
                        return@createTrainingFromFoundLines
                    }
                    screenContext.onNavigate(
                        ScreenType.CreateTrainingFromLineIds(
                            lineIds = foundLineIds,
                            backTarget = ScreenType.PositionSearch,
                            initialTrainingName = null,
                            screenTitle = strings.createTrainingScreenTitle,
                            linesCountLabel = strings.linesFoundCountLabel,
                        )
                    )
                },
                showTemplateAction = !simpleViewEnabled,
                onCreateTemplateClick = ::openTemplateNameDialog,
                onShowLinesClick = showLinesFromFoundLines@{
                    val foundLineIds = uiState.foundLineIds ?: return@showLinesFromFoundLines
                    val currentFen = uiState.fenText
                    uiState = uiState.copy(foundLineIds = null)
                    onShowFoundLinesClick(foundLineIds, currentFen)
                },
                templateNameDialogState = templateNameDialogState,
                onTemplateNameChange = { templateName ->
                    templateNameDialogState?.let { currentDialogState ->
                        templateNameDialogState = currentDialogState.copy(templateName = templateName)
                    }
                },
                onTemplateNameDismiss = { templateNameDialogState = null },
                onConfirmTemplateName = ::createTemplateFromFoundLines,
            ),
            feedback = PositionSearchScreenActions.Feedback(
                onFenErrorDismiss = { uiState = uiState.copy(fenError = null) },
                onInfoDialogDismiss = { uiState = uiState.copy(infoDialog = null) }
            )
        ),
        navigation = PositionSearchNavigationActions(
            onBackClick = screenContext.onBackClick,
            onHomeClick = { screenContext.onNavigate(ScreenType.Home) },
            onNavigate = screenContext.onNavigate,
            onSettingsClick = { onNavigateToSettings(uiState.fenText) }
        ),
        modifier = modifier
    )
}

@Composable
private fun PositionSearchScreen(
    lineController: LineController,
    state: PositionSearchScreenState,
    actions: PositionSearchScreenActions,
    navigation: PositionSearchNavigationActions = PositionSearchNavigationActions(),
    modifier: Modifier = Modifier
) {
    RenderPositionSearchFenError(
        fenError = state.search.fenError,
        onDismiss = actions.feedback.onFenErrorDismiss
    )
    RenderPositionSearchResultDialog(
        foundLineIds = state.search.foundLineIds,
        actions = actions.foundLinesDialog
    )
    RenderPositionSearchInfoDialog(
        infoDialog = state.search.infoDialog,
        onDismiss = actions.feedback.onInfoDialogDismiss
    )
    RenderPositionSearchSaveDialog(
        saveDialogState = state.saveDialog,
        actions = actions.saveDialog
    )

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.position_search_title),
                onBackClick = navigation.onBackClick,
                actions = {
                    HomeIconButton(onClick = navigation.onHomeClick)
                    SettingsIconButton(onClick = navigation.onSettingsClick)
                    IconButton(onClick = actions.topBar.onSavePositionClick) {
                        IconMd(
                            imageVector = Icons.Default.Save,
                            contentDescription = stringResource(R.string.position_search_save_content_description),
                            tint = TrainingAccentTeal,
                        )
                    }
                    IconButton(onClick = actions.topBar.onFindLinesClick) {
                        IconMd(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.position_search_find_lines_content_description),
                            tint = TrainingTextPrimary,
                        )
                    }
                }
            )
        },
        bottomBar = {
            PositionSearchBoardControlsBar(
                selectedSide = state.search.selectedSide,
                canGoBack = state.canGoBack,
                canGoForward = state.canGoForward,
                onSideSelected = actions.position.onSideSelected,
                onResetClick = actions.topBar.onSetInitialPositionClick,
                onClearBoardClick = actions.topBar.onClearBoardClick,
                onBackClick = actions.topBar.onHistoryBackClick,
                onForwardClick = actions.topBar.onHistoryForwardClick,
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag(PositionSearchListTestTag),
            contentPadding = PaddingValues(horizontal = AppDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            item {
                Spacer(modifier = Modifier.height(AppDimens.spaceXs))
            }

            item {
                PasteInputBlock(
                    title = stringResource(R.string.position_search_fen_label),
                    text = state.search.fenText,
                    onTextChange = actions.position.onFenTextChange,
                    placeholder = stringResource(R.string.position_search_fen_placeholder),
                    minLines = 3
                )
            }

            item {
                PositionSearchBoardSection(
                    lineController = lineController,
                    onBoardSquareClick = actions.board.onBoardSquareClick,
                    onBoardPieceMove = actions.board.onBoardPieceMove
                )
            }

            item {
                PositionSearchPaletteSection(
                    selectedPiece = state.search.selectedPiece,
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
private fun RenderPositionSearchInfoDialog(
    infoDialog: PositionSearchInfoDialog?,
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
private fun RenderPositionSearchFenError(
    fenError: String?,
    onDismiss: () -> Unit
) {
    if (fenError == null) {
        return
    }

    AppMessageDialog(
        title = stringResource(R.string.position_search_invalid_fen_title),
        message = fenError,
        onDismiss = onDismiss
    )
}


@Composable
private fun PositionSearchBoardSection(
    lineController: LineController,
    onBoardSquareClick: (String) -> Unit,
    onBoardPieceMove: (String, String) -> Unit
) {
    val boardState = lineController.boardState

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(AppDimens.radiusXl))
    ) {
        key(boardState) {
            PositionSearchBoardWithCoordinates(
                lineController = lineController,
                onSquareClick = onBoardSquareClick,
                onPieceMove = onBoardPieceMove,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}


@Composable
private fun PositionSearchPaletteSection(
    selectedPiece: PositionSearchPieceOption?,
    onPieceSelected: (PositionSearchPieceOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        PositionSearchPieceGrid(
            selectedPiece = selectedPiece,
            onPieceSelected = onPieceSelected
        )
    }
}

@Composable
private fun PositionSearchPieceGrid(
    selectedPiece: PositionSearchPieceOption?,
    onPieceSelected: (PositionSearchPieceOption) -> Unit
) {
    PositionSearchPieceOptions.chunked(6).forEach { rowOptions ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            rowOptions.forEach { pieceOption ->
                PositionSearchPieceIcon(
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
private fun PositionSearchPieceIcon(
    pieceOption: PositionSearchPieceOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val pieceLabel = stringResource(pieceOption.labelResId)

    Box(
        modifier = Modifier
            .size(PositionSearchPalettePieceSize)
            .semantics { contentDescription = pieceLabel }
            .background(
                color = if (isSelected) SideButtonSelectedBg else Color.Transparent,
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        PositionSearchPalettePiece(letter = pieceOption.letter)
    }
}

@Composable
private fun PositionSearchPalettePiece(
    letter: Char,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawPieceGlyph(
            letter = letter,
            left = 0f,
            top = 0f,
            squareSize = size.minDimension,
        )
    }
}

@Composable
private fun PositionSearchBoardControlsBar(
    selectedSide: EditableLineSide,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onSideSelected: (EditableLineSide) -> Unit,
    onResetClick: () -> Unit,
    onClearBoardClick: () -> Unit,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = positionSearchBoardControlStrings()

    BoardActionNavigationBar(
        modifier = modifier,
        maxVisibleItems = 6,
        items = EditableLineSide.entries.map { side ->
            val sideLabel = strings.sideLabel(side)
            BoardActionNavigationItem(
                label = sideLabel,
                selected = side == selectedSide,
                onClick = { onSideSelected(side) },
            ) {
                PositionSearchSideIcon(
                    side = side,
                    selected = side == selectedSide,
                    contentDescription = sideLabel,
                )
            }
        } + listOf(
            BoardActionNavigationItem(
                label = strings.resetLabel,
                modifier = Modifier.testTag(PositionSearchInitialPositionTestTag),
                onClick = onResetClick,
            ) {
                IconMd(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = strings.initialPositionContentDescription,
                    tint = BottomBarContentColor,
                )
            },
            BoardActionNavigationItem(
                label = strings.clearLabel,
                modifier = Modifier.testTag(PositionSearchClearBoardTestTag),
                onClick = onClearBoardClick,
            ) {
                IconMd(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = strings.clearBoardContentDescription,
                    tint = BottomBarContentColor,
                )
            },
            BoardActionNavigationItem(
                label = strings.backLabel,
                enabled = canGoBack,
                onClick = onBackClick,
            ) {
                IconMd(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = strings.backLabel,
                    tint = if (canGoBack) {
                        BottomBarContentColor
                    } else {
                        BottomBarContentColor.copy(alpha = 0.5f)
                    },
                )
            },
            BoardActionNavigationItem(
                label = strings.forwardLabel,
                enabled = canGoForward,
                onClick = onForwardClick,
            ) {
                IconMd(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = strings.forwardLabel,
                    tint = if (canGoForward) {
                        BottomBarContentColor
                    } else {
                        BottomBarContentColor.copy(alpha = 0.5f)
                    },
                )
            },
        ),
    )
}

@Composable
private fun PositionSearchSideIcon(
    side: EditableLineSide,
    selected: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(R.drawable.ic_king),
        contentDescription = contentDescription,
        tint = if (selected) TrainingAccentTeal else BottomBarContentColor,
        modifier = modifier.size(AppIconSizes.Lg),
    )
}

private fun normalizePositionSearchFen(
    fen: String,
    selectedSide: EditableLineSide
): String {
    val trimmedFen = fen.trim()
    if (trimmedFen.isBlank()) {
        return resolveEmptyBoardFen(selectedSide)
    }

    val fenParts = trimmedFen.split(Regex("\\s+"))
    if (fenParts.size == 1) {
        return buildPositionSearchFen(
            boardPart = fenParts.first(),
            sidePart = resolveFenSideToken(selectedSide),
            castlingPart = "-",
            enPassantPart = "-"
        )
    }

    return buildPositionSearchFen(
        boardPart = fenParts[0],
        sidePart = fenParts.getOrNull(1)?.takeIf { it == "w" || it == "b" }
            ?: resolveFenSideToken(selectedSide),
        castlingPart = fenParts.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "-",
        enPassantPart = fenParts.getOrNull(3)?.takeIf { it.isNotBlank() } ?: "-"
    )
}

private fun replaceFenSide(
    fen: String,
    selectedSide: EditableLineSide
): String {
    val normalizedFen = normalizePositionSearchFen(
        fen = fen,
        selectedSide = selectedSide
    )
    val fenParts = normalizedFen.split(Regex("\\s+"))

    return buildPositionSearchFen(
        boardPart = fenParts[0],
        sidePart = resolveFenSideToken(selectedSide),
        castlingPart = fenParts[2],
        enPassantPart = fenParts[3]
    )
}

private fun resolveEmptyBoardFen(selectedSide: EditableLineSide): String {
    return buildPositionSearchFen(
        boardPart = EmptyBoardBoardPart,
        sidePart = resolveFenSideToken(selectedSide),
        castlingPart = "-",
        enPassantPart = "-"
    )
}

private fun resolveFenSideToken(selectedSide: EditableLineSide): String {
    if (selectedSide == EditableLineSide.AS_BLACK) {
        return "b"
    }

    return "w"
}

private fun buildPositionSearchFen(
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

private fun toLoadableFen(positionFen: String): String {
    val normalizedFen = positionFen.trim()
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
