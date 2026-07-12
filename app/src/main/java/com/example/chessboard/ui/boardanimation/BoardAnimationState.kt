package com.example.chessboard.ui.boardanimation

/**
 * Immutable queue state for timed board playback.
 * Keep queue bookkeeping and render-scene ownership here for animated and instant playback actions.
 * Do not add screen workflow flags, persistence data, or Compose drawing helpers to this file.
 * Validation date: 2026-07-10
 */

import com.example.chessboard.ui.boardrender.BoardRenderScene

data class BoardAnimationState(
    val currentScene: BoardRenderScene? = null,
    val pendingActions: List<BoardPlaybackAction> = emptyList(),
    val activeAction: BoardPlaybackAction? = null,
    val renderPly: Int = 0,
) {
    val isPlaying: Boolean
        get() = activeAction != null
}
