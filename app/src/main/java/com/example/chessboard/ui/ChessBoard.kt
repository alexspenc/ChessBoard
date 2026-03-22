package com.example.chessboard.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectTapGestures

import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

import com.example.chessboard.R
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.ui.theme.ChessDark
import com.example.chessboard.ui.theme.ChessLight

enum class BoardOrientation {
    WHITE,
    BLACK
}

private const val CellCount = 8

private fun getColor(row : Int, col : Int) : Color {
    val isLight = (row + col) % 2 == 0
    return if (isLight) ChessLight else ChessDark
}

private fun getRowOrColumn(orientation: BoardOrientation, rowCol : Int ) : Int {
    if (orientation == BoardOrientation.WHITE) return rowCol
    return 7 - rowCol
}

private fun getSquareFromOffset(
    offset: Offset,
    squareSizePx: Float,
    orientation: BoardOrientation
): String {
    val col = (offset.x / squareSizePx).toInt()
    val row = (offset.y / squareSizePx).toInt()

    val realRow = getRowOrColumn(orientation, row)
    val realCol = getRowOrColumn(orientation, col)

    val file = 'a' + realCol
    val rank = 8 - realRow

    return "$file$rank"
}

private fun squareToBoardCoords(
    square: String,
    orientation: BoardOrientation
): Pair<Int, Int> {
    // проверка входных данных
    if (
        square.length != 2 ||
        square[0] !in 'a'..'h' ||
        square[1] !in '1'..'8'
    ) {
        return squareToBoardCoords("a1", orientation)
    }

    val file = square[0] - 'a'
    val rank = square[1].digitToInt()

    val row = CellCount - rank
    val col = file

    if (orientation == BoardOrientation.WHITE) {
        return Pair(row, col)
    }
    return Pair(7 - row, 7 - col)
}

private fun DrawScope.drawSquare(
    selectedSquare: String?,
    orientation: BoardOrientation,
    squareSizePx: Float,
    color : Color
) {
    selectedSquare?.let {
        val (row, col) = squareToBoardCoords(it, orientation)

        drawRect(
            color,
            topLeft = Offset(col * squareSizePx, row * squareSizePx),
            size = Size(squareSizePx, squareSizePx)
        )
    }
}

private fun DrawScope.drawStartSqueare(
    selectedSquare: String?,
    orientation: BoardOrientation,
    squareSizePx: Float,
) {
    val color = Color.Yellow.copy(alpha = 0.4f)
    drawSquare(selectedSquare, orientation, squareSizePx, color)
}

private fun DrawScope.SelectStartSquareOrDoMove(
    gameController: GameController,
    selectedSquare: String?,
    squareSizePx : Float
) {
    val orientation = gameController.getOrientation()

    if (gameController.getStartSquare() != null) {
        if (gameController.setDestinationSquareAndTryMove(selectedSquare)) {
            return
        }
    }
    if (gameController.setStartSquare(selectedSquare)) {
        drawStartSqueare(selectedSquare, orientation, squareSizePx)
    }
}


@Composable
private fun dpToPixel(pixels : Float): Float {
    val density = LocalDensity.current
    return with(density) { pixels.dp.toPx() }
}

