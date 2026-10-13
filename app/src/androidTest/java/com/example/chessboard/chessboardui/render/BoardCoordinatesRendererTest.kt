package com.example.chessboard.chessboardui.render

/**
 * Checks coordinate text, placement, color, and opacity in Android bitmaps.
 * Keep label image checks here; do not add pieces, app screens, or device-specific font snapshots.
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
import com.example.chessboard.chessboardui.model.BoardOrientation
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

private const val TestSquareSizePx = 100f
private const val TestBoardSizePx = 8 * TestSquareSizePx

@RunWith(AndroidJUnit4::class)
class BoardCoordinatesRendererTest {

    /** Explicit label sequences and placement formulas form the reference, without production label selection. */
    @Test
    fun drawsExpectedLabelsInBothOrientationsAtMultipleSizes() {
        data class ExpectedLabels(val orientation: BoardOrientation, val ranks: List<String>, val files: List<String>)

        val cases = listOf(
            ExpectedLabels(
                BoardOrientation.WHITE,
                ranks = listOf("8", "7", "6", "5", "4", "3", "2", "1"),
                files = listOf("a", "b", "c", "d", "e", "f", "g", "h"),
            ),
            ExpectedLabels(
                BoardOrientation.BLACK,
                ranks = listOf("1", "2", "3", "4", "5", "6", "7", "8"),
                files = listOf("h", "g", "f", "e", "d", "c", "b", "a"),
            ),
        )
        for (expected in cases) {
            for (squareSizePx in listOf(TestSquareSizePx, TestSquareSizePx / 2, TestSquareSizePx / 4)) {
                val labels = mutableListOf<ExpectedLabel>()
                for ((row, rank) in expected.ranks.withIndex()) {
                    labels.add(
                        ExpectedLabel(
                            rank,
                            Offset(squareSizePx * 0.1f, row * squareSizePx + squareSizePx * 0.3f),
                            Paint.Align.LEFT,
                        ),
                    )
                }
                for ((column, file) in expected.files.withIndex()) {
                    labels.add(
                        ExpectedLabel(
                            file,
                            Offset(column * squareSizePx + squareSizePx * 0.8f, 8 * squareSizePx - squareSizePx * 0.2f),
                            Paint.Align.CENTER,
                        ),
                    )
                }
                assertLabelImage(expected.orientation, squareSizePx, labels)
            }
        }
    }

    /** Compares opaque and half-opacity blue labels pixel by pixel, including antialiased coverage. */
    @Test
    fun usesSuppliedColorAndAlphaWithoutExtraOpacity() {
        val translucentColor = Color.Blue.copy(alpha = 0.5f)
        val suppliedAlpha = translucentColor.toArgb() ushr 24
        val expectedRgb = Color.Blue.toArgb() and 0x00FFFFFF
        for (orientation in BoardOrientation.entries) {
            val opaque = createBitmap(TestBoardSizePx.toInt())
            val translucent = createBitmap(TestBoardSizePx.toInt())
            try {
                drawCoordinatesToBitmap(opaque, orientation, TestSquareSizePx, Color.Blue)
                drawCoordinatesToBitmap(translucent, orientation, TestSquareSizePx, translucentColor)
                val opaquePixels = readPixels(opaque)
                val translucentPixels = readPixels(translucent)
                assertOpacityMatches(opaquePixels, translucentPixels, suppliedAlpha, expectedRgb)
            } finally {
                opaque.recycle()
                translucent.recycle()
            }
        }
    }

    /** A transparent coordinate color must preserve the existing target for both orientations. */
    @Test
    fun transparentColorLeavesExistingPixelsUntouched() {
        val bitmap = createBitmap(TestBoardSizePx.toInt())
        bitmap.eraseColor(Color.Magenta.toArgb())
        val original = readPixels(bitmap)
        try {
            for (orientation in BoardOrientation.entries) {
                drawCoordinatesToBitmap(bitmap, orientation, TestSquareSizePx, Color.Transparent)
            }
            assertArrayEquals(original, readPixels(bitmap))
        } finally {
            bitmap.recycle()
        }
    }

    /** Invalid square sizes must fail before changing even one target pixel. */
    @Test
    fun rejectsNonPositiveAndNonFiniteSizesBeforeDrawing() {
        val invalidSizes = listOf(
            0f, -0f, -1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
        )
        for (orientation in BoardOrientation.entries) {
            for (squareSizePx in invalidSizes) {
                assertRejectedSize(orientation, squareSizePx)
            }
        }
    }

    private data class ExpectedLabel(val text: String, val baseline: Offset, val alignment: Paint.Align)

    private fun assertLabelImage(orientation: BoardOrientation, squareSizePx: Float, labels: List<ExpectedLabel>) {
        val bitmapSizePx = (8 * squareSizePx).toInt()
        val actual = createBitmap(bitmapSizePx)
        val expected = createBitmap(bitmapSizePx)
        actual.eraseColor(Color.Magenta.toArgb())
        expected.eraseColor(Color.Magenta.toArgb())
        try {
            drawCoordinatesToBitmap(actual, orientation, squareSizePx, Color.Green)
            val canvas = android.graphics.Canvas(expected)
            val paint = Paint()
            paint.color = Color.Green.toArgb()
            paint.textSize = squareSizePx / 4
            paint.isAntiAlias = true
            for (label in labels) {
                paint.textAlign = label.alignment
                canvas.drawText(label.text, label.baseline.x, label.baseline.y, paint)
            }
            assertArrayEquals(
                "Coordinates at $orientation, squareSizePx=$squareSizePx",
                readPixels(expected),
                readPixels(actual),
            )
        } finally {
            actual.recycle()
            expected.recycle()
        }
    }

    private fun assertOpacityMatches(opaque: IntArray, translucent: IntArray, suppliedAlpha: Int, expectedRgb: Int) {
        var visiblePixels = 0
        for (index in opaque.indices) {
            val coverageAlpha = opaque[index] ushr 24
            val expectedAlpha = (coverageAlpha * suppliedAlpha + 127) / 255
            val actualAlpha = translucent[index] ushr 24
            // Raster coverage and color alpha can round separately by one alpha level.
            if (abs(expectedAlpha - actualAlpha) > 1) {
                fail("Alpha at pixel $index: expected $expectedAlpha, actual $actualAlpha")
            }
            if (actualAlpha == 0) {
                continue
            }
            assertEquals("Color at pixel $index", expectedRgb, translucent[index] and 0x00FFFFFF)
            visiblePixels++
        }
        assertTrue("Labels must have visible pixels", visiblePixels > 0)
    }

    private fun assertRejectedSize(orientation: BoardOrientation, squareSizePx: Float) {
        val bitmap = createBitmap(1)
        val background = Color.Magenta.toArgb()
        bitmap.eraseColor(background)
        try {
            assertThrows(IllegalArgumentException::class.java) {
                drawCoordinatesToBitmap(bitmap, orientation, squareSizePx, Color.Green)
            }
            assertEquals(background, bitmap.getPixel(0, 0))
        } finally {
            bitmap.recycle()
        }
    }

    private fun createBitmap(sizePx: Int): Bitmap {
        return Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    }

    private fun readPixels(bitmap: Bitmap): IntArray {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels
    }

    private fun drawCoordinatesToBitmap(
        bitmap: Bitmap,
        orientation: BoardOrientation,
        squareSizePx: Float,
        color: Color,
    ) {
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = Canvas(bitmap.asImageBitmap()),
            size = Size(bitmap.width.toFloat(), bitmap.height.toFloat()),
        ) {
            drawCoordinates(drawScope, orientation, squareSizePx, color)
        }
    }
}
