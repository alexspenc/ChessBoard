@file:Suppress("FunctionName")

package com.example.chessboard.ui.screen.gameOpeningAnalysis.results

/*
 * File role: formats game-opening analysis result models for this feature's UI screens.
 * Allowed here:
 * - screen-facing labels, detail strings, board-preview FEN selection, and small display helpers for imported games/results
 * - pure UI presentation helpers shared by the results list and result detail screens
 * Not allowed here:
 * - Compose screen layout, analyzer execution, database access, PGN parsing, or result mutation
 * Validation date: 2026-06-28
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.analysis.GameOpeningAnalysisResult
import com.example.chessboard.analysis.GameOpeningBookTooShort
import com.example.chessboard.analysis.GameOpeningDeviation
import com.example.chessboard.analysis.GameOpeningInvalidGameMove
import com.example.chessboard.analysis.GameOpeningInvalidInitialPosition
import com.example.chessboard.analysis.GameOpeningMatchesKnownOpening
import com.example.chessboard.analysis.GameOpeningNoMatchingOpening
import com.example.chessboard.analysis.GameOpeningOpponentLeftBook
import com.example.chessboard.analysis.OpeningSide
import com.example.chessboard.runtimecontext.ImportedGameItem
import com.example.chessboard.ui.BoardOrientation

@Composable
internal fun resultTypeLabel(result: GameOpeningAnalysisResult): String =
    when (result) {
        is GameOpeningDeviation -> stringResource(R.string.game_opening_analysis_result_deviation)
        is GameOpeningOpponentLeftBook -> stringResource(R.string.game_opening_analysis_result_opponent_left_book)
        is GameOpeningBookTooShort -> stringResource(R.string.game_opening_analysis_result_book_too_short)
        is GameOpeningMatchesKnownOpening -> stringResource(R.string.game_opening_analysis_result_matches_known_opening)
        is GameOpeningNoMatchingOpening -> stringResource(R.string.game_opening_analysis_result_no_matching_opening)
        is GameOpeningInvalidGameMove -> stringResource(R.string.game_opening_analysis_result_invalid_game_move)
        is GameOpeningInvalidInitialPosition -> stringResource(R.string.game_opening_analysis_result_invalid_initial_position)
    }

@Composable
internal fun resultDetailText(result: GameOpeningAnalysisResult): String =
    when (result) {
        is GameOpeningDeviation -> {
            stringResource(R.string.game_opening_analysis_result_ply, result.ply)
        }

        is GameOpeningOpponentLeftBook -> {
            stringResource(R.string.game_opening_analysis_result_ply, result.ply)
        }

        is GameOpeningBookTooShort -> {
            stringResource(R.string.game_opening_analysis_result_matched_ply, result.matchedPly)
        }

        is GameOpeningMatchesKnownOpening -> {
            stringResource(R.string.game_opening_analysis_result_matched_ply, result.matchedPly)
        }

        is GameOpeningNoMatchingOpening -> {
            stringResource(R.string.game_opening_analysis_result_ply, result.ply)
        }

        is GameOpeningInvalidGameMove -> {
            stringResource(
                R.string.game_opening_analysis_result_invalid_move_detail,
                result.moveUci,
                result.reason.name,
            )
        }

        is GameOpeningInvalidInitialPosition -> {
            stringResource(R.string.game_opening_analysis_result_invalid_initial_position_detail)
        }
    }

@Composable
internal fun resultExtraDetailTexts(result: GameOpeningAnalysisResult): List<String> {
    when (result) {
        is GameOpeningDeviation -> {
            return listOf(stringResource(R.string.game_opening_analysis_result_played_move, result.playedMoveUci))
        }

        is GameOpeningOpponentLeftBook -> {
            return listOf(stringResource(R.string.game_opening_analysis_result_played_move, result.playedMoveUci))
        }

        is GameOpeningBookTooShort -> {
            val nextMove = result.nextGameMoveUci ?: return emptyList()
            return listOf(stringResource(R.string.game_opening_analysis_result_next_game_move, nextMove))
        }

        is GameOpeningNoMatchingOpening -> {
            return listOf(stringResource(R.string.game_opening_analysis_result_played_move, result.playedMoveUci))
        }

        is GameOpeningMatchesKnownOpening,
        is GameOpeningInvalidGameMove,
        is GameOpeningInvalidInitialPosition,
        -> {
            return emptyList()
        }
    }
}

internal fun resultPreviewFen(result: GameOpeningAnalysisResult): String? =
    when (result) {
        is GameOpeningDeviation -> result.positionFen
        is GameOpeningOpponentLeftBook -> result.positionFen
        is GameOpeningBookTooShort -> result.lastKnownPositionFen
        is GameOpeningMatchesKnownOpening -> result.finalPositionFen
        is GameOpeningNoMatchingOpening -> result.positionFen
        is GameOpeningInvalidGameMove -> result.positionFen
        is GameOpeningInvalidInitialPosition -> null
    }

internal fun resolveResultBoardOrientation(result: GameOpeningAnalysisResult): BoardOrientation {
    if (result.selectedSide == OpeningSide.BLACK) {
        return BoardOrientation.BLACK
    }

    return BoardOrientation.WHITE
}

internal fun ImportedGameItem.displayEvent(unknownEvent: String): String {
    val event = headers[EVENT_HEADER]
    if (!event.isNullOrBlank()) {
        return event
    }

    return unknownEvent
}

internal fun ImportedGameItem.displayPlayers(unknownPlayer: String): String {
    val white = headers[WHITE_HEADER]?.takeIf { value -> value.isNotBlank() } ?: unknownPlayer
    val black = headers[BLACK_HEADER]?.takeIf { value -> value.isNotBlank() } ?: unknownPlayer
    return "$white - $black"
}

private const val EVENT_HEADER = "Event"
private const val WHITE_HEADER = "White"
private const val BLACK_HEADER = "Black"
