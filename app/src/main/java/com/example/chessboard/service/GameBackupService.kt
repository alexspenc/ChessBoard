package com.example.chessboard.service

import com.example.chessboard.repository.AppDatabase
import java.io.File

class GameBackupService(
    private val database: AppDatabase
) {

    suspend fun getAllGamePgns(): List<String> {
        return database.gameDao().getAllGames().map { game ->
            game.pgn.trim()
        }
    }

    suspend fun writeBackup(file: File) {
        val backupText = getAllGamePgns().joinToString(separator = "\n\n") { pgn ->
            pgn.trim()
        }
        file.writeText(backupText, Charsets.UTF_8)
    }
}
