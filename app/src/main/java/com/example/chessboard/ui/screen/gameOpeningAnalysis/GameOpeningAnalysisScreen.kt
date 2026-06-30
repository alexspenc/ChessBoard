@file:Suppress("FunctionName")

package com.example.chessboard.ui.screen.gameOpeningAnalysis

/*
 * File role: renders the game-opening analysis screen entry point and screen-level mode switching.
 * Allowed here:
 * - screen-level UI for imported game opening analysis
 * - summary, empty state, import, filter, analysis dialog orchestration, imported-game list rendering, selected-game preview, and result detail routing
 * - switching between imported-games, analysis-results, and result-detail screen modes
 * - file-picker orchestration for importing PGN text into the existing runtime import flow
 * - thin container wiring that supplies saved opening lines to the runtime batch-analysis runner
 * Not allowed here:
 * - PGN parsing, analyzer algorithms, persistence writes, or reusable generic components
 * Validation date: 2026-06-29
 */

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.chessboard.R
import com.example.chessboard.analysis.OpeningSide
import com.example.chessboard.boardmodel.InitialBoardFenWithoutMoveNumbers
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.runtimecontext.GameOpeningAnalysisFilter
import com.example.chessboard.runtimecontext.GameOpeningAnalysisOptions
import com.example.chessboard.runtimecontext.GameOpeningAnalysisRuntimeContext
import com.example.chessboard.runtimecontext.GameOpeningAnalysisView
import com.example.chessboard.runtimecontext.GameOpeningBatchAnalysisSummary
import com.example.chessboard.runtimecontext.ImportGamesSummary
import com.example.chessboard.runtimecontext.ImportedGameAnalysisResult
import com.example.chessboard.runtimecontext.ImportedGameItem
import com.example.chessboard.runtimecontext.analyzeImportedGameOpeningsAgainstBook
import com.example.chessboard.runtimecontext.parseGameOpeningAnalysisPgnCandidatesWithProgress
import com.example.chessboard.runtimecontext.resolveGameOpeningAnalysisParallelism
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.GameOpeningAnalysisAddGamesTestTag
import com.example.chessboard.ui.GameOpeningAnalysisAnalyzeActionTestTag
import com.example.chessboard.ui.GameOpeningAnalysisClearFilterTestTag
import com.example.chessboard.ui.GameOpeningAnalysisContentTestTag
import com.example.chessboard.ui.GameOpeningAnalysisEmptyStateTestTag
import com.example.chessboard.ui.GameOpeningAnalysisGameListTestTag
import com.example.chessboard.ui.GameOpeningAnalysisImportConfirmTestTag
import com.example.chessboard.ui.GameOpeningAnalysisImportDialogTestTag
import com.example.chessboard.ui.GameOpeningAnalysisImportFromFileTestTag
import com.example.chessboard.ui.GameOpeningAnalysisImportSummaryDialogTestTag
import com.example.chessboard.ui.GameOpeningAnalysisImportTextInputTestTag
import com.example.chessboard.ui.GameOpeningAnalysisNextGamesPageTestTag
import com.example.chessboard.ui.GameOpeningAnalysisNextMoveTestTag
import com.example.chessboard.ui.GameOpeningAnalysisNextResultsPageTestTag
import com.example.chessboard.ui.GameOpeningAnalysisPreviewTestTag
import com.example.chessboard.ui.GameOpeningAnalysisPreviousGamesPageTestTag
import com.example.chessboard.ui.GameOpeningAnalysisPreviousMoveTestTag
import com.example.chessboard.ui.GameOpeningAnalysisPreviousResultsPageTestTag
import com.example.chessboard.ui.GameOpeningAnalysisSearchActionTestTag
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BoardActionNavigationBar
import com.example.chessboard.ui.components.BoardActionNavigationItem
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.ChessBoardSection
import com.example.chessboard.ui.components.HomeIconButton
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.LineMoveTreeSection
import com.example.chessboard.ui.components.PasteInputBlock
import com.example.chessboard.ui.components.SecondaryButton
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.BottomBarContentColor
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

