package com.example.chessboard.ui.screen.training

/*
 * Shared success model for create/edit training save flows.
 *
 * Keep only the common data returned to training save success dialogs here so
 * create and edit screens can reuse one model without duplicating fields.
 * Do not add dialog composables or persistence logic to this file.
 */

internal data class TrainingSaveSuccess(
    val trainingId: Long,
    val trainingName: String,
    val gamesCount: Int,
)
