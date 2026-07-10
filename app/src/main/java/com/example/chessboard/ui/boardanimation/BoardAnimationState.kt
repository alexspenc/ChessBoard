package com.example.chessboard.ui.boardanimation

/**
 * Immutable queue state for animated board playback.
 * Keep queue bookkeeping and render-scene ownership here for the board animation layer.
 * Do not add screen workflow flags, persistence data, or Compose drawing helpers to this file.
 * Validation date: 2026-07-10
 */

import com.example.chessboard.ui.boardrender.BoardRenderScene

data class BoardAnimationState(
    val currentScene: BoardRenderScene? = null,
    val pendingActions: List<AnimateSimpleMoveAction> = emptyList(),
    val activeAction: AnimateSimpleMoveAction? = null,
    val renderPly: Int = 0,
) {
    val isAnimating: Boolean
        get() = activeAction != null
}
