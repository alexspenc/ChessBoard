package com.example.chessboard.chessboardui.render

/**
 * Checks single-piece colors, outlines, translation, and input validation in Android bitmaps.
 * Keep device-independent image properties here; do not add app screens or scene orchestration.
 * Validation date: 2026-10-13.
 */

import android.graphics.Bitmap
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
import com.example.chessboard.chessboardui.model.BoardRenderStyle
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val TestSquareSizePx = 100f
private const val TestBitmapSizePx = 4 * TestSquareSizePx

private val TestStyle = BoardRenderStyle(
    lightSquareColor = Color.Gray,
    darkSquareColor = Color.DarkGray,
    whitePieceColor = Color.Red,
    blackPieceColor = Color.Green,
    whitePieceOutlineColor = Color.Blue,
    blackPieceOutlineColor = Color.Yellow,
    coordinateColor = Color.Cyan,
)

@RunWith(AndroidJUnit4::class)
class BoardPieceRendererTest {

    /** Checks all six types and both sides against four distinct caller-supplied piece colors. */
    @Test
    fun drawsEveryPieceWithItsSidesFillAndOutlineColors() {
        val center = Offset((1 + 0.5f) * TestSquareSizePx, (1 + 0.5f) * TestSquareSizePx)
        val fillOnlyStyle = TestStyle.copy(
            whitePieceOutlineColor = Color.Transparent,
            blackPieceOutlineColor = Color.Transparent,
        )
        for (type in PieceType.entries) {
            for (side in Side.entries) {
                val bitmap = renderPiece(Piece(side, type), center, TestSquareSizePx, TestStyle)
                val fillOnlyBitmap = renderPiece(Piece(side, type), center, TestSquareSizePx, fillOnlyStyle)
                try {
                    assertPieceColors(readPixels(bitmap), readPixels(fillOnlyBitmap), side, type)
                } finally {
                    bitmap.recycle()
                    fillOnlyBitmap.recycle()
                }
            }
        }
    }

    /** Giving both sides identical colors must produce identical shapes for each piece type. */
    @Test
    fun sameColorsProduceSameShapeForBothSides() {
        val style = TestStyle.copy(
            blackPieceColor = TestStyle.whitePieceColor,
            blackPieceOutlineColor = TestStyle.whitePieceOutlineColor,
        )
        val center = Offset((1 + 0.5f) * TestSquareSizePx, (1 + 0.5f) * TestSquareSizePx)
        for (type in PieceType.entries) {
            val whiteBitmap = renderPiece(Piece(Side.WHITE, type), center, TestSquareSizePx, style)
            val blackBitmap = renderPiece(Piece(Side.BLACK, type), center, TestSquareSizePx, style)
            try {
                assertArrayEquals("Shape for $type", readPixels(whiteBitmap), readPixels(blackBitmap))
            } finally {
                whiteBitmap.recycle()
                blackBitmap.recycle()
            }
        }
    }

    /** Moving the center by one square along both axes must translate every glyph by that offset. */
    @Test
    fun changingCenterTranslatesEveryPieceWithoutChangingItsPixels() {
        val center = Offset((1 + 0.5f) * TestSquareSizePx, (1 + 0.5f) * TestSquareSizePx)
        val translation = Offset(TestSquareSizePx, TestSquareSizePx)
        for (type in PieceType.entries) {
            val piece = Piece(Side.WHITE, type)
            val original = renderPiece(piece, center, TestSquareSizePx, TestStyle)
            val translated = renderPiece(piece, center + translation, TestSquareSizePx, TestStyle)
            try {
                assertImageTranslation(original, translated, translation.x.toInt(), translation.y.toInt())
            } finally {
                original.recycle()
                translated.recycle()
            }
        }
    }

