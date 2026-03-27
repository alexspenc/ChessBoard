package com.example.chessboard.ui

import android.util.Log
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.chessboard.R
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.ui.theme.ChessDark
import com.example.chessboard.ui.theme.ChessLight
import com.example.chessboard.ui.theme.ChessPieceDark

enum class BoardOrientation { WHITE, BLACK }

private const val CellCount = 8
private const val ChessBoardLogTag = "ChessBoard"

// ──────────────────────────────────────────────────────────────────────────────
// Pure coordinate helpers
// ──────────────────────────────────────────────────────────────────────────────

private fun getColor(row: Int, col: Int): Color {
    val isLight = (row + col) % 2 == 0
    return if (isLight) ChessLight else ChessDark
}

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

private fun squareToBoardCoords(
    square: String,
    orientation: BoardOrientation
): Pair<Int, Int> {
    if (square.length != 2 || square[0] !in 'a'..'h' || square[1] !in '1'..'8')
        return squareToBoardCoords("a1", orientation)

    val file = square[0] - 'a'
    val rank = square[1].digitToInt()
    val row = CellCount - rank
    val col = file
    return if (orientation == BoardOrientation.WHITE) row to col else (7 - row) to (7 - col)
}

// ──────────────────────────────────────────────────────────────────────────────
// Draw helpers
// ──────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawHighlight(
    square: String?,
    orientation: BoardOrientation,
    squareSizePx: Float,
    color: Color
) {
    square ?: return
    val (row, col) = squareToBoardCoords(square, orientation)
    drawRect(
        color = color,
        topLeft = Offset(col * squareSizePx, row * squareSizePx),
        size = Size(squareSizePx, squareSizePx)
    )
}

/** Draws a piece at its normal board square. */
private fun DrawScope.drawFigure(
    letter: Char,
    fieldName: String,
    squareSize: Float,
    orientation: BoardOrientation,
    painters: Map<Char, Painter>
) {
    fun fieldToBoardCoords(field: String): Pair<Int, Int> {
        val col = field[0] - 'a'
        val row = CellCount - (field[1] - '0')
        return row to col
    }

    val (row, col) = fieldToBoardCoords(fieldName)
    val displayRow = if (orientation == BoardOrientation.WHITE) row else 7 - row
    val displayCol = if (orientation == BoardOrientation.WHITE) col else 7 - col

    drawPieceAt(
        letter = letter,
        left = displayCol * squareSize,
        top = displayRow * squareSize,
        squareSize = squareSize,
        painters = painters
    )
}

/** Draws a piece centered on [centerOffset] – used for the piece being dragged. */
private fun DrawScope.drawFigureDragged(
    letter: Char,
    centerOffset: Offset,
    squareSize: Float,
    painters: Map<Char, Painter>
) {
    val pieceSize = squareSize * 0.770f
    drawPieceAt(
        letter = letter,
        left = centerOffset.x - pieceSize / 2,
        top = centerOffset.y - pieceSize / 2,
        squareSize = squareSize,
        painters = painters
    )
}

