package com.example.chessboard.ui.screen.trainSingleGame

/**
 * File role: groups fingerprint helpers for validating whether saved training progress still matches the current line.
 * Allowed here:
 * - pure helpers that derive stable line fingerprints and snapshot-restore compatibility checks
 * - snapshot-validation logic that depends only on training line inputs and saved metadata
 * Not allowed here:
 * - Compose UI, runtime-context mutation, or database access
 * - broader training-session orchestration that belongs in TrainSingleGameScreen
 * Validation date: 2026-04-26
 */

import java.security.MessageDigest

internal fun resolveTrainingLineFingerprint(
    startFen: String?,
    uciMoves: List<String>,
): String {
    val normalizedStartFen = startFen ?: "startpos"
    val lineValue = buildString {
        append(normalizedStartFen)
        append('|')
        append(uciMoves.joinToString(","))
    }

    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(lineValue.toByteArray(Charsets.UTF_8))
    return hashBytes.joinToString(separator = "") { byte ->
        "%02x".format(byte)
    }
}

internal fun shouldRestoreTrainingSnapshot(
    snapshotFingerprint: String,
    currentFingerprint: String,
): Boolean {
    return snapshotFingerprint == currentFingerprint
}
