package com.example.chessboard.chessboardui.render

/**
 * Checks scene-piece placement, source-copy exclusion, and layer order against explicit piece draws.
 * Keep scene image checks here; do not add animation timing, gestures, app screens, or chess rules.
 * Validation date: 2026-10-13.
 */

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import org.junit.Test
import org.junit.runner.RunWith

private const val TestSquareSizePx = 100f
private const val TestBoardSizePx = 8 * TestSquareSizePx

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
class BoardScenePiecesRendererTest {

    /** Expected display cells are specified independently of production square-to-center geometry. */
    @Test
    fun drawsStationaryPiecesInBothOrientationsAtMultipleSizes() {
        data class ExpectedCell(val piece: Piece, val column: Int, val row: Int)
        data class ExpectedOrientation(val orientation: BoardOrientation, val cells: List<ExpectedCell>)

        val knight = BoardRenderPiece(Piece(Side.WHITE, PieceType.KNIGHT), Square('e', 3))
        val rook = BoardRenderPiece(Piece(Side.BLACK, PieceType.ROOK), Square('a', 8))
        val queen = BoardRenderPiece(Piece(Side.WHITE, PieceType.QUEEN), Square('h', 1))
        val orientations = listOf(
            ExpectedOrientation(
                BoardOrientation.WHITE,
                listOf(
                    ExpectedCell(knight.piece, column = 4, row = 5),
                    ExpectedCell(rook.piece, column = 0, row = 0),
                    ExpectedCell(queen.piece, column = 7, row = 7),
                ),
            ),
            ExpectedOrientation(
                BoardOrientation.BLACK,
                listOf(
                    ExpectedCell(knight.piece, column = 3, row = 2),
                    ExpectedCell(rook.piece, column = 7, row = 7),
                    ExpectedCell(queen.piece, column = 0, row = 0),
                ),
            ),
        )
        for (expected in orientations) {
            val scene = BoardRenderScene(listOf(knight, rook, queen), expected.orientation)
            for (squareSizePx in listOf(TestSquareSizePx, TestSquareSizePx / 2)) {
                val draws = mutableListOf<ExpectedDraw>()
                for (cell in expected.cells) {
                    draws.add(ExpectedDraw(cell.piece, displayCellCenter(cell.column, cell.row, squareSizePx)))
                }
                assertSceneImage(scene, draws, squareSizePx)
            }
        }
    }

    /** The animated center is already a display coordinate and must not be mirrored or copied at the source. */
    @Test
    fun drawsAnimatedPieceAtSuppliedCenterWithoutSourceCopy() {
        val knight = BoardRenderPiece(Piece(Side.WHITE, PieceType.KNIGHT), Square('e', 3))
        val center = Offset((1 + 0.25f) * TestSquareSizePx, (2 + 0.75f) * TestSquareSizePx)
        for (orientation in BoardOrientation.entries) {
            val scene = BoardRenderScene(
                pieces = listOf(knight),
                orientation = orientation,
                animatedPieces = listOf(BoardRenderAnimatedPiece(knight.square, center)),
            )
            assertSceneImage(scene, listOf(ExpectedDraw(knight.piece, center)), TestSquareSizePx)
        }
    }

    /** The dragged piece appears only at dragOffset, independently of the viewing orientation. */
    @Test
    fun drawsDraggedPieceAtSuppliedCenterWithoutSourceCopy() {
        val pawn = BoardRenderPiece(Piece(Side.BLACK, PieceType.PAWN), Square('d', 2))
        val center = Offset((5 + 0.25f) * TestSquareSizePx, (2 + 0.75f) * TestSquareSizePx)
        for (orientation in BoardOrientation.entries) {
            val scene = BoardRenderScene(
                pieces = listOf(pawn),
                orientation = orientation,
                dragFromSquare = pawn.square,
                dragOffset = center,
            )
            assertSceneImage(scene, listOf(ExpectedDraw(pawn.piece, center)), TestSquareSizePx)
        }
    }

