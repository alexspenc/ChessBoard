package com.example.chessboard.ui.screen.createOpening

/**
 * File role: groups create-opening save planning and persistence orchestration for this screen.
 * Allowed here:
 * - screen-specific save snapshots and results
 * - mapping create-opening state into saved games and trainings
 * - save helpers for manual, imported single-chapter, and imported multi-chapter flows
 * Not allowed here:
 * - compose UI rendering or launcher setup
 * - reusable app-wide persistence helpers unrelated to create-opening
 * Validation date: 2026-05-05
 */

import com.example.chessboard.entity.GameEntity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.GameSaver
import com.example.chessboard.service.OneGameTrainingData
import com.example.chessboard.service.TrainingService
import com.example.chessboard.service.buildStoredPgnFromUci
import com.example.chessboard.service.uciMovesToMoves
import com.example.chessboard.ui.screen.EditableGameSide
import com.github.bhlangonijr.chesslib.move.Move

internal data class CreateOpeningSaveSnapshot(
    val openingName: String,
    val ecoCode: String,
    val selectedSide: EditableGameSide,
    val importedChapters: List<ImportedChapter>,
    val movesSnapshot: List<Move>,
    val generatedPgn: String,
    val simpleViewEnabled: Boolean,
)

internal sealed interface CreateOpeningSaveResult {
    data object NavigateBack : CreateOpeningSaveResult
    data class OpenPostSaveFlow(
        val state: CreateOpeningPostSaveState,
    ) : CreateOpeningSaveResult
    data class ShowError(
        val message: String,
    ) : CreateOpeningSaveResult
}

internal data class ImportedOpeningLineSavePlan(
    val entity: GameEntity,
    val uciMoves: List<String>,
)

internal suspend fun saveOpening(
    snapshot: CreateOpeningSaveSnapshot,
    dbProvider: DatabaseProvider,
    gameSaver: GameSaver,
    trainingService: TrainingService,
): CreateOpeningSaveResult {
    if (snapshot.importedChapters.size > 1) {
        return saveImportedChaptersAsTrainings(
            snapshot = snapshot,
            gameSaver = gameSaver,
            trainingService = trainingService,
        )
    }

    return saveSingleOpening(
        snapshot = snapshot,
        dbProvider = dbProvider,
        gameSaver = gameSaver,
    )
}

internal fun buildImportedLineSavePlans(
    baseName: String,
    importedChapter: ImportedChapter,
    ecoCode: String,
    selectedSide: EditableGameSide,
): List<ImportedOpeningLineSavePlan> {
    val resolvedEcoCode = resolveImportedEcoCode(
        ecoCode = ecoCode,
        importedChapter = importedChapter,
    )
    val whiteName = importedChapter.headerValue("White") ?: "White"
    val blackName = importedChapter.headerValue("Black") ?: "Black"

    return importedChapter.uciLines.mapIndexed { index, uciMoves ->
        val eventName = buildImportedLineEventName(
            baseName = baseName,
            index = index,
            total = importedChapter.uciLines.size,
        )

        ImportedOpeningLineSavePlan(
            entity = GameEntity(
                white = importedChapter.headerValue("White"),
                black = importedChapter.headerValue("Black"),
                result = importedChapter.headerValue("Result"),
                site = importedChapter.headerValue("Site"),
                round = importedChapter.headerValue("Round"),
                event = eventName,
                eco = resolvedEcoCode,
                pgn = buildStoredPgnFromUci(
                    uciMoves = uciMoves,
                    event = eventName ?: "Opening",
                    whiteName = whiteName,
                    blackName = blackName,
                ),
                initialFen = "",
                sideMask = selectedSide.sideMask,
            ),
            uciMoves = uciMoves,
        )
    }
}

internal fun buildManualOpeningEntity(
    openingName: String,
    ecoCode: String,
    generatedPgn: String,
    selectedSide: EditableGameSide,
): GameEntity {
    return GameEntity(
        event = openingName.ifBlank { null },
        eco = ecoCode.ifBlank { null },
        pgn = generatedPgn,
        initialFen = "",
        sideMask = selectedSide.sideMask,
    )
}

