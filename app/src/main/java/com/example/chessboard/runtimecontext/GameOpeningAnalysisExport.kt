package com.example.chessboard.runtimecontext

/*
 * File role: serializes game-opening analysis imported games into normalized PGN text.
 * Allowed here:
 * - pure conversion from ImportedGameItem values to PGN records
 * - preserving imported headers and rebuilding main-line SAN movetext from stored UCI moves
 * Not allowed here:
 * - Compose UI, Android document picker wiring, file I/O, database access, or runtime state mutation
 * Validation date: 2026-06-30
 */

import com.example.chessboard.service.resolvePgnSanMove
import com.github.bhlangonijr.chesslib.Board

private const val PGN_EVENT_HEADER = "Event"
private const val PGN_SITE_HEADER = "Site"
private const val PGN_DATE_HEADER = "Date"
private const val PGN_ROUND_HEADER = "Round"
private const val PGN_WHITE_HEADER = "White"
private const val PGN_BLACK_HEADER = "Black"
private const val PGN_RESULT_HEADER = "Result"
private const val DEFAULT_EXPORTED_EVENT = "Imported game"
private const val DEFAULT_EXPORTED_HEADER_VALUE = "?"
private const val DEFAULT_EXPORTED_RESULT = "*"

private val standardPgnHeaderOrder =
    listOf(
        PGN_EVENT_HEADER,
        PGN_SITE_HEADER,
        PGN_DATE_HEADER,
        PGN_ROUND_HEADER,
        PGN_WHITE_HEADER,
        PGN_BLACK_HEADER,
        PGN_RESULT_HEADER,
    )

private val validPgnResults = setOf("1-0", "0-1", "1/2-1/2", DEFAULT_EXPORTED_RESULT)

/** Builds normalized PGN text containing [games] in their current order. */
fun buildGameOpeningAnalysisGamesPgn(games: List<ImportedGameItem>): String {
    return games
        .joinToString(separator = "\n\n") { game -> buildGameOpeningAnalysisGamePgn(game) }
        .trim()
}

private fun buildGameOpeningAnalysisGamePgn(game: ImportedGameItem): String {
    return buildString {
        appendLine(buildGameOpeningAnalysisHeadersPgn(game.headers))
        appendLine()
        append(buildGameOpeningAnalysisMainLinePgn(game.mainLineMoves, game.headers[PGN_RESULT_HEADER]))
    }
}

private fun buildGameOpeningAnalysisHeadersPgn(headers: Map<String, String>): String {
    val normalizedHeaders = linkedMapOf<String, String>()
    standardPgnHeaderOrder.forEach { headerName ->
        normalizedHeaders[headerName] = resolveGameOpeningAnalysisHeaderValue(
            headerName = headerName,
            headers = headers,
        )
    }
    headers.forEach { (name, value) ->
        if (!normalizedHeaders.containsKey(name)) {
            normalizedHeaders[name] = value
        }
    }

    return normalizedHeaders.entries.joinToString(separator = "\n") { (name, value) ->
        "[$name \"${normalizeGameOpeningAnalysisHeaderValue(value)}\"]"
    }
}

private fun resolveGameOpeningAnalysisHeaderValue(
    headerName: String,
    headers: Map<String, String>,
): String {
    val value = headers[headerName]?.takeIf { it.isNotBlank() }
    if (headerName == PGN_RESULT_HEADER) {
        return value?.takeIf { it in validPgnResults } ?: DEFAULT_EXPORTED_RESULT
    }

    if (value != null) {
        return value
    }

    if (headerName == PGN_EVENT_HEADER) {
        return DEFAULT_EXPORTED_EVENT
    }

    return DEFAULT_EXPORTED_HEADER_VALUE
}

private fun normalizeGameOpeningAnalysisHeaderValue(value: String): String {
    return value.replace("\"", "'")
}

private fun buildGameOpeningAnalysisMainLinePgn(
    uciMoves: List<String>,
    rawResult: String?,
): String {
    val board = Board()
    val result = rawResult?.takeIf { it in validPgnResults } ?: DEFAULT_EXPORTED_RESULT
    val moveText = StringBuilder()

    uciMoves.forEachIndexed { index, uciMove ->
        if (index % 2 == 0) {
            moveText.append("${index / 2 + 1}. ")
        }

        val resolvedMove = resolvePgnSanMove(
            uciMove = uciMove,
            board = board,
        )
        moveText.append(resolvedMove.san)
        moveText.append(' ')
        board.doMove(resolvedMove.move)
    }

    moveText.append(result)
    return moveText.toString().trim()
}