internal typealias GameOpeningAnalysisRunner = suspend (
    runtimeContext: GameOpeningAnalysisRuntimeContext,
    options: GameOpeningAnalysisOptions,
    shouldCancel: () -> Boolean,
) -> GameOpeningBatchAnalysisSummary

private sealed interface GameOpeningAnalysisRunMessage {
    data object NoResults : GameOpeningAnalysisRunMessage
}

@Composable
fun GameOpeningAnalysisScreenContainer(
    screenContext: ScreenContainerContext,
    modifier: Modifier = Modifier,
) {
    val analysisErrorMessage = stringResource(R.string.game_opening_analysis_failed)
    GameOpeningAnalysisScreen(
        runtimeContext = screenContext.runtimeContext.gameOpeningAnalysis,
        onBackClick = screenContext.onBackClick,
        onHomeClick = { screenContext.onNavigate(ScreenType.Home) },
        modifier = modifier,
        analysisRunner = { runtimeContext, options, shouldCancel ->
            val bookLines =
                withContext(Dispatchers.IO) {
                    screenContext.inDbProvider.getAllLines()
                }
            withContext(Dispatchers.Default) {
                analyzeImportedGameOpeningsAgainstBook(
                    runtimeContext = runtimeContext,
                    options = options,
                    gameInitialFen = InitialBoardFenWithoutMoveNumbers,
                    bookLines = bookLines,
                    shouldCancel = shouldCancel,
                )
            }
        },
        onAnalysisError = { error ->
            screenContext.errorReporter.report(error, message = analysisErrorMessage)
        },
    )
}