private suspend fun saveImportedChaptersAsTrainings(
    snapshot: CreateOpeningSaveSnapshot,
    gameSaver: GameSaver,
    trainingService: TrainingService,
): CreateOpeningSaveResult {
    var savedChaptersCount = 0

    snapshot.importedChapters.forEachIndexed { chapterIndex, importedChapter ->
        val chapterName = resolveImportedChapterSaveName(
            importedChapter = importedChapter,
            fallbackOpeningName = snapshot.openingName,
            chapterIndex = chapterIndex,
        )
        val savePlans = buildImportedLineSavePlans(
            baseName = chapterName,
            importedChapter = importedChapter,
            ecoCode = snapshot.ecoCode,
            selectedSide = snapshot.selectedSide,
        )
        val savedGameIds = saveImportedLinePlans(
            gameSaver = gameSaver,
            savePlans = savePlans,
        )

        if (savedGameIds.isEmpty()) {
            return@forEachIndexed
        }

        trainingService.createTrainingFromGames(
            name = chapterName,
            games = savedGameIds.map { gameId ->
                OneGameTrainingData(gameId = gameId, weight = 1)
            },
        )
        savedChaptersCount++
    }

    if (savedChaptersCount == 0) {
        return CreateOpeningSaveResult.ShowError("None of the imported chapters could be saved")
    }

    return CreateOpeningSaveResult.NavigateBack
}

private suspend fun saveSingleOpening(
    snapshot: CreateOpeningSaveSnapshot,
    dbProvider: DatabaseProvider,
    gameSaver: GameSaver,
): CreateOpeningSaveResult {
    val firstChapter = snapshot.importedChapters.firstOrNull()
    if (firstChapter == null) {
        return saveManualOpening(
            snapshot = snapshot,
            dbProvider = dbProvider,
            gameSaver = gameSaver,
        )
    }

    val savedGameIds = saveImportedLinePlans(
        gameSaver = gameSaver,
        savePlans = buildImportedLineSavePlans(
            baseName = snapshot.openingName,
            importedChapter = firstChapter,
            ecoCode = snapshot.ecoCode,
            selectedSide = snapshot.selectedSide,
        ),
    )
    if (savedGameIds.isEmpty()) {
        return CreateOpeningSaveResult.ShowError("None of the imported lines could be saved")
    }

    if (snapshot.simpleViewEnabled) {
        createOpeningTraining(
            dbProvider = dbProvider,
            savedGames = SavedOpeningGames(
                name = snapshot.openingName.ifBlank { "Opening" },
                gameIds = savedGameIds,
            ),
        )
        return CreateOpeningSaveResult.NavigateBack
    }

    return CreateOpeningSaveResult.OpenPostSaveFlow(
        state = startCreateOpeningPostSaveFlow(
            openingName = snapshot.openingName,
            savedGameIds = savedGameIds,
        ),
    )
}

private suspend fun saveManualOpening(
    snapshot: CreateOpeningSaveSnapshot,
    dbProvider: DatabaseProvider,
    gameSaver: GameSaver,
): CreateOpeningSaveResult {
    val entity = buildManualOpeningEntity(
        openingName = snapshot.openingName,
        ecoCode = snapshot.ecoCode,
        generatedPgn = snapshot.generatedPgn,
        selectedSide = snapshot.selectedSide,
    )
    val savedGameId = gameSaver.saveGame(
        game = entity,
        moves = snapshot.movesSnapshot,
        sideMask = entity.sideMask,
    )
    if (savedGameId == null) {
        return CreateOpeningSaveResult.ShowError("Failed to save opening")
    }

    if (snapshot.simpleViewEnabled) {
        createOpeningTraining(
            dbProvider = dbProvider,
            savedGames = SavedOpeningGames(
                name = snapshot.openingName.ifBlank { "Opening" },
                gameIds = listOf(savedGameId),
            ),
        )
        return CreateOpeningSaveResult.NavigateBack
    }

    return CreateOpeningSaveResult.OpenPostSaveFlow(
        state = startCreateOpeningPostSaveFlow(
            openingName = snapshot.openingName,
            savedGameIds = listOf(savedGameId),
        ),
    )
}

private suspend fun saveImportedLinePlans(
    gameSaver: GameSaver,
    savePlans: List<ImportedOpeningLineSavePlan>,
): List<Long> {
    val savedGameIds = mutableListOf<Long>()

    for (savePlan in savePlans) {
        val savedId = gameSaver.saveOrGetExistingGameId(
            game = savePlan.entity,
            moves = uciMovesToMoves(savePlan.uciMoves),
            sideMask = savePlan.entity.sideMask,
        )
        if (savedId != null) {
            savedGameIds.add(savedId)
        }
    }

    return savedGameIds
}

private fun resolveImportedChapterSaveName(
    importedChapter: ImportedChapter,
    fallbackOpeningName: String,
    chapterIndex: Int,
): String {
    val importedChapterName = importedChapter.resolveChapterName()
    if (importedChapterName != null) {
        return importedChapterName
    }

    if (fallbackOpeningName.isNotBlank()) {
        return fallbackOpeningName
    }

    return "Chapter ${chapterIndex + 1}"
}

private fun resolveImportedEcoCode(
    ecoCode: String,
    importedChapter: ImportedChapter,
): String? {
    if (ecoCode.isNotBlank()) {
        return ecoCode
    }

    return importedChapter.headerValue("ECO")
}
