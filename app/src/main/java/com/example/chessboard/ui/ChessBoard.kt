package com.example.chessboard.ui

import android.util.Log
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.ui.boardrender.BoardSceneRenderer
import com.example.chessboard.ui.boardrender.buildBoardRenderScene

enum class BoardOrientation { WHITE, BLACK }

private const val CellCount = 8
private const val ChessBoardLogTag = "ChessBoard"

// ──────────────────────────────────────────────────────────────────────────────
// Pure coordinate helpers
// ──────────────────────────────────────────────────────────────────────────────

private fun getRowOrColumn(orientation: BoardOrientation, rowCol: Int): Int =
    if (orientation == BoardOrientation.WHITE) rowCol else 7 - rowCol

/** Converts a canvas offset to a chess square name (e.g. "e4"). Returns null when off-board. */
private fun getSquareFromOffset(
    offset: Offset,
    squareSizePx: Float,
    orientation: BoardOrientation
): String? {
    val col = (offset.x / squareSizePx).toInt()
    val row = (offset.y / squareSizePx).toInt()
    if (col !in 0..7 || row !in 0..7) return null

    val realRow = getRowOrColumn(orientation, row)
    val realCol = getRowOrColumn(orientation, col)
    return "${'a' + realCol}${8 - realRow}"
}

