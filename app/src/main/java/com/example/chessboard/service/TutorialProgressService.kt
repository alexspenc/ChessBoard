package com.example.chessboard.service

/**
 * File role: groups persistence-oriented tutorial progress operations on top of TutorialProgressDao.
 * Allowed here:
 * - loading and saving persisted tutorial progress rows
 * - small persistence helpers for active/completed/aborted tutorial records
 * Not allowed here:
 * - screen gating decisions
 * - tutorial UI text or navigation orchestration
 * Validation date: 2026-05-02
 */
import com.example.chessboard.entity.TutorialProgressEntity
import com.example.chessboard.entity.TutorialStage
import com.example.chessboard.entity.TutorialType
import com.example.chessboard.repository.TutorialProgressDao

class TutorialProgressService(
    private val dao: TutorialProgressDao
) {

    suspend fun getActiveTutorial(): TutorialProgressEntity? {
        return dao.getActive()
    }

    suspend fun getLatestTutorial(): TutorialProgressEntity? {
        return dao.getLatest()
    }

    suspend fun getTutorialById(tutorialId: Long): TutorialProgressEntity? {
        return dao.getById(tutorialId)
    }

    suspend fun createTutorial(
        tutorialType: TutorialType,
        stage: TutorialStage,
        startedAt: Long
    ): Long {
        return dao.insert(
            TutorialProgressEntity(
                tutorialType = tutorialType,
                stage = stage,
                startedAt = startedAt
            )
        )
    }

    suspend fun updateTutorial(progress: TutorialProgressEntity) {
        dao.update(progress)
    }
}
