package com.example.chessboard.runtimecontext

/**
 * File role: groups runtime-only state for in-progress training sessions.
 * Allowed here:
 * - active training line tracking, editor-selected line state, and per-line progress snapshots
 * - in-memory helpers that restore unfinished training sessions after screen changes
 * Not allowed here:
 * - composable UI, navigation rendering, or screen layout code
 * - database persistence, repository access, or long-term storage logic
 * Validation date: 2026-04-25
 */

import androidx.compose.runtime.mutableStateMapOf
import com.example.chessboard.ui.screen.trainSingleLine.TrainSingleLinePhase
import com.example.chessboard.ui.screen.trainSingleLine.TrainSingleLineUiState

class TrainingRuntimeContext {
    internal data class LineProgressSnapshot(
        val currentPly: Int,
        val lineFingerprint: String,
        val uiState: TrainSingleLineUiState,
    )

    private data class TrainingSession(
        val lineIdInTraining: Long? = null,
        val lineProgressById: Map<Long, LineProgressSnapshot> = emptyMap(),
    )

    private val lineCollections = LineCollectionRuntimeContext()
    private val sessionsByTrainingId = mutableStateMapOf<Long, TrainingSession>()

    fun rememberLaunch(
        trainingId: Long,
        lineId: Long,
        orderedLineIds: List<Long>,
    ) {
        lineCollections.setOrderedLineIds(trainingId, orderedLineIds)
        lineCollections.setSelectedLineId(trainingId, lineId)
        updateSession(trainingId) { session ->
            session.copy(
                lineIdInTraining = lineId,
            )
        }
    }

    fun orderedLineIds(trainingId: Long): List<Long> {
        return lineCollections.orderedLineIds(trainingId)
    }

    fun lineIdInTraining(trainingId: Long): Long? {
        return sessionsByTrainingId[trainingId]?.lineIdInTraining
    }

    fun selectedLineId(trainingId: Long): Long? {
        val session = sessionsByTrainingId[trainingId]
        val lineIdInTraining = session?.lineIdInTraining
        if (lineIdInTraining != null) {
            return lineIdInTraining
        }

        val selectedLineId = lineCollections.selectedLineId(trainingId)
        if (selectedLineId != null) {
            return selectedLineId
        }

        return lineCollections.resolveSelectedLineId(trainingId)
    }

    fun firstStartedLineId(trainingId: Long): Long? {
        val session = sessionsByTrainingId[trainingId] ?: return null
        if (session.lineProgressById.isEmpty()) {
            return null
        }

        val firstStartedOrderedLineId = orderedLineIds(trainingId).firstOrNull { lineId ->
            session.lineProgressById.containsKey(lineId)
        }
        if (firstStartedOrderedLineId != null) {
            return firstStartedOrderedLineId
        }

        val lineIdInTraining = session.lineIdInTraining
        if (lineIdInTraining != null && session.lineProgressById.containsKey(lineIdInTraining)) {
            return lineIdInTraining
        }

        return session.lineProgressById.keys.firstOrNull()
    }

    fun setLineIdInTraining(trainingId: Long, lineId: Long?) {
        updateSession(trainingId) { session ->
            session.copy(lineIdInTraining = lineId)
        }
    }

    fun setSelectedLineId(trainingId: Long, lineId: Long?) {
        lineCollections.setSelectedLineId(trainingId, lineId)
    }

    fun resolveNextLineId(trainingId: Long, currentLineId: Long): Long? {
        return lineCollections.resolveNextLineId(trainingId, currentLineId)
    }

    fun sessionCurrent(trainingId: Long, lineId: Long): Int {
        return orderedLineIds(trainingId).indexOf(lineId).coerceAtLeast(0) + 1
    }

    fun sessionTotal(trainingId: Long): Int {
        return orderedLineIds(trainingId).size
    }

    internal fun restoreLineProgress(trainingId: Long, lineId: Long): LineProgressSnapshot? {
        return sessionsByTrainingId[trainingId]?.lineProgressById?.get(lineId)
    }

    internal fun saveLineProgress(
        trainingId: Long,
        lineId: Long,
        currentPly: Int,
        lineFingerprint: String,
        uiState: TrainSingleLineUiState,
    ) {
        updateSession(trainingId) { session ->
            session.copy(
                lineIdInTraining = lineId,
                lineProgressById = session.lineProgressById + (
                    lineId to LineProgressSnapshot(
                        currentPly = currentPly,
                        lineFingerprint = lineFingerprint,
                        uiState = sanitizeUiState(uiState),
                    )
                ),
            )
        }
    }

    fun clearLineProgress(trainingId: Long, lineId: Long) {
        val currentSession = sessionsByTrainingId[trainingId] ?: return
        sessionsByTrainingId[trainingId] = currentSession.copy(
            lineProgressById = currentSession.lineProgressById - lineId,
        )
    }

    fun clearTrainingSession(trainingId: Long) {
        sessionsByTrainingId.remove(trainingId)
        lineCollections.clearSession(trainingId)
    }

    private fun updateSession(
        trainingId: Long,
        update: (TrainingSession) -> TrainingSession,
    ) {
        val currentSession = sessionsByTrainingId[trainingId] ?: TrainingSession()
        sessionsByTrainingId[trainingId] = update(currentSession)
    }

    private fun sanitizeUiState(uiState: TrainSingleLineUiState): TrainSingleLineUiState {
        var sanitizedState = uiState.copy(wrongMoveSquare = null)
        if (sanitizedState.phase == TrainSingleLinePhase.ShowingLine) {
            sanitizedState = sanitizedState.copy(phase = TrainSingleLinePhase.Idle)
        }

        return sanitizedState
    }
}
