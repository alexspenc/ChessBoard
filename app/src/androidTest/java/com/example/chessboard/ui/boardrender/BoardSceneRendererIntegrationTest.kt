package com.example.chessboard.ui.boardrender

/**
 * Checks the application's Compose wrapper, scene adapter, and chessboardui renderer together.
 * Keep application palette, label opacity, highlight order, and moving-frame rendering checks here.
 * Do not add gestures, chess-rule changes, playback queue lifecycle, or library component tests.
 * Validation date: 2026-10-13.
 */

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.example.chessboard.boardmodel.LastMoveHighlight
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.theme.ChessDark
import com.example.chessboard.ui.theme.ChessLight
import com.example.chessboard.ui.theme.ChessPieceDark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.ceil

private const val CellCount = 8
private const val TestSquareSizeDp = 40
private const val TestBoardSizeDp = CellCount * TestSquareSizeDp
private const val BoardTestTag = "application-board-renderer-integration"
private const val RgbChannelTolerance = 2
// Outline contrast must exceed rounding noise by at least four times the RGB allowance.
private const val MinimumOutlineContrast = 4 * RgbChannelTolerance

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26) // Compose captureToImage requires Android O or later.
class BoardSceneRendererIntegrationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun rendersInitialPositionInBothOrientationsWithApplicationPalette() {
        val orientation = mutableStateOf(BoardOrientation.WHITE)
        val pieces = mutableStateOf(createInitialPieces())
        showScene { BoardRenderScene(pieces.value, orientation.value) }

        for (view in BoardOrientation.entries) {
            composeRule.runOnIdle { orientation.value = view }
            val pixels = captureBoard()
            // Rank 5 in White's view and rank 4 in Black's view are empty initially.
            assertCellCenterColor(pixels, 0, 3, ChessDark)
            assertCellCenterColor(pixels, 1, 3, ChessLight)
            for (column in 0 until CellCount) {
                for (rank in listOf(1, 2, 7, 8)) {
                    var displayColumn = column
                    var displayRow = CellCount - rank
                    if (view == BoardOrientation.BLACK) {
                        displayColumn = CellCount - 1 - column
                        displayRow = rank - 1
                    }
                    var fillColor = ChessPieceDark
                    if (rank <= 2) {
                        fillColor = Color.White
                    }
                    assertCellContainsColor(pixels, displayColumn, displayRow, fillColor)
                }
            }
        }