@Composable
internal fun GameOpeningAnalysisScreen(
    runtimeContext: GameOpeningAnalysisRuntimeContext,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
    analysisRunner: GameOpeningAnalysisRunner = ::runEmptyGameOpeningAnalysis,
    onAnalysisError: (Throwable) -> Unit = {},
) {
    val importedGames = runtimeContext.importedGames
    val visibleGames = runtimeContext.visibleGames()
    val filteredGames = runtimeContext.filteredGames()
    val selectedGame = visibleGames.firstOrNull { game -> game.id == runtimeContext.selectedGameId }
    val visibleResults = runtimeContext.visibleResults()
    val selectedAnalysisResult = runtimeContext.selectedAnalysisResult()
    val currentView = runtimeContext.currentView
    val showingResults = currentView == GameOpeningAnalysisView.ANALYSIS_RESULTS
    val showingResultDetail = currentView == GameOpeningAnalysisView.ANALYSIS_RESULT_DETAIL
    val lineController = remember { LineController(resolveBoardOrientation(runtimeContext.filter.side)) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val hasActiveFilter = runtimeContext.filter != GameOpeningAnalysisFilter()
    var showImportDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showAnalysisOptionsDialog by remember { mutableStateOf(false) }
    var draftFilter by remember { mutableStateOf(runtimeContext.filter) }
    var draftAnalysisOptions by remember { mutableStateOf(runtimeContext.lastAnalysisOptions) }
    var analysisCancelFlag by remember { mutableStateOf<AtomicBoolean?>(null) }
    var importJob by remember { mutableStateOf<Job?>(null) }
    var importProgress by remember { mutableStateOf<GameOpeningAnalysisImportProgress?>(null) }
    var analysisRunMessage by remember { mutableStateOf<GameOpeningAnalysisRunMessage?>(null) }
    var importPgnText by remember { mutableStateOf("") }
    var importSummary by remember { mutableStateOf<ImportGamesSummary?>(null) }
    var importFileErrorMessage by remember { mutableStateOf<String?>(null) }
    val failedReadSelectedFileMessage = stringResource(R.string.game_opening_analysis_failed_read_selected_file)
    val failedReadFileMessage = stringResource(R.string.game_opening_analysis_failed_read_file)

    fun startImport(
        loadPgnText: suspend () -> String?,
        onLoadFailed: () -> Unit,
    ) {
        showImportDialog = false
        val importParallelism = resolveGameOpeningAnalysisParallelism()
        val job =
            coroutineScope.launch(start = CoroutineStart.LAZY) {
                try {
                    importProgress = GameOpeningAnalysisImportProgress(
                        processedCount = 0,
                        totalCount = 0,
                        parallelism = importParallelism,
                    )
                    val pgnText = loadPgnText()
                    if (pgnText == null) {
                        onLoadFailed()
                        return@launch
                    }

                    val candidates =
                        withContext(Dispatchers.Default) {
                            parseGameOpeningAnalysisPgnCandidatesWithProgress(
                                pgnText = pgnText,
                                parallelism = importParallelism,
                                onProgress = { processedCount, totalCount ->
                                    withContext(Dispatchers.Main) {
                                        importProgress =
                                            GameOpeningAnalysisImportProgress(
                                                processedCount = processedCount,
                                                totalCount = totalCount,
                                                parallelism = importParallelism,
                                            )
                                    }
                                },
                            )
                        }
                    importSummary = runtimeContext.addImportedGames(candidates)
                    importPgnText = ""
                } finally {
                    importProgress = null
                    importJob = null
                }
            }
        importJob = job
        job.start()
    }

    fun startPastedTextImport(pgnText: String) {
        startImport(
            loadPgnText = { pgnText },
            onLoadFailed = {},
        )
    }

    val filePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
            onResult = { uri ->
                val selectedUri = uri ?: return@rememberLauncherForActivityResult
                var fileReadErrorHandled = false
                startImport(
                    loadPgnText = {
                        try {
                            withContext(Dispatchers.IO) {
                                readGameOpeningAnalysisPgnText(context = context, uri = selectedUri)
                            }
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Exception) {
                            fileReadErrorHandled = true
                            importFileErrorMessage = failedReadFileMessage
                            null
                        }
                    },
                    onLoadFailed = {
                        if (!fileReadErrorHandled) {
                            importFileErrorMessage = failedReadSelectedFileMessage
                        }
                    },
                )
            },
        )

    fun startAnalysis(options: GameOpeningAnalysisOptions) {
        showAnalysisOptionsDialog = false
        val cancelFlag = AtomicBoolean(false)
        analysisCancelFlag = cancelFlag
        coroutineScope.launch {
            try {
                val summary = analysisRunner(runtimeContext, options) { cancelFlag.get() }
                analysisCancelFlag = null
                if (summary.wasCancelled) {
                    return@launch
                }

                if (summary.keptResultCount > 0) {
                    runtimeContext.openAnalysisResults()
                    return@launch
                }

                analysisRunMessage = GameOpeningAnalysisRunMessage.NoResults
            } catch (error: CancellationException) {
                analysisCancelFlag = null
                runtimeContext.cancelAnalysis()
                throw error
            } catch (error: Throwable) {
                analysisCancelFlag = null
                runtimeContext.cancelAnalysis()
                onAnalysisError(error)
            }
        }
    }

    fun handleBackClick() {
        if (showingResultDetail) {
            runtimeContext.openAnalysisResults()
            return
        }

        if (showingResults) {
            runtimeContext.openImportedGames()
            return
        }

        onBackClick()
    }

    LaunchedEffect(selectedGame?.id, runtimeContext.filter.side) {
        val orientation = resolveBoardOrientation(runtimeContext.filter.side)
        lineController.setOrientation(orientation)
        if (selectedGame == null) {
            lineController.resetToStartPosition()
            lineController.setUserMovesEnabled(false)
            return@LaunchedEffect
        }

        lineController.loadFromUciMoves(selectedGame.mainLineMoves, targetPly = 0)
        lineController.setUserMovesEnabled(false)
    }

    if (showImportDialog) {
        GameOpeningAnalysisImportDialog(
            pgnText = importPgnText,
            onPgnTextChange = { importPgnText = it },
            onDismiss = { showImportDialog = false },
            onImportClick = { startPastedTextImport(importPgnText) },
            onImportFromFileClick = { filePickerLauncher.launch(arrayOf("*/*")) },
        )
    }

    val currentImportFileErrorMessage = importFileErrorMessage
    if (currentImportFileErrorMessage != null) {
        AppMessageDialog(
            title = stringResource(R.string.game_opening_analysis_import_failed_title),
            message = currentImportFileErrorMessage,
            onDismiss = { importFileErrorMessage = null },
        )
    }

    val currentImportSummary = importSummary
    if (currentImportSummary != null) {
        AppMessageDialog(
            title = stringResource(R.string.game_opening_analysis_import_summary_title),
            message = gameOpeningAnalysisImportSummaryMessage(currentImportSummary),
            onDismiss = { importSummary = null },
            modifier = Modifier.testTag(GameOpeningAnalysisImportSummaryDialogTestTag),
        )
    }

    GameOpeningAnalysisFilterDialog(
        visible = showFilterDialog,
        filter = draftFilter,
        onFilterChange = { draftFilter = it },
        onDismiss = { showFilterDialog = false },
        onApplyClick = {
            runtimeContext.updateFilter(draftFilter)
            showFilterDialog = false
        },
    )

    GameOpeningAnalysisOptionsDialog(
        visible = showAnalysisOptionsDialog,
        options = draftAnalysisOptions,
        onOptionsChange = { draftAnalysisOptions = it },
        onDismiss = { showAnalysisOptionsDialog = false },
        onAnalyzeClick = { startAnalysis(draftAnalysisOptions) },
    )

    GameOpeningAnalysisProgressDialog(
        progress = runtimeContext.analysisProgress,
        onCancel = { analysisCancelFlag?.set(true) },
    )

    GameOpeningAnalysisImportProgressDialog(
        progress = importProgress,
        onCancel = { importJob?.cancel() },
    )

    val currentAnalysisRunMessage = analysisRunMessage
    if (currentAnalysisRunMessage != null) {
        AppMessageDialog(
            title = analysisRunMessageTitle(currentAnalysisRunMessage),
            message = analysisRunMessageBody(currentAnalysisRunMessage),
            onDismiss = { analysisRunMessage = null },
        )
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = gameOpeningAnalysisTopBarTitle(currentView),
                subtitle =
                    gameOpeningAnalysisTopBarSubtitle(
                        currentView = currentView,
                        gamesCount = filteredGames.size,
                        currentGamesPage = runtimeContext.currentGamesPage(),
                        totalGamesPages = runtimeContext.totalGamesPages(),
                        analysisResultsCount = runtimeContext.analysisResults.size,
                        visibleResultsCount = visibleResults.size,
                        selectedAnalysisResult = selectedAnalysisResult,
                    ),
                onBackClick = ::handleBackClick,
                handleSystemBack = true,
                filledBackButton = true,
                actions = {
                    HomeIconButton(onClick = onHomeClick)
                    if (showingResults) {
                        IconButton(
                            onClick = { runtimeContext.openPreviousResultsPage() },
                            enabled = runtimeContext.canOpenPreviousResultsPage(),
                            modifier = Modifier.testTag(GameOpeningAnalysisPreviousResultsPageTestTag),
                        ) {
                            IconMd(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.common_previous),
                                tint =
                                    resolveGameOpeningAnalysisPageArrowTint(
                                        runtimeContext.canOpenPreviousResultsPage(),
                                    ),
                            )
                        }
                        IconButton(
                            onClick = { runtimeContext.openNextResultsPage() },
                            enabled = runtimeContext.canOpenNextResultsPage(),
                            modifier = Modifier.testTag(GameOpeningAnalysisNextResultsPageTestTag),
                        ) {
                            IconMd(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.common_next),
                                tint =
                                    resolveGameOpeningAnalysisPageArrowTint(
                                        runtimeContext.canOpenNextResultsPage(),
                                    ),
                            )
                        }
                    }
                    if (!showingResults && !showingResultDetail) {
                        IconButton(
                            onClick = {
                                draftFilter = runtimeContext.filter
                                showFilterDialog = true
                            },
                            modifier = Modifier.testTag(GameOpeningAnalysisSearchActionTestTag),
                        ) {
                            IconMd(
                                imageVector = Icons.Default.Search,
                                contentDescription =
                                    stringResource(
                                        R.string.game_opening_analysis_filter_content_description,
                                    ),
                                tint = TextColor.Primary,
                            )
                        }
                        if (hasActiveFilter) {
                            IconButton(
                                onClick = { runtimeContext.clearFilter() },
                                modifier = Modifier.testTag(GameOpeningAnalysisClearFilterTestTag),
                            ) {
                                IconMd(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription =
                                        stringResource(
                                            R.string.game_opening_analysis_clear_filter_content_description,
                                        ),
                                    tint = TextColor.Primary,
                                )
                            }
                        }
                        IconButton(
                            onClick = { runtimeContext.openPreviousGamesPage() },
                            enabled = runtimeContext.canOpenPreviousGamesPage(),
                            modifier = Modifier.testTag(GameOpeningAnalysisPreviousGamesPageTestTag),
                        ) {
                            IconMd(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription =
                                    stringResource(
                                        R.string.game_opening_analysis_previous_games_page,
                                    ),
                                tint =
                                    resolveGameOpeningAnalysisPageArrowTint(
                                        runtimeContext.canOpenPreviousGamesPage(),
                                    ),
                            )
                        }
                        IconButton(
                            onClick = { runtimeContext.openNextGamesPage() },
                            enabled = runtimeContext.canOpenNextGamesPage(),
                            modifier = Modifier.testTag(GameOpeningAnalysisNextGamesPageTestTag),
                        ) {
                            IconMd(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription =
                                    stringResource(
                                        R.string.game_opening_analysis_next_games_page,
                                    ),
                                tint =
                                    resolveGameOpeningAnalysisPageArrowTint(
                                        runtimeContext.canOpenNextGamesPage(),
                                    ),
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (!showingResults && !showingResultDetail) {
                GameOpeningAnalysisBoardControlsBar(
                    canUndo = selectedGame != null && lineController.canUndo,
                    canRedo = selectedGame != null && lineController.canRedo,
                    onPreviousMoveClick = { lineController.undoMove() },
                    onNextMoveClick = { lineController.redoMove() },
                    onAddGamesClick = { showImportDialog = true },
                    onAnalyzeClick = {
                        draftAnalysisOptions = runtimeContext.lastAnalysisOptions
                        showAnalysisOptionsDialog = true
                    },
                    canAnalyze = filteredGames.isNotEmpty() && runtimeContext.analysisProgress == null,
                )
            }
        },
    ) { paddingValues ->
        if (showingResultDetail) {
            GameOpeningAnalysisResultDetailContent(
                analysisResult = selectedAnalysisResult,
                modifier = Modifier.padding(paddingValues),
            )
            return@AppScreenScaffold
        }

        if (showingResults) {
            GameOpeningAnalysisResultsContent(
                runtimeContext = runtimeContext,
                modifier = Modifier.padding(paddingValues),
            )
            return@AppScreenScaffold
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(AppDimens.spaceLg)
                    .testTag(GameOpeningAnalysisContentTestTag),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
        ) {
            if (visibleGames.isEmpty()) {
                GameOpeningAnalysisEmptyState(hasImportedGames = importedGames.isNotEmpty())
                return@Column
            }

            BodySecondaryText(
                text = stringResource(R.string.game_opening_analysis_list_hint),
                color = TextColor.Secondary,
            )

            visibleGames.forEach { game ->
                if (game.id == selectedGame?.id) {
                    ImportedGamePreview(
                        game = game,
                        lineController = lineController,
                        onMovePlyClick = { targetPly ->
                            lineController.loadFromUciMoves(game.mainLineMoves, targetPly = targetPly)
                            lineController.setUserMovesEnabled(false)
                        },
                    )
                    return@forEach
                }

                ImportedGameCard(
                    game = game,
                    onClick = {
                        runtimeContext.selectGame(game.id)
                        lineController.setOrientation(resolveBoardOrientation(runtimeContext.filter.side))
                        lineController.loadFromUciMoves(game.mainLineMoves, targetPly = 0)
                        lineController.setUserMovesEnabled(false)
                    },
                )
            }
        }
    }
}

private fun readGameOpeningAnalysisPgnText(
    context: Context,
    uri: Uri,
): String? =
    context.contentResolver
        .openInputStream(uri)
        ?.bufferedReader()
        ?.use { reader -> reader.readText() }

@Composable
private fun gameOpeningAnalysisImportSummaryMessage(summary: ImportGamesSummary): String {
    return listOf(
        stringResource(R.string.game_opening_analysis_import_summary_scanned, summary.scannedCount),
        stringResource(R.string.game_opening_analysis_import_summary_added, summary.addedCount),
        stringResource(
            R.string.game_opening_analysis_import_summary_skipped_duplicates,
            summary.skippedDuplicateCount,
        ),
        stringResource(
            R.string.game_opening_analysis_import_summary_skipped_parse_errors,
            summary.skippedParseErrorCount,
        ),
    ).joinToString(separator = "\n")
}

@Composable
private fun gameOpeningAnalysisTopBarTitle(currentView: GameOpeningAnalysisView): String {
    when (currentView) {
        GameOpeningAnalysisView.ANALYSIS_RESULTS -> {
            return stringResource(R.string.game_opening_analysis_results_title)
        }

        GameOpeningAnalysisView.ANALYSIS_RESULT_DETAIL -> {
            return stringResource(R.string.game_opening_analysis_result_detail_title)
        }

        GameOpeningAnalysisView.IMPORTED_GAMES -> {
            return stringResource(R.string.game_opening_analysis_title)
        }
    }
}

@Composable
private fun gameOpeningAnalysisTopBarSubtitle(
    currentView: GameOpeningAnalysisView,
    gamesCount: Int,
    currentGamesPage: Int,
    totalGamesPages: Int,
    analysisResultsCount: Int,
    visibleResultsCount: Int,
    selectedAnalysisResult: ImportedGameAnalysisResult?,
): String {
    when (currentView) {
        GameOpeningAnalysisView.ANALYSIS_RESULTS -> {
            return stringResource(
                R.string.game_opening_analysis_results_subtitle,
                analysisResultsCount,
                visibleResultsCount,
            )
        }

        GameOpeningAnalysisView.ANALYSIS_RESULT_DETAIL -> {
            val unknownEvent = stringResource(R.string.game_opening_analysis_unknown_event)
            return selectedAnalysisResult?.game?.displayEvent(unknownEvent).orEmpty()
        }

        GameOpeningAnalysisView.IMPORTED_GAMES -> {
            return stringResource(
                R.string.game_opening_analysis_subtitle,
                gamesCount,
                currentGamesPage,
                totalGamesPages,
            )
        }
    }
}

private fun resolveGameOpeningAnalysisPageArrowTint(enabled: Boolean): Color {
    if (enabled) {
        return TextColor.Primary
    }

    return TextColor.Secondary
}

@Composable
private fun GameOpeningAnalysisImportDialog(
    pgnText: String,
    onPgnTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onImportClick: () -> Unit,
    onImportFromFileClick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val maxDialogHeight = LocalConfiguration.current.screenHeightDp.dp * 0.88f

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.spaceLg)
                    .heightIn(max = maxDialogHeight)
                    .testTag(GameOpeningAnalysisImportDialogTestTag),
            shape = RoundedCornerShape(AppDimens.radiusLg),
            color = Background.ScreenDark,
        ) {
            Column(
                modifier =
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(AppDimens.spaceLg),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            ) {
                SectionTitleText(text = stringResource(R.string.game_opening_analysis_import_dialog_title))
                PasteInputBlock(
                    title = stringResource(R.string.game_opening_analysis_import_pgn_label),
                    text = pgnText,
                    onTextChange = onPgnTextChange,
                    placeholder = stringResource(R.string.game_opening_analysis_import_pgn_placeholder),
                    minLines = 10,
                    inputTestTag = GameOpeningAnalysisImportTextInputTestTag,
                    onImportFromFileClick = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        onImportFromFileClick()
                    },
                    importFromFileTestTag = GameOpeningAnalysisImportFromFileTestTag,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SecondaryButton(
                        text = stringResource(R.string.common_cancel),
                        onClick = onDismiss,
                    )
                    SecondaryButton(
                        text = stringResource(R.string.game_opening_analysis_import_action),
                        onClick = {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            onImportClick()
                        },
                        enabled = pgnText.isNotBlank(),
                        modifier = Modifier.testTag(GameOpeningAnalysisImportConfirmTestTag),
                    )
                }
            }
        }
    }
}

