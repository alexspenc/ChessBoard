package com.example.chessboard.service

/**
 * File role: owns tutorial flow rules on top of persisted tutorial progress and existing app data.
 * Allowed here:
 * - decisions about whether tutorial should be offered
 * - valid tutorial lifecycle transitions such as start, abort, and early stage advancement
 * - coordination between tutorial progress persistence and SimpleView enforcement
 * Not allowed here:
 * - Compose UI, dialogs, or screen rendering
 * - direct navigation orchestration
 * Validation date: 2026-05-03
 */
import com.example.chessboard.entity.TutorialProgressEntity
import com.example.chessboard.entity.TutorialRunStatus
import com.example.chessboard.entity.TutorialStage
import com.example.chessboard.entity.TutorialType

class TutorialService(
    private val tutorialProgressService: TutorialProgressService,
    private val gameListService: GameListService,
    private val globalTrainingStatsService: GlobalTrainingStatsService,
    private val userProfileService: UserProfileService,
) {

    suspend fun getActiveTutorial(): TutorialProgressEntity? {
        return tutorialProgressService.getActiveTutorial()
    }

    suspend fun shouldOfferManualTutorial(): Boolean {
        val activeTutorial = tutorialProgressService.getActiveTutorial()
        if (activeTutorial != null) {
            return false
        }

        val gamesCount = gameListService.getGamesCount()
        if (gamesCount != 0) {
            return false
        }

        val stats = globalTrainingStatsService.getStats()
        if (stats.totalTrainingsCount != 0) {
            return false
        }

        return true
    }

    suspend fun startManualFirstFlowTutorial(
        startedAt: Long = System.currentTimeMillis()
    ): TutorialProgressEntity? {
        val activeTutorial = tutorialProgressService.getActiveTutorial()
        if (activeTutorial != null) {
            return activeTutorial
        }

        userProfileService.setSimpleViewEnabled(true)

        val tutorialId = tutorialProgressService.createTutorial(
            tutorialType = TutorialType.MANUAL_FIRST_FLOW,
            stage = TutorialStage.START,
            startedAt = startedAt
        )

        return tutorialProgressService.getTutorialById(tutorialId)
    }

    suspend fun abortActiveTutorial(
        abortedAt: Long = System.currentTimeMillis()
    ): TutorialProgressEntity? {
        val activeTutorial = tutorialProgressService.getActiveTutorial() ?: return null

        val abortedTutorial = activeTutorial.copy(
            runStatus = TutorialRunStatus.ABORTED,
            abortedAt = abortedAt
        )
        tutorialProgressService.updateTutorial(abortedTutorial)
        return abortedTutorial
    }

    suspend fun markGameCreated(gameId: Long): TutorialProgressEntity? {
        if (!doesGameExist(gameId)) {
            return null
        }

        val activeTutorial = tutorialProgressService.getActiveTutorial() ?: return null
        if (activeTutorial.tutorialType != TutorialType.MANUAL_FIRST_FLOW) {
            return null
        }
        if (activeTutorial.stage != TutorialStage.START) {
            return activeTutorial
        }

        val updatedTutorial = activeTutorial.copy(
            stage = TutorialStage.GAME_CREATED,
            trackedGameId = gameId
        )
        tutorialProgressService.updateTutorial(updatedTutorial)
        return updatedTutorial
    }

    private suspend fun doesGameExist(gameId: Long): Boolean {
        val games = gameListService.getGamesByIds(listOf(gameId))
        if (games.isEmpty()) {
            return false
        }

        return true
    }
}
