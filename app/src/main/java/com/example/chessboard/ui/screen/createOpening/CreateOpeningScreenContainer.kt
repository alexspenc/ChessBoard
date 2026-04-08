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
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.service.OneGameTrainingData
import com.example.chessboard.service.buildStoredPgnFromUci
import com.example.chessboard.service.extractPgnHeaders
import com.example.chessboard.service.parsePgnToUciLines
import com.example.chessboard.service.splitPgnChapters
import com.example.chessboard.service.uciMovesToMoves
import com.example.chessboard.ui.screen.EditableGameSide
import com.example.chessboard.ui.screen.ScreenContainerContext
import kotlinx.coroutines.Dispatchers
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
    var postSaveState by remember { mutableStateOf(CreateOpeningPostSaveState()) }
    var importedChapters by remember { mutableStateOf<List<ImportedChapter>>(emptyList()) }

    // Derived: move-tree display always follows the first chapter
    val importedUciLines = importedChapters.firstOrNull()?.uciLines ?: emptyList()

    LaunchedEffect(selectedSide) {
        gameController.setOrientation(selectedSide.orientation)
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
                                importedChapters = emptyList()
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
            importedChapters = emptyList()
        },
        importedUciLines = importedUciLines,
        importedChapterCount = importedChapters.size,
        pgnImportError = pgnImportError,
        onPgnImportErrorDismiss = { pgnImportError = null },
        saveError = saveError,
        onSaveErrorDismiss = { saveError = null },
        onImportFromFileClick = { filePickerLauncher.launch(arrayOf("*/*")) },
        onImportPgnClick = {
            (activity as? LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.Default) {
                try {
                    val chapters = splitPgnChapters(pgnText).mapNotNull { chapterPgn ->
                        val headers = extractPgnHeaders(chapterPgn)
                        val uciLines = parsePgnToUciLines(chapterPgn)
                        if (uciLines.isEmpty()) null
                        else ImportedChapter(headers = headers, uciLines = uciLines)
                    }

                    withContext(Dispatchers.Main) {
                        if (chapters.isEmpty()) {
                            pgnImportError = "No valid moves found in PGN text"
                        } else {
                            val first = chapters.first()
                            first.headers["Event"]
                                ?.takeIf { it.isNotBlank() && it != "?" }
                                ?.let { openingName = it }
                            first.headers["ECO"]
                                ?.takeIf { it.isNotBlank() && it != "?" }
                                ?.let { ecoCode = it }
                            importedChapters = chapters
                            gameController.loadFromUciMoves(first.uciLines.first())
                            pgnImportError = null
                        }
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        pgnImportError = "Failed to parse PGN"
                    }
                }
            }
        },
        onSave = {
            val isMultiChapter = importedChapters.size > 1
            if (!isMultiChapter && openingName.isBlank()) {
                nameError = true
                return@CreateOpeningScreen
            }

            if (isMultiChapter) {
                val chaptersSnapshot = importedChapters
                val ecoCodeSnapshot = ecoCode
                val selectedSideSnapshot = selectedSide

                (activity as? LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.IO) {
                    var savedChaptersCount = 0

                    for ((chapterIndex, chapter) in chaptersSnapshot.withIndex()) {
                        val chapterName = chapter.chapterName
                            ?: openingName.ifBlank { null }
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
                                dbProvider.addGameAndGetId(entity, uciMovesToMoves(uciMoves))
                                    ?.let { add(it) }
                            }
                        }

                        if (savedIds.isNotEmpty()) {
                            dbProvider.createTrainingFromGames(
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
                val openingNameSnapshot = openingName
                val ecoCodeSnapshot = ecoCode
                val selectedSideSnapshot = selectedSide
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
                        listOfNotNull(dbProvider.addGameAndGetId(entity, movesSnapshot))
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
                                dbProvider.addGameAndGetId(entity, uciMovesToMoves(uciMoves))
                                    ?.let { add(it) }
                            }
                        }
                    }

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
        },
        modifier = modifier,
    )
}
