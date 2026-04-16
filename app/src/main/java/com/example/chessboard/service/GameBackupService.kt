package com.example.chessboard.service

import com.example.chessboard.entity.GameEntity
import com.example.chessboard.entity.SideMask
import com.example.chessboard.repository.AppDatabase
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

data class GameBackupRestoreResult(
    val restoredGamesCount: Int,
    val skippedGamesCount: Int
)

data class GameBackupRestoreProgress(
    val totalGames: Int,
    val processedGamesCount: Int,
    val restoredGamesCount: Int,
    val skippedGamesCount: Int,
)

class GameBackupService(
    private val database: AppDatabase
) {

    suspend fun getAllGamePgns(): List<String> {
        return database.gameDao().getAllGames().map { game ->
            buildBackupPgn(game)
        }
    }

    suspend fun writeBackup(outputStream: OutputStream) {
        val backupText = getAllGamePgns().joinToString(separator = "\n\n") { pgn ->
            pgn.trim()
        }
        outputStream.writer(Charsets.UTF_8).use { writer ->
            writer.write(backupText)
        }
    }

    suspend fun restoreBackup(
        inputStream: InputStream,
        onProgress: suspend (GameBackupRestoreProgress) -> Unit = {}
    ): GameBackupRestoreResult {
        val backupText = inputStream.reader(Charsets.UTF_8).use { reader ->
            reader.readText()
        }
        val backupGames = extractBackupGames(backupText)
        if (backupGames.isEmpty()) {
            onProgress(
                GameBackupRestoreProgress(
                    totalGames = 0,
                    processedGamesCount = 0,
                    restoredGamesCount = 0,
                    skippedGamesCount = 0
                )
            )
            return GameBackupRestoreResult(
                restoredGamesCount = 0,
                skippedGamesCount = 0
            )
        }

        val gameSaver = GameSaver(database)
        var restoredGamesCount = 0
        var skippedGamesCount = 0
        var processedGamesCount = 0

        suspend fun reportProgress() {
            onProgress(
                GameBackupRestoreProgress(
                    totalGames = backupGames.size,
                    processedGamesCount = processedGamesCount,
                    restoredGamesCount = restoredGamesCount,
                    skippedGamesCount = skippedGamesCount
                )
            )
        }

        reportProgress()

        backupGames.forEach { pgn ->
            currentCoroutineContext().ensureActive()

            val game = buildBackupGameEntity(pgn)
            val moves = runCatching {
                uciMovesToMoves(extractBackupMoves(pgn))
            }.getOrNull()

            if (moves == null) {
                skippedGamesCount += 1
                processedGamesCount += 1
                reportProgress()
                return@forEach
            }

            val gameId = gameSaver.saveGame(game, moves, game.sideMask)
            if (gameId == null) {
                skippedGamesCount += 1
                processedGamesCount += 1
                reportProgress()
                return@forEach
            }

            restoredGamesCount += 1
            processedGamesCount += 1
            reportProgress()
        }

        return GameBackupRestoreResult(
            restoredGamesCount = restoredGamesCount,
            skippedGamesCount = skippedGamesCount
        )
    }

    private fun buildBackupPgn(game: GameEntity): String {
        val normalizedPgn = game.pgn.trim()
        val lines = normalizedPgn.lines().toMutableList()
        val headerEndIndex = lines.indexOfFirst { it.isBlank() }
        val insertIndex = if (headerEndIndex >= 0) {
            headerEndIndex
        } else {
            lines.size
        }

        replaceHeaderOrInsert(
            lines = lines,
            headerName = "SideMask",
            headerValue = "[SideMask \"${game.sideMask}\"]",
            insertIndex = insertIndex
        )

        if (game.initialFen.isNotBlank()) {
            replaceHeaderOrInsert(
                lines = lines,
                headerName = "FEN",
                headerValue = "[FEN \"${game.initialFen}\"]",
                insertIndex = insertIndex
            )
            replaceHeaderOrInsert(
                lines = lines,
                headerName = "SetUp",
                headerValue = "[SetUp \"1\"]",
                insertIndex = insertIndex
            )
        }

        return lines.joinToString(separator = "\n").trim()
    }

    private fun replaceHeaderOrInsert(
        lines: MutableList<String>,
        headerName: String,
        headerValue: String,
        insertIndex: Int
    ) {
        val headerPrefix = "[$headerName "
        val existingIndex = lines.indexOfFirst { line ->
            line.startsWith(headerPrefix)
        }
        if (existingIndex >= 0) {
            lines[existingIndex] = headerValue
            return
        }

        lines.add(insertIndex, headerValue)
    }

    private fun extractBackupGames(backupText: String): List<String> {
        val normalizedText = backupText.trim()
        if (normalizedText.isBlank()) {
            return emptyList()
        }

        val gameStartRegex = Regex("""(?m)(?=^\[Event\s+")""")
        val splitGames = normalizedText
            .split(gameStartRegex)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (splitGames.isNotEmpty()) {
            return splitGames
        }

        return listOf(normalizedText)
    }

    private fun buildBackupGameEntity(pgn: String): GameEntity {
        val headers = extractPgnHeaders(pgn)

        fun normalizeHeaderValue(value: String?): String? {
            val trimmed = value?.trim() ?: return null
            if (trimmed.isBlank() || trimmed == "?") {
                return null
            }

            return trimmed
        }

        return GameEntity(
            white = normalizeHeaderValue(headers["White"]),
            black = normalizeHeaderValue(headers["Black"]),
            result = normalizeHeaderValue(headers["Result"]),
            event = normalizeHeaderValue(headers["Event"]),
            site = normalizeHeaderValue(headers["Site"]),
            round = normalizeHeaderValue(headers["Round"]),
            eco = normalizeHeaderValue(headers["ECO"]),
            pgn = pgn.trim(),
            initialFen = normalizeHeaderValue(headers["FEN"]) ?: "",
            sideMask = resolveBackupSideMask(headers)
        )
    }

    private fun resolveBackupSideMask(headers: Map<String, String>): Int {
        val rawSideMask = headers["SideMask"]?.trim()?.toIntOrNull() ?: return SideMask.WHITE
        if (rawSideMask == SideMask.WHITE || rawSideMask == SideMask.BLACK || rawSideMask == SideMask.BOTH) {
            return rawSideMask
        }

        return SideMask.WHITE
    }

    private fun extractBackupMoves(pgn: String): List<String> {
        val uciRegex = Regex("[a-h][1-8][a-h][1-8][qrbnQRBN]?")
        return pgn.lines()
            .filterNot { it.trim().startsWith("[") }
            .joinToString(" ")
            .split("\\s+".toRegex())
            .filter { uciRegex.matches(it) }
    }
}