@Composable
private fun GameOpeningAnalysisBoardControlsBar(
    canUndo: Boolean,
    canRedo: Boolean,
    onPreviousMoveClick: () -> Unit,
    onNextMoveClick: () -> Unit,
    onAddGamesClick: () -> Unit,
    onAnalyzeClick: () -> Unit,
    canAnalyze: Boolean,
    modifier: Modifier = Modifier,
) {
    BoardActionNavigationBar(
        modifier = modifier,
        maxVisibleItems = 4,
        items =
            listOf(
                BoardActionNavigationItem(
                    label = stringResource(R.string.game_opening_analysis_add_games_action),
                    selected = true,
                    modifier = Modifier.testTag(GameOpeningAnalysisAddGamesTestTag),
                    onClick = onAddGamesClick,
                ) {
                    IconMd(
                        imageVector = Icons.Default.Add,
                        contentDescription =
                            stringResource(
                                R.string.game_opening_analysis_add_games_content_description,
                            ),
                        tint = TrainingAccentTeal,
                    )
                },
                BoardActionNavigationItem(
                    label = stringResource(R.string.game_opening_analysis_analyze_action),
                    selected = true,
                    enabled = canAnalyze,
                    modifier = Modifier.testTag(GameOpeningAnalysisAnalyzeActionTestTag),
                    onClick = onAnalyzeClick,
                ) {
                    IconMd(
                        imageVector = Icons.Default.Biotech,
                        contentDescription = stringResource(R.string.game_opening_analysis_analyze_action),
                        tint = resolveMoveControlTint(canAnalyze),
                    )
                },
                BoardActionNavigationItem(
                    label = stringResource(R.string.common_back),
                    enabled = canUndo,
                    modifier = Modifier.testTag(GameOpeningAnalysisPreviousMoveTestTag),
                    onClick = onPreviousMoveClick,
                ) {
                    IconMd(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription =
                            stringResource(
                                R.string.game_opening_analysis_previous_move_content_description,
                            ),
                        tint = resolveMoveControlTint(canUndo),
                    )
                },
                BoardActionNavigationItem(
                    label = stringResource(R.string.common_forward),
                    enabled = canRedo,
                    modifier = Modifier.testTag(GameOpeningAnalysisNextMoveTestTag),
                    onClick = onNextMoveClick,
                ) {
                    IconMd(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription =
                            stringResource(
                                R.string.game_opening_analysis_next_move_content_description,
                            ),
                        tint = resolveMoveControlTint(canRedo),
                    )
                },
            ),
    )
}

