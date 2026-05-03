package com.example.chessboard.entity

/**
 * File role: defines persisted enum types used by tutorial progress records.
 * Allowed here:
 * - tutorial type, stage, and lifecycle enums saved in Room
 * - small enum-only helpers tightly coupled to persisted tutorial progress state
 * Not allowed here:
 * - Room entities
 * - tutorial transition logic or UI text
 * Validation date: 2026-05-02
 */

enum class TutorialType {
    MANUAL_FIRST_FLOW,
}

enum class TutorialStage {
    START,
    GAME_CREATED,
    TRAINING_CREATED,
    TRAINING_STARTED,
    TRAINING_COMPLETED,
}

enum class TutorialRunStatus {
    ACTIVE,
    COMPLETED,
    ABORTED,
}
