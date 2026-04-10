package com.example.chessboard.service

import com.example.chessboard.entity.GameEntity
import com.example.chessboard.repository.GameDao

class GameListService(
    private val gameDao: GameDao
) {

    suspend fun getGamesPage(limit: Int, offset: Int): List<GameEntity> {
        val normalizedLimit = normalizeLimit(limit)
        val normalizedOffset = normalizeOffset(offset)
        return gameDao.getGamesPage(limit = normalizedLimit, offset = normalizedOffset)
    }

    suspend fun getGameIdsPage(limit: Int, offset: Int): List<Long> {
        val normalizedLimit = normalizeLimit(limit)
        val normalizedOffset = normalizeOffset(offset)
        return gameDao.getGameIdsPage(limit = normalizedLimit, offset = normalizedOffset)
    }

    suspend fun getGamesCount(): Int {
        return gameDao.getGamesCount()
    }

    suspend fun getGamesByIds(gameIds: List<Long>): List<GameEntity> {
        if (gameIds.isEmpty()) {
            return emptyList()
        }

        val distinctGameIds = gameIds.distinct()
        val gamesById = gameDao.getByIds(distinctGameIds)
            .associateBy { game -> game.id }

        return gameIds.mapNotNull { gameId -> gamesById[gameId] }
    }

    suspend fun searchGamesByName(
        query: String,
        isCaseSensitive: Boolean,
        limit: Int,
        offset: Int
    ): List<GameEntity> {
        if (query.isBlank()) {
            return getGamesPage(limit = limit, offset = offset)
        }

        val normalizedLimit = normalizeLimit(limit)
        val normalizedOffset = normalizeOffset(offset)
        if (isCaseSensitive) {
            return gameDao.searchGamesByEventCaseSensitive(
                query = query,
                limit = normalizedLimit,
                offset = normalizedOffset
            )
        }

        return gameDao.searchGamesByEvent(
            query = query,
            limit = normalizedLimit,
            offset = normalizedOffset
        )
    }

    suspend fun searchGameIdsByName(
        query: String,
        isCaseSensitive: Boolean,
        limit: Int,
        offset: Int
    ): List<Long> {
        if (query.isBlank()) {
            return getGameIdsPage(limit = limit, offset = offset)
        }

        val normalizedLimit = normalizeLimit(limit)
        val normalizedOffset = normalizeOffset(offset)
        if (isCaseSensitive) {
            return gameDao.searchGameIdsByEventCaseSensitive(
                query = query,
                limit = normalizedLimit,
                offset = normalizedOffset
            )
        }

        return gameDao.searchGameIdsByEvent(
            query = query,
            limit = normalizedLimit,
            offset = normalizedOffset
        )
    }

    suspend fun countGamesByName(
        query: String,
        isCaseSensitive: Boolean
    ): Int {
        if (query.isBlank()) {
            return getGamesCount()
        }

        if (isCaseSensitive) {
            return gameDao.countGamesByEventCaseSensitive(query)
        }

        return gameDao.countGamesByEvent(query)
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
