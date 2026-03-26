package com.example.chessboard.service

import com.example.chessboard.entity.TrainingEntity
import com.example.chessboard.repository.TrainingDao
import com.example.chessboard.repository.TrainingTemplateDao

class TrainingService(
    private val dao: TrainingDao,
    private val templateDao: TrainingTemplateDao
) {

    suspend fun createEmptyTraining(name: String = "FullTraining"): Long {
        return dao.insert(TrainingEntity(name = name))
    }

    suspend fun createTrainingFromGames(
        games: List<OneGameTrainingData>,
        name: String = "FullTraining"
    ): Long? {
        if (games.isEmpty()) return null
        val gamesJson = OneGameTrainingData.toJson(games)

        return dao.insert(
            TrainingEntity(
                name = name,
                gamesJson = gamesJson
            )
        )
    }

    suspend fun addGameToTraining(trainingId: Long, gameId: Long, weight: Int = 1): Boolean {
        val training = dao.getById(trainingId) ?: return false

        val games = OneGameTrainingData.fromJson(training.gamesJson).toMutableList()
        val index = games.indexOfFirst { it.gameId == gameId }

        if (index >= 0) { return false }

        games.add(OneGameTrainingData(gameId, weight))
        dao.update(training.copy(gamesJson = OneGameTrainingData.toJson(games)))
        return true
    }

    suspend fun addGamesToTraining(
        trainingId: Long,
        gamesForTraining: List<OneGameTrainingData>
    ): Boolean {
        val training = dao.getById(trainingId) ?: return false
        if (gamesForTraining.isEmpty()) return true

        val existingGames = OneGameTrainingData.fromJson(training.gamesJson).toMutableList()
        val existingGameIds = existingGames.map { it.gameId }.toHashSet()
        val filteredGames = mutableListOf<OneGameTrainingData>()
        val newGameIds = mutableSetOf<Long>()

        for (game in gamesForTraining) {
            if (!newGameIds.add(game.gameId)) return false
            if (game.gameId !in existingGameIds) {
                filteredGames.add(game)
            }
        }

        if (filteredGames.isEmpty()) return true

        existingGames.addAll(filteredGames)
        dao.update(training.copy(gamesJson = OneGameTrainingData.toJson(existingGames)))
        return true
    }

    suspend fun createFromTemplate(templateId: Long): Long {
        val template = templateDao.getById(templateId) ?: return -1 //todo throw exception or return template what is -1?
        return dao.insert(
            TrainingEntity(name = template.name, gamesJson = template.gamesJson)
        )
    }

    // Decreases the weight of a game.
    // If weight reaches 0 → game is removed.
    // If no games remain → training is deleted.
    suspend fun decreaseLineWeight(trainingId: Long, gameId: Long): Boolean { //todo why its return boolean?
        val training = dao.getById(trainingId) ?: return false

        val games = OneGameTrainingData.fromJson(training.gamesJson).toMutableList()
        val index = games.indexOfFirst { it.gameId == gameId }

        if (index < 0) return false // todo mb index < 1?

        val newWeight = games[index].weight - 1

        if (newWeight <= 0) {
            games.removeAt(index)
        } else {
            games[index] = games[index].copy(weight = newWeight)
        }

        if (games.isEmpty()) {
            dao.deleteById(trainingId)
            return true
        }

        dao.update(training.copy(gamesJson = OneGameTrainingData.toJson(games)))
        return true
    }

    suspend fun increaseLineWeight(trainingId: Long, gameId: Long, delta: Int = 1): Boolean {
        val training = dao.getById(trainingId) ?: return false

        val games = OneGameTrainingData.fromJson(training.gamesJson).toMutableList()
        val index = games.indexOfFirst { it.gameId == gameId }

        if (index < 0) return false

        games[index] = games[index].copy(weight = games[index].weight + delta)

        dao.update(training.copy(gamesJson = OneGameTrainingData.toJson(games)))
        return true
    }
}
