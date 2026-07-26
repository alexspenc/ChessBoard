package com.example.chessboard.ui.boardanimation

/**
 * Produces the currently visible scene for queued board playback.
 * Keep animation timing, active-action completion, and temporary scene projection here.
 * Do not add board gestures, screen orchestration, or chess-rule mutations to this file.
 * Validation date: 2026-07-26
 */

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.chessboard.ui.boardrender.BoardRenderScene
import kotlinx.coroutines.delay

@Composable
internal fun rememberBoardPlaybackScene(
    controller: BoardAnimationQueueController,
    squareSizePx: Float,
    fallbackScene: BoardRenderScene? = null,
): BoardRenderScene? {
    val state = controller.state
    val currentScene = state.currentScene ?: fallbackScene ?: return null
    val activeAction = state.activeAction
    var progress by remember(activeAction) { mutableFloatStateOf(0f) }

    LaunchedEffect(activeAction) {
        if (activeAction == null) {
            progress = 0f
            return@LaunchedEffect
        }

        when (activeAction) {
            is AnimatedBoardMoveAction -> {
                val animationProgress = Animatable(0f)
                animationProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = activeAction.durationMs),
                ) {
                    progress = value
                }
            }
            is ApplyBoardSceneAction -> delay(activeAction.durationMs.toLong())
        }
        controller.completeActiveAction()
    }

    return buildBoardPlaybackScene(
        currentScene = currentScene,
        activeAction = activeAction,
        progress = progress,
        squareSizePx = squareSizePx,
    )
}

private fun buildBoardPlaybackScene(
    currentScene: BoardRenderScene,
    activeAction: BoardPlaybackAction?,
    progress: Float,
    squareSizePx: Float,
): BoardRenderScene {
    if (activeAction == null) {
        return currentScene
    }

    return when (activeAction) {
        is AnimatedBoardMoveAction -> buildAnimatedBoardRenderScene(
            baseScene = currentScene,
            activeAction = activeAction,
            progress = progress,
            squareSizePx = squareSizePx,
        )
        is ApplyBoardSceneAction -> activeAction.scene
    }
}