    /** Uses different types and sides, with frame order differing from placement order, to check reference lookup. */
    @Test
    fun drawsStationaryAnimatedAndDraggedPiecesTogether() {
        val king = BoardRenderPiece(Piece(Side.WHITE, PieceType.KING), Square('a', 8))
        val rook = BoardRenderPiece(Piece(Side.BLACK, PieceType.ROOK), Square('h', 1))
        val bishop = BoardRenderPiece(Piece(Side.WHITE, PieceType.BISHOP), Square('f', 8))
        val pawn = BoardRenderPiece(Piece(Side.BLACK, PieceType.PAWN), Square('b', 2))
        val rookCenter = Offset((1 + 0.25f) * TestSquareSizePx, (1 + 0.75f) * TestSquareSizePx)
        val bishopCenter = Offset((3 + 0.25f) * TestSquareSizePx, (2 + 0.75f) * TestSquareSizePx)
        val pawnCenter = Offset((5 + 0.25f) * TestSquareSizePx, (6 + 0.75f) * TestSquareSizePx)
        val scene = BoardRenderScene(
            pieces = listOf(pawn, bishop, king, rook),
            orientation = BoardOrientation.BLACK,
            dragFromSquare = pawn.square,
            dragOffset = pawnCenter,
            animatedPieces = listOf(
                BoardRenderAnimatedPiece(rook.square, rookCenter),
                BoardRenderAnimatedPiece(bishop.square, bishopCenter),
            ),
        )
        val expected = listOf(
            ExpectedDraw(king.piece, displayCellCenter(7, 7, TestSquareSizePx)),
            ExpectedDraw(rook.piece, rookCenter),
            ExpectedDraw(bishop.piece, bishopCenter),
            ExpectedDraw(pawn.piece, pawnCenter),
        )
        assertSceneImage(scene, expected, TestSquareSizePx)
    }

    /** Overlapping frames must follow their list order even when placement order is the opposite. */
    @Test
    fun drawsAnimatedPiecesInTheirListOrderWhenCentersOverlap() {
        val whiteKing = BoardRenderPiece(Piece(Side.WHITE, PieceType.KING), Square('a', 8))
        val blackKing = BoardRenderPiece(Piece(Side.BLACK, PieceType.KING), Square('h', 1))
        val center = displayCellCenter(4, 4, TestSquareSizePx)
        val forward = BoardRenderScene(
            pieces = listOf(blackKing, whiteKing),
            orientation = BoardOrientation.WHITE,
            animatedPieces = listOf(
                BoardRenderAnimatedPiece(whiteKing.square, center),
                BoardRenderAnimatedPiece(blackKing.square, center),
            ),
        )
        assertSceneImage(
            forward,
            listOf(ExpectedDraw(whiteKing.piece, center), ExpectedDraw(blackKing.piece, center)),
            TestSquareSizePx,
        )

        val reversed = BoardRenderScene(
            pieces = listOf(whiteKing, blackKing),
            orientation = BoardOrientation.WHITE,
            animatedPieces = listOf(
                BoardRenderAnimatedPiece(blackKing.square, center),
                BoardRenderAnimatedPiece(whiteKing.square, center),
            ),
        )
        assertSceneImage(
            reversed,
            listOf(ExpectedDraw(blackKing.piece, center), ExpectedDraw(whiteKing.piece, center)),
            TestSquareSizePx,
        )
    }

    /** Animation covers stationary drawing, and dragging covers both, regardless of placement order. */
    @Test
    fun drawsStationaryThenAnimatedThenDraggedLayersWhenCentersOverlap() {
        val stationary = BoardRenderPiece(Piece(Side.WHITE, PieceType.KING), Square('e', 4))
        val animated = BoardRenderPiece(Piece(Side.BLACK, PieceType.KING), Square('b', 8))
        val dragged = BoardRenderPiece(Piece(Side.WHITE, PieceType.KING), Square('c', 8))
        val center = displayCellCenter(4, 4, TestSquareSizePx)
        val animatedScene = BoardRenderScene(
            pieces = listOf(animated, stationary),
            orientation = BoardOrientation.WHITE,
            animatedPieces = listOf(BoardRenderAnimatedPiece(animated.square, center)),
        )
        assertSceneImage(
            animatedScene,
            listOf(ExpectedDraw(stationary.piece, center), ExpectedDraw(animated.piece, center)),
            TestSquareSizePx,
        )

        val draggedScene = BoardRenderScene(
            pieces = listOf(dragged, animated, stationary),
            orientation = BoardOrientation.WHITE,
            dragFromSquare = dragged.square,
            dragOffset = center,
            animatedPieces = listOf(BoardRenderAnimatedPiece(animated.square, center)),
        )
        assertSceneImage(
            draggedScene,
            listOf(
                ExpectedDraw(stationary.piece, center),
                ExpectedDraw(animated.piece, center),
                ExpectedDraw(dragged.piece, center),
            ),
            TestSquareSizePx,
        )
    }

