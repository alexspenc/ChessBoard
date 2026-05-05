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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.boardmodel.GameDraft
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.entity.tutorial.TutorialStage
import com.example.chessboard.entity.tutorial.TutorialType
import com.example.chessboard.service.parsePgnMoves
import com.example.chessboard.ui.screen.EditableGameSide
import com.example.chessboard.ui.screen.ScreenContainerContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PgnImportDebounceMs = 600L

@Composable
internal fun CreateOpeningScreenContainer(
    activity: Activity,
    screenContext: ScreenContainerContext,
    initialDraft: GameDraft = GameDraft(),
    modifier: Modifier = Modifier,
) {
    val dbProvider = screenContext.inDbProvider
    val gameSaver = remember(dbProvider) { dbProvider.createGameSaver() }
    val trainingService = remember(dbProvider) { dbProvider.createTrainingService() }
    val tutorialService = remember(dbProvider) { dbProvider.createTutorialService() }
    val userProfileService = remember(dbProvider) { dbProvider.createUserProfileService() }
    val gameController = remember { GameController() }
    var gameDraft by remember(initialDraft) { mutableStateOf(initialDraft) }
    var showOpeningNameError by remember { mutableStateOf(false) }
    var pgnText by remember { mutableStateOf("") }
    var pgnImportError by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var postSaveState by remember { mutableStateOf(CreateOpeningPostSaveState()) }
    var importedChapters by remember { mutableStateOf<List<ImportedChapter>>(emptyList()) }
    var simpleViewEnabled by remember { mutableStateOf(false) }
    var manualTutorialMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val profile = withContext(Dispatchers.IO) { userProfileService.getProfile() }
        val activeTutorial = withContext(Dispatchers.IO) { tutorialService.getActiveTutorial() }
        simpleViewEnabled = profile.simpleViewEnabled
        manualTutorialMode = activeTutorial?.tutorialType == TutorialType.MANUAL_FIRST_FLOW &&
            activeTutorial.stage == TutorialStage.START
    }

    // Derived: move-tree display always follows the first chapter
    val importedUciLines = importedChapters.firstOrNull()?.uciLines ?: emptyList()

    LaunchedEffect(initialDraft) {
        gameDraft = initialDraft
        importedChapters = emptyList()
        pgnText = ""
        loadDraftPosition(
            initialDraft = initialDraft,
            gameController = gameController,
        )
    }

    LaunchedEffect(gameDraft.game.sideMask) {
        gameController.setOrientation(EditableGameSide.fromSideMask(gameDraft.game.sideMask).orientation)
    }

    LaunchedEffect(pgnText) {
        if (pgnText.isBlank()) {
            importedChapters = emptyList()
            pgnImportError = null
            return@LaunchedEffect
        }

        if (manualTutorialMode) {
            importedChapters = emptyList()
            pgnImportError = null
            return@LaunchedEffect
        }

        // Debounce PGN parsing so we do not re-parse on every keystroke while the user is still typing.
        delay(PgnImportDebounceMs)

        try {
            val chapters = withContext(Dispatchers.Default) { parseImportedChapters(pgnText) }
            if (chapters.isEmpty()) {
                importedChapters = emptyList()
                pgnImportError = "No valid moves found in PGN text"
                return@LaunchedEffect
            }

            val firstChapter = chapters.first()
            gameDraft = applyImportedChapterToDraft(
                gameDraft = gameDraft,
                importedChapter = firstChapter,
            )
            importedChapters = chapters
            gameController.loadFromUciMoves(firstChapter.uciLines.first())
            pgnImportError = null
        } catch (_: Exception) {
            importedChapters = emptyList()
            pgnImportError = "Failed to parse PGN"
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
                            pgnImportError = "Could not read the selected file"
                            return@withContext
                        }

                        pgnText = content
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        pgnImportError = "Failed to read file"
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

    CreateOpeningScreen(
        gameController = gameController,
        state = CreateOpeningScreenState(
            selectedSide = EditableGameSide.fromSideMask(gameDraft.game.sideMask),
            openingName = gameDraft.game.event.orEmpty(),
            ecoCode = gameDraft.game.eco.orEmpty(),
            showOpeningNameError = showOpeningNameError,
            manualTutorialMode = manualTutorialMode,
            pgnText = pgnText,
            importedUciLines = importedUciLines,
            importedChapterCount = importedChapters.size,
            pgnImportError = pgnImportError,
            saveError = saveError,
        ),
        actions = CreateOpeningScreenActions(
            onSideSelected = { selectedSide ->
                gameDraft = updateDraftGame(gameDraft) { draftGame ->
                    draftGame.copy(sideMask = selectedSide.sideMask)
                }
            },
            onBackClick = screenContext.onBackClick,
            onOpeningNameChange = {
                gameDraft = updateDraftGame(gameDraft) { draftGame ->
                    draftGame.copy(event = it)
                }
                showOpeningNameError = false
            },
            onEcoCodeChange = { eco ->
                gameDraft = updateDraftGame(gameDraft) { draftGame ->
                    draftGame.copy(eco = eco)
                }
            },
            onPgnTextChange = onPgnTextChange@{
                if (manualTutorialMode) {
                    return@onPgnTextChange
                }
                pgnText = it
                importedChapters = emptyList()
                pgnImportError = null
            },
            onPgnImportErrorDismiss = { pgnImportError = null },
            onSaveErrorDismiss = { saveError = null },
            onImportFromFileClick = onImportFromFileClick@{
                if (manualTutorialMode) {
                    return@onImportFromFileClick
                }
                filePickerLauncher.launch(arrayOf("*/*"))
            },
            onSave = onSaveAction@{ scrollToNameField ->
                val isMultiChapter = importedChapters.size > 1
                if (!isMultiChapter && gameDraft.game.event.isNullOrBlank()) {
                    showOpeningNameError = true
                    scrollToNameField()
                    return@onSaveAction
                }

                val lifecycleOwner = activity as? LifecycleOwner ?: return@onSaveAction
                if (manualTutorialMode) {
                    val openingNameSnapshot = gameDraft.game.event.orEmpty()
                    val ecoCodeSnapshot = gameDraft.game.eco.orEmpty()
                    val selectedSideSnapshot = EditableGameSide.fromSideMask(gameDraft.game.sideMask)
                    val entity = buildManualOpeningEntity(
                        openingName = openingNameSnapshot,
                        ecoCode = ecoCodeSnapshot,
                        generatedPgn = gameController.generatePgn(
                            event = openingNameSnapshot.ifBlank { "Opening" }
                        ),
                        selectedSide = selectedSideSnapshot,
                    )
                    val movesSnapshot = gameController.getMovesCopy()

                    lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        val savedGameId = gameSaver.saveGame(
                            game = entity,
                            moves = movesSnapshot,
                            sideMask = entity.sideMask,
                        )
                        if (savedGameId == null) {
                            withContext(Dispatchers.Main) {
                                saveError = "Failed to save opening"
                            }
                            return@launch
                        }

                        val updatedTutorial = tutorialService.markGameCreated(savedGameId)
                        withContext(Dispatchers.Main) {
                            if (updatedTutorial == null) {
                                saveError = "Tutorial game was saved, but the tutorial step could not be updated"
                            } else {
                                screenContext.onBackClick()
                            }
                        }
                    }
                    return@onSaveAction
                }

                val saveSnapshot = buildCreateOpeningSaveSnapshot(
                    gameDraft = gameDraft,
                    importedChapters = importedChapters,
                    gameController = gameController,
                    simpleViewEnabled = simpleViewEnabled,
                )

                lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    val saveResult = saveOpening(
                        snapshot = saveSnapshot,
                        dbProvider = dbProvider,
                        gameSaver = gameSaver,
                        trainingService = trainingService,
                    )
                    withContext(Dispatchers.Main) {
                        applyCreateOpeningSaveResult(
                            saveResult = saveResult,
                            onBackClick = screenContext.onBackClick,
                            onPostSaveStateChange = { postSaveState = it },
                            onSaveError = { message -> saveError = message },
                        )
                    }
                }
            }
        ),
        modifier = modifier,
    )
}

