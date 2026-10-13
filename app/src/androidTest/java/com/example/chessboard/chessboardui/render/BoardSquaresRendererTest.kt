package com.example.chessboard.chessboardui.render

/**
 * Checks board-square rendering by inspecting pixels in an Android bitmap.
 * Keep background rendering checks here; do not add app screens, gestures, or piece drawing.
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

private const val TestSquareSizePx = 100f
private const val TestBoardSizePx = 8 * TestSquareSizePx

@RunWith(AndroidJUnit4::class)
class BoardSquaresRendererTest {

    /**
     * Draws each supplied palette at three square sizes and checks the centers of all 64 squares.
     * The expected rows explicitly describe the alternating pattern, starting with a light square.
     */
    @Test
    fun drawsAll64SquaresAtMultipleSizesWithSuppliedColors() {
        data class SquareColors(val light: Color, val dark: Color)

        val palettes = listOf(
            SquareColors(Color.Yellow, Color.Blue),
            SquareColors(Color.Red, Color.Green),
        )
        val squareSizes = listOf(TestSquareSizePx * 0.01f, TestSquareSizePx / 8, TestSquareSizePx)
        for (colors in palettes) {
            for (squareSizePx in squareSizes) {
                val boardSizePx = (8 * squareSizePx).toInt()
                val bitmap = Bitmap.createBitmap(boardSizePx, boardSizePx, Bitmap.Config.ARGB_8888)
                try {
                    drawBoardToBitmap(bitmap, squareSizePx, colors.light, colors.dark)

                    assertSquareCenterColors(bitmap, squareSizePx, colors.light, colors.dark)
                } finally {
                    bitmap.recycle()
                }
            }
        }
    }

    /** Checks pixels on both sides of the first horizontal and vertical internal boundaries. */
    @Test
    fun fillsSquaresUpToTheirInternalEdges() {
        val boardSizePx = TestBoardSizePx.toInt()
        val bitmap = Bitmap.createBitmap(boardSizePx, boardSizePx, Bitmap.Config.ARGB_8888)
        try {
            drawBoardToBitmap(bitmap, TestSquareSizePx, Color.Yellow, Color.Blue)

            val edge = TestSquareSizePx.toInt()
            val justBeforeEdge = edge - 1
            assertEquals(Color.Yellow.toArgb(), bitmap.getPixel(justBeforeEdge, 0))
            assertEquals(Color.Blue.toArgb(), bitmap.getPixel(edge, 0))
            assertEquals(Color.Yellow.toArgb(), bitmap.getPixel(0, justBeforeEdge))
            assertEquals(Color.Blue.toArgb(), bitmap.getPixel(0, edge))
            assertEquals(Color.Yellow.toArgb(), bitmap.getPixel(justBeforeEdge, justBeforeEdge))
            assertEquals(Color.Blue.toArgb(), bitmap.getPixel(edge, justBeforeEdge))
            assertEquals(Color.Blue.toArgb(), bitmap.getPixel(justBeforeEdge, edge))
            assertEquals(Color.Yellow.toArgb(), bitmap.getPixel(edge, edge))
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Uses a canvas one pixel larger than the board in each direction.
     * Board corners must be filled; the extra right column and bottom row must keep their background.
     */
    @Test
    fun fillsBoardCornersAndLeavesPixelsOutsideBoardUntouched() {
        val boardEdge = TestBoardSizePx.toInt()
        val bitmap = Bitmap.createBitmap(boardEdge + 1, boardEdge + 1, Bitmap.Config.ARGB_8888)
        val background = Color.Magenta.toArgb()
        bitmap.eraseColor(background)
        try {
            drawBoardToBitmap(bitmap, TestSquareSizePx, Color.Yellow, Color.Blue)

            val lastBoardPixel = boardEdge - 1
            assertEquals(Color.Yellow.toArgb(), bitmap.getPixel(0, 0))
            assertEquals(Color.Blue.toArgb(), bitmap.getPixel(lastBoardPixel, 0))
            assertEquals(Color.Blue.toArgb(), bitmap.getPixel(0, lastBoardPixel))
            assertEquals(Color.Yellow.toArgb(), bitmap.getPixel(lastBoardPixel, lastBoardPixel))
            for (pixel in 0..boardEdge) {
                assertEquals("Right edge at y=$pixel", background, bitmap.getPixel(boardEdge, pixel))
                assertEquals("Bottom edge at x=$pixel", background, bitmap.getPixel(pixel, boardEdge))
            }
        } finally {
            bitmap.recycle()
        }
    }

    /** Invalid square sizes must fail before any drawing changes the target bitmap. */
    @Test
    fun rejectsNonPositiveAndNonFiniteSquareSizesBeforeDrawing() {
        val invalidSizes = listOf(
            0f,
            -0f,
            -1f,
            Float.NaN,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
        )
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val background = Color.Magenta.toArgb()
        bitmap.eraseColor(background)
        try {
            for (squareSizePx in invalidSizes) {
                assertThrows(IllegalArgumentException::class.java) {
                    drawBoardToBitmap(bitmap, squareSizePx, Color.Yellow, Color.Blue)
                }
                assertEquals(background, bitmap.getPixel(0, 0))
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun drawBoardToBitmap(
        bitmap: Bitmap,
        squareSizePx: Float,
        lightColor: Color,
        darkColor: Color,
    ) {
        CanvasDrawScope().draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = Canvas(bitmap.asImageBitmap()),
            size = Size(bitmap.width.toFloat(), bitmap.height.toFloat()),
        ) {
            drawBoardSquares(squareSizePx, lightColor, darkColor)
        }
    }

    private fun assertSquareCenterColors(
        bitmap: Bitmap,
        squareSizePx: Float,
        lightColor: Color,
        darkColor: Color,
    ) {
        val lightFirstRow = listOf(lightColor, darkColor, lightColor, darkColor, lightColor, darkColor, lightColor, darkColor)
        val darkFirstRow = listOf(darkColor, lightColor, darkColor, lightColor, darkColor, lightColor, darkColor, lightColor)
        val expectedRows = listOf(
            lightFirstRow, darkFirstRow, lightFirstRow, darkFirstRow,
            lightFirstRow, darkFirstRow, lightFirstRow, darkFirstRow,
        )
        for ((row, colors) in expectedRows.withIndex()) {
            for ((column, expectedColor) in colors.withIndex()) {
                val centerX = ((column + 0.5f) * squareSizePx).toInt()
                val centerY = ((row + 0.5f) * squareSizePx).toInt()
                assertEquals(
                    "Square at column=$column, row=$row, squareSizePx=$squareSizePx",
                    expectedColor.toArgb(),
                    bitmap.getPixel(centerX, centerY),
                )
            }
        }
    }
}
