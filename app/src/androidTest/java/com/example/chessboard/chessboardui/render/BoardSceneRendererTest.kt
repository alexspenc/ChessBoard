package com.example.chessboard.chessboardui.render

/**
 * Checks complete board assembly and observable layer ordering through the public drawing entry point.
 * Keep composition image checks here; do not duplicate component algorithms or add app adapters and gestures.
 * Validation date: 2026-10-13.
 */

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.chessboard.chesscore.model.Piece
import com.example.chessboard.chesscore.model.PieceType
import com.example.chessboard.chesscore.model.Side
import com.example.chessboard.chesscore.model.Square
import com.example.chessboard.chessboardui.model.BoardOrientation
import com.example.chessboard.chessboardui.model.BoardRenderAnimatedPiece
import com.example.chessboard.chessboardui.model.BoardRenderPiece
import com.example.chessboard.chessboardui.model.BoardRenderScene
import com.example.chessboard.chessboardui.model.BoardRenderStyle
import com.example.chessboard.chessboardui.model.BoardSquareDecoration
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

private const val TestSquareSizePx = 100f
private const val TestBoardSizePx = 8 * TestSquareSizePx

private val TestStyle = BoardRenderStyle(
    lightSquareColor = Color.White,
    darkSquareColor = Color.Gray,
    whitePieceColor = Color.Red,
    blackPieceColor = Color.Blue,
    whitePieceOutlineColor = Color.Green,
    blackPieceOutlineColor = Color.Yellow,
    coordinateColor = Color.Magenta,
)

@RunWith(AndroidJUnit4::class)
class BoardSceneRendererTest {

    /** Empty placements still show all squares and labels, with the orientation's corner texts. */
    @Test
    fun drawsBoardAndLabelsForEmptySceneInBothOrientations() {
        data class ExpectedCorners(val orientation: BoardOrientation, val topRank: String, val lastFile: String)

        val cases = listOf(
            ExpectedCorners(BoardOrientation.WHITE, topRank = "8", lastFile = "h"),
            ExpectedCorners(BoardOrientation.BLACK, topRank = "1", lastFile = "a"),
        )
        for (expected in cases) {
            val scene = BoardRenderScene(emptyList(), expected.orientation)
            val bitmap = createBitmap(TestBoardSizePx.toInt())
            try {
                drawSceneToBitmap(bitmap, scene, TestSquareSizePx)

                assertBoardCenters(bitmap)
                assertAllLabelsVisible(bitmap)
                assertCornerLabel(bitmap, expected.topRank, column = 0, row = 0, alignment = Paint.Align.LEFT)
                assertCornerLabel(bitmap, expected.lastFile, column = 7, row = 7, alignment = Paint.Align.CENTER)
            } finally {
                bitmap.recycle()
            }
        }
    }

    /** Opaque corner decorations must erase the rank and file labels underneath them. */
    @Test
    fun decorationsCoverCoordinateLabels() {
        for (orientation in BoardOrientation.entries) {
            val scene = BoardRenderScene(
                pieces = emptyList(),
                orientation = orientation,
                decorations = listOf(
                    BoardSquareDecoration(Square('a', 8), Color.Cyan),
                    BoardSquareDecoration(Square('h', 1), Color.Cyan),
                ),
            )
            val bitmap = createBitmap(TestBoardSizePx.toInt())
            try {
                drawSceneToBitmap(bitmap, scene, TestSquareSizePx)

                assertSolidCell(bitmap, column = 0, row = 0, color = Color.Cyan)
                assertSolidCell(bitmap, column = 7, row = 7, color = Color.Cyan)
            } finally {
                bitmap.recycle()
            }
        }
    }

    /** The figure's fill must remain visible inside an opaque decoration of its occupied square. */
    @Test
    fun piecesRemainVisibleAboveDecorationFills() {
        val square = Square('e', 4)
        val scene = BoardRenderScene(
            pieces = listOf(BoardRenderPiece(Piece(Side.WHITE, PieceType.KING), square)),
            orientation = BoardOrientation.WHITE,
            decorations = listOf(BoardSquareDecoration(square, Color.Cyan)),
        )
        val bitmap = createBitmap(TestBoardSizePx.toInt())
        try {
            drawSceneToBitmap(bitmap, scene, TestSquareSizePx)

            val left = (4 * TestSquareSizePx).toInt()
            val top = (4 * TestSquareSizePx).toInt()
            val right = (5 * TestSquareSizePx).toInt()
            val bottom = (5 * TestSquareSizePx).toInt()
            assertColorVisibleInArea(bitmap, left, top, right, bottom, TestStyle.whitePieceColor)
        } finally {
            bitmap.recycle()
        }
    }

