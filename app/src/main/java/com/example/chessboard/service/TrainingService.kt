package com.example.chessboard.service

/**
 * File role: coordinates persistence operations for training records and their line lists.
 * Allowed here:
 * - loading, creating, updating, validating, and deleting stored trainings
 * - translating stored training JSON into training line data
 * - database-backed rules for keeping trainings consistent with saved lines
 * Not allowed here:
 * - composable UI state or navigation decisions
 * - active training runtime/session state
 * Validation date: 2026-05-14
 */
import androidx.room.withTransaction
import com.example.chessboard.entity.TrainingEntity
import com.example.chessboard.repository.AppDatabase
import com.example.chessboard.repository.LineDao
import com.example.chessboard.repository.TrainingDao
import com.example.chessboard.repository.TrainingTemplateDao

class TrainingService(
    private val database: AppDatabase,
    private val lineDao: LineDao,
    private val dao: TrainingDao,
    private val templateDao: TrainingTemplateDao
) {

    private data class TrainingLinesContext(
        val training: TrainingEntity,
        val lines: MutableList<OneLineTrainingData>
    )

    private data class TrainingLineContext(
        val training: TrainingEntity,
        val lines: MutableList<OneLineTrainingData>,
        val index: Int
    )

    suspend fun getAllTrainings(): List<TrainingEntity> {
        return dao.getAll()
    }

    suspend fun hasAnyTraining(): Boolean {
        return dao.getFirst() != null
    }

    suspend fun getTrainingById(trainingId: Long): TrainingEntity? {
        return validateTraining(trainingId)
    }

    suspend fun createEmptyTraining(name: String): Long {
        return dao.insert(TrainingEntity(name = name))
    }

    suspend fun deleteTraining(trainingId: Long): Boolean {
        val training = dao.getById(trainingId) ?: return false
        dao.deleteById(training.id)
        return true
    }

    suspend fun createTrainingFromLines(
        lines: List<OneLineTrainingData>,
        name: String,
    ): Long? {
        if (lines.isEmpty()) return null
        val linesJson = OneLineTrainingData.toJson(lines)

        return dao.insert(
            TrainingEntity(
                name = name,
                linesJson = linesJson
            )
        )
    }

    suspend fun updateTrainingFromLines(
        trainingId: Long,
        lines: List<OneLineTrainingData>,
        name: String,
    ): Boolean {
        if (lines.isEmpty()) return false

        val training = validateTraining(trainingId) ?: return false
        dao.update(
            training.copy(
                name = name,
                linesJson = OneLineTrainingData.toJson(lines)
            )
        )
        return true
    }

    suspend fun addLineToTraining(trainingId: Long, lineId: Long, weight: Int = 1): Boolean {
        val context = loadTrainingLines(trainingId) ?: return false
        if (context.lines.any { it.lineId == lineId }) return false

        context.lines.add(OneLineTrainingData(lineId, weight))
        dao.update(context.training.copy(linesJson = OneLineTrainingData.toJson(context.lines)))
        return true
    }

    suspend fun addLinesToTraining(
        trainingId: Long,
        linesForTraining: List<OneLineTrainingData>
    ): Boolean {
        val training = dao.getById(trainingId) ?: return false
        if (linesForTraining.isEmpty()) return true

        val existingLines = OneLineTrainingData.fromJson(training.linesJson).toMutableList()
        val existingLineIds = existingLines.map { it.lineId }.toHashSet()
        val filteredLines = mutableListOf<OneLineTrainingData>()
        val newLineIds = mutableSetOf<Long>()

        for (line in linesForTraining) {
            if (!newLineIds.add(line.lineId)) return false
            if (line.lineId !in existingLineIds) {
                filteredLines.add(line)
            }
        }

        if (filteredLines.isEmpty()) return true

        existingLines.addAll(filteredLines)
        dao.update(training.copy(linesJson = OneLineTrainingData.toJson(existingLines)))
        return true
    }

    suspend fun createFromTemplate(templateId: Long): Long {
        val template = templateDao.getById(templateId) ?: return -1
        return dao.insert(
            TrainingEntity(name = template.name, linesJson = template.linesJson)
        )
    }

    suspend fun validateTraining(trainingId: Long): TrainingEntity? {
        return database.withTransaction {
            val training = dao.getById(trainingId) ?: return@withTransaction null
            val lines = OneLineTrainingData.fromJson(training.linesJson)
            if (lines.isEmpty()) {
                dao.deleteById(trainingId)
                return@withTransaction null
            }

            val validLines = mutableListOf<OneLineTrainingData>()

            for (line in lines) {
                val existingLine = lineDao.getById(line.lineId)
                if (existingLine != null) {
                    validLines.add(line)
                }
            }

            if (validLines.isEmpty()) {
                dao.deleteById(trainingId)
                return@withTransaction null
            }

            if (validLines.size == lines.size) {
                return@withTransaction training
            }

            val validatedTraining = training.copy(
                linesJson = OneLineTrainingData.toJson(validLines)
            )
            dao.update(validatedTraining)
            return@withTransaction validatedTraining
        }
    }

    suspend fun decreaseLineWeight(trainingId: Long, lineId: Long, keepIfZero: Boolean = false): Boolean {
        val context = findTrainingLine(trainingId, lineId) ?: return false
        val newWeight = context.lines[context.index].weight - 1

        if (newWeight <= 0 && !keepIfZero) {
            context.lines.removeAt(context.index)
        } else {
            context.lines[context.index] = context.lines[context.index].copy(weight = newWeight.coerceAtLeast(0))
        }

        if (context.lines.isEmpty()) {
            dao.deleteById(trainingId)
            return true
        }

        dao.update(context.training.copy(linesJson = OneLineTrainingData.toJson(context.lines)))
        return true
    }

    private suspend fun loadTrainingLines(trainingId: Long): TrainingLinesContext? {
        val training = dao.getById(trainingId) ?: return null
        val lines = OneLineTrainingData.fromJson(training.linesJson).toMutableList()
        return TrainingLinesContext(training = training, lines = lines)
    }

    private suspend fun findTrainingLine(trainingId: Long, lineId: Long): TrainingLineContext? {
        val context = loadTrainingLines(trainingId) ?: return null
        val index = context.lines.indexOfFirst { it.lineId == lineId }
        if (index < 0) return null

        return TrainingLineContext(
            training = context.training,
            lines = context.lines,
            index = index
        )
    }

    suspend fun increaseLineWeight(trainingId: Long, lineId: Long, delta: Int = 1): Boolean {
        val context = findTrainingLine(trainingId, lineId) ?: return false
        context.lines[context.index] = context.lines[context.index].copy(
            weight = context.lines[context.index].weight + delta
        )

        dao.update(context.training.copy(linesJson = OneLineTrainingData.toJson(context.lines)))
        return true
    }
}