@Composable
private fun GameOpeningAnalysisEmptyState(hasImportedGames: Boolean) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .testTag(GameOpeningAnalysisEmptyStateTestTag),
        contentAlignment = Alignment.Center,
    ) {
        val emptyMessage =
            if (hasImportedGames) {
                stringResource(R.string.game_opening_analysis_empty_filtered)
            } else {
                stringResource(R.string.game_opening_analysis_empty)
            }

        BodySecondaryText(
            text = emptyMessage,
            color = TextColor.Secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ImportedGameCard(
    game: ImportedGameItem,
    onClick: () -> Unit,
) {
    val unknownEvent = stringResource(R.string.game_opening_analysis_unknown_event)
    val unknownPlayer = stringResource(R.string.game_opening_analysis_unknown_player)

    CardSurface(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(GameOpeningAnalysisGameListTestTag),
        onClick = onClick,
    ) {
        ImportedGameHeader(
            game = game,
            unknownEvent = unknownEvent,
            unknownPlayer = unknownPlayer,
        )
    }
}

@Composable
private fun ImportedGamePreview(
    game: ImportedGameItem,
    lineController: LineController,
    onMovePlyClick: (Int) -> Unit,
) {
    val unknownEvent = stringResource(R.string.game_opening_analysis_unknown_event)
    val unknownPlayer = stringResource(R.string.game_opening_analysis_unknown_player)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(GameOpeningAnalysisPreviewTestTag),
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
    ) {
        ChessBoardSection(lineController = lineController)
        ImportedGameHeader(
            game = game,
            unknownEvent = unknownEvent,
            unknownPlayer = unknownPlayer,
        )
        LineMoveTreeSection(
            importedUciLines = listOf(game.mainLineMoves),
            lineController = lineController,
            modifier = Modifier.fillMaxWidth(),
            onMoveSelected = { _, targetPly -> onMovePlyClick(targetPly) },
        )
    }
}

@Composable
private fun ImportedGameHeader(
    game: ImportedGameItem,
    unknownEvent: String,
    unknownPlayer: String,
) {
    SectionTitleText(text = game.eventTitle(unknownEvent))
    Spacer(modifier = Modifier.height(AppDimens.spaceSm))
    BodySecondaryText(
        text =
            stringResource(
                R.string.game_opening_analysis_players,
                game.playerName(WHITE_HEADER, unknownPlayer),
                game.playerName(BLACK_HEADER, unknownPlayer),
            ),
        color = TextColor.Secondary,
    )
    Spacer(modifier = Modifier.height(AppDimens.spaceSm))
    CardMetaText(
        text = stringResource(R.string.game_opening_analysis_game_ply_count, game.mainLineMoves.size),
        color = TextColor.Secondary,
    )
}

private suspend fun runEmptyGameOpeningAnalysis(
    runtimeContext: GameOpeningAnalysisRuntimeContext,
    options: GameOpeningAnalysisOptions,
    shouldCancel: () -> Boolean,
): GameOpeningBatchAnalysisSummary {
    runtimeContext.setAnalysisOptions(options)
    if (shouldCancel()) {
        runtimeContext.cancelAnalysis()
        return GameOpeningBatchAnalysisSummary(
            analyzedCount = 0,
            keptResultCount = 0,
            wasCancelled = true,
        )
    }

    runtimeContext.replaceAnalysisResults(emptyList())
    return GameOpeningBatchAnalysisSummary(
        analyzedCount = 0,
        keptResultCount = 0,
        wasCancelled = false,
    )
}

@Composable
private fun analysisRunMessageTitle(message: GameOpeningAnalysisRunMessage): String =
    when (message) {
        GameOpeningAnalysisRunMessage.NoResults -> {
            stringResource(R.string.game_opening_analysis_no_results_title)
        }
    }

@Composable
private fun analysisRunMessageBody(message: GameOpeningAnalysisRunMessage): String =
    when (message) {
        GameOpeningAnalysisRunMessage.NoResults -> {
            stringResource(R.string.game_opening_analysis_no_results_message)
        }
    }

private fun resolveBoardOrientation(side: OpeningSide): BoardOrientation {
    if (side == OpeningSide.BLACK) {
        return BoardOrientation.BLACK
    }

    return BoardOrientation.WHITE
}

private fun resolveMoveControlTint(enabled: Boolean): Color {
    if (enabled) {
        return BottomBarContentColor
    }

    return BottomBarContentColor.copy(alpha = 0.5f)
}

private fun ImportedGameItem.eventTitle(unknownEvent: String): String {
    val event = headers[EVENT_HEADER]
    if (!event.isNullOrBlank()) {
        return event
    }

    return unknownEvent
}

private fun ImportedGameItem.playerName(
    headerName: String,
    unknownPlayer: String,
): String = headers[headerName].orUnknownPlayer(unknownPlayer)

private fun String?.orUnknownPlayer(unknownPlayer: String): String {
    if (!isNullOrBlank()) {
        return this
    }

    return unknownPlayer
}

private const val EVENT_HEADER = "Event"
private const val WHITE_HEADER = "White"
private const val BLACK_HEADER = "Black"
