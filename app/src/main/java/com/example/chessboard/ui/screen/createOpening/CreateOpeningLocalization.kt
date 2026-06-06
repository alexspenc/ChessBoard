package com.example.chessboard.ui.screen.createOpening

/**
 * File role: groups create-opening localization holders and compose-side resource reads.
 * Allowed here:
 * - create-opening string holder factories built from Compose string resources
 * - small formatting helpers that belong to create-opening UI text
 * Not allowed here:
 * - screen orchestration, PGN parsing, save logic, or persistence behavior
 * Validation date: 2026-06-06
 */
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.service.PgnParseErrorStrings

@Composable
internal fun createOpeningPgnParseErrorStrings(): PgnParseErrorStrings {
    return PgnParseErrorStrings(
        mainLine = stringResource(R.string.create_opening_pgn_error_main_line),
        variation = stringResource(R.string.create_opening_pgn_error_variation),
        whiteSide = stringResource(R.string.create_opening_pgn_error_white_side),
        blackSide = stringResource(R.string.create_opening_pgn_error_black_side),
        lineParseFailed = stringResource(R.string.create_opening_pgn_error_line_parse_failed),
        unrecognizedNotation = stringResource(R.string.create_opening_pgn_error_unrecognized_notation),
        illegalMove = stringResource(R.string.create_opening_pgn_error_illegal_move),
    )
}
