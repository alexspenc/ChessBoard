package com.example.chessboard.ui.screen.trainSingleLine

/**
 * File role: keeps the pure decision rule for suggesting the full training UI.
 * Allowed here:
 * - side-effect-free prompt eligibility checks for single-line training completion
 * - small constants tied to this prompt behavior
 * Not allowed here:
 * - Compose rendering, navigation, or database access
 * Validation date: 2026-06-06
 */

import com.example.chessboard.service.clampSimpleViewUpgradePromptInterval

internal fun shouldShowSimpleViewUpgradePrompt(
    simpleViewEnabled: Boolean,
    promptDisabled: Boolean,
    totalTrainingsCount: Int,
    promptInterval: Int,
): Boolean {
    if (!simpleViewEnabled) {
        return false
    }

    if (promptDisabled) {
        return false
    }

    if (totalTrainingsCount <= 0) {
        return false
    }

    val interval = clampSimpleViewUpgradePromptInterval(promptInterval)
    return totalTrainingsCount % interval == 0
}
