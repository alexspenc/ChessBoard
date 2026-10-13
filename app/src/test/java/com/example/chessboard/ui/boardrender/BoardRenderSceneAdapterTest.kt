package com.example.chessboard.ui.boardrender

/**
 * Tests the application-to-chessboardui scene bridge, including existing animation frame producers.
 * Keep conversion, highlight order, and input-error coverage here.
 * Do not add pixel rendering, gestures, playback queue lifecycle, or screen tests.
 * Validation date: 2026-10-13.
 */

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.chessboard.boardmodel.LastMoveHighlight
import com.example.chessboard.chessboardui.TestBoardSizePx
import com.example.chessboard.chessboardui.TestSquareSizePx
import com.example.chessboard.chessboardui.model.BoardSquareBorder
import com.example.chessboard.chessboardui.model.BoardSquareDecoration
import com.example.chessboard.chesscore.model.Piece
import com.example.chessboard.chesscore.model.PieceType
import com.example.chessboard.chesscore.model.Side
import com.example.chessboard.chesscore.model.Square
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.boardanimation.AnimateCaptureMoveAction
import com.example.chessboard.ui.boardanimation.AnimateCastlingMoveAction
import com.example.chessboard.ui.boardanimation.AnimateSimpleMoveAction
import com.example.chessboard.ui.boardanimation.buildAnimatedBoardRenderScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.chessboard.chessboardui.model.BoardOrientation as LibraryBoardOrientation
import com.example.chessboard.chessboardui.model.BoardRenderAnimatedPiece as LibraryBoardRenderAnimatedPiece
import com.example.chessboard.chessboardui.model.BoardRenderPiece as LibraryBoardRenderPiece

class BoardRenderSceneAdapterTest {

    @Test
    fun mapsAllTwelvePieceLettersAndPreservesPlacementOrder() {
        val input = listOf(
            BoardRenderPiece('K', "a1"), BoardRenderPiece('Q', "b1"),
            BoardRenderPiece('R', "c1"), BoardRenderPiece('B', "d1"),
            BoardRenderPiece('N', "e1"), BoardRenderPiece('P', "f1"),
            BoardRenderPiece('k', "a8"), BoardRenderPiece('q', "b8"),
            BoardRenderPiece('r', "c8"), BoardRenderPiece('b', "d8"),
            BoardRenderPiece('n', "e8"), BoardRenderPiece('p', "f8"),
        )
        val expected = listOf(
            LibraryBoardRenderPiece(Piece(Side.WHITE, PieceType.KING), Square('a', 1)),
            LibraryBoardRenderPiece(Piece(Side.WHITE, PieceType.QUEEN), Square('b', 1)),
            LibraryBoardRenderPiece(Piece(Side.WHITE, PieceType.ROOK), Square('c', 1)),
            LibraryBoardRenderPiece(Piece(Side.WHITE, PieceType.BISHOP), Square('d', 1)),
            LibraryBoardRenderPiece(Piece(Side.WHITE, PieceType.KNIGHT), Square('e', 1)),
            LibraryBoardRenderPiece(Piece(Side.WHITE, PieceType.PAWN), Square('f', 1)),
            LibraryBoardRenderPiece(Piece(Side.BLACK, PieceType.KING), Square('a', 8)),
            LibraryBoardRenderPiece(Piece(Side.BLACK, PieceType.QUEEN), Square('b', 8)),
            LibraryBoardRenderPiece(Piece(Side.BLACK, PieceType.ROOK), Square('c', 8)),
            LibraryBoardRenderPiece(Piece(Side.BLACK, PieceType.BISHOP), Square('d', 8)),
            LibraryBoardRenderPiece(Piece(Side.BLACK, PieceType.KNIGHT), Square('e', 8)),
            LibraryBoardRenderPiece(Piece(Side.BLACK, PieceType.PAWN), Square('f', 8)),
        )
        for (orientation in BoardOrientation.entries) {
            val scene = BoardRenderScene(input, orientation)

            val adapted = adaptBoardRenderScene(scene)

            assertEquals(expected, adapted.pieces)
            assertEquals(input, scene.pieces)
        }
    }

