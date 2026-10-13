package com.example.chessboard.chessboardui.render

/**
 * Checks square-decoration placement, compositing order, and border widths in Android bitmaps.
 * Keep decoration rendering tests here; do not add app highlights, gestures, or chess rules.
 * Validation date: 2026-10-13.
 */

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.chessboard.chesscore.model.Square
import com.example.chessboard.chessboardui.model.BoardOrientation
import com.example.chessboard.chessboardui.model.BoardSquareBorder
import com.example.chessboard.chessboardui.model.BoardSquareDecoration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

private const val TestSquareSizePx = 100f
private const val TestBoardSizePx = 8 * TestSquareSizePx
private const val TestBorderWidthFraction = 0.08f

@RunWith(AndroidJUnit4::class)
class BoardSquareDecorationsRendererTest {

    /** Checks the e3 fill against independently specified display cells in both orientations. */
    @Test
    fun fillsOnlyTargetSquareInBothOrientations() {
        data class ExpectedPlacement(val orientation: BoardOrientation, val column: Int, val row: Int)

        val placements = listOf(
            ExpectedPlacement(BoardOrientation.WHITE, column = 4, row = 5),
            ExpectedPlacement(BoardOrientation.BLACK, column = 3, row = 2),
        )
        val decoration = BoardSquareDecoration(Square('e', 3), Color.Yellow)
        val background = Color.Magenta.toArgb()
        for (expected in placements) {
            val bitmap = createBoardBitmap(background)
            try {
                drawDecorationsToBitmap(bitmap, listOf(decoration), expected.orientation, TestSquareSizePx)

                assertOnlyTargetSquareFilled(bitmap, expected.column, expected.row, Color.Yellow, background)
            } finally {
                bitmap.recycle()
            }
        }
    }

    /** The border must cover the fill at square edges while leaving the filled center visible. */
    @Test
    fun drawsBorderAfterFillInBothOrientations() {
        val decoration = BoardSquareDecoration(
            Square('e', 4), Color.Yellow, BoardSquareBorder(Color.Blue, TestBorderWidthFraction),
        )
        for (orientation in BoardOrientation.entries) {
            var cellIndex = 4
            if (orientation == BoardOrientation.BLACK) {
                cellIndex = 3
            }
            val bitmap = createBoardBitmap(Color.Magenta.toArgb())
            try {
                drawDecorationsToBitmap(bitmap, listOf(decoration), orientation, TestSquareSizePx)

                val topLeft = (cellIndex * TestSquareSizePx).toInt()
                val center = ((cellIndex + 0.5f) * TestSquareSizePx).toInt()
                val bottomRight = ((cellIndex + 1) * TestSquareSizePx).toInt()
                assertEquals(Color.Yellow.toArgb(), bitmap.getPixel(center, center))
                assertEquals(Color.Blue.toArgb(), bitmap.getPixel(topLeft, center))
                assertEquals(Color.Blue.toArgb(), bitmap.getPixel(center, topLeft))
                assertEquals(Color.Blue.toArgb(), bitmap.getPixel(bottomRight, center))
                assertEquals(Color.Blue.toArgb(), bitmap.getPixel(center, bottomRight))
            } finally {
                bitmap.recycle()
            }
        }
    }

