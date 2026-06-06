package com.example.chessboard.ui.screen.trainSingleLine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleViewUpgradePromptPolicyTest {

    @Test
    fun `shows prompt on completed training interval while simple view is enabled`() {
        assertTrue(
            shouldShowSimpleViewUpgradePrompt(
                simpleViewEnabled = true,
                promptDisabled = false,
                totalTrainingsCount = 20,
                promptInterval = 20,
            )
        )
        assertTrue(
            shouldShowSimpleViewUpgradePrompt(
                simpleViewEnabled = true,
                promptDisabled = false,
                totalTrainingsCount = 40,
                promptInterval = 20,
            )
        )
    }

    @Test
    fun `does not show prompt between completed training intervals`() {
        assertFalse(
            shouldShowSimpleViewUpgradePrompt(
                simpleViewEnabled = true,
                promptDisabled = false,
                totalTrainingsCount = 21,
                promptInterval = 20,
            )
        )
    }

    @Test
    fun `does not show prompt when full view is already enabled`() {
        assertFalse(
            shouldShowSimpleViewUpgradePrompt(
                simpleViewEnabled = false,
                promptDisabled = false,
                totalTrainingsCount = 20,
                promptInterval = 20,
            )
        )
    }

    @Test
    fun `does not show prompt when user disabled suggestions`() {
        assertFalse(
            shouldShowSimpleViewUpgradePrompt(
                simpleViewEnabled = true,
                promptDisabled = true,
                totalTrainingsCount = 20,
                promptInterval = 20,
            )
        )
    }

    @Test
    fun `does not show prompt before any training is completed`() {
        assertFalse(
            shouldShowSimpleViewUpgradePrompt(
                simpleViewEnabled = true,
                promptDisabled = false,
                totalTrainingsCount = 0,
                promptInterval = 20,
            )
        )
    }

    @Test
    fun `clamps prompt interval before checking divisibility`() {
        assertTrue(
            shouldShowSimpleViewUpgradePrompt(
                simpleViewEnabled = true,
                promptDisabled = false,
                totalTrainingsCount = 20,
                promptInterval = 1,
            )
        )
        assertTrue(
            shouldShowSimpleViewUpgradePrompt(
                simpleViewEnabled = true,
                promptDisabled = false,
                totalTrainingsCount = 100,
                promptInterval = 200,
            )
        )
    }
}
