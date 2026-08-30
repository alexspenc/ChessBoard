package com.example.chessboard.service

/*
 * File role: coordinates persistence operations for positions in the FEN catalog.
 * Allowed here:
 * - normalizing position data before storage
 * - detecting duplicate FEN positions and exposing position read/delete operations
 * Not allowed here:
 * - descriptions, continuations, catalog sorting, or UI state
 * Validation date: 2026-08-30
 */

import com.example.chessboard.entity.FenPositionEntity
import com.example.chessboard.repository.FenPositionDao

sealed interface CreateFenPositionResult {
    data class Success(val id: Long) : CreateFenPositionResult
    data object DuplicateFen : CreateFenPositionResult
}

class FenPositionService(
    private val dao: FenPositionDao,
) {
    suspend fun create(
        fen: String,
        name: String,
        theme: String,
    ): CreateFenPositionResult {
        val normalizedFen = normalizePositionFen(fen)
        val id = dao.insert(
            FenPositionEntity(
                fen = normalizedFen,
                name = name.trim(),
                theme = theme.trim(),
            ),
        )
        if (id == -1L) {
            return CreateFenPositionResult.DuplicateFen
        }

        return CreateFenPositionResult.Success(id)
    }

    suspend fun getAll(): List<FenPositionEntity> {
        return dao.getAll()
    }

    suspend fun getById(id: Long): FenPositionEntity? {
        return dao.getById(id)
    }

    suspend fun getByFen(fen: String): FenPositionEntity? {
        return dao.getByFen(normalizePositionFen(fen))
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    private fun normalizePositionFen(fen: String): String {
        return normalizeFenWithoutMoveNumbers(
            fen = fen,
            includeEnPassant = true,
        )
    }
}
