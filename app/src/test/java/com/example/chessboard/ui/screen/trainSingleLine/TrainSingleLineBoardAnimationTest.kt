package com.example.chessboard.ui.screen.trainSingleLine

/**
 * Focused JVM coverage for TrainSingleLine active-training animation planning.
 * Keep only happy-path planning rules for user move plus optional forced reply here.
 * Do not add Compose UI tests, queue-engine tests, or replay-screen coverage to this file.
 * Validation date: 2026-07-11
 */

import com.example.chessboard.boardmodel.LastMoveHighlight
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.boardanimation.AnimateSimpleMoveAction
import com.example.chessboard.ui.boardrender.buildBoardRenderScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrainSingleLineBoardAnimationTest {

    @Test
    fun buildTrainSingleLineProgressAnimationActions_returnsUserMoveAndForcedReply() {
        val lineController = LineController(BoardOrientation.WHITE)
        val uiState = TrainSingleLineUiState(
            phase = TrainSingleLinePhase.Training,
            expectedPly = 0,
        )
        val scene = buildBoardRenderScene(
            position = lineController.getBoardPosition(),
            orientation = lineController.getSide(),
        )

        lineController.tryMove("e2", "e4")

        val actions = buildTrainSingleLineProgressAnimationActions(
            scene = scene,
            uiState = uiState,
            lineController = lineController,
            uciMoves = listOf("e2e4", "e7e5"),
            currentOrientation = BoardOrientation.WHITE,
            hasMoveCap = false,
        )

        assertEquals(
            listOf(
                AnimateSimpleMoveAction(
                    from = "e2",
                    to = "e4",
                    lastMoveHighlight = LastMoveHighlight(from = "e2", to = "e4"),
                    logicalPlyAfter = 1,
                    durationMs = TrainSingleLineSimpleMoveDurationMs,
                ),
                AnimateSimpleMoveAction(
                    from = "e7",
                    to = "e5",
                    lastMoveHighlight = LastMoveHighlight(from = "e7", to = "e5"),
                    logicalPlyAfter = 2,
                    durationMs = TrainSingleLineSimpleMoveDurationMs,
                ),
            ),
            actions,
        )
    }

    @Test
    fun buildTrainSingleLineProgressAnimationActions_returnsOnlyUserMoveWhenMoveCapSkipsReply() {
        val lineController = LineController(BoardOrientation.WHITE)
        val uiState = TrainSingleLineUiState(
            phase = TrainSingleLinePhase.Training,
            expectedPly = 0,
        )
        val scene = buildBoardRenderScene(
            position = lineController.getBoardPosition(),
            orientation = lineController.getSide(),
        )

        lineController.tryMove("e2", "e4")

        val actions = buildTrainSingleLineProgressAnimationActions(
            scene = scene,
            uiState = uiState,
            lineController = lineController,
            uciMoves = listOf("e2e4", "e7e5"),
            currentOrientation = BoardOrientation.WHITE,
            hasMoveCap = true,
        )

        assertEquals(1, actions?.size)
        assertEquals("e2", actions?.single()?.from)
        assertEquals("e4", actions?.single()?.to)
    }

    @Test
    fun buildTrainSingleLineProgressAnimationActions_returnsNullWhenForcedReplyIsNotSimple() {
        val lineController = LineController(BoardOrientation.WHITE)
        val uiState = TrainSingleLineUiState(
            phase = TrainSingleLinePhase.Training,
            expectedPly = 0,
        )
        val scene = buildBoardRenderScene(
            position = lineController.getBoardPosition(),
            orientation = lineController.getSide(),
        )

        lineController.tryMove("e2", "e4")

        val actions = buildTrainSingleLineProgressAnimationActions(
            scene = scene,
            uiState = uiState,
            lineController = lineController,
            uciMoves = listOf("e2e4", "e7e8q"),
            currentOrientation = BoardOrientation.WHITE,
            hasMoveCap = false,
        )

        assertNull(actions)
    }
}
