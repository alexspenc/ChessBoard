package com.example.chessboard.ui.screen.trainSingleLine

/**
 * Focused JVM coverage for TrainSingleLine active-training animation planning.
 * Keep only happy-path planning rules for user move plus optional forced reply here.
 * Do not add Compose UI tests, queue-engine tests, or replay-screen coverage to this file.
 * Validation date: 2026-07-11
 */

import com.example.chessboard.boardmodel.LastMoveHighlight
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.boardmodel.PromotionPiece
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.boardanimation.AnimateCaptureMoveAction
import com.example.chessboard.ui.boardanimation.AnimateSimpleMoveAction
import com.example.chessboard.ui.boardanimation.DefaultBoardMoveAnimationDurationMs
import com.example.chessboard.ui.boardrender.buildBoardRenderScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainSingleLineBoardAnimationTest {

    @Test
    fun isTrainSingleLineCorrectUserMove_recognizesPromotionSuffix() {
        val uciMoves = listOf(
            "e2e4",
            "c7c5",
            "e4e5",
            "d7d6",
            "e5e6",
            "b8c6",
            "e6f7",
            "e8d7",
            "f7g8q",
        )
        val lineController = LineController(BoardOrientation.WHITE)
        lineController.loadFromUciMoves(uciMoves, targetPly = 8)
        assertTrue(lineController.setStartSquare("f7"))
        assertTrue(lineController.tryMoveWithPromotion("g8", PromotionPiece.QUEEN))

        val isCorrectMove = isTrainSingleLineCorrectUserMove(
            uiState = TrainSingleLineUiState(
                phase = TrainSingleLinePhase.Training,
                expectedPly = 8,
            ),
            lineController = lineController,
            uciMoves = uciMoves,
            currentOrientation = BoardOrientation.WHITE,
        )

        assertTrue(isCorrectMove)
    }

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
                    durationMs = DefaultBoardMoveAnimationDurationMs,
                ),
                AnimateSimpleMoveAction(
                    from = "e7",
                    to = "e5",
                    lastMoveHighlight = LastMoveHighlight(from = "e7", to = "e5"),
                    logicalPlyAfter = 2,
                    durationMs = DefaultBoardMoveAnimationDurationMs,
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

    @Test
    fun buildTrainSingleLineProgressAnimationActions_returnsCaptureUserMoveAndForcedReply() {
        val lineController = LineController(BoardOrientation.WHITE)
        lineController.loadFromUciMoves(listOf("e2e4", "d7d5"), 2)
        val uiState = TrainSingleLineUiState(
            phase = TrainSingleLinePhase.Training,
            expectedPly = 2,
        )
        val scene = buildBoardRenderScene(
            position = lineController.getBoardPosition(),
            orientation = lineController.getSide(),
        )

        lineController.tryMove("e4", "d5")

        val actions = buildTrainSingleLineProgressAnimationActions(
            scene = scene,
            uiState = uiState,
            lineController = lineController,
            uciMoves = listOf("e2e4", "d7d5", "e4d5", "g8f6"),
            currentOrientation = BoardOrientation.WHITE,
            hasMoveCap = false,
        )

        assertEquals(
            listOf(
                AnimateCaptureMoveAction(
                    from = "e4",
                    to = "d5",
                    capturedSquare = "d5",
                    lastMoveHighlight = LastMoveHighlight(from = "e4", to = "d5"),
                    logicalPlyAfter = 3,
                    durationMs = DefaultBoardMoveAnimationDurationMs,
                ),
                AnimateSimpleMoveAction(
                    from = "g8",
                    to = "f6",
                    lastMoveHighlight = LastMoveHighlight(from = "g8", to = "f6"),
                    logicalPlyAfter = 4,
                    durationMs = DefaultBoardMoveAnimationDurationMs,
                ),
            ),
            actions,
        )
    }

    @Test
    fun buildTrainSingleLineProgressAnimationActions_returnsQuietUserMoveAndForcedReplyCapture() {
        val lineController = LineController(BoardOrientation.WHITE)
        lineController.loadFromUciMoves(listOf("g1f3"), 1)
        val uiState = TrainSingleLineUiState(
            phase = TrainSingleLinePhase.Training,
            expectedPly = 1,
        )
        val scene = buildBoardRenderScene(
            position = lineController.getBoardPosition(),
            orientation = lineController.getSide(),
        )

        lineController.tryMove("e7", "e5")

        val actions = buildTrainSingleLineProgressAnimationActions(
            scene = scene,
            uiState = uiState,
            lineController = lineController,
            uciMoves = listOf("g1f3", "e7e5", "f3e5"),
            currentOrientation = BoardOrientation.BLACK,
            hasMoveCap = false,
        )

        assertEquals(
            listOf(
                AnimateSimpleMoveAction(
                    from = "e7",
                    to = "e5",
                    lastMoveHighlight = LastMoveHighlight(from = "e7", to = "e5"),
                    logicalPlyAfter = 2,
                    durationMs = DefaultBoardMoveAnimationDurationMs,
                ),
                AnimateCaptureMoveAction(
                    from = "f3",
                    to = "e5",
                    capturedSquare = "e5",
                    lastMoveHighlight = LastMoveHighlight(from = "f3", to = "e5"),
                    logicalPlyAfter = 3,
                    durationMs = DefaultBoardMoveAnimationDurationMs,
                ),
            ),
            actions,
        )
    }
}
