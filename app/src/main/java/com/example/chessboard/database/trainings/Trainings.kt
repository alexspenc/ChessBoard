package com.example.chessboard.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// DTO for JSON
// ==============================================================================
// ==============================================================================

@kotlinx.serialization.Serializable
data class GameData(
    val gameId: Long,
    val weight: Int
)

private fun parse(jsonString: String): List<GameData> {
    return try {
        Json.decodeFromString(jsonString)
    } catch (e: Exception) {
        val method = Throwable().stackTrace[1].methodName
        println("Error [${e}] on [${method}]")
        emptyList()
    }
}

private fun serialize(games: List<GameData>): String {
    return Json.encodeToString(games)
}

// Training template
// ==============================================================================
// ==============================================================================
/**
 * Training template.
 *
 * gamesJson format:
 * [
 *   {"gameId": 1, "weight": 3},
 *   {"gameId": 5, "weight": 1}
 * ]
 */
@Entity(tableName = "training_templates")
data class TrainingTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val gamesJson: String = "[]"
)

@Dao
interface TrainingTemplateDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(template: TrainingTemplateEntity): Long

    @Query("SELECT * FROM training_templates WHERE id = :id")
    suspend fun getById(id: Long): TrainingTemplateEntity?

    @Query("DELETE FROM training_templates WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Update
    suspend fun update(template: TrainingTemplateEntity)
}

// ======================================================
// Manager
// ======================================================

class TrainingTemplateManager(
    private val trainingTemplateDao: TrainingTemplateDao
) {

    suspend fun createTemplate(name: String): Long {
        return trainingTemplateDao.insert(
            TrainingTemplateEntity(name = name)
        )
    }

    suspend fun addGame(
        templateId: Long,
        gameId: Long,
        weight: Int
    ): Boolean {

        val template = trainingTemplateDao.getById(templateId) ?: return false

        val updatedJson = updateGamesJson(
            template.gamesJson,
            gameId,
            weight
        )

        trainingTemplateDao.update(
            template.copy(gamesJson = updatedJson)
        )

        return true
    }

    private fun updateGamesJson(
        json: String,
        gameId: Long,
        weight: Int
    ): String {

        val games = parse(json).toMutableList()

        val index = games.indexOfFirst { it.gameId == gameId }

        if (index >= 0) {
            games[index] = games[index].copy(weight = weight)
            return serialize(games)
        }

        games.add(GameData(gameId, weight))
        return serialize(games)
    }

    suspend fun removeGame(
        templateId: Long,
        gameId: Long
    ) : Boolean {
        val template = trainingTemplateDao.getById(templateId) ?: return false

        val games = parse(template.gamesJson)
            .filter { it.gameId != gameId }

        trainingTemplateDao.update(
            template.copy(gamesJson = serialize(games))
        )
        return true
    }

    suspend fun getGames(templateId: Long): List<GameData> {
        val template = trainingTemplateDao.getById(templateId) ?: return emptyList()
        return parse(template.gamesJson)
    }
}

// Training data (what changes after train)
// ==============================================================================
// ==============================================================================
@Entity(tableName = "trainings")
data class TrainingsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val gamesJson: String = "[]"
)

@Dao
interface TrainingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(training: TrainingsEntity): Long

    @Query("SELECT * FROM trainings WHERE id = :id")
    suspend fun getById(id: Long): TrainingsEntity?

    @Query("DELETE FROM trainings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Update
    suspend fun update(training: TrainingsEntity)
}

class TrainingManager(
    private val trainingDao: TrainingDao,
    private val templateDao: TrainingTemplateDao
) {
    suspend fun createFromTemplate(templateId: Long): Long {
        val template = templateDao.getById(templateId) ?: return -1

        return trainingDao.insert(
            TrainingsEntity(
                name = template.name,
                gamesJson = template.gamesJson
            )
        )
    }

     // Decrease weight of a game.
     // If weight becomes 0 → remove game
     // If no games left → delete training
    suspend fun decreaseWeight(
        trainingId: Long,
        gameId: Long
    ): Boolean {

        val training = trainingDao.getById(trainingId) ?: return false

        val updatedJson = decreaseWeightJson(
            training.gamesJson,
            gameId
        ) ?: return false

        if (updatedJson == "[]") {
            trainingDao.deleteById(trainingId)
            return true
        }

        trainingDao.update(
            training.copy(gamesJson = updatedJson)
        )

        return true
    }

    suspend fun increaseWeight(
        trainingId: Long,
        gameId: Long,
        delta: Int = 1
    ): Boolean {

        val training = trainingDao.getById(trainingId) ?: return false

        val updatedJson = increaseWeightJson(
            training.gamesJson,
            gameId,
            delta
        ) ?: return false

        trainingDao.update(
            training.copy(gamesJson = updatedJson)
        )

        return true
    }

    // ======================================================
    // JSON logic
    // ======================================================

    private fun decreaseWeightJson(
        jsonString: String,
        gameId: Long
    ): String? {

        val games = parse(jsonString).toMutableList()
        val index = games.indexOfFirst { it.gameId == gameId }

        if (index < 0) { return null }

        val game = games[index]
        val newWeight = game.weight - 1

        if (newWeight <= 0) {
            games.removeAt(index)
            return serialize(games)
        }

        games[index] = game.copy(weight = newWeight)
        return serialize(games)
    }

    private fun increaseWeightJson(
        jsonString: String,
        gameId: Long,
        delta: Int
    ): String? {

        val games = parse(jsonString).toMutableList()
        val index = games.indexOfFirst { it.gameId == gameId }

        if (index < 0) { return null }

        val game = games[index]
        games[index] = game.copy(weight = game.weight + delta)

        return serialize(games)
    }
}