    /** Increasing square size must increase visible width and height without relying on a device font snapshot. */
    @Test
    fun increasingSquareSizeMakesEveryGlyphLarger() {
        val center = Offset(2 * TestSquareSizePx, 2 * TestSquareSizePx)
        for (type in PieceType.entries) {
            val piece = Piece(Side.WHITE, type)
            val smallBitmap = renderPiece(piece, center, TestSquareSizePx / 2, TestStyle)
            val largeBitmap = renderPiece(piece, center, TestSquareSizePx, TestStyle)
            try {
                val smallInk = getInkSize(smallBitmap)
                val largeInk = getInkSize(largeBitmap)
                assertTrue("Width for $type", largeInk.width > smallInk.width)
                assertTrue("Height for $type", largeInk.height > smallInk.height)
            } finally {
                smallBitmap.recycle()
                largeBitmap.recycle()
            }
        }
    }

    /** A center left of the target must crop the translated glyph, rather than clamp it to the edge. */
    @Test
    fun acceptsFiniteCentersOutsideTargetWithoutClamping() {
        val piece = Piece(Side.WHITE, PieceType.KING)
        val insideCenter = Offset(TestSquareSizePx / 2, TestSquareSizePx / 2)
        val outsideCenter = Offset(-TestSquareSizePx * 0.01f, insideCenter.y)
        val inside = renderPiece(piece, insideCenter, TestSquareSizePx, TestStyle)
        val outside = renderPiece(piece, outsideCenter, TestSquareSizePx, TestStyle)
        try {
            assertImageTranslation(inside, outside, (outsideCenter.x - insideCenter.x).toInt(), 0)
            assertTrue("The visible part of the glyph remains", getInkSize(outside).width > 0f)
        } finally {
            inside.recycle()
            outside.recycle()
        }
    }

    /** Invalid square sizes must fail before changing even one target pixel. */
    @Test
    fun rejectsNonPositiveAndNonFiniteSizesBeforeDrawing() {
        val invalidSizes = listOf(
            0f, -0f, -1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
        )
        val center = Offset(TestSquareSizePx / 2, TestSquareSizePx / 2)
        for (squareSizePx in invalidSizes) {
            assertRejectedInput(squareSizePx, center)
        }
    }

