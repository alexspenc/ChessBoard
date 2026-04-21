package com.example.chessboard.ui.screen.positions

/**
 * Unit coverage for saved-position board preview helpers.
 *
 * Keep pure helper tests here. Do not add Compose rendering, Room, or navigation coverage to this file.
 */
import com.example.chessboard.boardmodel.InitialBoardFen
import com.example.chessboard.ui.BoardOrientation
import org.junit.Assert.assertEquals
import org.junit.Test

class SavedPositionBoardPreviewTest {

    @Test
    fun `resolveSavedPositionBoardOrientation returns white for white to move`() {
        val position = savedPosition(fenFull = InitialBoardFen)

        val orientation = resolveSavedPositionBoardOrientation(position)

        assertEquals(BoardOrientation.WHITE, orientation)
    }

    @Test
    fun `resolveSavedPositionBoardOrientation returns black for black to move`() {
        val position = savedPosition(
            fenFull = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 0 1"
        )

        val orientation = resolveSavedPositionBoardOrientation(position)

        assertEquals(BoardOrientation.BLACK, orientation)
    }

    @Test
    fun `toLoadableSavedPositionFen keeps complete fen unchanged`() {
        val fen = InitialBoardFen

        val loadableFen = toLoadableSavedPositionFen(fen)

        assertEquals(fen, loadableFen)
    }

    @Test
    fun `toLoadableSavedPositionFen adds en passant and counters to search fen`() {
        val fenForSearch = "8/8/8/8/8/8/8/8 b -"

        val loadableFen = toLoadableSavedPositionFen(fenForSearch)

        assertEquals("8/8/8/8/8/8/8/8 b - - 0 1", loadableFen)
    }

    private fun savedPosition(
        fenForSearch: String = InitialBoardFen,
        fenFull: String? = InitialBoardFen,
    ): SavedPositionListItem {
        return SavedPositionListItem(
            id = 1L,
            name = "Preview",
            fenForSearch = fenForSearch,
            fenFull = fenFull,
        )
    }
}
