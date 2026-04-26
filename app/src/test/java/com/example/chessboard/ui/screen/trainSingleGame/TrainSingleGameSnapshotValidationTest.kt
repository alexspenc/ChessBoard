package com.example.chessboard.ui.screen.trainSingleGame

/**
 * File role: groups unit tests for single-game training snapshot fingerprint helpers.
 * Allowed here:
 * - pure tests for line fingerprint stability and snapshot-restore compatibility checks
 * - assertions about how startFen and move-list changes affect saved-progress validity
 * Not allowed here:
 * - Compose UI tests, runtime-context mutation checks, or database-backed behavior
 * - broader training-session orchestration tests that belong in other files
 * Validation date: 2026-04-26
 */

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainSingleGameSnapshotValidationTest {

    @Test
    fun `resolveTrainingLineFingerprint returns same value for same start fen and moves`() {
        val first = resolveTrainingLineFingerprint(
            startFen = null,
            uciMoves = listOf("e2e4", "e7e5"),
        )
        val second = resolveTrainingLineFingerprint(
            startFen = null,
            uciMoves = listOf("e2e4", "e7e5"),
        )

        assertEquals(first, second)
    }

    @Test
    fun `resolveTrainingLineFingerprint changes when start fen changes`() {
        val first = resolveTrainingLineFingerprint(
            startFen = null,
            uciMoves = listOf("e2e4", "e7e5"),
        )
        val second = resolveTrainingLineFingerprint(
            startFen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
            uciMoves = listOf("e2e4", "e7e5"),
        )

        assertFalse(first == second)
    }

    @Test
    fun `resolveTrainingLineFingerprint changes when move list changes`() {
        val first = resolveTrainingLineFingerprint(
            startFen = null,
            uciMoves = listOf("e2e4", "e7e5"),
        )
        val second = resolveTrainingLineFingerprint(
            startFen = null,
            uciMoves = listOf("e2e4", "c7c5"),
        )

        assertFalse(first == second)
    }

    @Test
    fun `shouldRestoreTrainingSnapshot returns true when fingerprints match`() {
        val fingerprint = resolveTrainingLineFingerprint(
            startFen = null,
            uciMoves = listOf("e2e4", "e7e5"),
        )

        assertTrue(
            shouldRestoreTrainingSnapshot(
                snapshotFingerprint = fingerprint,
                currentFingerprint = fingerprint,
            )
        )
    }

    @Test
    fun `shouldRestoreTrainingSnapshot returns false when fingerprints differ`() {
        val savedFingerprint = resolveTrainingLineFingerprint(
            startFen = null,
            uciMoves = listOf("e2e4", "e7e5", "g1f3"),
        )
        val currentFingerprint = resolveTrainingLineFingerprint(
            startFen = null,
            uciMoves = listOf("e2e4", "e7e5"),
        )

        assertFalse(
            shouldRestoreTrainingSnapshot(
                snapshotFingerprint = savedFingerprint,
                currentFingerprint = currentFingerprint,
            )
        )
    }
}
