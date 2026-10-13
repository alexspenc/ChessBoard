package com.example.chessboard.chessboardui.model

/**
 * Tests scene collection ownership and drawing-input validation.
 * Do not add renderer, gesture, chess-rule, or application-controller tests here.
 * Validation date: 2026-10-13.
 */

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.chessboard.chesscore.model.Piece
import com.example.chessboard.chesscore.model.PieceType
import com.example.chessboard.chesscore.model.Side
import com.example.chessboard.chesscore.model.Square
import com.example.chessboard.chessboardui.TestBoardSizePx
import com.example.chessboard.chessboardui.TestSquareSizePx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardRenderSceneTest {

    @Test
    fun acceptsEmptyBoardWithDefaultDrawingState() {
        val scene = BoardRenderScene(emptyList(), BoardOrientation.BLACK)

        assertTrue(scene.pieces.isEmpty())
        assertEquals(BoardOrientation.BLACK, scene.orientation)
        assertTrue(scene.decorations.isEmpty())
        assertNull(scene.dragFromSquare)
        assertEquals(Offset.Zero, scene.dragOffset)
        assertTrue(scene.animatedPieces.isEmpty())
    }

    @Test
    fun acceptsArbitraryPlacementWithoutChessLegalityChecks() {
        val pieces = listOf(
            BoardRenderPiece(Piece(Side.WHITE, PieceType.KING), Square('a', 1)),
            BoardRenderPiece(Piece(Side.WHITE, PieceType.KING), Square('b', 1)),
            BoardRenderPiece(Piece(Side.BLACK, PieceType.PAWN), Square('h', 1)),
        )

        val scene = BoardRenderScene(pieces, BoardOrientation.WHITE)

        assertEquals(pieces, scene.pieces)
    }

    @Test
    fun ownsAllThreeListsAfterCallerReplacesTheirContents() {
        val king = BoardRenderPiece(Piece(Side.WHITE, PieceType.KING), Square('e', 1))
        val rook = BoardRenderPiece(Piece(Side.WHITE, PieceType.ROOK), Square('h', 1))
        val pawn = BoardRenderPiece(Piece(Side.WHITE, PieceType.PAWN), Square('e', 2))
        val firstDecoration = BoardSquareDecoration(Square('e', 4), Color.Red)
        val secondDecoration = BoardSquareDecoration(Square('e', 4), Color.Green)
        val kingCenter = Offset((4 + 0.5f) * TestSquareSizePx, (7 + 0.5f) * TestSquareSizePx)
        val rookCenter = Offset((7 + 0.5f) * TestSquareSizePx, (7 + 0.5f) * TestSquareSizePx)
        // The king is half a square to the right of e1; the rook is one square left of h1.
        val kingFrame = BoardRenderAnimatedPiece(king.square, kingCenter + Offset(TestSquareSizePx / 2, 0f))
        val rookFrame = BoardRenderAnimatedPiece(rook.square, rookCenter - Offset(TestSquareSizePx, 0f))
        // The pawn is halfway between the centers of e2 and e3.
        val dragCenter = Offset((4 + 0.5f) * TestSquareSizePx, 6 * TestSquareSizePx)
        val pieces = mutableListOf(king, rook, pawn)
        val decorations = mutableListOf(firstDecoration, secondDecoration)
        val animatedPieces = mutableListOf(kingFrame, rookFrame)
        val scene = BoardRenderScene(
            pieces = pieces,
            orientation = BoardOrientation.WHITE,
            decorations = decorations,
            dragFromSquare = pawn.square,
            dragOffset = dragCenter,
            animatedPieces = animatedPieces,
        )

        pieces.clear()
        pieces.add(BoardRenderPiece(Piece(Side.BLACK, PieceType.QUEEN), Square('d', 8)))
        decorations.clear()
        decorations.add(BoardSquareDecoration(Square('a', 1), Color.Blue))
        animatedPieces.clear()
        animatedPieces.add(BoardRenderAnimatedPiece(Square('d', 8), Offset.Zero))

        assertEquals(listOf(king, rook, pawn), scene.pieces)
        assertEquals(listOf(firstDecoration, secondDecoration), scene.decorations)
        assertEquals(listOf(kingFrame, rookFrame), scene.animatedPieces)
        assertEquals(pawn.square, scene.dragFromSquare)
        assertEquals(dragCenter, scene.dragOffset)
    }

    @Test
    fun sceneListsCannotBeMutatedThroughMutableListCasts() {
        val piece = BoardRenderPiece(Piece(Side.WHITE, PieceType.KING), Square('e', 1))
        val decoration = BoardSquareDecoration(Square('e', 4), Color.Red)
        val kingCenter = Offset((4 + 0.5f) * TestSquareSizePx, (7 + 0.5f) * TestSquareSizePx)
        val animatedPiece = BoardRenderAnimatedPiece(piece.square, kingCenter)
        val scene = BoardRenderScene(
            pieces = listOf(piece),
            orientation = BoardOrientation.WHITE,
            decorations = listOf(decoration),
            animatedPieces = listOf(animatedPiece),
        )

        assertThrows(UnsupportedOperationException::class.java) {
            (scene.pieces as MutableList<BoardRenderPiece>).clear()
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (scene.decorations as MutableList<BoardSquareDecoration>).clear()
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (scene.animatedPieces as MutableList<BoardRenderAnimatedPiece>).clear()
        }
        assertEquals(listOf(piece), scene.pieces)
        assertEquals(listOf(decoration), scene.decorations)
        assertEquals(listOf(animatedPiece), scene.animatedPieces)
    }

    @Test
    fun keepsOrderedDecorationsOnTheSameEmptySquare() {
        val square = Square('e', 4)
        val fill = BoardSquareDecoration(square, Color.Red)
        val border = BoardSquareDecoration(
            square,
            Color.Transparent,
            BoardSquareBorder(Color.Green, 0.07f),
        )

        val scene = BoardRenderScene(
            pieces = emptyList(),
            orientation = BoardOrientation.WHITE,
            decorations = listOf(fill, border),
        )

        assertEquals(listOf(fill, border), scene.decorations)
    }

    @Test
    fun acceptsDistinctMovingPiecesWithCentersOutsideBoard() {
        val pawn = BoardRenderPiece(Piece(Side.WHITE, PieceType.PAWN), Square('e', 2))
        val rook = BoardRenderPiece(Piece(Side.WHITE, PieceType.ROOK), Square('h', 1))
        val rookCenter = Offset(TestBoardSizePx + TestSquareSizePx, -TestSquareSizePx / 4)
        val rookFrame = BoardRenderAnimatedPiece(rook.square, rookCenter)
        val dragCenter = Offset(-TestSquareSizePx / 5, TestBoardSizePx + 2 * TestSquareSizePx)

        val scene = BoardRenderScene(
            pieces = listOf(pawn, rook),
            orientation = BoardOrientation.WHITE,
            dragFromSquare = pawn.square,
            dragOffset = dragCenter,
            animatedPieces = listOf(rookFrame),
        )

        assertEquals(dragCenter, scene.dragOffset)
        assertEquals(listOf(rookFrame), scene.animatedPieces)
    }

    @Test
    fun rejectsTwoPiecesOnOneSquareAndIdentifiesTheSquare() {
        val square = Square('e', 4)
        val error = assertThrows(IllegalArgumentException::class.java) {
            BoardRenderScene(
                pieces = listOf(
                    BoardRenderPiece(Piece(Side.WHITE, PieceType.ROOK), square),
                    BoardRenderPiece(Piece(Side.BLACK, PieceType.KNIGHT), square),
                ),
                orientation = BoardOrientation.WHITE,
            )
        }

        assertTrue(error.message.orEmpty().contains("e4"))
    }

    @Test
    fun rejectsMissingDraggedPieceAndIdentifiesTheSquare() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            BoardRenderScene(
                pieces = emptyList(),
                orientation = BoardOrientation.WHITE,
                dragFromSquare = Square('e', 2),
            )
        }

        assertTrue(error.message.orEmpty().contains("e2"))
    }

    @Test
    fun rejectsMissingAnimatedPieceAndIdentifiesTheSquare() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            BoardRenderScene(
                pieces = emptyList(),
                orientation = BoardOrientation.BLACK,
                animatedPieces = listOf(BoardRenderAnimatedPiece(Square('h', 1), Offset.Zero)),
            )
        }

        assertTrue(error.message.orEmpty().contains("h1"))
    }

    @Test
    fun rejectsRepeatedAnimatedSourceAndIdentifiesTheSquare() {
        val piece = BoardRenderPiece(Piece(Side.WHITE, PieceType.KING), Square('e', 1))
        val error = assertThrows(IllegalArgumentException::class.java) {
            BoardRenderScene(
                pieces = listOf(piece),
                orientation = BoardOrientation.WHITE,
                animatedPieces = listOf(
                    BoardRenderAnimatedPiece(
                        piece.square,
                        Offset((4 + 0.5f) * TestSquareSizePx, (7 + 0.5f) * TestSquareSizePx),
                    ),
                    BoardRenderAnimatedPiece(
                        piece.square,
                        Offset((5 + 0.5f) * TestSquareSizePx, (7 + 0.5f) * TestSquareSizePx),
                    ),
                ),
            )
        }

        assertTrue(error.message.orEmpty().contains("e1"))
    }

    @Test
    fun rejectsPieceReferencedByBothDragAndAnimation() {
        val piece = BoardRenderPiece(Piece(Side.WHITE, PieceType.PAWN), Square('e', 2))
        val error = assertThrows(IllegalArgumentException::class.java) {
            BoardRenderScene(
                pieces = listOf(piece),
                orientation = BoardOrientation.WHITE,
                dragFromSquare = piece.square,
                animatedPieces = listOf(BoardRenderAnimatedPiece(piece.square, Offset.Zero)),
            )
        }

        assertTrue(error.message.orEmpty().contains("e2"))
    }

    @Test
    fun rejectsNonFiniteDragCoordinatesEvenWithoutDraggedPiece() {
        for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            for (offset in listOf(Offset(value, 0f), Offset(0f, value))) {
                assertThrows(IllegalArgumentException::class.java) {
                    BoardRenderScene(
                        pieces = emptyList(),
                        orientation = BoardOrientation.WHITE,
                        dragOffset = offset,
                    )
                }
            }
        }
    }

    @Test
    fun rejectsNonFiniteAnimatedCoordinates() {
        val piece = BoardRenderPiece(Piece(Side.WHITE, PieceType.KING), Square('e', 1))
        for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            for (offset in listOf(Offset(value, 0f), Offset(0f, value))) {
                assertThrows(IllegalArgumentException::class.java) {
                    BoardRenderScene(
                        pieces = listOf(piece),
                        orientation = BoardOrientation.WHITE,
                        animatedPieces = listOf(BoardRenderAnimatedPiece(piece.square, offset)),
                    )
                }
            }
        }
    }
}
