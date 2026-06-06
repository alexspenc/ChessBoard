package com.example.chessboard.ui.screen.createOpening

/**
 * File role: groups the create-opening screen container and its compose-side orchestration.
 * Allowed here:
 * - screen state, effects, launcher wiring, and UI callback orchestration
 * - short transitions between compose state and create-opening helper files
 * - navigation callbacks owned by this screen container
 * Not allowed here:
 * - large presentational UI blocks that belong in CreateOpeningScreen.kt
 * - long PGN import parsing helpers or save-mapping logic that belong in create-opening helper files
 * Validation date: 2026-05-05
 */
import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.chessboard.R
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.boardmodel.LineDraft
import com.example.chessboard.entity.LineEntity
import com.example.chessboard.service.parsePgnMoves
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.screen.EditableLineSide
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PgnImportDebounceMs = 600L

@Composable
internal fun CreateOpeningScreenContainer(
    activity: Activity,
    screenContext: ScreenContainerContext,
    initialDraft: LineDraft = LineDraft(),
    modifier: Modifier = Modifier,
    saveOpeningRunner: CreateOpeningSaveRunner? = null,
) {
    val dbProvider = screenContext.inDbProvider
    val lineSaver = remember(dbProvider) { dbProvider.createLineSaver() }
    val trainingService = remember(dbProvider) { dbProvider.createTrainingService() }
    val userProfileService = remember(dbProvider) { dbProvider.createUserProfileService() }
    val lineController = remember { LineController() }
    var lineDraft by remember(initialDraft) { mutableStateOf(initialDraft) }
    var showOpeningNameError by remember { mutableStateOf(false) }
    var pgnText by remember { mutableStateOf("") }
    var pgnImportError by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var saveRuntimeState by remember { mutableStateOf(CreateOpeningSaveRuntimeState()) }
    var postSaveState by remember { mutableStateOf(CreateOpeningPostSaveState()) }
    var importedChapters by remember { mutableStateOf<List<ImportedChapter>>(emptyList()) }
    var simpleViewEnabled by remember { mutableStateOf(false) }
    val noValidMovesFoundMessage = stringResource(R.string.create_opening_no_valid_moves)
    val failedParsePgnMessage = stringResource(R.string.create_opening_failed_parse_pgn)
    val failedReadSelectedFileMessage = stringResource(R.string.create_opening_failed_read_selected_file)
    val failedReadFileMessage = stringResource(R.string.create_opening_failed_read_file)
    val saveLinesTitle = stringResource(R.string.create_opening_save_lines_title)
    val pgnParseErrorStrings = createOpeningPgnParseErrorStrings()
    val saveStrings = createOpeningSaveStrings()

    LaunchedEffect(Unit) {
        simpleViewEnabled = withContext(Dispatchers.IO) { userProfileService.getProfile().simpleViewEnabled }
    }

    // Derived: move-tree display always follows the first chapter
    val importedUciLines = importedChapters.firstOrNull()?.uciLines ?: emptyList()

    LaunchedEffect(initialDraft) {
        lineDraft = initialDraft
        importedChapters = emptyList()
        pgnText = ""
        loadDraftPosition(
            initialDraft = initialDraft,
            lineController = lineController,
        )
    }

    LaunchedEffect(lineDraft.line.sideMask) {
        lineController.setOrientation(EditableLineSide.fromSideMask(lineDraft.line.sideMask).orientation)
    }

    LaunchedEffect(
        pgnText,
        pgnParseErrorStrings,
        noValidMovesFoundMessage,
        failedParsePgnMessage,
    ) {
        if (pgnText.isBlank()) {
            importedChapters = emptyList()
            pgnImportError = null
            return@LaunchedEffect
        }

        // Debounce PGN parsing so we do not re-parse on every keystroke while the user is still typing.
        delay(PgnImportDebounceMs)

        try {
            val chapters = withContext(Dispatchers.Default) {
                parseImportedChapters(
                    pgnText = pgnText,
                    errorStrings = pgnParseErrorStrings,
                )
            }
            if (chapters.isEmpty()) {
                importedChapters = emptyList()
                pgnImportError = noValidMovesFoundMessage
                return@LaunchedEffect
            }

            val firstChapter = chapters.first()
            lineDraft = applyImportedChapterToDraft(
                lineDraft = lineDraft,
                importedChapter = firstChapter,
            )
            importedChapters = chapters
            lineController.loadFromUciMoves(firstChapter.uciLines.first())
            pgnImportError = null
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            importedChapters = emptyList()
            pgnImportError = error.message ?: failedParsePgnMessage
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            val lifecycleOwner = activity as? LifecycleOwner ?: return@rememberLauncherForActivityResult
            val selectedUri = uri ?: return@rememberLauncherForActivityResult

            lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val content = readImportedPgnText(
                        activity = activity,
                        uri = selectedUri,
                    )
                    withContext(Dispatchers.Main) {
                        if (content == null) {
                            pgnImportError = failedReadSelectedFileMessage
                            return@withContext
                        }

                        pgnText = content
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        pgnImportError = failedReadFileMessage
                    }
                }
            }
        }
    )

    CreateOpeningPostSaveDialogs(
        activity = activity,
        dbProvider = dbProvider,
        state = postSaveState,
        onStateChange = { postSaveState = it },
        onFinished = {
            postSaveState = CreateOpeningPostSaveState()
            screenContext.onBackClick()
        },
        onError = { message ->
            postSaveState = CreateOpeningPostSaveState()
            saveError = message
        },
    )

    val currentSaveRuntimeState = saveRuntimeState
    if (currentSaveRuntimeState.message != null) {
        AppMessageDialog(
            title = saveLinesTitle,
            message = currentSaveRuntimeState.message,
            onDismiss = {
                saveRuntimeState = saveRuntimeState.copy(message = null)
            }
        )
    }

    if (currentSaveRuntimeState.progress != null) {
        CreateOpeningSaveProgressDialog(
            progress = currentSaveRuntimeState.progress,
            onCancel = {
                saveRuntimeState.job?.cancel()
            }
        )
    }

    CreateOpeningScreen(
        lineController = lineController,
        state = CreateOpeningScreenState(
            selectedSide = EditableLineSide.fromSideMask(lineDraft.line.sideMask),
            openingName = lineDraft.line.event.orEmpty(),
            ecoCode = lineDraft.line.eco.orEmpty(),
            showOpeningNameError = showOpeningNameError,
            pgnText = pgnText,
            importedUciLines = importedUciLines,
            importedChapterCount = importedChapters.size,
            pgnImportError = pgnImportError,
            saveError = saveError,
        ),
        actions = CreateOpeningScreenActions(
            onSideSelected = { selectedSide ->
                lineDraft = updateDraftLine(lineDraft) { draftLine ->
                    draftLine.copy(sideMask = selectedSide.sideMask)
                }
            },
            onBackClick = screenContext.onBackClick,
            onHomeClick = { screenContext.onNavigate(ScreenType.Home) },
            onOpeningNameChange = {
                lineDraft = updateDraftLine(lineDraft) { draftLine ->
                    draftLine.copy(event = it)
                }
                showOpeningNameError = false
            },
            onEcoCodeChange = { eco ->
                lineDraft = updateDraftLine(lineDraft) { draftLine ->
                    draftLine.copy(eco = eco)
                }
            },
            onPgnTextChange = {
                pgnText = it
                importedChapters = emptyList()
                pgnImportError = null
            },
            onPgnImportErrorDismiss = { pgnImportError = null },
            onSaveErrorDismiss = { saveError = null },
            onImportFromFileClick = { filePickerLauncher.launch(arrayOf("*/*")) },
            onSave = onSaveAction@{ scrollToNameField ->
                val isMultiChapter = importedChapters.size > 1
                if (!isMultiChapter && lineDraft.line.event.isNullOrBlank()) {
                    showOpeningNameError = true
                    scrollToNameField()
                    return@onSaveAction
                }

                if (saveRuntimeState.job != null) return@onSaveAction
                val lifecycleOwner = activity as? LifecycleOwner ?: return@onSaveAction

                val saveSnapshot = buildCreateOpeningSaveSnapshot(
                    lineDraft = lineDraft,
                    importedChapters = importedChapters,
                    lineController = lineController,
                    defaultOpeningName = saveStrings.defaultOpeningName,
                    simpleViewEnabled = simpleViewEnabled,
                )
                val initialSaveProgress = CreateOpeningSaveProgress(
                    totalLines = countCreateOpeningSaveTargets(saveSnapshot),
                    processedLinesCount = 0,
                    savedLinesCount = 0,
                    skippedLinesCount = 0,
                )

                val runner = saveOpeningRunner ?: ::saveOpening
                val job = lifecycleOwner.lifecycleScope.launch(
                    context = Dispatchers.IO,
                    start = CoroutineStart.LAZY,
                ) {
                    try {
                        val saveResult = runner(
                            saveSnapshot,
                            dbProvider,
                            lineSaver,
                            trainingService,
                            saveStrings,
                        ) { progress ->
                            withContext(Dispatchers.Main) {
                                saveRuntimeState = saveRuntimeState.copy(progress = progress)
                            }
                        }

                        withContext(Dispatchers.Main) {
                            saveRuntimeState = CreateOpeningSaveRuntimeState()
                            applyCreateOpeningSaveResult(
                                saveResult = saveResult,
                                onBackClick = screenContext.onBackClick,
                                onPostSaveStateChange = { postSaveState = it },
                                onSaveError = { message -> saveError = message },
                            )
                        }
                    } catch (_: CancellationException) {
                        withContext(NonCancellable + Dispatchers.Main) {
                            saveRuntimeState = CreateOpeningSaveRuntimeState(
                                message = resolveCreateOpeningSaveCanceledMessage(
                                    progress = saveRuntimeState.progress,
                                    strings = saveStrings.runtime,
                                )
                            )
                        }
                    } catch (error: Exception) {
                        withContext(Dispatchers.Main) {
                            saveRuntimeState = CreateOpeningSaveRuntimeState()
                            saveError = error.message ?: saveStrings.failedSaveOpening
                        }
                    }
                }
                saveRuntimeState = CreateOpeningSaveRuntimeState(
                    progress = initialSaveProgress,
                    job = job,
                )
                job.start()
            }
        ),
        modifier = modifier,
    )
}

