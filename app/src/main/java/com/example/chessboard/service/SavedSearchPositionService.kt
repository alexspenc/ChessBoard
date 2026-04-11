package com.example.chessboard.service

import com.example.chessboard.entity.SavedSearchPositionEntity
import com.example.chessboard.repository.SavedSearchPositionDao

sealed interface SaveSavedSearchPositionResult {
    data class Success(val id: Long) : SaveSavedSearchPositionResult
    data object DuplicateName : SaveSavedSearchPositionResult
    data object DuplicateSearchFen : SaveSavedSearchPositionResult
    data object DuplicateFullFen : SaveSavedSearchPositionResult
}

class SavedSearchPositionService(
    private val dao: SavedSearchPositionDao
) {
    suspend fun getAll(): List<SavedSearchPositionEntity> {
        return dao.getAll()
    }

    suspend fun getById(id: Long): SavedSearchPositionEntity? {
        return dao.getById(id)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun create(
        name: String,
        fenForSearch: String,
        fenFull: String? = null
    ): SaveSavedSearchPositionResult {
        val payload = buildEntity(
            id = 0,
            name = name,
            fenForSearch = fenForSearch,
            fenFull = fenFull,
            createdAt = System.currentTimeMillis()
        )

        val duplicate = findDuplicate(payload, currentId = null)
        if (duplicate != null) {
            return duplicate
        }

        val id = dao.insert(payload)
        return SaveSavedSearchPositionResult.Success(id)
    }

    suspend fun update(
        id: Long,
        name: String,
        fenForSearch: String,
        fenFull: String? = null
    ): SaveSavedSearchPositionResult {
        val existing = dao.getById(id) ?: return SaveSavedSearchPositionResult.DuplicateName
        val payload = buildEntity(
            id = existing.id,
            name = name,
            fenForSearch = fenForSearch,
            fenFull = fenFull,
            createdAt = existing.createdAt
        )

        val duplicate = findDuplicate(payload, currentId = id)
        if (duplicate != null) {
            return duplicate
        }

        dao.update(payload)
        return SaveSavedSearchPositionResult.Success(id)
    }

    private suspend fun findDuplicate(
        payload: SavedSearchPositionEntity,
        currentId: Long?
    ): SaveSavedSearchPositionResult? {
        val duplicateByName = dao.getByName(payload.name)
        if (duplicateByName != null && duplicateByName.id != currentId) {
            return SaveSavedSearchPositionResult.DuplicateName
        }

        val duplicateBySearchFen = dao.getBySearchFen(
            hashForSearch = payload.hashForSearch,
            fenForSearch = payload.fenForSearch
        )
        if (duplicateBySearchFen != null && duplicateBySearchFen.id != currentId) {
            return SaveSavedSearchPositionResult.DuplicateSearchFen
        }

        val normalizedFullFen = payload.fenFull
        val fullHash = payload.hashFull
        if (normalizedFullFen != null && fullHash != null) {
            val duplicateByFullFen = dao.getByFullFen(
                hashFull = fullHash,
                fenFull = normalizedFullFen
            )
            if (duplicateByFullFen != null && duplicateByFullFen.id != currentId) {
                return SaveSavedSearchPositionResult.DuplicateFullFen
            }
        }

        return null
    }

    private fun buildEntity(
        id: Long,
        name: String,
        fenForSearch: String,
        fenFull: String?,
        createdAt: Long
    ): SavedSearchPositionEntity {
        val normalizedName = name.trim()
        val normalizedFenForSearch = normalizeFenForSearchStorage(fenForSearch)
        val normalizedFenFull = normalizeFullFenStorage(fenFull)

        return SavedSearchPositionEntity(
            id = id,
            name = normalizedName,
            fenForSearch = normalizedFenForSearch,
            hashForSearch = calculateFenHashForSearchStorage(normalizedFenForSearch),
            fenFull = normalizedFenFull,
            hashFull = calculateFullFenHashStorage(normalizedFenFull),
            createdAt = createdAt
        )
    }
}

private fun normalizeFenForSearchStorage(fen: String): String {
    return normalizeFenWithoutMoveNumbers(fen = fen, includeEnPassant = false)
}

private fun calculateFenHashForSearchStorage(fenForSearch: String): Long {
    return fenForSearch.hashCode().toLong()
}

private fun normalizeFullFenStorage(fen: String?): String? {
    val normalized = fen?.trim().orEmpty()
    if (normalized.isBlank()) {
        return null
    }

    return normalized
}

private fun calculateFullFenHashStorage(fen: String?): Long? {
    return fen?.hashCode()?.toLong()
}