    @Test
    fun acceptsEmptySceneAndMapsBothOrientations() {
        val orientations = listOf(
            BoardOrientation.WHITE to LibraryBoardOrientation.WHITE,
            BoardOrientation.BLACK to LibraryBoardOrientation.BLACK,
        )
        for ((inputOrientation, expectedOrientation) in orientations) {
            val adapted = adaptBoardRenderScene(BoardRenderScene(emptyList(), inputOrientation))

            assertEquals(expectedOrientation, adapted.orientation)
            assertTrue(adapted.pieces.isEmpty())
            assertTrue(adapted.decorations.isEmpty())
            assertTrue(adapted.animatedPieces.isEmpty())
            assertNull(adapted.dragFromSquare)
            assertEquals(Offset.Zero, adapted.dragOffset)
        }
    }

    @Test
    fun acceptsArbitraryPlacementWithoutCheckingChessLegality() {
        val scene = BoardRenderScene(
            pieces = listOf(
                BoardRenderPiece('K', "a1"),
                BoardRenderPiece('K', "b1"),
                BoardRenderPiece('p', "h1"),
            ),
            orientation = BoardOrientation.WHITE,
        )

        val adapted = adaptBoardRenderScene(scene)

        assertEquals(
            listOf(
                LibraryBoardRenderPiece(Piece(Side.WHITE, PieceType.KING), Square('a', 1)),
                LibraryBoardRenderPiece(Piece(Side.WHITE, PieceType.KING), Square('b', 1)),
                LibraryBoardRenderPiece(Piece(Side.BLACK, PieceType.PAWN), Square('h', 1)),
            ),
            adapted.pieces,
        )
    }

    @Test
    fun mapsAllHighlightsToExactOrderedFillsAndHintBorderInBothOrientations() {
        val expected = listOf(
            BoardSquareDecoration(Square('e', 2), Color(0xFFFFD600).copy(alpha = 0.55f)),
            BoardSquareDecoration(Square('e', 4), Color(0xFFFFFF80).copy(alpha = 0.50f)),
            BoardSquareDecoration(Square('c', 3), Color.Yellow.copy(alpha = 0.4f)),
            BoardSquareDecoration(Square('g', 7), Color.Yellow.copy(alpha = 0.4f)),
            BoardSquareDecoration(Square('b', 4), Color.Red.copy(alpha = 0.45f)),
            BoardSquareDecoration(
                Square('h', 5),
                Color(0xFF1DB584).copy(alpha = 0.22f),
                BoardSquareBorder(Color(0xFF1DB584).copy(alpha = 0.85f), 0.07f),
            ),
        )
        for (orientation in BoardOrientation.entries) {
            val scene = BoardRenderScene(
                pieces = listOf(BoardRenderPiece('p', "g7")),
                orientation = orientation,
                lastMoveHighlight = LastMoveHighlight("e2", "e4"),
                selectedSquare = "c3",
                dragFromSquare = "g7",
                wrongMoveSquare = "b4",
                hintSquare = "h5",
            )

            assertEquals(expected, adaptBoardRenderScene(scene).decorations)
        }
    }

    @Test
    fun preservesAllSixDecorationsWhenTheyTargetTheSameSquare() {
        val square = Square('e', 4)
        val scene = BoardRenderScene(
            pieces = listOf(BoardRenderPiece('P', "e4")),
            orientation = BoardOrientation.WHITE,
            lastMoveHighlight = LastMoveHighlight("e4", "e4"),
            selectedSquare = "e4",
            dragFromSquare = "e4",
            wrongMoveSquare = "e4",
            hintSquare = "e4",
        )

        assertEquals(
            listOf(
                BoardSquareDecoration(square, Color(0xFFFFD600).copy(alpha = 0.55f)),
                BoardSquareDecoration(square, Color(0xFFFFFF80).copy(alpha = 0.50f)),
                BoardSquareDecoration(square, Color.Yellow.copy(alpha = 0.4f)),
                BoardSquareDecoration(square, Color.Yellow.copy(alpha = 0.4f)),
                BoardSquareDecoration(square, Color.Red.copy(alpha = 0.45f)),
                BoardSquareDecoration(
                    square,
                    Color(0xFF1DB584).copy(alpha = 0.22f),
                    BoardSquareBorder(Color(0xFF1DB584).copy(alpha = 0.85f), 0.07f),
                ),
            ),
            adaptBoardRenderScene(scene).decorations,
        )
    }