@Composable
private fun minScreenSizeDp(k : Float): Float {
    val configuration = LocalConfiguration.current

    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp

    return minOf(screenWidthDp, screenHeightDp) * k
}
@Composable
private fun minScreenSizePx(k: Float): Float {
    val tmp = minScreenSizeDp(k)
    return dpToPixel(tmp)
}
@Composable
fun ChessBoard(
    gameController: GameController,
    squareSizePx : Float,
    selectedSquare: String?,
    onSquareClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val orientation = gameController.getOrientation()
    val rookPainter = painterResource(id = R.drawable.ic_rook)
    val pawnPainter = painterResource(id = R.drawable.ic_pawn)
    val knightPainter = painterResource(id = R.drawable.ic_knight)
    val bishopPainter = painterResource(id = R.drawable.ic_bishop)
    val kingPainter = painterResource(id = R.drawable.ic_king)
    val queenPainter = painterResource(id = R.drawable.ic_queen)

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val square = getSquareFromOffset(offset, squareSizePx, orientation)
                    onSquareClick(square)
                }
            }
    ) {
        for (row in 0 until CellCount) {
            for (col in 0 until CellCount) {
                drawRect(
                    color = getColor(row, col),
                    topLeft = Offset(col * squareSizePx, row * squareSizePx),
                    size = Size(squareSizePx, squareSizePx)
                )
            }
        }

        // Draw row and column labels
        var ranks: IntProgression = 1..CellCount
        var files = listOf("h", "g", "f", "e", "d", "c", "b", "a")
        if (orientation == BoardOrientation.WHITE) {
            ranks = CellCount downTo 1
            files = listOf("a", "b", "c", "d", "e", "f", "g", "h")
        }

        // Draw rank numbers on the left side (vertical, top-left corner)
        for (i in ranks) {
            val yPos = if (orientation == BoardOrientation.WHITE) {
                ((CellCount - i) * squareSizePx) + squareSizePx * 0.3f
            } else {
                (i * squareSizePx) + squareSizePx * 0.3f
            }
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

        // Draw file letters on the bottom right (horizontal)
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

        SelectStartSquareOrDoMove(gameController, selectedSquare, squareSizePx)

        val position = gameController.getBoardPosition()
        position.pieces.forEach { piece ->
            drawFigure(
                piece.letter,
                piece.field,
                squareSizePx,
                orientation,
                mapOf(
                    'r' to rookPainter,
                    'p' to pawnPainter,
                    'n' to knightPainter,
                    'b' to bishopPainter,
                    'k' to kingPainter,
                    'q' to queenPainter
                )
            )
        }
    }
}

@Composable
fun ChessBoardWithCoordinates(
    gameController: GameController,
    modifier: Modifier = Modifier,
) {
    // Observe board state changes to trigger recomposition
    val boardState = gameController.boardState

    val orientation = gameController.getOrientation()
    val squareSizeDp = minScreenSizeDp(0.8f)
    val squareSizePx = minScreenSizePx(0.8f) / CellCount

    var selectedSquare by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        key(boardState) {
            ChessBoard(
                gameController,
                squareSizePx,
                selectedSquare = selectedSquare,
                onSquareClick = { square ->
                    println("Clicked: $square")
                    selectedSquare = square
                },
                modifier = Modifier.size(squareSizeDp.dp) // фиксированный размер
            )
        }
    }
}

private fun DrawScope.drawFigure(
    letter: Char,
    fieldName: String,
    squareSize: Float,
    orientation: BoardOrientation,
    painters: Map<Char, Painter>
) {
    fun fieldToBoardCoords(field: String): Pair<Int, Int> {
        val file = field[0]          // 'a'..'h'
        val rank = field[1]          // '1'..'8'

        val col = file - 'a'
        val row = CellCount - (rank - '0')

        return row to col
    }

    val (row, col) = fieldToBoardCoords(fieldName)

    val displayRow = if (orientation == BoardOrientation.WHITE) row else 7 - row
    val displayCol = if (orientation == BoardOrientation.WHITE) col else 7 - col

    val lowerLetter = letter.lowercaseChar()
    val painter = painters[lowerLetter]

    if (painter != null) {
        val pieceColor = if (letter.isUpperCase()) Color.White else Color(0xFF312E2B)
        val outlineColor = if (letter.isUpperCase()) Color.Black else Color.White
        val pieceSize = squareSize * 0.770f
        val piecePadding = (squareSize - pieceSize) / 2

        translate(left = displayCol * squareSize + piecePadding, top = displayRow * squareSize + piecePadding) {
            // Draw outline (slight shadow/border effect)
            val strokeWidth = 1.dp.toPx()
            val offsets = listOf(
                Offset(-strokeWidth, 0f),
                Offset(strokeWidth, 0f),
                Offset(0f, -strokeWidth),
                Offset(0f, strokeWidth)
            )

            offsets.forEach { offset ->
                translate(offset.x, offset.y) {
                    with(painter) {
                        draw(
                            size = Size(pieceSize, pieceSize),
                            colorFilter = ColorFilter.tint(outlineColor)
                        )
                    }
                }
            }

            // Draw main body
            with(painter) {
                draw(
                    size = Size(pieceSize, pieceSize),
                    colorFilter = ColorFilter.tint(pieceColor)
                )
            }
        }
    } else {
        val x = displayCol * squareSize + squareSize / 2
        val y = displayRow * squareSize + squareSize / 2

        drawContext.canvas.nativeCanvas.drawText(
            letter.toString(),
            x,
            y + squareSize / 4,
            Paint().apply {
                textAlign = Paint.Align.CENTER
                textSize = squareSize * 0.6f
                isAntiAlias = true
            }
        )
    }
}