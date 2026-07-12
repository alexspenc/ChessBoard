package com.example.chessboard.ui.boardanimation

/**
 * Compose host that replays timed board actions on top of the shared board renderer.
 * Keep animated and instant playback wiring plus temporary scene projection here.
 * Do not add screen orchestration, controller mutations, or gesture handling to this file.
 * Validation date: 2026-07-10
 */

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import com.example.chessboard.ui.boardrender.BoardSceneRenderer
import kotlinx.coroutines.delay

@Composable
fun AnimatedBoardSceneHost(
    controller: BoardAnimationQueueController,
    squareSizePx: Float,
    modifier: Modifier = Modifier,
) {
    val state = controller.state
    val currentScene = state.currentScene ?: return
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

    val sceneToRender = buildSceneToRender(
        currentScene = currentScene,
        activeAction = activeAction,
        progress = progress,
        squareSizePx = squareSizePx,
    )

    BoardSceneRenderer(
        scene = sceneToRender,
        squareSizePx = squareSizePx,
        modifier = modifier,
    )
}

private fun buildSceneToRender(
    currentScene: com.example.chessboard.ui.boardrender.BoardRenderScene,
    activeAction: BoardPlaybackAction?,
    progress: Float,
    squareSizePx: Float,
): com.example.chessboard.ui.boardrender.BoardRenderScene {
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