    /** Either center coordinate being NaN or infinite must fail before any drawing. */
    @Test
    fun rejectsNonFiniteCentersBeforeDrawing() {
        val finiteCoordinate = TestSquareSizePx / 2
        for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            for (center in listOf(Offset(value, finiteCoordinate), Offset(finiteCoordinate, value))) {
                assertRejectedInput(TestSquareSizePx, center)
            }
        }
    }

    /** Transparent fills and outlines must preserve pixels already present on the target. */
    @Test
    fun transparentPieceColorsLeaveExistingPixelsUntouched() {
        val style = TestStyle.copy(
            whitePieceColor = Color.Transparent,
            blackPieceColor = Color.Transparent,
            whitePieceOutlineColor = Color.Transparent,
            blackPieceOutlineColor = Color.Transparent,
        )
        val bitmap = createBitmap()
        bitmap.eraseColor(Color.Magenta.toArgb())
        val originalPixels = readPixels(bitmap)
        val center = Offset((1 + 0.5f) * TestSquareSizePx, (1 + 0.5f) * TestSquareSizePx)
        try {
            for (side in Side.entries) {
                drawPieceToBitmap(bitmap, Piece(side, PieceType.KING), center, TestSquareSizePx, style)
            }
            assertArrayEquals(originalPixels, readPixels(bitmap))
        } finally {
            bitmap.recycle()
        }
    }

    private fun assertPieceColors(pixels: IntArray, fillOnlyPixels: IntArray, side: Side, type: PieceType) {
        var fillColor = TestStyle.whitePieceColor
        var outlineColor = TestStyle.whitePieceOutlineColor
        var unusedFill = TestStyle.blackPieceColor
        var unusedOutline = TestStyle.blackPieceOutlineColor
        if (side == Side.BLACK) {
            fillColor = TestStyle.blackPieceColor
            outlineColor = TestStyle.blackPieceOutlineColor
            unusedFill = TestStyle.whitePieceColor
            unusedOutline = TestStyle.whitePieceOutlineColor
        }

        val fillPixels = countPixelsWithRgbColor(pixels, fillColor)
        val outlinePixels = countPixelsWithRgbColor(pixels, outlineColor)
        assertTrue("Fill for $side $type", fillPixels > 0)
        assertTrue("Outline for $side $type", outlinePixels > 0)
        assertEquals("Unused fill for $side $type", 0, countPixelsWithRgbColor(pixels, unusedFill))
        assertEquals("Unused outline for $side $type", 0, countPixelsWithRgbColor(pixels, unusedOutline))

        var opaqueFillPixels = 0
        for (index in fillOnlyPixels.indices) {
            if ((fillOnlyPixels[index] ushr 24) != 255) {
                continue
            }
            // Fully covered fill pixels must hide the outline underneath them.
            assertEquals("Filled interior for $side $type", fillColor.toArgb(), pixels[index])
            opaqueFillPixels++
        }
        assertTrue("Opaque fill for $side $type", opaqueFillPixels > 0)
    }

    private fun countPixelsWithRgbColor(pixels: IntArray, color: Color): Int {
        val expectedRgb = color.toArgb() and 0x00FFFFFF
        var count = 0
        for (pixel in pixels) {
            // Ignore coverage alpha so antialiased outline pixels count too.
            if ((pixel ushr 24) != 0 && (pixel and 0x00FFFFFF) == expectedRgb) {
                count++
            }
        }
        return count
    }

    private fun assertImageTranslation(original: Bitmap, translated: Bitmap, deltaX: Int, deltaY: Int) {
        val originalPixels = readPixels(original)
        val expectedPixels = IntArray(originalPixels.size)
        for (index in originalPixels.indices) {
            val x = index % original.width + deltaX
            val y = index / original.width + deltaY
            if (x !in 0 until original.width || y !in 0 until original.height) {
                continue
            }
            expectedPixels[y * original.width + x] = originalPixels[index]
        }
        assertArrayEquals("Translation by ($deltaX, $deltaY)", expectedPixels, readPixels(translated))
    }

    private fun getInkSize(bitmap: Bitmap): Size {
        val pixels = readPixels(bitmap)
        var left = bitmap.width
        var top = bitmap.height
        var right = -1
        var bottom = -1
        for (index in pixels.indices) {
            if ((pixels[index] ushr 24) == 0) {
                continue
            }
            val x = index % bitmap.width
            val y = index / bitmap.width
            left = minOf(left, x)
            top = minOf(top, y)
            right = maxOf(right, x)
            bottom = maxOf(bottom, y)
        }
        assertTrue("The glyph must have visible pixels", right >= left && bottom >= top)
        return Size((right - left + 1).toFloat(), (bottom - top + 1).toFloat())
    }

    private fun assertRejectedInput(squareSizePx: Float, center: Offset) {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val background = Color.Magenta.toArgb()
        bitmap.eraseColor(background)
        try {
            assertThrows(IllegalArgumentException::class.java) {
                drawPieceToBitmap(bitmap, Piece(Side.WHITE, PieceType.KING), center, squareSizePx, TestStyle)
            }
            assertEquals(background, bitmap.getPixel(0, 0))
        } finally {
            bitmap.recycle()
        }
    }

    private fun createBitmap(): Bitmap {
        val sizePx = TestBitmapSizePx.toInt()
        return Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    }

    private fun renderPiece(piece: Piece, center: Offset, squareSizePx: Float, style: BoardRenderStyle): Bitmap {
        val bitmap = createBitmap()
        drawPieceToBitmap(bitmap, piece, center, squareSizePx, style)
        return bitmap
    }

    private fun readPixels(bitmap: Bitmap): IntArray {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels
    }

    private fun drawPieceToBitmap(
        bitmap: Bitmap,
        piece: Piece,
        center: Offset,
        squareSizePx: Float,
        style: BoardRenderStyle,
    ) {
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = Canvas(bitmap.asImageBitmap()),
            size = Size(bitmap.width.toFloat(), bitmap.height.toFloat()),
        ) {
            drawPiece(
                drawScope = drawScope,
                piece = piece,
                centerOffset = center,
                squareSizePx = squareSizePx,
                style = style,
            )
        }
    }
}
