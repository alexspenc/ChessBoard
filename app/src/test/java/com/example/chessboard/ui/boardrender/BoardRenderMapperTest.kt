package com.example.chessboard.ui.boardrender

/**
 * Locks the step-1 board render-scene contract in place.
 * Keep focused mapper tests here for translating boardmodel snapshots into render input state.
 * Do not add animation queue tests or screen-level Compose behavior to this file.
 * Validation date: 2026-07-10
 */

import androidx.compose.ui.geometry.Offset
import com.example.chessboard.boardmodel.BoardPiece
import com.example.chessboard.boardmodel.BoardPosition
import com.example.chessboard.boardmodel.LastMoveHighlight
import com.example.chessboard.ui.BoardOrientation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoardRenderMapperTest {

    @Test
    fun buildBoardRenderScene_mapsBoardPiecesIntoRenderPieces() {
        val position = BoardPosition(
            pieces = listOf(
                BoardPiece(letter = 'K', field = "e1"),
                BoardPiece(letter = 'p', field = "d5"),
            ),
        )

        val scene = buildBoardRenderScene(
            position = position,
            orientation = BoardOrientation.WHITE,
        )

        assertEquals(
            listOf(
                BoardRenderPiece(letter = 'K', square = "e1"),
                BoardRenderPiece(letter = 'p', square = "d5"),
            ),
            scene.pieces,
        )
        assertEquals(BoardOrientation.WHITE, scene.orientation)
        assertNull(scene.lastMoveHighlight)
        assertNull(scene.selectedSquare)
        assertNull(scene.dragFromSquare)
        assertEquals(Offset.Zero, scene.dragOffset)
        assertEquals(emptyList<BoardRenderAnimatedPiece>(), scene.animatedPieces)
        assertNull(scene.wrongMoveSquare)
        assertNull(scene.hintSquare)
    }

    @Test
    fun buildBoardRenderScene_keepsBoardOverlayMetadata() {
        val lastMoveHighlight = LastMoveHighlight(from = "e2", to = "e4")

        val scene = buildBoardRenderScene(
            position = BoardPosition(pieces = emptyList()),
            orientation = BoardOrientation.BLACK,
            lastMoveHighlight = lastMoveHighlight,
            selectedSquare = "c6",
            dragFromSquare = "g7",
            dragOffset = Offset(12f, 18f),
            wrongMoveSquare = "b4",
            hintSquare = "h5",
        )

        assertEquals(BoardOrientation.BLACK, scene.orientation)
        assertEquals(lastMoveHighlight, scene.lastMoveHighlight)
        assertEquals("c6", scene.selectedSquare)
        assertEquals("g7", scene.dragFromSquare)
        assertEquals(Offset(12f, 18f), scene.dragOffset)
        assertEquals("b4", scene.wrongMoveSquare)
        assertEquals("h5", scene.hintSquare)
    }
}
