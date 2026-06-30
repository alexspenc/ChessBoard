package com.example.chessboard.runtimecontext

/*
 * File role: defines shared worker-count selection for game-opening analysis screen jobs.
 * Allowed here:
 * - pure helper logic that chooses bounded parallelism for import and analysis workflows
 * Not allowed here:
 * - coroutine task execution, runtime state mutation, UI rendering, PGN parsing, or chess analysis
 * Validation date: 2026-06-30
 */

internal fun resolveGameOpeningAnalysisParallelism(
    availableProcessors: Int = Runtime.getRuntime().availableProcessors(),
): Int {
    val safeProcessors = availableProcessors.coerceAtLeast(1)
    if (safeProcessors <= 2) {
        return 1
    }

    return (safeProcessors + 1) / 2
}
