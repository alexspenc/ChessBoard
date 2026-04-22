package com.example.chessboard.ui.screen.analysis

import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.boardmodel.GameVariationLineState
import org.junit.Assert.assertEquals
import org.junit.Test

class GameAnalysisStateTest {

    @Test
    fun `resolveNextVariationState records played move`() {
        val gameController = GameController()

        gameController.tryMove("e2", "e4")
        val state = resolveNextVariationState(
            variationState = GameVariationLineState(),
            gameController = gameController,
        )

        assertEquals(listOf(listOf("e2e4")), state.lines)
        assertEquals(listOf("e2e4"), state.currentPath)
    }

    @Test
    fun `resolveNextVariationState keeps line after undo`() {
        val gameController = GameController()
        gameController.tryMove("e2", "e4")
        gameController.undoMove()

        val state = resolveNextVariationState(
            variationState = GameVariationLineState(),
            gameController = gameController,
        )

        assertEquals(listOf(listOf("e2e4")), state.lines)
        assertEquals(emptyList<String>(), state.currentPath)
    }

    @Test
    fun `resolveNextVariationState creates branch from middle`() {
        val gameController = GameController()
        gameController.tryMove("e2", "e4")
        gameController.tryMove("e7", "e5")
        gameController.tryMove("g1", "f3")
        val firstLineState = resolveNextVariationState(
            variationState = GameVariationLineState(),
            gameController = gameController,
        )

        gameController.undoMove()
        gameController.tryMove("d2", "d4")
        val branchedState = resolveNextVariationState(
            variationState = firstLineState,
            gameController = gameController,
        )

        assertEquals(
            listOf(
                listOf("e2e4", "e7e5", "g1f3"),
                listOf("e2e4", "e7e5", "d2d4"),
            ),
            branchedState.lines,
        )
        assertEquals(listOf("e2e4", "e7e5", "d2d4"), branchedState.currentPath)
    }
}
