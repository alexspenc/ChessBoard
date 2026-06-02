package com.example.chessboard.ui.screen.training.template

/*
 * Localization holder for training-template helpers that cannot read Compose resources directly.
 * Keep small precomputed string groups for coroutine and non-composable template workflows here.
 * Do not add screen UI labels, navigation, loading state, or PGN export behavior to this file.
 * Validation date: 2026-06-02
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R

internal data class TrainingTemplatePgnStrings(
    val unavailableTitle: String,
    val unavailableMessage: String,
    val clipLabel: String,
    val copiedTitle: String,
    val copiedMessage: String,
)

@Composable
internal fun trainingTemplatePgnStrings(): TrainingTemplatePgnStrings {
    return TrainingTemplatePgnStrings(
        unavailableTitle = stringResource(R.string.training_template_pgn_unavailable_title),
        unavailableMessage = stringResource(R.string.training_template_pgn_unavailable_message),
        clipLabel = stringResource(R.string.training_template_pgn_clip_label),
        copiedTitle = stringResource(R.string.training_template_pgn_copied_title),
        copiedMessage = stringResource(R.string.training_template_pgn_copied_message),
    )
}