    @Test
    fun acceptsHighlightsOnEmptySquaresAndOmitsAbsentHighlights() {
        val scene = BoardRenderScene(
            pieces = emptyList(),
            orientation = BoardOrientation.BLACK,
            selectedSquare = "a8",
            hintSquare = "h1",
        )

        assertEquals(
            listOf(
                BoardSquareDecoration(Square('a', 8), Color.Yellow.copy(alpha = 0.4f)),
                BoardSquareDecoration(
                    Square('h', 1),
                    Color(0xFF1DB584).copy(alpha = 0.22f),
                    BoardSquareBorder(Color(0xFF1DB584).copy(alpha = 0.85f), 0.07f),
                ),
            ),
            adaptBoardRenderScene(scene).decorations,
        )
    }

    @Test
    fun preservesDragAndAnimationCentersAndOrderWithoutRotatingOrClampingThem() {
        val dragCenter = Offset(-TestSquareSizePx / 2, TestBoardSizePx + TestSquareSizePx)
        val rookCenter = Offset(TestBoardSizePx + TestSquareSizePx / 4, -TestSquareSizePx)
        val kingCenter = Offset(3 * TestSquareSizePx, 2 * TestSquareSizePx)
        val frames = listOf(
            BoardRenderAnimatedPiece("h1", rookCenter),
            BoardRenderAnimatedPiece("e1", kingCenter),
        )
        for (orientation in BoardOrientation.entries) {
            val scene = BoardRenderScene(
                pieces = listOf(
                    BoardRenderPiece('K', "e1"), BoardRenderPiece('R', "h1"), BoardRenderPiece('P', "e2"),
                ),
                orientation = orientation,
                dragFromSquare = "e2",
                dragOffset = dragCenter,
                animatedPieces = frames,
            )

            val adapted = adaptBoardRenderScene(scene)

            assertEquals(Square('e', 2), adapted.dragFromSquare)
            assertEquals(dragCenter, adapted.dragOffset)
            assertEquals(
                listOf(
                    LibraryBoardRenderAnimatedPiece(Square('h', 1), rookCenter),
                    LibraryBoardRenderAnimatedPiece(Square('e', 1), kingCenter),
                ),
                adapted.animatedPieces,
            )
            assertEquals(frames, scene.animatedPieces)
            assertEquals(dragCenter, scene.dragOffset)
        }
    }

    @Test
    fun preservesFiniteDragCenterEvenWhenNoPieceIsDragged() {
        val center = Offset(-TestSquareSizePx, TestBoardSizePx + TestSquareSizePx)
        val scene = BoardRenderScene(emptyList(), BoardOrientation.WHITE, dragOffset = center)

        val adapted = adaptBoardRenderScene(scene)

        assertNull(adapted.dragFromSquare)
        assertEquals(center, adapted.dragOffset)
    }

    @Test
    fun rejectsUnknownPieceLettersAndIdentifiesTheirInputField() {
        for (letter in listOf('X', 'x', ' ', '1')) {
            val scene = BoardRenderScene(
                listOf(BoardRenderPiece('K', "e1"), BoardRenderPiece(letter, "e2")),
                BoardOrientation.WHITE,
            )

            val error = assertThrows(IllegalArgumentException::class.java) {
                adaptBoardRenderScene(scene)
            }

            assertTrue(error.message.orEmpty().contains("pieces[1].letter"))
            assertTrue(error.message.orEmpty().contains("'$letter'"))
        }
    }

