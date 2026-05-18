package com.example.chessboard.service

/*
 * Defines the tunable parameters for statistics-based training recommendations.
 *
 * Keep formula coefficients, caps, boosts, and score-to-weight thresholds here.
 * Do not add database access, UI state, or screen-specific selection settings
 * such as the currently requested recommendation limit.
 *
 * Validation date: 2026-05-18
 */

data class StatisticsTrainingFormulaSettings(
    /** Number of latest training results used to calculate recent mistakes and perfect rate for one line. */
    val recentResultsPerLine: Int = 5,
    /** Maximum days since last training that can contribute to the recency part of the score. */
    val recencyDaysCap: Int = 7,
    /** Score multiplier for mistakes in the most recent training result for the line. */
    val lastMistakeWeight: Double = 4.0,
    /** Maximum recent-result mistake count that can contribute to the last-mistake score part. */
    val maxMistakesLast: Int = 3,
    /** Score multiplier for average mistakes across recent training results for the line. */
    val avgMistakesWeight: Double = 2.0,
    /** Maximum average mistakes value that can contribute to the average-mistake score part. */
    val maxAvgMistakesRecent: Double = 3.0,
    /** Score multiplier for days since the line was last trained. */
    val recencyWeight: Double = 2.0,
    /** Score penalty multiplier for the share of recent perfect trainings. */
    val perfectRatePenaltyWeight: Double = 2.0,
    /** Score boost for a line that has never been trained. */
    val noAttemptsBoost: Double = 3.0,
    /** Score boost for a line that has exactly one recorded training attempt. */
    val oneAttemptBoost: Double = 2.0,
    /** Score boost for a line that has exactly two recorded training attempts. */
    val twoAttemptsBoost: Double = 1.0,
    /** Minimum score that maps a recommended line to weight 5. */
    val weight5ScoreThreshold: Double = 10.0,
    /** Minimum score that maps a recommended line to weight 4. */
    val weight4ScoreThreshold: Double = 7.0,
    /** Minimum score that maps a recommended line to weight 3. */
    val weight3ScoreThreshold: Double = 4.0,
    /** Minimum score that maps a recommended line to weight 2. Lower scores map to weight 1. */
    val weight2ScoreThreshold: Double = 2.0,
)