    /** A transparent fill must preserve existing pixels under a border-only decoration. */
    @Test
    fun transparentFillPreservesBackgroundUnderBorderOnlyDecoration() {
        val decoration = BoardSquareDecoration(
            Square('e', 4), Color.Transparent, BoardSquareBorder(Color.Blue, TestBorderWidthFraction),
        )
        val background = Color.Yellow.toArgb()
        val bitmap = createBoardBitmap(background)
        try {
            drawDecorationsToBitmap(bitmap, listOf(decoration), BoardOrientation.WHITE, TestSquareSizePx)

            val leftEdge = (4 * TestSquareSizePx).toInt()
            val center = ((4 + 0.5f) * TestSquareSizePx).toInt()
            assertEquals(background, bitmap.getPixel(center, center))
            assertEquals(Color.Blue.toArgb(), bitmap.getPixel(leftEdge, center))
            assertEquals(background, bitmap.getPixel(0, 0))
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Checks that the stroke scales with square size and extends equally inside and outside edges.
     * These sizes give integral half-widths so samples lie away from antialiased boundaries.
     */
    @Test
    fun borderWidthScalesWithSquareSizeAndIsCenteredOnEdges() {
        val decoration = BoardSquareDecoration(
            Square('e', 4), Color.Transparent, BoardSquareBorder(Color.Blue, TestBorderWidthFraction),
        )
        val background = Color.Magenta.toArgb()
        for (squareSizePx in listOf(TestSquareSizePx, TestSquareSizePx / 2)) {
            val bitmap = createBoardBitmap(background)
            try {
                drawDecorationsToBitmap(bitmap, listOf(decoration), BoardOrientation.WHITE, squareSizePx)

                val edge = (4 * squareSizePx).toInt()
                val center = ((4 + 0.5f) * squareSizePx).toInt()
                val halfWidth = (squareSizePx * TestBorderWidthFraction / 2).toInt()
                assertEquals(background, bitmap.getPixel(edge - halfWidth - 1, center))
                assertEquals(Color.Blue.toArgb(), bitmap.getPixel(edge - halfWidth, center))
                assertEquals(Color.Blue.toArgb(), bitmap.getPixel(edge + halfWidth - 1, center))
                assertEquals(background, bitmap.getPixel(edge + halfWidth, center))
                assertEquals(background, bitmap.getPixel(center, edge - halfWidth - 1))
                assertEquals(Color.Blue.toArgb(), bitmap.getPixel(center, edge - halfWidth))
                assertEquals(Color.Blue.toArgb(), bitmap.getPixel(center, edge + halfWidth - 1))
                assertEquals(background, bitmap.getPixel(center, edge + halfWidth))
            } finally {
                bitmap.recycle()
            }
        }
    }

    /**
     * A fill between two borders must cover the first border's inner half before the second is drawn.
     * Drawing all fills first and all borders afterwards would produce the wrong inner color.
     */
    @Test
    fun appliesRepeatedSquareDecorationsInListOrder() {
        val square = Square('e', 4)
        val decorations = listOf(
            BoardSquareDecoration(
                square, Color.Transparent, BoardSquareBorder(Color.Blue, 2 * TestBorderWidthFraction),
            ),
            BoardSquareDecoration(square, Color.Yellow),
            BoardSquareDecoration(
                square, Color.Transparent, BoardSquareBorder(Color.Green, TestBorderWidthFraction),
            ),
        )
        val bitmap = createBoardBitmap(Color.Magenta.toArgb())
        try {
            drawDecorationsToBitmap(bitmap, decorations, BoardOrientation.WHITE, TestSquareSizePx)

            val edge = (4 * TestSquareSizePx).toInt()
            val center = ((4 + 0.5f) * TestSquareSizePx).toInt()
            // Halfway between the inner and outer borders' half-widths.
            val betweenBorders = (TestSquareSizePx * TestBorderWidthFraction * 0.75f).toInt()
            assertEquals(Color.Yellow.toArgb(), bitmap.getPixel(center, center))
            assertEquals(Color.Green.toArgb(), bitmap.getPixel(edge, center))
            assertEquals(Color.Yellow.toArgb(), bitmap.getPixel(edge + betweenBorders, center))
            assertEquals(Color.Blue.toArgb(), bitmap.getPixel(edge - betweenBorders, center))
        } finally {
            bitmap.recycle()
        }
    }

    /** Empty decorations must preserve every pixel of the drawing target. */
    @Test
    fun emptyDecorationsLeaveBitmapUntouched() {
        val background = Color.Magenta.toArgb()
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(background)
        try {
            drawDecorationsToBitmap(bitmap, emptyList(), BoardOrientation.WHITE, TestSquareSizePx)

            assertEquals(background, bitmap.getPixel(0, 0))
        } finally {
            bitmap.recycle()
        }
    }

    /** Invalid sizes must fail without drawing, whether the decoration list is empty or populated. */
    @Test
    fun rejectsNonPositiveAndNonFiniteSizesEvenForEmptyDecorations() {
        fun assertRejectedSize(squareSizePx: Float, decorations: List<BoardSquareDecoration>) {
            val background = Color.Magenta.toArgb()
            val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(background)
            try {
                assertThrows(IllegalArgumentException::class.java) {
                    drawDecorationsToBitmap(bitmap, decorations, BoardOrientation.WHITE, squareSizePx)
                }
                assertEquals(background, bitmap.getPixel(0, 0))
            } finally {
                bitmap.recycle()
            }
        }

        val invalidSizes = listOf(
            0f, -0f, -1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
        )
        val decorationLists = listOf(emptyList(), listOf(BoardSquareDecoration(Square('e', 4), Color.Yellow)))
        for (squareSizePx in invalidSizes) {
            for (decorations in decorationLists) {
                assertRejectedSize(squareSizePx, decorations)
            }
        }
    }

    private fun createBoardBitmap(background: Int): Bitmap {
        val sizePx = TestBoardSizePx.toInt()
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(background)
        return bitmap
    }

    private fun drawDecorationsToBitmap(
        bitmap: Bitmap,
        decorations: List<BoardSquareDecoration>,
        orientation: BoardOrientation,
        squareSizePx: Float,
    ) {
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = Canvas(bitmap.asImageBitmap()),
            size = Size(bitmap.width.toFloat(), bitmap.height.toFloat()),
        ) {
            drawSquareDecorations(
                drawScope = drawScope,
                decorations = decorations,
                orientation = orientation,
                squareSizePx = squareSizePx,
            )
        }
    }

    private fun assertOnlyTargetSquareFilled(
        bitmap: Bitmap,
        column: Int,
        row: Int,
        fillColor: Color,
        background: Int,
    ) {
        for (displayRow in 0 until 8) {
            for (displayColumn in 0 until 8) {
                var expectedColor = background
                if (displayRow == row && displayColumn == column) {
                    expectedColor = fillColor.toArgb()
                }
                val centerX = ((displayColumn + 0.5f) * TestSquareSizePx).toInt()
                val centerY = ((displayRow + 0.5f) * TestSquareSizePx).toInt()
                assertEquals(expectedColor, bitmap.getPixel(centerX, centerY))
            }
        }

        val left = (column * TestSquareSizePx).toInt()
        val top = (row * TestSquareSizePx).toInt()
        val lastX = ((column + 1) * TestSquareSizePx).toInt() - 1
        val lastY = ((row + 1) * TestSquareSizePx).toInt() - 1
        assertEquals(fillColor.toArgb(), bitmap.getPixel(left, top))
        assertEquals(fillColor.toArgb(), bitmap.getPixel(lastX, top))
        assertEquals(fillColor.toArgb(), bitmap.getPixel(left, lastY))
        assertEquals(fillColor.toArgb(), bitmap.getPixel(lastX, lastY))
    }
}
