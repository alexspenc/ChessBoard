package com.example.chessboard.runtimecontext

/**
 * File role: groups runtime-only state for in-progress training sessions.
 * Allowed here:
 * - active training game tracking, move-range state, and per-game progress snapshots
 * - in-memory helpers that restore unfinished training sessions after screen changes
 * Not allowed here:
 * - composable UI, navigation rendering, or screen layout code
 * - database persistence, repository access, or long-term storage logic
 * Validation date: 2026-04-25
 */

import com.example.chessboard.ui.screen.trainSingleGame.TrainSingleGamePhase
import com.example.chessboard.ui.screen.trainSingleGame.TrainSingleGameUiState

class TrainingRuntimeContext {
    internal data class GameProgressSnapshot(
        val currentPly: Int,
        val lineFingerprint: String,
        val uiState: TrainSingleGameUiState,
    )

    private data class TrainingSession(
        val currentGameId: Long? = null,
        val orderedGameIds: List<Long> = emptyList(),
        val gameProgressById: Map<Long, GameProgressSnapshot> = emptyMap(),
    )

    private val sessionsByTrainingId = mutableMapOf<Long, TrainingSession>()

    fun rememberLaunch(
        trainingId: Long,
        gameId: Long,
        orderedGameIds: List<Long>,
    ) {
        val currentSession = sessionsByTrainingId[trainingId] ?: TrainingSession()
        sessionsByTrainingId[trainingId] = currentSession.copy(
            currentGameId = gameId,
            orderedGameIds = orderedGameIds,
        )
    }

    fun orderedGameIds(trainingId: Long): List<Long> {
        return sessionsByTrainingId[trainingId]?.orderedGameIds ?: emptyList()
    }

    fun activeGameId(trainingId: Long): Long? {
        return sessionsByTrainingId[trainingId]?.currentGameId
    }

    fun firstStartedGameId(trainingId: Long): Long? {
        val session = sessionsByTrainingId[trainingId] ?: return null
        if (session.gameProgressById.isEmpty()) {
            return null
        }

        val firstStartedOrderedGameId = session.orderedGameIds.firstOrNull { gameId ->
            session.gameProgressById.containsKey(gameId)
        }
        if (firstStartedOrderedGameId != null) {
            return firstStartedOrderedGameId
        }

        val activeGameId = session.currentGameId
        if (activeGameId != null && session.gameProgressById.containsKey(activeGameId)) {
            return activeGameId
        }

        return session.gameProgressById.keys.firstOrNull()
    }

    fun setCurrentGameId(trainingId: Long, gameId: Long?) {
        val currentSession = sessionsByTrainingId[trainingId] ?: return
        sessionsByTrainingId[trainingId] = currentSession.copy(currentGameId = gameId)
    }

    fun resolveNextGameId(trainingId: Long, currentGameId: Long): Long? {
        val orderedGameIds = orderedGameIds(trainingId)
        val currentIndex = orderedGameIds.indexOf(currentGameId)
        if (currentIndex < 0) {
            return null
        }

        return orderedGameIds.getOrNull(currentIndex + 1)
    }

    fun sessionCurrent(trainingId: Long, gameId: Long): Int {
        return orderedGameIds(trainingId).indexOf(gameId).coerceAtLeast(0) + 1
    }

    fun sessionTotal(trainingId: Long): Int {
        return orderedGameIds(trainingId).size
    }

    internal fun restoreGameProgress(trainingId: Long, gameId: Long): GameProgressSnapshot? {
        return sessionsByTrainingId[trainingId]?.gameProgressById?.get(gameId)
    }

    internal fun saveGameProgress(
        trainingId: Long,
        gameId: Long,
        currentPly: Int,
        lineFingerprint: String,
        uiState: TrainSingleGameUiState,
    ) {
        val currentSession = sessionsByTrainingId[trainingId] ?: TrainingSession()
        sessionsByTrainingId[trainingId] = currentSession.copy(
            currentGameId = gameId,
            gameProgressById = currentSession.gameProgressById + (
                gameId to GameProgressSnapshot(
                    currentPly = currentPly,
                    lineFingerprint = lineFingerprint,
                    uiState = sanitizeUiState(uiState),
                )
            ),
        )
    }

    fun clearGameProgress(trainingId: Long, gameId: Long) {
        val currentSession = sessionsByTrainingId[trainingId] ?: return
        sessionsByTrainingId[trainingId] = currentSession.copy(
            gameProgressById = currentSession.gameProgressById - gameId,
        )
    }

    fun clearTrainingSession(trainingId: Long) {
        sessionsByTrainingId.remove(trainingId)
    }

    private fun sanitizeUiState(uiState: TrainSingleGameUiState): TrainSingleGameUiState {
        var sanitizedState = uiState.copy(wrongMoveSquare = null)
        if (sanitizedState.phase == TrainSingleGamePhase.ShowingLine) {
            sanitizedState = sanitizedState.copy(phase = TrainSingleGamePhase.Idle)
        }

        return sanitizedState
    }
}
