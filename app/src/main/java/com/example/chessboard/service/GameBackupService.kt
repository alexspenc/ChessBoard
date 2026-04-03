package com.example.chessboard.service

import com.example.chessboard.entity.GameEntity
import com.example.chessboard.entity.SideMask
import com.example.chessboard.repository.AppDatabase
import java.io.InputStream
import java.io.OutputStream

data class GameBackupRestoreResult(
    val restoredGamesCount: Int,
    val skippedGamesCount: Int
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

    suspend fun restoreBackup(inputStream: InputStream): GameBackupRestoreResult {
        val backupText = inputStream.reader(Charsets.UTF_8).use { reader ->
            reader.readText()
        }
        val backupGames = extractBackupGames(backupText)
        if (backupGames.isEmpty()) {
            return GameBackupRestoreResult(
                restoredGamesCount = 0,
                skippedGamesCount = 0
            )
        }

        val gameSaver = GameSaver(database)
        var restoredGamesCount = 0
        var skippedGamesCount = 0

        backupGames.forEach { pgn ->
            val game = buildBackupGameEntity(pgn)
            val moves = runCatching {
                uciMovesToMoves(extractBackupMoves(pgn))
            }.getOrNull()

            if (moves == null) {
                skippedGamesCount += 1
                return@forEach
            }

            val gameId = gameSaver.saveGame(game, moves, game.sideMask)
            if (gameId == null) {
                skippedGamesCount += 1
                return@forEach
            }

            restoredGamesCount += 1
        }

        return GameBackupRestoreResult(
            restoredGamesCount = restoredGamesCount,
            skippedGamesCount = skippedGamesCount
        )
    }

    private fun buildBackupPgn(game: GameEntity): String {
        val normalizedPgn = game.pgn.trim()
        if (game.initialFen.isBlank()) {
            return normalizedPgn
        }

        val lines = normalizedPgn.lines().toMutableList()
        val fenHeader = "[FEN \"${game.initialFen}\"]"
        val setupHeader = "[SetUp \"1\"]"
        val headerEndIndex = lines.indexOfFirst { it.isBlank() }
        val insertIndex = if (headerEndIndex >= 0) {
            headerEndIndex
        } else {
            lines.size
        }

        replaceHeaderOrInsert(
            lines = lines,
            headerName = "FEN",
            headerValue = fenHeader,
            insertIndex = insertIndex
        )
        replaceHeaderOrInsert(
            lines = lines,
            headerName = "SetUp",
            headerValue = setupHeader,
            insertIndex = insertIndex
        )

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
            sideMask = SideMask.BOTH
        )
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
