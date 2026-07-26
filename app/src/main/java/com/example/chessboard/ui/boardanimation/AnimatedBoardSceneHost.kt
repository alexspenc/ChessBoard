package com.example.chessboard.ui.boardanimation

/**
 * Compose host that replays timed board actions on top of the shared board renderer.
 * Keep animated and instant playback wiring plus temporary scene projection here.
 * Do not add screen orchestration, controller mutations, or gesture handling to this file.
 * Validation date: 2026-07-10
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.chessboard.ui.boardrender.BoardSceneRenderer

@Composable
fun AnimatedBoardSceneHost(
    controller: BoardAnimationQueueController,
    squareSizePx: Float,
    modifier: Modifier = Modifier,
) {
    val sceneToRender = rememberBoardPlaybackScene(
        controller = controller,
        squareSizePx = squareSizePx,
    ) ?: return

    BoardSceneRenderer(
        scene = sceneToRender,
        squareSizePx = squareSizePx,
        modifier = modifier,
    )
}