// ──────────────────────────────────────────────────────────────────────────────
// ChessBoard – pure renderer, no gesture logic
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun ChessBoard(
    lineController: LineController,
    boardState: Int,
    squareSizePx: Float,
    selectedSquare: String?,
    dragFromSquare: String?,
    dragOffset: Offset,
    wrongMoveSquare: String? = null,
    hintSquare: String? = null,
    modifier: Modifier = Modifier
) {
    val orientation = lineController.getSide()
    val lastMoveHighlight = lineController.getLastMoveHighlight()

    SideEffect {
        Log.d(
            ChessBoardLogTag,
                "draw controller=${System.identityHashCode(lineController)} " +
                "boardState=$boardState " +
                "moveIndex=${lineController.currentMoveIndex} " +
                "fen=${lineController.getFen()}"
        )
    }

    val scene = buildBoardRenderScene(
        position = lineController.getBoardPosition(),
        orientation = orientation,
        lastMoveHighlight = lastMoveHighlight,
        selectedSquare = selectedSquare,
        dragFromSquare = dragFromSquare,
        dragOffset = dragOffset,
        wrongMoveSquare = wrongMoveSquare,
        hintSquare = hintSquare,
    )

    BoardSceneRenderer(
        scene = scene,
        squareSizePx = squareSizePx,
        modifier = modifier,
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// ChessBoardWithCoordinates – state + gesture owner
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun PositionSearchBoardWithCoordinates(
    lineController: LineController,
    onSquareClick: (String) -> Unit,
    onPieceMove: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val boardState = lineController.boardState
    val currentFen = lineController.getFen()
    val orientation = lineController.getSide()
    var dragFromSquare by remember(orientation) { mutableStateOf<String?>(null) }
    var dragOffset by remember(orientation) { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val squareSizePx = constraints.maxWidth / CellCount.toFloat()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(InteractiveChessBoardTestTag)
                .semantics { stateDescription = currentFen }
                .pointerInput(squareSizePx, orientation, boardState) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startSquare = getSquareFromOffset(
                            down.position,
                            squareSizePx,
                            orientation
                        ) ?: return@awaitEachGesture
                        val touchSlop = viewConfiguration.touchSlop
                        val startPosition = down.position
                        var latestPosition = startPosition
                        var isDragging = false
                        val hasPieceOnStartSquare = lineController.getBoardPosition().pieces.any { piece ->
                            piece.field == startSquare
                        }

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            latestPosition = change.position

                            if (!change.pressed) {
                                break
                            }

                            val movedDistance = (latestPosition - startPosition).getDistance()
                            if (!isDragging && hasPieceOnStartSquare && movedDistance > touchSlop) {
                                isDragging = true
                                dragFromSquare = startSquare
                                dragOffset = latestPosition
                                change.consume()
                                continue
                            }

                            if (!isDragging) {
                                continue
                            }

                            dragOffset = latestPosition
                            change.consume()
                        }

                        if (isDragging) {
                            val targetSquare = getSquareFromOffset(
                                latestPosition,
                                squareSizePx,
                                orientation
                            )
                            val sourceSquare = dragFromSquare

                            if (targetSquare != null && sourceSquare != null) {
                                onPieceMove(sourceSquare, targetSquare)
                            }

                            dragFromSquare = null
                            dragOffset = Offset.Zero
                            return@awaitEachGesture
                        }

                        onSquareClick(startSquare)
                    }
                }
        ) {
            ChessBoard(
                lineController = lineController,
                boardState = boardState,
                squareSizePx = squareSizePx,
                selectedSquare = null,
                dragFromSquare = dragFromSquare,
                dragOffset = dragOffset,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun ChessBoardWithCoordinates(
    lineController: LineController,
    wrongMoveSquare: String? = null,
    hintSquare: String? = null,
    modifier: Modifier = Modifier,
) {
    val boardState = lineController.boardState
    val currentFen = lineController.getFen()

    LaunchedEffect(boardState) {
        Log.d(
            ChessBoardLogTag,
            "boardState changed controller=${System.identityHashCode(lineController)} " +
                "boardState=$boardState " +
                "moveIndex=${lineController.currentMoveIndex} " +
                "fen=${lineController.getFen()}"
        )
    }

    val orientation = lineController.getSide()

    // Tap selection state
    var selectedSquare by remember(orientation) { mutableStateOf<String?>(null) }

    // Drag state
    var dragFromSquare by remember(orientation) { mutableStateOf<String?>(null) }
    var dragOffset by remember(orientation) { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag(InteractiveChessBoardTestTag)
            .semantics { stateDescription = currentFen },
        contentAlignment = Alignment.Center
    ) {
        val squareSizePx = constraints.maxWidth / CellCount.toFloat()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(squareSizePx, orientation, boardState) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startPos = down.position
                        val touchSlop = viewConfiguration.touchSlop

                        var latestPos = startPos
                        var isDragging = false

                        // Determine what square was touched
                        val startSquare = getSquareFromOffset(startPos, squareSizePx, orientation)
                        val canDrag = startSquare != null && lineController.canSelectSquare(startSquare)

                        // Track pointer until release
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            latestPos = change.position

                            if (!change.pressed) break  // finger lifted

                            val moved = (latestPos - startPos).getDistance()

                            if (!isDragging && canDrag && moved > touchSlop) {
                                // Transition to drag mode
                                isDragging = true
                                dragFromSquare = startSquare
                                dragOffset = latestPos
                                selectedSquare = null
                                change.consume()
                            } else if (isDragging) {
                                dragOffset = latestPos
                                change.consume()
                            }
                        }

                        // ── Finger lifted ──
                        if (isDragging) {
                            // Validate target is on the board, then attempt move
                            val targetSquare = getSquareFromOffset(latestPos, squareSizePx, orientation)
                            if (targetSquare != null && dragFromSquare != null) {
                                lineController.setStartSquare(dragFromSquare)
                                lineController.setDestinationSquareAndTryMove(targetSquare)
                            }
                            dragFromSquare = null
                        } else {
                            // Tap: two-tap select-then-move flow
                            if (startSquare == null) return@awaitEachGesture

                            if (lineController.getStartSquare() != null) {
                                // A piece is already selected — try moving to tapped square
                                val moved = lineController.setDestinationSquareAndTryMove(startSquare)
                                if (moved) {
                                    selectedSquare = null
                                } else {
                                    // Not a valid destination — try selecting a new piece instead
                                    selectedSquare = if (lineController.setStartSquare(startSquare)) startSquare else null
                                }
                            } else {
                                // Nothing selected yet — try selecting the tapped piece
                                selectedSquare = if (lineController.setStartSquare(startSquare)) startSquare else null
                            }
                        }
                    }
                }
        ) {
            ChessBoard(
                lineController = lineController,
                boardState = boardState,
                squareSizePx = squareSizePx,
                selectedSquare = selectedSquare,
                dragFromSquare = dragFromSquare,
                dragOffset = dragOffset,
                wrongMoveSquare = wrongMoveSquare,
                hintSquare = hintSquare,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