    /** A larger target exposes animation beyond the board; a negative drag center remains partially visible. */
    @Test
    fun usesFiniteMovingCentersOutsideBoardWithoutClamping() {
        val rook = BoardRenderPiece(Piece(Side.BLACK, PieceType.ROOK), Square('h', 1))
        val king = BoardRenderPiece(Piece(Side.WHITE, PieceType.KING), Square('a', 8))
        val animatedCenter = Offset(TestBoardSizePx + TestSquareSizePx / 2, (3 + 0.5f) * TestSquareSizePx)
        val draggedCenter = Offset(-TestSquareSizePx * 0.01f, (6 + 0.5f) * TestSquareSizePx)
        val scene = BoardRenderScene(
            pieces = listOf(king, rook),
            orientation = BoardOrientation.BLACK,
            dragFromSquare = king.square,
            dragOffset = draggedCenter,
            animatedPieces = listOf(BoardRenderAnimatedPiece(rook.square, animatedCenter)),
        )
        assertSceneImage(
            scene,
            listOf(ExpectedDraw(rook.piece, animatedCenter), ExpectedDraw(king.piece, draggedCenter)),
            TestSquareSizePx,
            bitmapSizePx = (TestBoardSizePx + 2 * TestSquareSizePx).toInt(),
        )
    }

    /** This function draws pieces only, so an empty placement must preserve the target even with decorations. */
    @Test
    fun emptySceneIgnoresDecorationsAndPreservesExistingPixels() {
        val scene = BoardRenderScene(
            pieces = emptyList(),
            orientation = BoardOrientation.BLACK,
            decorations = listOf(BoardSquareDecoration(Square('e', 3), Color.Yellow)),
            dragOffset = Offset(TestSquareSizePx, TestSquareSizePx),
        )
        assertSceneImage(scene, emptyList(), TestSquareSizePx)
    }

    /** The size check must happen before drawing for both empty and populated scenes. */
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

    private data class ExpectedDraw(val piece: Piece, val center: Offset)

    private fun displayCellCenter(column: Int, row: Int, squareSizePx: Float): Offset {
        return Offset((column + 0.5f) * squareSizePx, (row + 0.5f) * squareSizePx)
    }

    private fun assertSceneImage(
        scene: BoardRenderScene,
        expectedDraws: List<ExpectedDraw>,
        squareSizePx: Float,
        bitmapSizePx: Int = TestBoardSizePx.toInt(),
    ) {
        val actual = createBitmap(bitmapSizePx)
        val expected = createBitmap(bitmapSizePx)
        try {
            drawSceneToBitmap(actual, scene, squareSizePx)
            drawToBitmap(expected) { drawScope ->
                for (draw in expectedDraws) {
                    drawPiece(drawScope, draw.piece, draw.center, squareSizePx, TestStyle)
                }
            }
            assertArrayEquals(
                "Scene at ${scene.orientation}, squareSizePx=$squareSizePx",
                readPixels(expected),
                readPixels(actual),
            )
        } finally {
            actual.recycle()
            expected.recycle()
        }
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
        bitmap.eraseColor(Color.Magenta.toArgb())
        return bitmap
    }

    private fun readPixels(bitmap: Bitmap): IntArray {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels
    }

    private fun drawSceneToBitmap(bitmap: Bitmap, scene: BoardRenderScene, squareSizePx: Float) {
        drawToBitmap(bitmap) { drawScope ->
            drawScenePieces(drawScope, scene, squareSizePx, TestStyle)
        }
    }

    private fun drawToBitmap(bitmap: Bitmap, draw: (DrawScope) -> Unit) {
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = Canvas(bitmap.asImageBitmap()),
            size = Size(bitmap.width.toFloat(), bitmap.height.toFloat()),
        ) {
            draw(drawScope)
        }
    }
}