    /** The public entry point must preserve orientation and supplied moving coordinates without source copies. */
    @Test
    fun rendersStationaryAnimatedAndDraggedFiguresTogether() {
        val king = BoardRenderPiece(Piece(Side.WHITE, PieceType.KING), Square('e', 1))
        val rook = BoardRenderPiece(Piece(Side.BLACK, PieceType.ROOK), Square('h', 1))
        val knight = BoardRenderPiece(Piece(Side.WHITE, PieceType.KNIGHT), Square('b', 1))
        val scene = BoardRenderScene(
            pieces = listOf(knight, rook, king),
            orientation = BoardOrientation.BLACK,
            dragFromSquare = knight.square,
            dragOffset = Offset((5 + 0.25f) * TestSquareSizePx, (3 + 0.75f) * TestSquareSizePx),
            animatedPieces = listOf(
                BoardRenderAnimatedPiece(
                    rook.square,
                    Offset((2 + 0.25f) * TestSquareSizePx, (2 + 0.75f) * TestSquareSizePx),
                ),
            ),
        )
        val bitmap = createBitmap(TestBoardSizePx.toInt())
        try {
            drawSceneToBitmap(bitmap, scene, TestSquareSizePx)

            assertPieceFillInCell(bitmap, column = 3, row = 0, color = TestStyle.whitePieceColor)
            assertPieceFillInCell(bitmap, column = 2, row = 2, color = TestStyle.blackPieceColor)
            assertPieceFillInCell(bitmap, column = 5, row = 3, color = TestStyle.whitePieceColor)
            assertNoPieceFillInCell(bitmap, column = 0, row = 0, color = TestStyle.blackPieceColor)
            assertNoPieceFillInCell(bitmap, column = 6, row = 0, color = TestStyle.whitePieceColor)
        } finally {
            bitmap.recycle()
        }
    }

    /** Invalid sizes must fail before the background or any later layer changes the target. */
    @Test
    fun rejectsInvalidSizesBeforeDrawingEvenForEmptyScene() {
        val scenes = listOf(
            BoardRenderScene(emptyList(), BoardOrientation.WHITE),
            BoardRenderScene(
                listOf(BoardRenderPiece(Piece(Side.WHITE, PieceType.KING), Square('a', 8))),
                BoardOrientation.BLACK,
            ),
        )
        val invalidSizes = listOf(
            0f, -0f, -1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
        )
        for (scene in scenes) {
            for (squareSizePx in invalidSizes) {
                assertRejectedSize(scene, squareSizePx)
            }
        }
    }

    private fun assertBoardCenters(bitmap: Bitmap) {
        for (row in 0 until 8) {
            for (column in 0 until 8) {
                var expected = TestStyle.lightSquareColor
                if ((row + column) % 2 != 0) {
                    expected = TestStyle.darkSquareColor
                }
                val x = ((column + 0.5f) * TestSquareSizePx).toInt()
                val y = ((row + 0.5f) * TestSquareSizePx).toInt()
                assertEquals("Square at column=$column, row=$row", expected.toArgb(), bitmap.getPixel(x, y))
            }
        }
    }

    private fun assertAllLabelsVisible(bitmap: Bitmap) {
        val boardEdge = TestBoardSizePx.toInt()
        for (index in 0 until 8) {
            val cellStart = (index * TestSquareSizePx).toInt()
            val cellEnd = ((index + 1) * TestSquareSizePx).toInt()
            assertColorVisibleInArea(
                bitmap,
                left = 0,
                top = cellStart,
                right = (TestSquareSizePx / 2).toInt(),
                bottom = cellStart + (TestSquareSizePx * 0.4f).toInt(),
                color = TestStyle.coordinateColor,
            )
            assertColorVisibleInArea(
                bitmap,
                left = cellStart + (TestSquareSizePx / 2).toInt(),
                top = boardEdge - (TestSquareSizePx * 0.4f).toInt(),
                right = cellEnd,
                bottom = boardEdge,
                color = TestStyle.coordinateColor,
            )
        }
    }

