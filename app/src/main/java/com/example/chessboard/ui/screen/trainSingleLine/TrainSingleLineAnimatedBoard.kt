package com.example.chessboard.ui.screen.trainSingleLine

/**
 * Animated interactive board host for the single-line training screen.
 * Keep only TrainSingleLine-specific gesture ownership and animated-scene rendering here.
 * Do not add screen flow orchestration, persistence logic, or generic app-wide board abstractions.
 * Validation date: 2026-07-11
 */

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.InteractiveChessBoardTestTag
import com.example.chessboard.ui.boardanimation.AnimateSimpleMoveAction
import com.example.chessboard.ui.boardanimation.BoardAnimationQueueController
import com.example.chessboard.ui.boardanimation.buildAnimatedBoardRenderScene
import com.example.chessboard.ui.boardrender.BoardRenderScene
import com.example.chessboard.ui.boardrender.BoardSceneRenderer
import com.example.chessboard.ui.boardrender.buildBoardRenderScene

private const val CellCount = 8

// Mirrors one board axis when the training board is shown from Black's side.
private fun getRowOrColumn(orientation: BoardOrientation, rowCol: Int): Int {
    if (orientation == BoardOrientation.WHITE) {
        return rowCol
    }

    return CellCount - 1 - rowCol
}

// Converts a touch/cursor offset inside the board box into a chess square name.
private fun getSquareFromOffset(
    offset: Offset,
    squareSizePx: Float,
    orientation: BoardOrientation,
): String? {
    val col = (offset.x / squareSizePx).toInt()
    val row = (offset.y / squareSizePx).toInt()
    if (col !in 0 until CellCount || row !in 0 until CellCount) {
        return null
    }

    val realRow = getRowOrColumn(orientation, row)
    val realCol = getRowOrColumn(orientation, col)
    return "${'a' + realCol}${CellCount - realRow}"
}