private fun DrawScope.drawPieceAt(
    letter: Char,
    left: Float,
    top: Float,
    squareSize: Float,
    painters: Map<Char, Painter>
) {
    val lowerLetter = letter.lowercaseChar()
    val painter = painters[lowerLetter]

    if (painter != null) {
        val pieceColor = if (letter.isUpperCase()) Color.White else ChessPieceDark
        val outlineColor = if (letter.isUpperCase()) Color.Black else Color.White
        val pieceSize = squareSize * 0.770f
        val piecePadding = (squareSize - pieceSize) / 2

        translate(left = left + piecePadding, top = top + piecePadding) {
            val strokeWidth = 1.dp.toPx()
            listOf(
                Offset(-strokeWidth, 0f),
                Offset(strokeWidth, 0f),
                Offset(0f, -strokeWidth),
                Offset(0f, strokeWidth)
            ).forEach { o ->
                translate(o.x, o.y) {
                    with(painter) {
                        draw(Size(pieceSize, pieceSize), colorFilter = ColorFilter.tint(outlineColor))
                    }
                }
            }
            with(painter) {
                draw(Size(pieceSize, pieceSize), colorFilter = ColorFilter.tint(pieceColor))
            }
        }
    } else {
        // Fallback: draw letter text
        val squareCenter = squareSize / 2
        drawContext.canvas.nativeCanvas.drawText(
            letter.toString(),
            left + squareCenter,
            top + squareCenter + squareSize / 4,
            Paint().apply {
                textAlign = Paint.Align.CENTER
                textSize = squareSize * 0.6f
                isAntiAlias = true
            }
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// ChessBoard – pure renderer, no gesture logic
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun ChessBoard(
    gameController: GameController,
    boardState: Int,
    squareSizePx: Float,
    selectedSquare: String?,
    dragFromSquare: String?,
    dragOffset: Offset,
    modifier: Modifier = Modifier
) {
    val orientation = gameController.getSide()

    SideEffect {
        Log.d(
            ChessBoardLogTag,
                "draw controller=${System.identityHashCode(gameController)} " +
                "boardState=$boardState " +
                "moveIndex=${gameController.currentMoveIndex} " +
                "fen=${gameController.getFen()}"
        )
    }

    val painters = mapOf(
        'r' to painterResource(R.drawable.ic_rook),
        'p' to painterResource(R.drawable.ic_pawn),
        'n' to painterResource(R.drawable.ic_knight),
        'b' to painterResource(R.drawable.ic_bishop),
        'k' to painterResource(R.drawable.ic_king),
        'q' to painterResource(R.drawable.ic_queen),
    )

    Canvas(modifier = modifier.aspectRatio(1f)) {
        // 1. Board squares
        for (row in 0 until CellCount) {
            for (col in 0 until CellCount) {
                drawRect(
                    color = getColor(row, col),
                    topLeft = Offset(col * squareSizePx, row * squareSizePx),
                    size = Size(squareSizePx, squareSizePx)
                )
            }
        }

        // 2. Coordinate labels
        val ranks: IntProgression
        val files: List<String>
        if (orientation == BoardOrientation.WHITE) {
            ranks = CellCount downTo 1
            files = listOf("a", "b", "c", "d", "e", "f", "g", "h")
        } else {
            ranks = 1..CellCount
            files = listOf("h", "g", "f", "e", "d", "c", "b", "a")
        }
        for (i in ranks) {
            val yPos = if (orientation == BoardOrientation.WHITE)
                ((CellCount - i) * squareSizePx) + squareSizePx * 0.3f
            else
                (i * squareSizePx) + squareSizePx * 0.3f
            drawContext.canvas.nativeCanvas.drawText(
                i.toString(),
                squareSizePx * 0.1f,
                yPos,
                Paint().apply {
                    textAlign = Paint.Align.LEFT
                    textSize = squareSizePx * 0.25f
                    isAntiAlias = true
                    alpha = 100
                }
            )
        }
        for ((index, file) in files.withIndex()) {
            drawContext.canvas.nativeCanvas.drawText(
                file,
                (index * squareSizePx) + squareSizePx * 0.8f,
                (CellCount * squareSizePx) - squareSizePx * 0.2f,
                Paint().apply {
                    textAlign = Paint.Align.CENTER
                    textSize = squareSizePx * 0.25f
                    isAntiAlias = true
                    alpha = 100
                }
            )
        }

        // 3. Highlights
        val highlightColor = Color.Yellow.copy(alpha = 0.4f)
        drawHighlight(selectedSquare, orientation, squareSizePx, highlightColor)
        drawHighlight(dragFromSquare, orientation, squareSizePx, highlightColor)

        // 4. Pieces — skip the one being dragged (drawn separately on top)
        val position = gameController.getBoardPosition()
        position.pieces.forEach { piece ->
            if (piece.field == dragFromSquare) return@forEach
            drawFigure(piece.letter, piece.field, squareSizePx, orientation, painters)
        }

        // 5. Dragged piece on top, centered on finger
        if (dragFromSquare != null) {
            val draggedPiece = position.pieces.find { it.field == dragFromSquare }
            draggedPiece?.let {
                drawFigureDragged(it.letter, dragOffset, squareSizePx, painters)
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// ChessBoardWithCoordinates – state + gesture owner
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun ChessBoardWithCoordinates(
    gameController: GameController,
    modifier: Modifier = Modifier,
) {
    // Observe board changes so this composable (and ChessBoard) redraws on moves
    @Suppress("UNUSED_VARIABLE")
    val boardState = gameController.boardState

    LaunchedEffect(boardState) {
        Log.d(
            ChessBoardLogTag,
            "boardState changed controller=${System.identityHashCode(gameController)} " +
                "boardState=$boardState " +
                "moveIndex=${gameController.currentMoveIndex} " +
                "fen=${gameController.getFen()}"
        )
    }

    val orientation = gameController.getSide()
    val squareSizeDp = minScreenSizeDp(0.8f)
    val squareSizePx = minScreenSizePx(0.8f) / CellCount

    // Tap selection state
    var selectedSquare by remember(orientation) { mutableStateOf<String?>(null) }

    // Drag state
    var dragFromSquare by remember(orientation) { mutableStateOf<String?>(null) }
    var dragOffset by remember(orientation) { mutableStateOf(Offset.Zero) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(squareSizeDp.dp)
                .pointerInput(squareSizePx, orientation) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startPos = down.position
                        val touchSlop = viewConfiguration.touchSlop

                        var latestPos = startPos
                        var isDragging = false

                        // Determine what square was touched
                        val startSquare = getSquareFromOffset(startPos, squareSizePx, orientation)
                        val canDrag = startSquare != null && gameController.canSelectSquare(startSquare)

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
                                gameController.setStartSquare(dragFromSquare)
                                gameController.setDestinationSquareAndTryMove(targetSquare)
                            }
                            dragFromSquare = null
                        } else {
                            // Tap: two-tap select-then-move flow
                            if (startSquare == null) return@awaitEachGesture

                            if (gameController.getStartSquare() != null) {
                                // A piece is already selected — try moving to tapped square
                                val moved = gameController.setDestinationSquareAndTryMove(startSquare)
                                if (moved) {
                                    selectedSquare = null
                                } else {
                                    // Not a valid destination — try selecting a new piece instead
                                    selectedSquare = if (gameController.setStartSquare(startSquare)) startSquare else null
                                }
                            } else {
                                // Nothing selected yet — try selecting the tapped piece
                                selectedSquare = if (gameController.setStartSquare(startSquare)) startSquare else null
                            }
                        }
                    }
                }
        ) {
            ChessBoard(
                gameController = gameController,
                boardState = boardState,
                squareSizePx = squareSizePx,
                selectedSquare = selectedSquare,
                dragFromSquare = dragFromSquare,
                dragOffset = dragOffset,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Utilities
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun dpToPixel(pixels: Float): Float {
    val density = LocalDensity.current
    return with(density) { pixels.dp.toPx() }
}

@Composable
private fun minScreenSizeDp(k: Float): Float {
    val configuration = LocalConfiguration.current
    return minOf(configuration.screenWidthDp, configuration.screenHeightDp) * k
}

@Composable
private fun minScreenSizePx(k: Float): Float = dpToPixel(minScreenSizeDp(k))
