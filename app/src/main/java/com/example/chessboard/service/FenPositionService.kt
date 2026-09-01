package com.example.chessboard.service

/*
 * File role: coordinates persistence operations for positions in the FEN catalog.
 * Allowed here:
 * - validating and normalizing position data before storage
 * - transactionally creating or updating a catalog position and its optional description
 * - detecting duplicate FEN positions and exposing position read/delete operations
 * - loading consistent catalog pages, position details, and newest-first neighbors from Room
 * Not allowed here:
 * - continuations or UI state
 * Validation date: 2026-09-01
 */

import androidx.room.withTransaction
import com.example.chessboard.entity.FenPositionDescriptionEntity
import com.example.chessboard.entity.FenPositionEntity
import com.example.chessboard.repository.AppDatabase

sealed interface CreateFenPositionResult {
    data class Success(val id: Long) : CreateFenPositionResult
    data object DuplicateFen : CreateFenPositionResult
    data object InvalidFen : CreateFenPositionResult
    data object BlankTheme : CreateFenPositionResult
}

sealed interface UpdateFenPositionResult {
    data object Success : UpdateFenPositionResult
    data object BlankTheme : UpdateFenPositionResult
    data object PositionNotFound : UpdateFenPositionResult
}

data class FenPositionCatalogPage(
    val positions: List<FenPositionEntity>,
    val totalCount: Int,
)

data class FenPositionDetailsData(
    val id: Long,
    val fen: String,
    val name: String,
    val theme: String,
    val description: String?,
    val catalogIndex: Int,
    val previousPositionId: Long?,
    val nextPositionId: Long?,
)

class FenPositionService(
    private val database: AppDatabase,
) {
    private val dao = database.fenPositionDao()
    private val descriptionDao = database.fenPositionDescriptionDao()

    suspend fun create(
        fen: String,
        name: String,
        theme: String,
        description: String,
    ): CreateFenPositionResult {
        val normalizedFen = normalizeValidFenPosition(fen)
            ?: return CreateFenPositionResult.InvalidFen
        val normalizedTheme = theme.trim()
        if (normalizedTheme.isBlank()) {
            return CreateFenPositionResult.BlankTheme
        }

        return database.withTransaction {
            val id = dao.insert(
                FenPositionEntity(
                    fen = normalizedFen,
                    name = name.trim(),
                    theme = normalizedTheme,
                ),
            )
            if (id == -1L) {
                return@withTransaction CreateFenPositionResult.DuplicateFen
            }

            val normalizedDescription = description.trim()
            if (normalizedDescription.isNotBlank()) {
                descriptionDao.insert(
                    FenPositionDescriptionEntity(
                        positionId = id,
                        description = normalizedDescription,
                    ),
                )
            }

            CreateFenPositionResult.Success(id)
        }
    }

    suspend fun getAll(): List<FenPositionEntity> {
        return dao.getAll()
    }

    suspend fun updateDetails(
        positionId: Long,
        name: String,
        theme: String,
        description: String,
    ): UpdateFenPositionResult {
        val normalizedTheme = theme.trim()
        if (normalizedTheme.isBlank()) {
            return UpdateFenPositionResult.BlankTheme
        }

        return database.withTransaction {
            val updatedPositions = dao.updateNameAndTheme(
                id = positionId,
                name = name.trim(),
                theme = normalizedTheme,
            )
            if (updatedPositions == 0) {
                return@withTransaction UpdateFenPositionResult.PositionNotFound
            }

            updateDescription(
                positionId = positionId,
                description = description.trim(),
            )
            UpdateFenPositionResult.Success
        }
    }

    private suspend fun updateDescription(
        positionId: Long,
        description: String,
    ) {
        if (description.isBlank()) {
            descriptionDao.deleteByPositionId(positionId)
            return
        }

        val updatedDescriptions = descriptionDao.updateByPositionId(
            positionId = positionId,
            description = description,
        )
        if (updatedDescriptions > 0) {
            return
        }

        descriptionDao.insert(
            FenPositionDescriptionEntity(
                positionId = positionId,
                description = description,
            ),
        )
    }

    suspend fun getCatalogPage(
        limit: Int,
        offset: Int,
    ): FenPositionCatalogPage {
        require(limit > 0) { "Page limit must be positive" }
        require(offset >= 0) { "Page offset must not be negative" }

        return database.withTransaction {
            FenPositionCatalogPage(
                positions = dao.getPage(limit = limit, offset = offset),
                totalCount = dao.getCount(),
            )
        }
    }

    suspend fun getById(id: Long): FenPositionEntity? {
        return dao.getById(id)
    }

    suspend fun getDetailsById(id: Long): FenPositionDetailsData? {
        return database.withTransaction {
            val position = dao.getById(id) ?: return@withTransaction null
            val description = descriptionDao.getByPositionId(position.id)

            FenPositionDetailsData(
                id = position.id,
                fen = position.fen,
                name = position.name,
                theme = position.theme,
                description = description?.description,
                catalogIndex = dao.getCatalogIndex(position.id),
                previousPositionId = dao.getPreviousCatalogPositionId(position.id),
                nextPositionId = dao.getNextCatalogPositionId(position.id),
            )
        }
    }

    suspend fun getByFen(fen: String): FenPositionEntity? {
        val normalizedFen = normalizeValidFenPosition(fen) ?: return null
        return dao.getByFen(normalizedFen)
    }

    suspend fun getDescriptionByFen(fen: String): FenPositionDescriptionEntity? {
        val normalizedFen = normalizeValidFenPosition(fen) ?: return null
        return descriptionDao.getByFen(normalizedFen)
    }

    suspend fun deleteById(id: Long): Boolean {
        return dao.deleteById(id) > 0
    }
}
