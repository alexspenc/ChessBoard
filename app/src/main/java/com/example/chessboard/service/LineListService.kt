package com.example.chessboard.service

/*
 * File role: provides normalized read access for persisted opening lines.
 * Allowed here:
 * - small service wrappers around line DAO reads
 * - search and pagination normalization for line-list consumers
 * Not allowed here:
 * - Compose UI state, navigation workflow, or write-side persistence orchestration
 * Validation date: 2026-05-27
 */

import com.example.chessboard.entity.LineEntity
import com.example.chessboard.repository.LineDao

class LineListService(
    private val lineDao: LineDao
) {

    suspend fun getLinesPage(limit: Int, offset: Int): List<LineEntity> {
        val normalizedLimit = normalizeLimit(limit)
        val normalizedOffset = normalizeOffset(offset)
        return lineDao.getLinesPage(limit = normalizedLimit, offset = normalizedOffset)
    }

    suspend fun getLineIdsPage(limit: Int, offset: Int): List<Long> {
        val normalizedLimit = normalizeLimit(limit)
        val normalizedOffset = normalizeOffset(offset)
        return lineDao.getLineIdsPage(limit = normalizedLimit, offset = normalizedOffset)
    }

    suspend fun getLinesCount(): Int {
        return lineDao.getLinesCount()
    }

    suspend fun getLinesByIds(lineIds: List<Long>): List<LineEntity> {
        if (lineIds.isEmpty()) {
            return emptyList()
        }

        val distinctLineIds = lineIds.distinct()
        val linesById = lineDao.getByIds(distinctLineIds)
            .associateBy { line -> line.id }

        return lineIds.mapNotNull { lineId -> linesById[lineId] }
    }

    suspend fun searchLinesByName(
        query: String,
        isCaseSensitive: Boolean,
        limit: Int,
        offset: Int
    ): List<LineEntity> {
        if (query.isBlank()) {
            return getLinesPage(limit = limit, offset = offset)
        }

        val normalizedLimit = normalizeLimit(limit)
        val normalizedOffset = normalizeOffset(offset)
        if (isCaseSensitive) {
            return lineDao.searchLinesByEventCaseSensitive(
                query = query,
                limit = normalizedLimit,
                offset = normalizedOffset
            )
        }

        return lineDao.searchLinesByEvent(
            query = query,
            limit = normalizedLimit,
            offset = normalizedOffset
        )
    }

    suspend fun searchLineIdsByName(
        query: String,
        isCaseSensitive: Boolean,
        limit: Int,
        offset: Int,
        sideMask: Int? = null,
    ): List<Long> {
        val normalizedLimit = normalizeLimit(limit)
        val normalizedOffset = normalizeOffset(offset)

        if (query.isBlank()) {
            if (sideMask != null) {
                return lineDao.getLineIdsPageBySideMask(
                    sideMask = sideMask,
                    limit = normalizedLimit,
                    offset = normalizedOffset,
                )
            }

            return lineDao.getLineIdsPage(limit = normalizedLimit, offset = normalizedOffset)
        }

        if (sideMask != null) {
            return searchLineIdsByNameAndSide(
                query = query,
                isCaseSensitive = isCaseSensitive,
                limit = normalizedLimit,
                offset = normalizedOffset,
                sideMask = sideMask,
            )
        }

        if (isCaseSensitive) {
            return lineDao.searchLineIdsByEventCaseSensitive(
                query = query,
                limit = normalizedLimit,
                offset = normalizedOffset
            )
        }

        return lineDao.searchLineIdsByEvent(
            query = query,
            limit = normalizedLimit,
            offset = normalizedOffset
        )
    }

    private suspend fun searchLineIdsByNameAndSide(
        query: String,
        isCaseSensitive: Boolean,
        limit: Int,
        offset: Int,
        sideMask: Int,
    ): List<Long> {
        if (isCaseSensitive) {
            return lineDao.searchLineIdsByEventAndSideMaskCaseSensitive(
                query = query,
                sideMask = sideMask,
                limit = limit,
                offset = offset,
            )
        }

        return lineDao.searchLineIdsByEventAndSideMask(
            query = query,
            sideMask = sideMask,
            limit = limit,
            offset = offset,
        )
    }

    suspend fun countLinesByName(
        query: String,
        isCaseSensitive: Boolean,
        sideMask: Int? = null,
    ): Int {
        if (query.isBlank()) {
            if (sideMask != null) {
                return lineDao.countLinesBySideMask(sideMask)
            }

            return getLinesCount()
        }

        if (sideMask != null) {
            return countLinesByNameAndSide(
                query = query,
                isCaseSensitive = isCaseSensitive,
                sideMask = sideMask,
            )
        }

        if (isCaseSensitive) {
            return lineDao.countLinesByEventCaseSensitive(query)
        }

        return lineDao.countLinesByEvent(query)
    }

    private suspend fun countLinesByNameAndSide(
        query: String,
        isCaseSensitive: Boolean,
        sideMask: Int,
    ): Int {
        if (isCaseSensitive) {
            return lineDao.countLinesByEventAndSideMaskCaseSensitive(
                query = query,
                sideMask = sideMask,
            )
        }

        return lineDao.countLinesByEventAndSideMask(
            query = query,
            sideMask = sideMask,
        )
    }

    private fun normalizeLimit(limit: Int): Int {
        if (limit <= 0) {
            return 1
        }

        return limit
    }

    private fun normalizeOffset(offset: Int): Int {
        if (offset < 0) {
            return 0
        }

        return offset
    }
}