private fun buildCreateOpeningSaveSnapshot(
    lineDraft: LineDraft,
    importedChapters: List<ImportedChapter>,
    lineController: LineController,
    defaultOpeningName: String,
    simpleViewEnabled: Boolean,
): CreateOpeningSaveSnapshot {
    val openingName = lineDraft.line.event.orEmpty()
    val generatedPgn = if (importedChapters.isEmpty()) {
        lineController.generatePgn(event = openingName.ifBlank { defaultOpeningName })
    } else {
        ""
    }

    return CreateOpeningSaveSnapshot(
        openingName = openingName,
        ecoCode = lineDraft.line.eco.orEmpty(),
        selectedSide = EditableLineSide.fromSideMask(lineDraft.line.sideMask),
        importedChapters = importedChapters,
        movesSnapshot = lineController.getMovesCopy(),
        generatedPgn = generatedPgn,
        simpleViewEnabled = simpleViewEnabled,
    )
}

private fun applyCreateOpeningSaveResult(
    saveResult: CreateOpeningSaveResult,
    onBackClick: () -> Unit,
    onPostSaveStateChange: (CreateOpeningPostSaveState) -> Unit,
    onSaveError: (String) -> Unit,
) {
    when (saveResult) {
        CreateOpeningSaveResult.NavigateBack -> onBackClick()
        is CreateOpeningSaveResult.OpenPostSaveFlow -> onPostSaveStateChange(saveResult.state)
        is CreateOpeningSaveResult.ShowError -> onSaveError(saveResult.message)
    }
}

private fun loadDraftPosition(
    initialDraft: LineDraft,
    lineController: LineController,
) {
    if (initialDraft.line.pgn.isBlank()) {
        lineController.resetToStartPosition()
        return
    }

    val uciMoves = parsePgnMoves(initialDraft.line.pgn)
    if (uciMoves.isEmpty()) {
        lineController.resetToStartPosition()
        return
    }

    lineController.loadFromUciMoves(uciMoves)
}

private fun readImportedPgnText(
    activity: Activity,
    uri: Uri,
): String? {
    return activity.contentResolver.openInputStream(uri)
        ?.bufferedReader()
        ?.use { reader -> reader.readText() }
}

private fun updateDraftLine(
    lineDraft: LineDraft,
    transform: (LineEntity) -> LineEntity,
): LineDraft {
    return lineDraft.copy(line = transform(lineDraft.line))
}
