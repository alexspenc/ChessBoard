package com.example.chessboard.ui.screen.createOpening

/**
 * Stateful container for the create-opening screen.
 *
 * Keep in this file:
 * - screen-level state
 * - wiring between UI callbacks and services
 * - import/save orchestration needed to prepare data for the UI and post-save flow
 * - navigation callbacks owned by the screen container
 *
 * It is acceptable to add here:
 * - additional screen state fields
 * - state transformations for this screen
 * - calls that prepare data snapshots before passing control to services or dialogs
 *
 * Do not add here:
 * - large chunks of presentational UI markup that belong in CreateOpeningScreen.kt
 * - reusable post-save training/template logic that belongs in CreateOpeningPostSaveFlow.kt
 * - generic repository or service code unrelated to this screen container
 */
import android.app.Activity
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
import com.example.chessboard.service.OneGameTrainingData
import com.example.chessboard.service.buildStoredPgnFromUci
import com.example.chessboard.service.extractPgnHeaders
import com.example.chessboard.service.parsePgnMoves
import com.example.chessboard.service.parsePgnToUciLines
import com.example.chessboard.service.splitPgnChapters
import com.example.chessboard.service.uciMovesToMoves
import com.example.chessboard.ui.screen.EditableGameSide
import com.example.chessboard.ui.screen.ScreenContainerContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One parsed chapter: its PGN headers and the expanded UCI lines. */
internal data class ImportedChapter(
    val headers: Map<String, String>,
    val uciLines: List<List<String>>,
) {
    /** Best human-readable name for this chapter (ChapterName → Event → StudyName → null). */
    val chapterName: String?
        get() = headers["ChapterName"]?.takeIf { it.isNotBlank() && it != "?" }
            ?: headers["Event"]?.takeIf { it.isNotBlank() && it != "?" }
            ?: headers["StudyName"]?.takeIf { it.isNotBlank() && it != "?" }
}

