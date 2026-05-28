package com.example.chessboard.service

/*
 * File role: keeps pure filtering helpers for dubious-line search results.
 * Allowed here:
 * - matching persisted dubious-line markers against loaded line metadata
 * - name filtering that mirrors lines-explorer search behavior
 * Not allowed here:
 * - Room calls, Compose state, or screen workflow decisions
 * Validation date: 2026-05-27
 */

import com.example.chessboard.entity.DubiousLineEntity
import com.example.chessboard.entity.LineEntity

internal fun filterDubiousLineIdsByName(
    dubiousLines: List<DubiousLineEntity>,
    lines: List<LineEntity>,
    query: String,
    isCaseSensitive: Boolean,
    sideMask: Int? = null,
): List<Long> {
    val linesById = lines.associateBy { line -> line.id }
    val normalizedQuery = normalizeDubiousLineFilterQuery(
        query = query,
        isCaseSensitive = isCaseSensitive,
    )

    return dubiousLines.mapNotNull { dubiousLine ->
        val line = linesById[dubiousLine.lineId] ?: return@mapNotNull null
        if (!matchesDubiousLineName(line, normalizedQuery, isCaseSensitive)) {
            return@mapNotNull null
        }

        if (!matchesDubiousLineSide(line, sideMask)) {
            return@mapNotNull null
        }

        line.id
    }
}

private fun matchesDubiousLineSide(
    line: LineEntity,
    sideMask: Int?,
): Boolean {
    if (sideMask == null) {
        return true
    }

    return (line.sideMask and sideMask) != 0
}

private fun normalizeDubiousLineFilterQuery(
    query: String,
    isCaseSensitive: Boolean,
): String {
    val trimmedQuery = query.trim()
    if (isCaseSensitive) {
        return trimmedQuery
    }

    return trimmedQuery.lowercase()
}

private fun matchesDubiousLineName(
    line: LineEntity,
    normalizedQuery: String,
    isCaseSensitive: Boolean,
): Boolean {
    if (normalizedQuery.isBlank()) {
        return true
    }

    var lineName = line.event.orEmpty()
    if (!isCaseSensitive) {
        lineName = lineName.lowercase()
    }

    return lineName.contains(normalizedQuery)
}
