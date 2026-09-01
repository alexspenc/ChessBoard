package com.example.chessboard.ui.screen.fenpositions

/*
 * File role: groups localized text shared by FEN position deletion entry points.
 * Allowed here:
 * - resource reads and text values used by the common deletion flow
 * Not allowed here:
 * - deletion state, persistence calls, screen layout, or navigation
 * Validation date: 2026-09-01
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R

internal data class FenPositionDeletionStrings(
    val title: String,
    val message: String,
    val confirm: String,
    val deletingTitle: String,
    val deletingMessage: String,
    val failedTitle: String,
    val failedMessage: String,
)

@Composable
internal fun fenPositionDeletionStrings(): FenPositionDeletionStrings {
    return FenPositionDeletionStrings(
        title = stringResource(R.string.fen_position_delete_title),
        message = stringResource(R.string.fen_position_delete_message),
        confirm = stringResource(R.string.fen_position_delete_confirm),
        deletingTitle = stringResource(R.string.fen_position_delete_deleting_title),
        deletingMessage = stringResource(R.string.fen_position_delete_deleting_message),
        failedTitle = stringResource(R.string.fen_position_delete_failed_title),
        failedMessage = stringResource(R.string.fen_position_delete_failed_message),
    )
}
