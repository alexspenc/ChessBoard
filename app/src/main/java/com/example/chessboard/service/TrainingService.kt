package com.example.chessboard.service

import androidx.room.withTransaction
import com.example.chessboard.entity.TrainingEntity
import com.example.chessboard.repository.AppDatabase
import com.example.chessboard.repository.GameDao
import com.example.chessboard.repository.TrainingDao
import com.example.chessboard.repository.TrainingTemplateDao

class TrainingService(
    private val database: AppDatabase,
    private val gameDao: GameDao,
    private val dao: TrainingDao,
    private val templateDao: TrainingTemplateDao
) {

    private data class TrainingGamesContext(
        val training: TrainingEntity,
        val games: MutableList<OneGameTrainingData>
    )

    private data class TrainingGameContext(
        val training: TrainingEntity,
        val games: MutableList<OneGameTrainingData>,
        val index: Int
    )

    suspend fun getAllTrainings(): List<TrainingEntity> {
        return dao.getAll()
    }

    suspend fun getTrainingById(trainingId: Long): TrainingEntity? {
        return validateTraining(trainingId)
    }

    suspend fun createEmptyTraining(name: String = "FullTraining"): Long {
        return dao.insert(TrainingEntity(name = name))
    }

    suspend fun deleteTraining(trainingId: Long): Boolean {
        val training = dao.getById(trainingId) ?: return false
        dao.deleteById(training.id)
        return true
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

    suspend fun updateTrainingFromGames(
        trainingId: Long,
        games: List<OneGameTrainingData>,
        name: String = "FullTraining"
    ): Boolean {
        if (games.isEmpty()) return false

        val training = validateTraining(trainingId) ?: return false
        dao.update(
            training.copy(
                name = name,
                gamesJson = OneGameTrainingData.toJson(games)
            )
        )
        return true
    }

    suspend fun addGameToTraining(trainingId: Long, gameId: Long, weight: Int = 1): Boolean {
        val context = loadTrainingGames(trainingId) ?: return false
        if (context.games.any { it.gameId == gameId }) return false

        context.games.add(OneGameTrainingData(gameId, weight))
        dao.update(context.training.copy(gamesJson = OneGameTrainingData.toJson(context.games)))
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
        val template = templateDao.getById(templateId) ?: return -1
        return dao.insert(
            TrainingEntity(name = template.name, gamesJson = template.gamesJson)
        )
    }

    suspend fun validateTraining(trainingId: Long): TrainingEntity? {
        return database.withTransaction {
            val training = dao.getById(trainingId) ?: return@withTransaction null
            val games = OneGameTrainingData.fromJson(training.gamesJson)
            if (games.isEmpty()) {
                dao.deleteById(trainingId)
                return@withTransaction null
            }

            val validGames = mutableListOf<OneGameTrainingData>()

            for (game in games) {
                val existingGame = gameDao.getById(game.gameId)
                if (existingGame != null) {
                    validGames.add(game)
                }
            }

            if (validGames.isEmpty()) {
                dao.deleteById(trainingId)
                return@withTransaction null
            }

            if (validGames.size == games.size) {
                return@withTransaction training
            }

            val validatedTraining = training.copy(
                gamesJson = OneGameTrainingData.toJson(validGames)
            )
            dao.update(validatedTraining)
            return@withTransaction validatedTraining
        }
    }

    suspend fun decreaseLineWeight(trainingId: Long, gameId: Long): Boolean {
        val context = findTrainingGame(trainingId, gameId) ?: return false
        val newWeight = context.games[context.index].weight - 1

        if (newWeight <= 0) {
            context.games.removeAt(context.index)
        } else {
            context.games[context.index] = context.games[context.index].copy(weight = newWeight)
        }

        if (context.games.isEmpty()) {
            dao.deleteById(trainingId)
            return true
        }

        dao.update(context.training.copy(gamesJson = OneGameTrainingData.toJson(context.games)))
        return true
    }

    private suspend fun loadTrainingGames(trainingId: Long): TrainingGamesContext? {
        val training = dao.getById(trainingId) ?: return null
        val games = OneGameTrainingData.fromJson(training.gamesJson).toMutableList()
        return TrainingGamesContext(training = training, games = games)
    }

    private suspend fun findTrainingGame(trainingId: Long, gameId: Long): TrainingGameContext? {
        val context = loadTrainingGames(trainingId) ?: return null
        val index = context.games.indexOfFirst { it.gameId == gameId }
        if (index < 0) return null

        return TrainingGameContext(
            training = context.training,
            games = context.games,
            index = index
        )
    }

    suspend fun increaseLineWeight(trainingId: Long, gameId: Long, delta: Int = 1): Boolean {
        val context = findTrainingGame(trainingId, gameId) ?: return false
        context.games[context.index] = context.games[context.index].copy(
            weight = context.games[context.index].weight + delta
        )

        dao.update(context.training.copy(gamesJson = OneGameTrainingData.toJson(context.games)))
        return true
    }
}