    @Test
    fun rejectsInvalidSquareStringsInEverySourceFieldWithoutNormalizingThem() {
        val baseScene = BoardRenderScene(
            pieces = listOf(BoardRenderPiece('P', "e2"), BoardRenderPiece('R', "h1")),
            orientation = BoardOrientation.WHITE,
            lastMoveHighlight = LastMoveHighlight("e2", "e4"),
            selectedSquare = "c3",
            dragFromSquare = "e2",
            wrongMoveSquare = "b4",
            hintSquare = "h5",
            animatedPieces = listOf(BoardRenderAnimatedPiece("h1", Offset.Zero)),
        )
        val fields: List<Pair<String, (String) -> BoardRenderScene>> = listOf(
            "pieces[0].square" to { value ->
                baseScene.copy(pieces = listOf(BoardRenderPiece('P', value), BoardRenderPiece('R', "h1")))
            },
            "lastMoveHighlight.from" to { value ->
                baseScene.copy(lastMoveHighlight = LastMoveHighlight(value, "e4"))
            },
            "lastMoveHighlight.to" to { value ->
                baseScene.copy(lastMoveHighlight = LastMoveHighlight("e2", value))
            },
            "selectedSquare" to { value -> baseScene.copy(selectedSquare = value) },
            "dragFromSquare" to { value -> baseScene.copy(dragFromSquare = value) },
            "wrongMoveSquare" to { value -> baseScene.copy(wrongMoveSquare = value) },
            "hintSquare" to { value -> baseScene.copy(hintSquare = value) },
            "animatedPieces[0].fromSquare" to { value ->
                baseScene.copy(animatedPieces = listOf(BoardRenderAnimatedPiece(value, Offset.Zero)))
            },
        )
        for ((field, createScene) in fields) {
            for (value in listOf("", "e", "e44", "i1", "a0", "a9", "E4", " e4", "e4 ")) {
                val error = assertThrows(IllegalArgumentException::class.java) {
                    adaptBoardRenderScene(createScene(value))
                }

                assertTrue(error.message.orEmpty().contains(field))
                assertTrue(error.message.orEmpty().contains("'$value'"))
            }
        }
    }

    @Test
    fun propagatesDuplicateOccupiedSquareError() {
        val scene = BoardRenderScene(
            listOf(BoardRenderPiece('P', "e4"), BoardRenderPiece('n', "e4")),
            BoardOrientation.WHITE,
        )

        val error = assertThrows(IllegalArgumentException::class.java) { adaptBoardRenderScene(scene) }

        assertTrue(error.message.orEmpty().contains("Multiple pieces on square e4"))
    }

    @Test
    fun propagatesMissingDraggedPieceError() {
        val scene = BoardRenderScene(emptyList(), BoardOrientation.WHITE, dragFromSquare = "e2")

        val error = assertThrows(IllegalArgumentException::class.java) { adaptBoardRenderScene(scene) }

        assertTrue(error.message.orEmpty().contains("No piece on moving square e2"))
    }

    @Test
    fun propagatesMissingAnimatedPieceError() {
        val scene = BoardRenderScene(
            emptyList(),
            BoardOrientation.BLACK,
            animatedPieces = listOf(BoardRenderAnimatedPiece("h1", Offset.Zero)),
        )

        val error = assertThrows(IllegalArgumentException::class.java) { adaptBoardRenderScene(scene) }

        assertTrue(error.message.orEmpty().contains("No piece on moving square h1"))
    }

    @Test
    fun propagatesRepeatedAnimationSourceError() {
        val scene = BoardRenderScene(
            listOf(BoardRenderPiece('R', "h1")),
            BoardOrientation.WHITE,
            animatedPieces = listOf(
                BoardRenderAnimatedPiece("h1", Offset.Zero),
                BoardRenderAnimatedPiece("h1", Offset(TestSquareSizePx, TestSquareSizePx)),
            ),
        )

        val error = assertThrows(IllegalArgumentException::class.java) { adaptBoardRenderScene(scene) }

        assertTrue(error.message.orEmpty().contains("Multiple moving references to square h1"))
    }

