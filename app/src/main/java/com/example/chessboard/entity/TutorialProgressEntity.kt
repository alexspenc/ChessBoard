package com.example.chessboard.entity

/**
 * File role: stores persisted onboarding/tutorial progress runs in Room.
 * Allowed here:
 * - Room fields that describe tutorial type, current stage, tracked entities, and terminal state
 * - small persistence-facing defaults used for saving and restoring tutorial progress
 * Not allowed here:
 * - tutorial transition logic
 * - UI gating or screen orchestration
 * Validation date: 2026-05-02
 */
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tutorial_progress")
data class TutorialProgressEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tutorialType: TutorialType,
    val stage: TutorialStage,
    val trackedGameId: Long? = null,
    val runStatus: TutorialRunStatus = TutorialRunStatus.ACTIVE,
    val startedAt: Long,
    val completedAt: Long? = null,
    val abortedAt: Long? = null,
)
