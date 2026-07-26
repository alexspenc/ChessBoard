package com.example.chessboard.ui.boardanimation

/**
 * Shared default values for queued board animation.
 * Keep only cross-screen defaults here so replay and training flows stay on the same timing contract.
 * Do not add screen-specific policies, queue state, or render-scene transformation logic to this file.
 * Validation date: 2026-07-11
 */

internal const val DefaultBoardMoveAnimationDurationMs = 80