@Composable
fun CreateOpeningScreenContainer(
    activity: Activity,
    screenContext: ScreenContainerContext,
    initialDraft: GameDraft = GameDraft(),
    modifier: Modifier = Modifier,
) {
    val dbProvider = screenContext.inDbProvider
    val gameSaver = remember(dbProvider) { dbProvider.createGameSaver() }
    val trainingService = remember(dbProvider) { dbProvider.createTrainingService() }
    val userProfileService = remember(dbProvider) { dbProvider.createUserProfileService() }
    val gameController = remember { GameController() }
    var gameDraft by remember(initialDraft) { mutableStateOf(initialDraft) }
    var nameError by remember { mutableStateOf(false) }
    var pgnText by remember { mutableStateOf("") }
    var pgnImportError by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var postSaveState by remember { mutableStateOf(CreateOpeningPostSaveState()) }
    var importedChapters by remember { mutableStateOf<List<ImportedChapter>>(emptyList()) }
    var simpleViewEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        simpleViewEnabled = withContext(Dispatchers.IO) { userProfileService.getProfile().simpleViewEnabled }
    }

    fun updateDraftGame(
        transform: (GameEntity) -> GameEntity
    ) {
        gameDraft = gameDraft.copy(game = transform(gameDraft.game))
    }

    // Derived: move-tree display always follows the first chapter
    val importedUciLines = importedChapters.firstOrNull()?.uciLines ?: emptyList()

    LaunchedEffect(initialDraft) {
        fun loadDraftPosition() {
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

        gameDraft = initialDraft
        importedChapters = emptyList()
        pgnText = ""
        loadDraftPosition()
    }

    LaunchedEffect(gameDraft.game.sideMask) {
        gameController.setOrientation(EditableGameSide.fromSideMask(gameDraft.game.sideMask).orientation)
    }

    LaunchedEffect(pgnText) {
        if (pgnText.isBlank()) {
            importedChapters = emptyList()
            return@LaunchedEffect
        }
        delay(600)
        try {
            val chapters = withContext(Dispatchers.Default) {
                splitPgnChapters(pgnText).mapNotNull { chapterPgn ->
                    val headers = extractPgnHeaders(chapterPgn)
                    val uciLines = parsePgnToUciLines(chapterPgn)
                    if (uciLines.isEmpty()) null
                    else ImportedChapter(headers = headers, uciLines = uciLines)
                }
            }
            if (chapters.isEmpty()) {
                pgnImportError = "No valid moves found in PGN text"
            } else {
                val first = chapters.first()
                first.headers["Event"]
                    ?.takeIf { it.isNotBlank() && it != "?" }
                    ?.let { event ->
                        updateDraftGame { draftGame -> draftGame.copy(event = event) }
                    }
                first.headers["ECO"]
                    ?.takeIf { it.isNotBlank() && it != "?" }
                    ?.let { eco ->
                        updateDraftGame { draftGame -> draftGame.copy(eco = eco) }
                    }
                importedChapters = chapters
                gameController.loadFromUciMoves(first.uciLines.first())
                pgnImportError = null
            }
        } catch (_: Exception) {
            pgnImportError = "Failed to parse PGN"
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                (activity as? LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.IO) {
                    try {
                        val content = activity.contentResolver.openInputStream(uri)
                            ?.bufferedReader()
                            ?.use { it.readText() }
                        withContext(Dispatchers.Main) {
                            if (content != null) {
                                pgnText = content
                            } else {
                                pgnImportError = "Could not read the selected file"
                            }
                        }
                    } catch (_: Exception) {
                        withContext(Dispatchers.Main) {
                            pgnImportError = "Failed to read file"
                        }
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
        selectedSide = EditableGameSide.fromSideMask(gameDraft.game.sideMask),
        onSideSelected = { selectedSide ->
            updateDraftGame { draftGame ->
                draftGame.copy(sideMask = selectedSide.sideMask)
            }
        },
        onBackClick = screenContext.onBackClick,
        openingName = gameDraft.game.event.orEmpty(),
        onOpeningNameChange = {
            updateDraftGame { draftGame ->
                draftGame.copy(event = it)
            }
            nameError = false
        },
        ecoCode = gameDraft.game.eco.orEmpty(),
        onEcoCodeChange = { eco ->
            updateDraftGame { draftGame ->
                draftGame.copy(eco = eco)
            }
        },
        nameError = nameError,
        pgnText = pgnText,
        onPgnTextChange = {
            pgnText = it
            importedChapters = emptyList()
        },
        importedUciLines = importedUciLines,
        importedChapterCount = importedChapters.size,
        pgnImportError = pgnImportError,
        onPgnImportErrorDismiss = { pgnImportError = null },
        saveError = saveError,
        onSaveErrorDismiss = { saveError = null },
        onImportFromFileClick = { filePickerLauncher.launch(arrayOf("*/*")) },
        onSave = { scrollToNameField ->
            val isMultiChapter = importedChapters.size > 1
            if (!isMultiChapter && gameDraft.game.event.isNullOrBlank()) {
                nameError = true
                scrollToNameField()
                return@CreateOpeningScreen
            }

            if (isMultiChapter) {
                val chaptersSnapshot = importedChapters
                val ecoCodeSnapshot = gameDraft.game.eco.orEmpty()
                val selectedSideSnapshot = EditableGameSide.fromSideMask(gameDraft.game.sideMask)

                (activity as? LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.IO) {
                    var savedChaptersCount = 0

                    for ((chapterIndex, chapter) in chaptersSnapshot.withIndex()) {
                        val chapterName = chapter.chapterName
                            ?: gameDraft.game.event.orEmpty().ifBlank { null }
                            ?: "Chapter ${chapterIndex + 1}"
                        val chapterEco = ecoCodeSnapshot.ifBlank {
                            chapter.headers["ECO"]?.takeIf { it.isNotBlank() && it != "?" }
                        }
                        val whiteName = chapter.headers["White"]
                            ?.takeIf { it.isNotBlank() && it != "?" } ?: "White"
                        val blackName = chapter.headers["Black"]
                            ?.takeIf { it.isNotBlank() && it != "?" } ?: "Black"

                        val savedIds = buildList {
                            chapter.uciLines.forEachIndexed { index, uciMoves ->
                                val eventName = buildImportedLineEventName(
                                    baseName = chapterName,
                                    index = index,
                                    total = chapter.uciLines.size,
                                )
                                val entity = GameEntity(
                                    white = whiteName.takeIf { it != "White" },
                                    black = blackName.takeIf { it != "Black" },
                                    result = chapter.headers["Result"]
                                        ?.takeIf { it.isNotBlank() && it != "?" },
                                    event = eventName,
                                    eco = chapterEco,
                                    pgn = buildStoredPgnFromUci(
                                        uciMoves = uciMoves,
                                        event = eventName ?: chapterName,
                                        whiteName = whiteName,
                                        blackName = blackName,
                                    ),
                                    initialFen = "",
                                    sideMask = selectedSideSnapshot.sideMask,
                                )
                                gameSaver.saveOrGetExistingGameId(entity, uciMovesToMoves(uciMoves), entity.sideMask)
                                    ?.let { add(it) }
                            }
                        }

                        if (savedIds.isNotEmpty()) {
                            trainingService.createTrainingFromGames(
                                name = chapterName,
                                games = savedIds.map { OneGameTrainingData(gameId = it, weight = 1) },
                            )
                            savedChaptersCount++
                        }
                    }

                    withContext(Dispatchers.Main) {
                        if (savedChaptersCount > 0) {
                            screenContext.onBackClick()
                        } else {
                            saveError = "None of the imported chapters could be saved"
                        }
                    }
                }
            } else {
                val firstChapter = importedChapters.firstOrNull()
                val importedHeaders = firstChapter?.headers ?: emptyMap()
                val importedLines = firstChapter?.uciLines ?: emptyList()
                val openingNameSnapshot = gameDraft.game.event.orEmpty()
                val ecoCodeSnapshot = gameDraft.game.eco.orEmpty()
                val selectedSideSnapshot = EditableGameSide.fromSideMask(gameDraft.game.sideMask)
                val movesSnapshot = gameController.getMovesCopy()
                val generatedPgnSnapshot = if (importedLines.isEmpty()) {
                    gameController.generatePgn(event = openingNameSnapshot.ifBlank { "Opening" })
                } else {
                    null
                }

                (activity as? LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.IO) {
                    val savedGameIds = if (importedLines.isEmpty()) {
                        val entity = GameEntity(
                            event = openingNameSnapshot.ifBlank { null },
                            eco = ecoCodeSnapshot.ifBlank { null },
                            pgn = generatedPgnSnapshot ?: "",
                            initialFen = "",
                            sideMask = selectedSideSnapshot.sideMask,
                        )
                        listOfNotNull(gameSaver.saveGame(entity, movesSnapshot, entity.sideMask))
                    } else {
                        buildList {
                            importedLines.forEachIndexed { index, uciMoves ->
                                val eventName = buildImportedLineEventName(
                                    baseName = openingNameSnapshot,
                                    index = index,
                                    total = importedLines.size,
                                )
                                val entity = GameEntity(
                                    white = importedHeaders["White"]
                                        ?.takeIf { it.isNotBlank() && it != "?" },
                                    black = importedHeaders["Black"]
                                        ?.takeIf { it.isNotBlank() && it != "?" },
                                    result = importedHeaders["Result"]
                                        ?.takeIf { it.isNotBlank() && it != "?" },
                                    site = importedHeaders["Site"]
                                        ?.takeIf { it.isNotBlank() && it != "?" },
                                    round = importedHeaders["Round"]
                                        ?.takeIf { it.isNotBlank() && it != "?" },
                                    event = eventName,
                                    eco = ecoCodeSnapshot.ifBlank { null },
                                    pgn = buildStoredPgnFromUci(
                                        uciMoves = uciMoves,
                                        event = eventName ?: "Opening",
                                        whiteName = importedHeaders["White"]
                                            ?.takeIf { it.isNotBlank() && it != "?" } ?: "White",
                                        blackName = importedHeaders["Black"]
                                            ?.takeIf { it.isNotBlank() && it != "?" } ?: "Black",
                                    ),
                                    initialFen = "",
                                    sideMask = selectedSideSnapshot.sideMask,
                                )
                                gameSaver.saveOrGetExistingGameId(entity, uciMovesToMoves(uciMoves), entity.sideMask)
                                    ?.let { add(it) }
                            }
                        }
                    }

                    if (savedGameIds.isNotEmpty() && simpleViewEnabled) {
                        createOpeningTraining(
                            dbProvider = dbProvider,
                            savedGames = SavedOpeningGames(
                                name = openingNameSnapshot.ifBlank { "Opening" },
                                gameIds = savedGameIds,
                            ),
                        )
                        withContext(Dispatchers.Main) { screenContext.onBackClick() }
                    } else {
                        withContext(Dispatchers.Main) {
                            if (savedGameIds.isNotEmpty()) {
                                postSaveState = startCreateOpeningPostSaveFlow(
                                    openingName = openingNameSnapshot,
                                    savedGameIds = savedGameIds,
                                )
                            } else {
                                saveError = if (importedLines.isEmpty()) {
                                    "Failed to save opening"
                                } else {
                                    "None of the imported lines could be saved"
                                }
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier,
    )
}