@Composable
// TODO: Split this composable into smaller local helpers for animation playback,
// gesture handling, and scene composition once the first TrainSingleLine subset
// is validated end-to-end.
// Owns the TrainSingleLine board surface: queued animation playback plus tap/drag move input.
internal fun TrainSingleLineAnimatedBoard(
    lineController: LineController,
    boardAnimationController: BoardAnimationQueueController,
    interactionEnabled: Boolean,
    wrongMoveSquare: String? = null,
    hintSquare: String? = null,
    modifier: Modifier = Modifier,
) {
    val boardState = lineController.boardState
    val currentFen = lineController.getFen()
    val orientation = lineController.getSide()
    val animationState = boardAnimationController.state
    val currentScene = animationState.currentScene ?: buildBoardRenderScene(
        position = lineController.getBoardPosition(),
        orientation = orientation,
        lastMoveHighlight = lineController.getLastMoveHighlight(),
    )
    val activeAction = animationState.activeAction

    var selectedSquare by remember(orientation) { mutableStateOf<String?>(null) }
    var dragFromSquare by remember(orientation) { mutableStateOf<String?>(null) }
    var dragOffset by remember(orientation) { mutableStateOf(Offset.Zero) }
    var progress by remember(activeAction) { mutableFloatStateOf(0f) }

    LaunchedEffect(interactionEnabled) {
        if (interactionEnabled) {
            return@LaunchedEffect
        }

        selectedSquare = null
        dragFromSquare = null
        dragOffset = Offset.Zero
    }

    LaunchedEffect(activeAction) {
        if (activeAction == null) {
            progress = 0f
            return@LaunchedEffect
        }

        val animationProgress = Animatable(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = activeAction.durationMs),
        ) {
            progress = value
        }
        boardAnimationController.completeActiveAction()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag(InteractiveChessBoardTestTag)
            .semantics { stateDescription = currentFen },
    ) {
        val squareSizePx = constraints.maxWidth / CellCount.toFloat()
        val baseScene = buildBaseScene(
            currentScene = currentScene,
            activeAction = activeAction,
            progress = progress,
            squareSizePx = squareSizePx,
        )
        val sceneToRender = buildSceneToRender(
            baseScene = baseScene,
            selectedSquare = selectedSquare,
            dragFromSquare = dragFromSquare,
            dragOffset = dragOffset,
            wrongMoveSquare = wrongMoveSquare,
            hintSquare = hintSquare,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(squareSizePx, orientation, boardState, interactionEnabled) {
                    if (!interactionEnabled) {
                        return@pointerInput
                    }

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startPos = down.position
                        val touchSlop = viewConfiguration.touchSlop

                        var latestPos = startPos
                        var isDragging = false

                        val startSquare = getSquareFromOffset(startPos, squareSizePx, orientation)
                        val canDrag = startSquare != null && lineController.canSelectSquare(startSquare)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            latestPos = change.position

                            if (!change.pressed) {
                                break
                            }

                            val moved = (latestPos - startPos).getDistance()
                            if (!isDragging && canDrag && moved > touchSlop) {
                                isDragging = true
                                dragFromSquare = startSquare
                                dragOffset = latestPos
                                selectedSquare = null
                                change.consume()
                                continue
                            }

                            if (!isDragging) {
                                continue
                            }

                            dragOffset = latestPos
                            change.consume()
                        }

                        if (isDragging) {
                            val targetSquare = getSquareFromOffset(latestPos, squareSizePx, orientation)
                            if (targetSquare != null && dragFromSquare != null) {
                                lineController.setStartSquare(dragFromSquare)
                                lineController.setDestinationSquareAndTryMove(targetSquare)
                            }
                            dragFromSquare = null
                            dragOffset = Offset.Zero
                            return@awaitEachGesture
                        }

                        if (startSquare == null) {
                            return@awaitEachGesture
                        }

                        if (lineController.getStartSquare() != null) {
                            val moved = lineController.setDestinationSquareAndTryMove(startSquare)
                            if (moved) {
                                selectedSquare = null
                            } else {
                                selectedSquare = if (lineController.setStartSquare(startSquare)) {
                                    startSquare
                                } else {
                                    null
                                }
                            }
                            return@awaitEachGesture
                        }

                        selectedSquare = if (lineController.setStartSquare(startSquare)) {
                            startSquare
                        } else {
                            null
                        }
                    }
                }
        ) {
            BoardSceneRenderer(
                scene = sceneToRender,
                squareSizePx = squareSizePx,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// Builds the currently visible board scene, projecting the active queued move when needed.
private fun buildBaseScene(
    currentScene: BoardRenderScene,
    activeAction: AnimateSimpleMoveAction?,
    progress: Float,
    squareSizePx: Float,
): BoardRenderScene {
    if (activeAction == null) {
        return currentScene
    }

    return buildAnimatedBoardRenderScene(
        baseScene = currentScene,
        activeAction = activeAction,
        progress = progress,
        squareSizePx = squareSizePx,
    )
}

// Overlays transient training-screen UI state on top of the base animated board scene.
private fun buildSceneToRender(
    baseScene: BoardRenderScene,
    selectedSquare: String?,
    dragFromSquare: String?,
    dragOffset: Offset,
    wrongMoveSquare: String?,
    hintSquare: String?,
): BoardRenderScene {
    var resolvedDragFromSquare = baseScene.dragFromSquare
    if (dragFromSquare != null) {
        resolvedDragFromSquare = dragFromSquare
    }

    var resolvedDragOffset = baseScene.dragOffset
    if (dragFromSquare != null) {
        resolvedDragOffset = dragOffset
    }

    return baseScene.copy(
        selectedSquare = selectedSquare,
        dragFromSquare = resolvedDragFromSquare,
        dragOffset = resolvedDragOffset,
        wrongMoveSquare = wrongMoveSquare,
        hintSquare = hintSquare,
    )
}
