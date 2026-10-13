package com.example.chessboard.chessboardui.render

/**
 * Shows the complete initial board through library drawing functions in a visible Compose Canvas.
 * Keep visual composition checks here; do not add app screens, gestures, or the production renderer.
 * Validation date: 2026-10-13.
 */

import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.chessboard.chesscore.model.Piece
import com.example.chessboard.chesscore.model.PieceType
import com.example.chessboard.chesscore.model.Side
import com.example.chessboard.chesscore.model.Square
import com.example.chessboard.chessboardui.model.BoardOrientation
import com.example.chessboard.chessboardui.model.BoardRenderPiece
import com.example.chessboard.chessboardui.model.BoardRenderScene
import com.example.chessboard.chessboardui.model.BoardRenderStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val CellCount = 8
private const val TestSquareSizeDp = 40
private const val TestBoardSizeDp = CellCount * TestSquareSizeDp
private const val VisualInspectionDurationMs = 3_000L
private const val BoardTestTag = "library-board-visual-test"

private val TestStyle = BoardRenderStyle(
    lightSquareColor = Color(0xFFF0D9B5),
    darkSquareColor = Color(0xFFB58863),
    whitePieceColor = Color.White,
    blackPieceColor = Color(0xFF212121),
    whitePieceOutlineColor = Color.Black,
    blackPieceOutlineColor = Color.White,
    coordinateColor = Color.Black,
)

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26) // Compose captureToImage requires Android O or later.
class BoardRenderingVisualTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** Shows all 32 initial pieces from White's side and keeps the rendered board visible for inspection. */
    @Test
    fun showsInitialPositionFromWhiteSide() {
        showAndCheckInitialPosition(BoardOrientation.WHITE)
    }

    /** Shows the same position from Black's side with the same three-second inspection pause. */
    @Test
    fun showsInitialPositionFromBlackSide() {
        showAndCheckInitialPosition(BoardOrientation.BLACK)
    }

    private fun showAndCheckInitialPosition(orientation: BoardOrientation) {
        val scene = BoardRenderScene(createInitialPieces(), orientation)
        composeRule.setContent {
            BoardForVisualTest(scene)
        }
        val board = composeRule.onNodeWithTag(BoardTestTag)
        board.assertIsDisplayed()
        composeRule.waitForIdle()
        try {
            val image = board.captureToImage()
            assertEquals("The visible board must be square", image.width, image.height)
            val pixels = image.toPixelMap()
            val squareSizePx = image.width.toFloat() / CellCount
            assertEmptySquareColor(
                pixels, column = 0, row = 3,
                squareSizePx = squareSizePx, color = TestStyle.darkSquareColor,
            )
            assertEmptySquareColor(
                pixels, column = 1, row = 3,
                squareSizePx = squareSizePx, color = TestStyle.lightSquareColor,
            )
            assertInitialPieceColors(pixels, squareSizePx, orientation)
        } finally {
            // Real time is intentional: advancing Compose's clock would not allow visual inspection.
            SystemClock.sleep(VisualInspectionDurationMs)
        }
    }

    @Composable
    private fun BoardForVisualTest(scene: BoardRenderScene) {
        var label = "White orientation"
        if (scene.orientation == BoardOrientation.BLACK) {
            label = "Black orientation"
        }
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFFEDEDED)).padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(label, modifier = Modifier.align(Alignment.TopCenter))
            Canvas(
                modifier = Modifier
                    .sizeIn(maxWidth = TestBoardSizeDp.dp, maxHeight = TestBoardSizeDp.dp)
                    .aspectRatio(1f)
                    .testTag(BoardTestTag),
            ) {
                val drawScope = this
                val squareSizePx = size.width / CellCount
                drawBoardSquares(drawScope, squareSizePx, TestStyle.lightSquareColor, TestStyle.darkSquareColor)
                drawScenePieces(drawScope, scene, squareSizePx, TestStyle)
            }
        }
    }

    private fun createInitialPieces(): List<BoardRenderPiece> {
        val backRankTypes = listOf(
            PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
            PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK,
        )
        val pieces = mutableListOf<BoardRenderPiece>()
        for (column in backRankTypes.indices) {
            val file = 'a' + column
            pieces.add(BoardRenderPiece(Piece(Side.WHITE, backRankTypes[column]), Square(file, 1)))
            pieces.add(BoardRenderPiece(Piece(Side.WHITE, PieceType.PAWN), Square(file, 2)))
            pieces.add(BoardRenderPiece(Piece(Side.BLACK, PieceType.PAWN), Square(file, 7)))
            pieces.add(BoardRenderPiece(Piece(Side.BLACK, backRankTypes[column]), Square(file, 8)))
        }
        return pieces
    }

    private fun assertEmptySquareColor(
        pixels: PixelMap,
        column: Int,
        row: Int,
        squareSizePx: Float,
        color: Color,
    ) {
        val x = ((column + 0.5f) * squareSizePx).toInt()
        val y = ((row + 0.5f) * squareSizePx).toInt()
        assertEquals("Empty square at column=$column, row=$row", color.toArgb(), pixels[x, y].toArgb())
    }

    private fun assertInitialPieceColors(pixels: PixelMap, squareSizePx: Float, orientation: BoardOrientation) {
        var topFill = TestStyle.blackPieceColor
        var bottomFill = TestStyle.whitePieceColor
        if (orientation == BoardOrientation.BLACK) {
            topFill = TestStyle.whitePieceColor
            bottomFill = TestStyle.blackPieceColor
        }
        for (column in 0 until CellCount) {
            assertPieceColorInCell(pixels, column, row = 0, squareSizePx = squareSizePx, color = topFill)
            assertPieceColorInCell(pixels, column, row = 1, squareSizePx = squareSizePx, color = topFill)
            assertPieceColorInCell(pixels, column, row = 6, squareSizePx = squareSizePx, color = bottomFill)
            assertPieceColorInCell(pixels, column, row = 7, squareSizePx = squareSizePx, color = bottomFill)
        }
    }

    private fun assertPieceColorInCell(
        pixels: PixelMap,
        column: Int,
        row: Int,
        squareSizePx: Float,
        color: Color,
    ) {
        val left = (column * squareSizePx).toInt()
        val top = (row * squareSizePx).toInt()
        val right = ((column + 1) * squareSizePx).toInt()
        val bottom = ((row + 1) * squareSizePx).toInt()
        val expectedColor = color.toArgb()
        for (y in top until bottom) {
            for (x in left until right) {
                if (pixels[x, y].toArgb() == expectedColor) {
                    return
                }
            }
        }
        fail("No piece fill at column=$column, row=$row")
    }
}
