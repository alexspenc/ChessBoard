package com.example.chessboard.runtimecontext

/**
 * File role: groups runtime-only line-selection state shared by line collections.
 * Allowed here:
 * - selected-line tracking, ordered line ids, and in-memory session helpers
 * - collection-agnostic selection behavior reused by trainings and templates
 * Not allowed here:
 * - training progress snapshots, composable UI, or persistence logic
 * - collection-specific business rules outside generic line-selection state
 * Validation date: 2026-05-23
 */

import androidx.compose.runtime.mutableStateMapOf

class LineCollectionRuntimeContext {
    private data class LineCollectionSession(
        val selectedLineId: Long? = null,
        val orderedLineIds: List<Long> = emptyList(),
    )

    private val sessionsByCollectionId = mutableStateMapOf<Long, LineCollectionSession>()

    fun orderedLineIds(collectionId: Long): List<Long> {
        return sessionsByCollectionId[collectionId]?.orderedLineIds ?: emptyList()
    }

    fun setOrderedLineIds(collectionId: Long, lineIds: List<Long>) {
        updateSession(collectionId) { session ->
            session.copy(orderedLineIds = lineIds)
        }
    }

    fun selectedLineId(collectionId: Long): Long? {
        return sessionsByCollectionId[collectionId]?.selectedLineId
    }

    fun setSelectedLineId(collectionId: Long, lineId: Long?) {
        updateSession(collectionId) { session ->
            session.copy(selectedLineId = lineId)
        }
    }

    fun resolveSelectedLineId(collectionId: Long): Long? {
        val session = sessionsByCollectionId[collectionId] ?: return null
        val selectedLineId = session.selectedLineId
        if (selectedLineId != null && selectedLineId in session.orderedLineIds) {
            return selectedLineId
        }

        return session.orderedLineIds.firstOrNull()
    }

    fun resolveNextLineId(collectionId: Long, currentLineId: Long): Long? {
        val orderedLineIds = orderedLineIds(collectionId)
        val currentIndex = orderedLineIds.indexOf(currentLineId)
        if (currentIndex < 0) {
            return null
        }

        return orderedLineIds.getOrNull(currentIndex + 1)
    }

    fun clearSession(collectionId: Long) {
        sessionsByCollectionId.remove(collectionId)
    }

    private fun updateSession(
        collectionId: Long,
        update: (LineCollectionSession) -> LineCollectionSession,
    ) {
        val currentSession = sessionsByCollectionId[collectionId] ?: LineCollectionSession()
        sessionsByCollectionId[collectionId] = update(currentSession)
    }
}
