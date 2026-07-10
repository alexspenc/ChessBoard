package com.example.chessboard.ui.boardanimation

/**
 * Owns the FIFO animation queue for chess-board render actions.
 * Keep queue mutation and action lifecycle rules here so screens can drive animations without duplicating logic.
 * Do not add Compose drawing code, chess-rule validation, or screen navigation state to this file.
 * Validation date: 2026-07-10
 */

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class BoardAnimationQueueController(
    initialState: BoardAnimationState = BoardAnimationState(),
) {

    var state by mutableStateOf(initialState)
        private set

    fun submit(action: BoardAnimationAction) {
        when (action) {
            is ResetBoardSceneAction -> applyResetAction(action)
            is AnimateSimpleMoveAction -> enqueueAnimatedMove(action)
        }
    }

    fun completeActiveAction() {
        val activeAction = state.activeAction ?: return
        val currentScene = state.currentScene ?: return

        val nextScene = applyAnimatedSimpleMove(
            scene = currentScene,
            action = activeAction,
        )
        val remainingPendingActions = state.pendingActions
        val nextActiveAction = remainingPendingActions.firstOrNull()

        state = state.copy(
            currentScene = nextScene,
            pendingActions = remainingPendingActions.drop(1),
            activeAction = nextActiveAction,
            renderPly = activeAction.logicalPlyAfter,
        )
    }

    private fun applyResetAction(action: ResetBoardSceneAction) {
        state = BoardAnimationState(
            currentScene = action.scene,
            pendingActions = emptyList(),
            activeAction = null,
            renderPly = action.renderPly,
        )
    }

    private fun enqueueAnimatedMove(action: AnimateSimpleMoveAction) {
        if (state.currentScene == null) {
            return
        }

        val activeAction = state.activeAction
        if (activeAction == null) {
            state = state.copy(activeAction = action)
            return
        }

        state = state.copy(pendingActions = state.pendingActions + action)
    }
}
