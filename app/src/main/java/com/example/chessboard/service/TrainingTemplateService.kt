package com.example.chessboard.service

import com.example.chessboard.entity.TrainingTemplateEntity
import com.example.chessboard.repository.TrainingTemplateDao

class TrainingTemplateService(
    private val dao: TrainingTemplateDao
) {

    suspend fun createTemplate(name: String): Long {
        return dao.insert(TrainingTemplateEntity(name = name))
    }

    suspend fun addGame(templateId: Long, gameId: Long, weight: Int): Boolean {
        val template = dao.getById(templateId) ?: return false

        val games = JsonParser.fromJson(template.gamesJson).toMutableList()
        val index = games.indexOfFirst { it.gameId == gameId }

        if (index >= 0) {
            games[index] = games[index].copy(weight = weight)
        } else {
            games.add(JsonParser(gameId, weight))
        }

        dao.update(template.copy(gamesJson = JsonParser.toJson(games)))
        return true
    }

    suspend fun removeGame(templateId: Long, gameId: Long): Boolean {
        val template = dao.getById(templateId) ?: return false

        val games = JsonParser.fromJson(template.gamesJson)
            .filter { it.gameId != gameId }

        dao.update(template.copy(gamesJson = JsonParser.toJson(games)))
        return true
    }

    suspend fun getGames(templateId: Long): List<JsonParser> {
        val template = dao.getById(templateId) ?: return emptyList()
        return JsonParser.fromJson(template.gamesJson)
    }
}