    private fun assertCornerLabel(bitmap: Bitmap, text: String, column: Int, row: Int, alignment: Paint.Align) {
        val cellSizePx = TestSquareSizePx.toInt()
        val expected = createBitmap(cellSizePx)
        expected.eraseColor(TestStyle.lightSquareColor.toArgb())
        try {
            val paint = Paint()
            paint.color = TestStyle.coordinateColor.toArgb()
            paint.textAlign = alignment
            paint.textSize = TestSquareSizePx / 4
            paint.isAntiAlias = true
            var x = TestSquareSizePx * 0.1f
            var baseline = TestSquareSizePx * 0.3f
            if (alignment == Paint.Align.CENTER) {
                x = TestSquareSizePx * 0.8f
                baseline = TestSquareSizePx - TestSquareSizePx * 0.2f
            }
            android.graphics.Canvas(expected).drawText(text, x, baseline, paint)

            val actualPixels = IntArray(cellSizePx * cellSizePx)
            bitmap.getPixels(
                actualPixels, 0, cellSizePx,
                (column * TestSquareSizePx).toInt(), (row * TestSquareSizePx).toInt(),
                cellSizePx, cellSizePx,
            )
            val expectedPixels = IntArray(actualPixels.size)
            expected.getPixels(expectedPixels, 0, cellSizePx, 0, 0, cellSizePx, cellSizePx)
            assertArrayEquals("Corner label $text", expectedPixels, actualPixels)
        } finally {
            expected.recycle()
        }
    }

    private fun assertSolidCell(bitmap: Bitmap, column: Int, row: Int, color: Color) {
        val left = (column * TestSquareSizePx).toInt()
        val top = (row * TestSquareSizePx).toInt()
        val right = ((column + 1) * TestSquareSizePx).toInt()
        val bottom = ((row + 1) * TestSquareSizePx).toInt()
        for (y in top until bottom) {
            for (x in left until right) {
                if (bitmap.getPixel(x, y) != color.toArgb()) {
                    fail("Decoration must cover pixel ($x, $y)")
                }
            }
        }
    }

    private fun assertPieceFillInCell(bitmap: Bitmap, column: Int, row: Int, color: Color) {
        assertColorVisibleInArea(
            bitmap,
            (column * TestSquareSizePx).toInt(),
            (row * TestSquareSizePx).toInt(),
            ((column + 1) * TestSquareSizePx).toInt(),
            ((row + 1) * TestSquareSizePx).toInt(),
            color,
        )
    }

    private fun assertNoPieceFillInCell(bitmap: Bitmap, column: Int, row: Int, color: Color) {
        val left = (column * TestSquareSizePx).toInt()
        val top = (row * TestSquareSizePx).toInt()
        val right = ((column + 1) * TestSquareSizePx).toInt()
        val bottom = ((row + 1) * TestSquareSizePx).toInt()
        for (y in top until bottom) {
            for (x in left until right) {
                if (bitmap.getPixel(x, y) == color.toArgb()) {
                    fail("Source-square piece copy at ($x, $y)")
                }
            }
        }
    }

    private fun assertColorVisibleInArea(bitmap: Bitmap, left: Int, top: Int, right: Int, bottom: Int, color: Color) {
        val expected = color.toArgb()
        for (y in top until bottom) {
            for (x in left until right) {
                if (bitmap.getPixel(x, y) == expected) {
                    return
                }
            }
        }
        fail("No expected color in rectangle ($left, $top)-($right, $bottom)")
    }

    private fun assertRejectedSize(scene: BoardRenderScene, squareSizePx: Float) {
        val bitmap = createBitmap(1)
        val background = bitmap.getPixel(0, 0)
        try {
            assertThrows(IllegalArgumentException::class.java) {
                drawSceneToBitmap(bitmap, scene, squareSizePx)
            }
            assertEquals(background, bitmap.getPixel(0, 0))
        } finally {
            bitmap.recycle()
        }
    }

    private fun createBitmap(sizePx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color(0xFF202020).toArgb())
        return bitmap
    }

    private fun drawSceneToBitmap(bitmap: Bitmap, scene: BoardRenderScene, squareSizePx: Float) {
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = Canvas(bitmap.asImageBitmap()),
            size = Size(bitmap.width.toFloat(), bitmap.height.toFloat()),
        ) {
            drawBoardScene(drawScope, scene, squareSizePx, TestStyle)
        }
    }
}