        // Central light squares avoid coordinate labels being mistaken for a dark outline.
        // White fill cannot darken a square; black fill cannot lighten it. Outlines must do so.
        composeRule.runOnIdle {
            pieces.value = listOf(BoardRenderPiece('K', "e4"), BoardRenderPiece('k', "d5"))
        }
        for (view in BoardOrientation.entries) {
            composeRule.runOnIdle { orientation.value = view }
            val pixels = captureBoard()
            var whiteKingColumn = 4
            var whiteKingRow = 4
            var blackKingColumn = 3
            var blackKingRow = 3
            if (view == BoardOrientation.BLACK) {
                whiteKingColumn = 3
                whiteKingRow = 3
                blackKingColumn = 4
                blackKingRow = 4
            }
            assertCellContainsColor(pixels, whiteKingColumn, whiteKingRow, Color.White)
            assertCellContainsColor(pixels, blackKingColumn, blackKingRow, ChessPieceDark)
            assertCellHasContrastingOutline(pixels, whiteKingColumn, whiteKingRow, ChessLight, darkOutline = true)
            assertCellHasContrastingOutline(pixels, blackKingColumn, blackKingRow, ChessLight, darkOutline = false)
        }
    }

    @Test
    fun keepsLegacyCoordinateOpacityInBothOrientations() {
        val orientation = mutableStateOf(BoardOrientation.WHITE)
        showScene { BoardRenderScene(emptyList(), orientation.value) }

        for (view in BoardOrientation.entries) {
            composeRule.runOnIdle { orientation.value = view }
            val pixels = captureBoard()
            val squareSizePx = pixels.width.toFloat() / CellCount
            var rankLabel = "8"
            if (view == BoardOrientation.BLACK) {
                rankLabel = "1"
            }
            // Use the previous Android Paint contract as an independent opacity reference.
            val expected = Bitmap.createBitmap(
                ceil(squareSizePx).toInt(), ceil(squareSizePx).toInt(), Bitmap.Config.ARGB_8888,
            )
            val canvas = AndroidCanvas(expected)
            canvas.drawColor(ChessLight.toArgb())
            val paint = Paint()
            paint.color = android.graphics.Color.BLACK
            paint.alpha = 100
            paint.textAlign = Paint.Align.LEFT
            paint.textSize = squareSizePx * 0.25f
            paint.isAntiAlias = true
            canvas.drawText(rankLabel, squareSizePx * 0.1f, squareSizePx * 0.3f, paint)

            var labelPixelCount = 0
            for (y in 0 until squareSizePx.toInt()) {
                for (x in 0 until squareSizePx.toInt()) {
                    val expectedColor = expected.getPixel(x, y)
                    if (expectedColor != ChessLight.toArgb()) {
                        labelPixelCount++
                    }
                    val actualColor = pixels[x, y].toArgb()
                    val context = "Rank label pixel ($x, $y), view=$view"
                    assertEquals("$context alpha", expectedColor ushr 24, actualColor ushr 24)
                    for (shift in listOf(16, 8, 0)) {
                        val expectedChannel = (expectedColor ushr shift) and 0xFF
                        val actualChannel = (actualColor ushr shift) and 0xFF
                        assertTrue(
                            "$context: expected ARGB ${expectedColor.toUInt().toString(16)}, " +
                                "got ${actualColor.toUInt().toString(16)}, channel shift=$shift",
                            abs(expectedChannel - actualChannel) <= RgbChannelTolerance,
                        )
                    }
                }
            }
            assertTrue("The opacity reference must include visible text", labelPixelCount > 0)
        }
    }

    @Test
    fun compositesAllHighlightLayersInOriginalOrderAndDrawsHintBorder() {
        val orientation = mutableStateOf(BoardOrientation.WHITE)
        showScene { squareSizePx ->
            BoardRenderScene(
                pieces = listOf(BoardRenderPiece('P', "e4")),
                orientation = orientation.value,
                lastMoveHighlight = LastMoveHighlight("e4", "e4"),
                selectedSquare = "e4",
                dragFromSquare = "e4",
                dragOffset = Offset((5 + 0.5f) * squareSizePx, (1 + 0.5f) * squareSizePx),
                wrongMoveSquare = "e4",
                hintSquare = "e4",
            )
        }
        // e4 is light in either orientation. The six fills must remain separate and ordered.
        val fills = listOf(
            Color(0xFFFFD600).copy(alpha = 0.55f),
            Color(0xFFFFFF80).copy(alpha = 0.50f),
            Color.Yellow.copy(alpha = 0.4f),
            Color.Yellow.copy(alpha = 0.4f),
            Color.Red.copy(alpha = 0.45f),
            Color(0xFF1DB584).copy(alpha = 0.22f),
        )
        val expectedFill = compositeColors(ChessLight, fills)
        val expectedBorder = compositeColors(
            ChessLight, fills + Color(0xFF1DB584).copy(alpha = 0.85f),
        )

        for (view in BoardOrientation.entries) {
            composeRule.runOnIdle { orientation.value = view }
            val pixels = captureBoard()
            val squareSizePx = pixels.width.toFloat() / CellCount
            var column = 4
            var row = 4
            if (view == BoardOrientation.BLACK) {
                column = 3
                row = 3
            }
            val centerX = ((column + 0.5f) * squareSizePx).toInt()
            val centerY = ((row + 0.5f) * squareSizePx).toInt()
            assertColorClose(expectedFill, pixels[centerX, centerY].toArgb())
            // The centered 7% border covers the first 3.5% inside the cell.
            val borderX = ((column + 0.01f) * squareSizePx).toInt()
            assertColorClose(expectedBorder, pixels[borderX, centerY].toArgb())
            assertCellDoesNotContainColor(pixels, column, row, Color.White)
            assertCellContainsColor(pixels, 5, 1, Color.White)
        }
    }

    @Test
    fun rendersStaticAnimatedAndDraggedPiecesThroughAdapterInBothOrientations() {
        val orientation = mutableStateOf(BoardOrientation.WHITE)
        showScene { squareSizePx ->
            BoardRenderScene(
                pieces = listOf(
                    BoardRenderPiece('k', "e8"), BoardRenderPiece('K', "e1"),
                    BoardRenderPiece('R', "h1"), BoardRenderPiece('P', "e2"),
                ),
                orientation = orientation.value,
                selectedSquare = "e8",
                hintSquare = "e8",
                dragFromSquare = "e2",
                dragOffset = Offset((2 + 0.5f) * squareSizePx, (3 + 0.5f) * squareSizePx),
                animatedPieces = listOf(
                    BoardRenderAnimatedPiece(
                        "e1", Offset((5 + 0.5f) * squareSizePx, (4 + 0.5f) * squareSizePx),
                    ),
                    BoardRenderAnimatedPiece(
                        "h1", Offset((6 + 0.5f) * squareSizePx, (2 + 0.5f) * squareSizePx),
                    ),
                ),
            )
        }

        for (view in BoardOrientation.entries) {
            composeRule.runOnIdle { orientation.value = view }
            val pixels = captureBoard()
            var kingColumn = 4
            var blackKingRow = 0
            var whiteKingRow = 7
            var rookColumn = 7
            var pawnRow = 6
            if (view == BoardOrientation.BLACK) {
                kingColumn = 3
                blackKingRow = 7
                whiteKingRow = 0
                rookColumn = 0
                pawnRow = 1
            }
            // Static glyphs must remain above the selection and hint fills.
            assertCellContainsColor(pixels, kingColumn, blackKingRow, ChessPieceDark)
            // Moving centers are already display coordinates and must not rotate with the board.
            assertCellContainsColor(pixels, 5, 4, Color.White)
            assertCellContainsColor(pixels, 6, 2, Color.White)
            assertCellContainsColor(pixels, 2, 3, Color.White)
            assertCellDoesNotContainColor(pixels, kingColumn, whiteKingRow, Color.White)
            assertCellDoesNotContainColor(pixels, rookColumn, whiteKingRow, Color.White)
            assertCellDoesNotContainColor(pixels, kingColumn, pawnRow, Color.White)
        }
    }

    private fun showScene(sceneFactory: (Float) -> BoardRenderScene) {
        composeRule.setContent {
            BoardForIntegrationTest(sceneFactory)
        }
    }

    @Composable
    private fun BoardForIntegrationTest(sceneFactory: (Float) -> BoardRenderScene) {
        BoxWithConstraints(
            modifier = Modifier
                .sizeIn(maxWidth = TestBoardSizeDp.dp, maxHeight = TestBoardSizeDp.dp)
                .aspectRatio(1f),
        ) {
            val squareSizePx = constraints.maxWidth.toFloat() / CellCount
            BoardSceneRenderer(
                scene = sceneFactory(squareSizePx),
                squareSizePx = squareSizePx,
                modifier = Modifier.fillMaxSize().testTag(BoardTestTag),
            )
        }
    }

    private fun captureBoard(): PixelMap {
        val board = composeRule.onNodeWithTag(BoardTestTag)
        board.assertIsDisplayed()
        composeRule.waitForIdle()
        val image = board.captureToImage()
        assertEquals("The application's board Canvas must remain square", image.width, image.height)
        assertTrue("The captured board must have a positive size", image.width > 0)
        return image.toPixelMap()
    }

    private fun createInitialPieces(): List<BoardRenderPiece> {
        val backRank = "RNBQKBNR"
        val pieces = mutableListOf<BoardRenderPiece>()
        for (column in backRank.indices) {
            val file = 'a' + column
            pieces.add(BoardRenderPiece(backRank[column], "${file}1"))
            pieces.add(BoardRenderPiece('P', "${file}2"))
            pieces.add(BoardRenderPiece('p', "${file}7"))
            pieces.add(BoardRenderPiece(backRank[column].lowercaseChar(), "${file}8"))
        }
        return pieces
    }

    private fun assertCellCenterColor(pixels: PixelMap, column: Int, row: Int, expected: Color) {
        val squareSizePx = pixels.width.toFloat() / CellCount
        val x = ((column + 0.5f) * squareSizePx).toInt()
        val y = ((row + 0.5f) * squareSizePx).toInt()
        assertEquals("Cell center ($column, $row)", expected.toArgb(), pixels[x, y].toArgb())
    }

    private fun assertCellContainsColor(pixels: PixelMap, column: Int, row: Int, color: Color) {
        val squareSizePx = pixels.width.toFloat() / CellCount
        for (y in (row * squareSizePx).toInt() until ((row + 1) * squareSizePx).toInt()) {
            for (x in (column * squareSizePx).toInt() until ((column + 1) * squareSizePx).toInt()) {
                if (pixels[x, y].toArgb() == color.toArgb()) {
                    return
                }
            }
        }
        fail("Expected color $color inside cell ($column, $row)")
    }

    private fun assertCellDoesNotContainColor(pixels: PixelMap, column: Int, row: Int, color: Color) {
        val squareSizePx = pixels.width.toFloat() / CellCount
        for (y in (row * squareSizePx).toInt() until ((row + 1) * squareSizePx).toInt()) {
            for (x in (column * squareSizePx).toInt() until ((column + 1) * squareSizePx).toInt()) {
                if (pixels[x, y].toArgb() == color.toArgb()) {
                    fail("Unexpected source-square piece color $color in cell ($column, $row)")
                }
            }
        }
    }

    private fun assertCellHasContrastingOutline(
        pixels: PixelMap,
        column: Int,
        row: Int,
        background: Color,
        darkOutline: Boolean,
    ) {
        val squareSizePx = pixels.width.toFloat() / CellCount
        val backgroundArgb = background.toArgb()
        val rgbShifts = listOf(16, 8, 0)
        var strongestContrast = 0
        for (y in (row * squareSizePx).toInt() until ((row + 1) * squareSizePx).toInt()) {
            for (x in (column * squareSizePx).toInt() until ((column + 1) * squareSizePx).toInt()) {
                val pixel = pixels[x, y].toArgb()
                var followsOutlineDirection = true
                var contrast = 0
                for (shift in rgbShifts) {
                    val backgroundChannel = (backgroundArgb ushr shift) and 0xFF
                    val actualChannel = (pixel ushr shift) and 0xFF
                    var channelContrast = actualChannel - backgroundChannel
                    if (darkOutline) {
                        channelContrast = backgroundChannel - actualChannel
                    }
                    // Accept antialiased mixtures, but reject a significant opposite color shift.
                    if (channelContrast < -RgbChannelTolerance) {
                        followsOutlineDirection = false
                        break
                    }
                    contrast = maxOf(contrast, channelContrast)
                }
                if (followsOutlineDirection) {
                    strongestContrast = maxOf(strongestContrast, contrast)
                }
            }
        }
        assertTrue(
            "Outline contrast in cell ($column, $row), dark=$darkOutline: " +
                "strongest=$strongestContrast, required=$MinimumOutlineContrast",
            strongestContrast >= MinimumOutlineContrast,
        )
    }

    private fun compositeColors(background: Color, overlays: List<Color>): Int {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        canvas.drawColor(background.toArgb())
        val paint = Paint()
        for (color in overlays) {
            paint.color = color.toArgb()
            canvas.drawRect(0f, 0f, 1f, 1f, paint)
        }
        return bitmap.getPixel(0, 0)
    }

    private fun assertColorClose(expected: Int, actual: Int) {
        // Successive alpha blends can differ by up to two channel units across drawing backends.
        for (shift in listOf(24, 16, 8, 0)) {
            val expectedChannel = (expected ushr shift) and 0xFF
            val actualChannel = (actual ushr shift) and 0xFF
            assertTrue(
                "Expected ARGB ${expected.toUInt().toString(16)}, got ${actual.toUInt().toString(16)}",
                abs(expectedChannel - actualChannel) <= 2,
            )
        }
    }
}
