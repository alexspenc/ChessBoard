package com.example.chessboard.ui.boardanimation

/**
 * Shared timed interactive chess-board host.
 * Keep gesture ownership, playback-scene rendering, and transient board overlays here.
 * Do not add screen flow orchestration, persistence logic, or unrelated app-wide UI abstractions.
 * Validation date: 2026-07-26
 */

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.chessboard.ui.boardrender.BoardRenderScene
import com.example.chessboard.ui.boardrender.BoardSceneRenderer
import com.example.chessboard.ui.boardrender.buildBoardRenderScene

private const val CellCount = 8

// Mirrors one board axis when the board is shown from Black's side.
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
// gesture handling, and scene composition after the shared interactive host is
// validated by more than one screen.
// TODO: Replace the training-specific wrongMoveSquare and hintSquare parameters
// with a generic List<BoardSquareDecoration>. TrainSingleLine should map its
// wrong-move and hint state to decoration styles before calling this shared host,
// so the boardanimation package receives only visual instructions and does not
// expose training concepts in its API.
// Owns the shared board surface: queued animation playback plus tap/drag move input.
internal fun AnimatedInteractiveChessBoard(
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
    val fallbackScene = buildBoardRenderScene(
        position = lineController.getBoardPosition(),
        orientation = orientation,
        lastMoveHighlight = lineController.getLastMoveHighlight(),
    )

    var selectedSquare by remember(orientation) { mutableStateOf<String?>(null) }
    var dragFromSquare by remember(orientation) { mutableStateOf<String?>(null) }
    var dragOffset by remember(orientation) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(interactionEnabled) {
        if (interactionEnabled) {
            return@LaunchedEffect
        }

        selectedSquare = null
        dragFromSquare = null
        dragOffset = Offset.Zero
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag(InteractiveChessBoardTestTag)
            .semantics { stateDescription = currentFen },
    ) {
        val squareSizePx = constraints.maxWidth / CellCount.toFloat()
        val baseScene = rememberBoardPlaybackScene(
            controller = boardAnimationController,
            squareSizePx = squareSizePx,
            fallbackScene = fallbackScene,
        ) ?: return@BoxWithConstraints
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

// Overlays transient interaction state on top of the base animated board scene.
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
