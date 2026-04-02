package com.example.chessboard.service

import com.example.chessboard.repository.AppDatabase
import java.io.OutputStream

class GameBackupService(
    private val database: AppDatabase
) {

    suspend fun getAllGamePgns(): List<String> {
        return database.gameDao().getAllGames().map { game ->
            game.pgn.trim()
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
}
