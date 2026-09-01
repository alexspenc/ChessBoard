package com.example.chessboard.service

/*
 * File role: coordinates persistence operations for positions in the FEN catalog.
 * Allowed here:
 * - validating and normalizing position data before storage
 * - transactionally creating a catalog position and its optional description
 * - detecting duplicate FEN positions and exposing position read/delete operations
 * - loading consistent catalog pages and position-card data from Room
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