    @Test
    fun propagatesDragAndAnimationConflictError() {
        val scene = BoardRenderScene(
            listOf(BoardRenderPiece('P', "e2")),
            BoardOrientation.WHITE,
            dragFromSquare = "e2",
            animatedPieces = listOf(BoardRenderAnimatedPiece("e2", Offset.Zero)),
        )

        val error = assertThrows(IllegalArgumentException::class.java) { adaptBoardRenderScene(scene) }

        assertTrue(error.message.orEmpty().contains("Multiple moving references to square e2"))
    }

    @Test
    fun propagatesNonFiniteDragCenterErrorsWithAndWithoutDraggedPiece() {
        for (source in listOf(null, "e2")) {
            for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
                for (center in listOf(Offset(value, 0f), Offset(0f, value))) {
                    val scene = BoardRenderScene(
                        listOf(BoardRenderPiece('P', "e2")),
                        BoardOrientation.WHITE,
                        dragFromSquare = source,
                        dragOffset = center,
                    )

                    val error = assertThrows(IllegalArgumentException::class.java) {
                        adaptBoardRenderScene(scene)
                    }

                    assertTrue(error.message.orEmpty().contains("dragOffset"))
                }
            }
        }
    }

    @Test
    fun propagatesNonFiniteAnimationCenterErrors() {
        for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            for (center in listOf(Offset(value, 0f), Offset(0f, value))) {
                val scene = BoardRenderScene(
                    listOf(BoardRenderPiece('R', "h1")),
                    BoardOrientation.BLACK,
                    animatedPieces = listOf(BoardRenderAnimatedPiece("h1", center)),
                )

                val error = assertThrows(IllegalArgumentException::class.java) {
                    adaptBoardRenderScene(scene)
                }

                assertTrue(error.message.orEmpty().contains("animated centerOffset"))
            }
        }
    }

    @Test
    fun adaptsActualSimpleMoveFramesAtStartMidpointAndEndInBothOrientations() {
        val action = AnimateSimpleMoveAction("e2", "e4", LastMoveHighlight("e2", "e4"), 1, 250)
        for (orientation in BoardOrientation.entries) {
            val baseScene = BoardRenderScene(listOf(BoardRenderPiece('P', "e2")), orientation)
            for (progress in listOf(0f, 0.5f, 1f)) {
                val projected = buildAnimatedBoardRenderScene(baseScene, action, progress, TestSquareSizePx)

                val adapted = adaptBoardRenderScene(projected)

                assertEquals(
                    listOf(LibraryBoardRenderPiece(Piece(Side.WHITE, PieceType.PAWN), Square('e', 2))),
                    adapted.pieces,
                )
                assertEquals(Square('e', 2), adapted.dragFromSquare)
                // e2 -> e4 moves upward by two cells when viewed from White.
                assertEquals(centerFromWhiteView(4.5f, 6.5f - 2 * progress, orientation), adapted.dragOffset)
                assertTrue(adapted.animatedPieces.isEmpty())
                assertEquals(
                    listOf(
                        BoardSquareDecoration(Square('e', 2), Color(0xFFFFD600).copy(alpha = 0.55f)),
                        BoardSquareDecoration(Square('e', 4), Color(0xFFFFFF80).copy(alpha = 0.50f)),
                        BoardSquareDecoration(Square('e', 2), Color.Yellow.copy(alpha = 0.4f)),
                    ),
                    adapted.decorations,
                )
            }
        }
    }

    @Test
    fun adaptsActualCaptureFramesWithCapturedPieceAlreadyRemoved() {
        val action = AnimateCaptureMoveAction("e4", "d5", "d5", LastMoveHighlight("e4", "d5"), 2, 250)
        for (orientation in BoardOrientation.entries) {
            val baseScene = BoardRenderScene(
                listOf(BoardRenderPiece('P', "e4"), BoardRenderPiece('p', "d5")),
                orientation,
            )
            for (progress in listOf(0f, 0.5f, 1f)) {
                val projected = buildAnimatedBoardRenderScene(baseScene, action, progress, TestSquareSizePx)

                val adapted = adaptBoardRenderScene(projected)

                assertEquals(
                    listOf(LibraryBoardRenderPiece(Piece(Side.WHITE, PieceType.PAWN), Square('e', 4))),
                    adapted.pieces,
                )
                assertEquals(Square('e', 4), adapted.dragFromSquare)
                // e4 -> d5 moves one cell left and one cell upward in the White view.
                assertEquals(
                    centerFromWhiteView(4.5f - progress, 4.5f - progress, orientation),
                    adapted.dragOffset,
                )
                assertTrue(adapted.animatedPieces.isEmpty())
                assertEquals(
                    listOf(
                        BoardSquareDecoration(Square('e', 4), Color(0xFFFFD600).copy(alpha = 0.55f)),
                        BoardSquareDecoration(Square('d', 5), Color(0xFFFFFF80).copy(alpha = 0.50f)),
                        BoardSquareDecoration(Square('e', 4), Color.Yellow.copy(alpha = 0.4f)),
                    ),
                    adapted.decorations,
                )
            }
        }
    }

    @Test
    fun adaptsActualCastlingFramesPreservingKingThenRookOrder() {
        val action = AnimateCastlingMoveAction(
            "e1", "g1", "h1", "f1", LastMoveHighlight("e1", "g1"), 1, 250,
        )
        for (orientation in BoardOrientation.entries) {
            val baseScene = BoardRenderScene(
                listOf(BoardRenderPiece('K', "e1"), BoardRenderPiece('R', "h1")),
                orientation,
            )
            for (progress in listOf(0f, 0.5f, 1f)) {
                val projected = buildAnimatedBoardRenderScene(baseScene, action, progress, TestSquareSizePx)

                val adapted = adaptBoardRenderScene(projected)

                assertEquals(
                    listOf(
                        LibraryBoardRenderPiece(Piece(Side.WHITE, PieceType.KING), Square('e', 1)),
                        LibraryBoardRenderPiece(Piece(Side.WHITE, PieceType.ROOK), Square('h', 1)),
                    ),
                    adapted.pieces,
                )
                assertNull(adapted.dragFromSquare)
                assertEquals(Offset.Zero, adapted.dragOffset)
                // The king moves two cells right, while the rook moves two cells left.
                assertEquals(
                    listOf(
                        LibraryBoardRenderAnimatedPiece(
                            Square('e', 1), centerFromWhiteView(4.5f + 2 * progress, 7.5f, orientation),
                        ),
                        LibraryBoardRenderAnimatedPiece(
                            Square('h', 1), centerFromWhiteView(7.5f - 2 * progress, 7.5f, orientation),
                        ),
                    ),
                    adapted.animatedPieces,
                )
                assertEquals(
                    listOf(
                        BoardSquareDecoration(Square('e', 1), Color(0xFFFFD600).copy(alpha = 0.55f)),
                        BoardSquareDecoration(Square('g', 1), Color(0xFFFFFF80).copy(alpha = 0.50f)),
                    ),
                    adapted.decorations,
                )
            }
        }
    }

    // Inputs are cell units from the White view's top left; Black rotates the center by 180 degrees.
    private fun centerFromWhiteView(columnCenter: Float, rowCenter: Float, orientation: BoardOrientation): Offset {
        if (orientation == BoardOrientation.BLACK) {
            return Offset((8 - columnCenter) * TestSquareSizePx, (8 - rowCenter) * TestSquareSizePx)
        }
        return Offset(columnCenter * TestSquareSizePx, rowCenter * TestSquareSizePx)
    }
}