private fun buildCreateOpeningSaveSnapshot(
    gameDraft: GameDraft,
    importedChapters: List<ImportedChapter>,
    gameController: GameController,
    simpleViewEnabled: Boolean,
): CreateOpeningSaveSnapshot {
    val openingName = gameDraft.game.event.orEmpty()
    val generatedPgn = if (importedChapters.isEmpty()) {
        gameController.generatePgn(event = openingName.ifBlank { "Opening" })
    } else {
        ""
    }

    return CreateOpeningSaveSnapshot(
        openingName = openingName,
        ecoCode = gameDraft.game.eco.orEmpty(),
        selectedSide = EditableGameSide.fromSideMask(gameDraft.game.sideMask),
        importedChapters = importedChapters,
        movesSnapshot = gameController.getMovesCopy(),
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
    initialDraft: GameDraft,
    gameController: GameController,
) {
    if (initialDraft.game.pgn.isBlank()) {
        gameController.resetToStartPosition()
        return
    }

    val uciMoves = parsePgnMoves(initialDraft.game.pgn)
    if (uciMoves.isEmpty()) {
        gameController.resetToStartPosition()
        return
    }

    gameController.loadFromUciMoves(uciMoves)
}

private fun readImportedPgnText(
    activity: Activity,
    uri: Uri,
): String? {
    return activity.contentResolver.openInputStream(uri)
        ?.bufferedReader()
        ?.use { reader -> reader.readText() }
}

private fun updateDraftGame(
    gameDraft: GameDraft,
    transform: (GameEntity) -> GameEntity,
): GameDraft {
    return gameDraft.copy(game = transform(gameDraft.game))
}
