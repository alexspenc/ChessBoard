package com.example.chessboard.ui.screen.trainSingleLine

/**
 * Focused JVM coverage for TrainSingleLine active-training playback planning.
 * Keep happy-path planning rules for animated and instant user moves plus optional forced replies here.
 * Do not add Compose UI tests, queue-engine tests, or replay-screen coverage to this file.
 * Validation date: 2026-07-11
 */

import com.example.chessboard.boardmodel.LastMoveHighlight
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.boardmodel.PromotionPiece
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.boardanimation.AnimateCaptureMoveAction
import com.example.chessboard.ui.boardanimation.AnimateCastlingMoveAction
import com.example.chessboard.ui.boardanimation.AnimateSimpleMoveAction
import com.example.chessboard.ui.boardanimation.ApplyBoardSceneAction
import com.example.chessboard.ui.boardanimation.DefaultBoardMoveAnimationDurationMs
import com.example.chessboard.ui.boardrender.BoardRenderScene
import com.example.chessboard.ui.boardrender.buildBoardRenderScene
import org.junit.Assert.assertEquals
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
    fun buildTrainSingleLineProgressPlaybackActions_returnsUserMoveAndForcedReply() {
        val uciMoves = listOf("e2e4", "e7e5")
        val uiState = TrainSingleLineUiState(
            phase = TrainSingleLinePhase.Training,
            expectedPly = 0,
        )

        val actions = buildTrainSingleLineProgressPlaybackActions(
            sceneBeforeUserMove = buildSceneAtPly(uciMoves, targetPly = 0),
            sceneAfterUserMove = buildSceneAtPly(uciMoves, targetPly = 1),
            sceneAfterProgress = buildSceneAtPly(uciMoves, targetPly = 2),
            uiState = uiState,
            uciMoves = uciMoves,
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
    fun buildTrainSingleLineProgressPlaybackActions_returnsOnlyUserMoveWhenMoveCapSkipsReply() {
        val uciMoves = listOf("e2e4", "e7e5")
        val uiState = TrainSingleLineUiState(
            phase = TrainSingleLinePhase.Training,
            expectedPly = 0,
        )

        val actions = buildTrainSingleLineProgressPlaybackActions(
            sceneBeforeUserMove = buildSceneAtPly(uciMoves, targetPly = 0),
            sceneAfterUserMove = buildSceneAtPly(uciMoves, targetPly = 1),
            sceneAfterProgress = buildSceneAtPly(uciMoves, targetPly = 1),
            uiState = uiState,
            uciMoves = uciMoves,
            currentOrientation = BoardOrientation.WHITE,
            hasMoveCap = true,
        )

        assertEquals(
            listOf(
                AnimateSimpleMoveAction(
                    from = "e2",
                    to = "e4",
                    lastMoveHighlight = LastMoveHighlight(from = "e2", to = "e4"),
                    logicalPlyAfter = 1,
                    durationMs = DefaultBoardMoveAnimationDurationMs,
                )
            ),
            actions,
        )
    }

    @Test
    fun buildTrainSingleLineProgressPlaybackActions_returnsInstantUserPromotion() {
        val uciMoves = PromotionUciMoves
        val uiState = TrainSingleLineUiState(
            phase = TrainSingleLinePhase.Training,
            expectedPly = 8,
        )
        val targetScene = buildSceneAtPly(uciMoves, targetPly = 9)

        val actions = buildTrainSingleLineProgressPlaybackActions(
            sceneBeforeUserMove = buildSceneAtPly(uciMoves, targetPly = 8),
            sceneAfterUserMove = targetScene,
            sceneAfterProgress = targetScene,
            uiState = uiState,
            uciMoves = uciMoves,
            currentOrientation = BoardOrientation.WHITE,
            hasMoveCap = false,
        )

        assertEquals(
            listOf(
                ApplyBoardSceneAction(
                    scene = targetScene,
                    logicalPlyAfter = 9,
                    durationMs = DefaultBoardMoveAnimationDurationMs,
                )
            ),
            actions,
        )
    }

    @Test
    fun buildTrainSingleLineProgressPlaybackActions_returnsAnimatedUserMoveAndInstantPromotionReply() {
        val uciMoves = PromotionUciMoves
        val uiState = TrainSingleLineUiState(
            phase = TrainSingleLinePhase.Training,
            expectedPly = 7,
        )
        val sceneAfterProgress = buildSceneAtPly(
            uciMoves = uciMoves,
            targetPly = 9,
            orientation = BoardOrientation.BLACK,
        )

        val actions = buildTrainSingleLineProgressPlaybackActions(
            sceneBeforeUserMove = buildSceneAtPly(
                uciMoves = uciMoves,
                targetPly = 7,
                orientation = BoardOrientation.BLACK,
            ),
            sceneAfterUserMove = buildSceneAtPly(
                uciMoves = uciMoves,
                targetPly = 8,
                orientation = BoardOrientation.BLACK,
            ),
            sceneAfterProgress = sceneAfterProgress,
            uiState = uiState,
            uciMoves = uciMoves,
            currentOrientation = BoardOrientation.BLACK,
            hasMoveCap = false,
        )

        assertEquals(
            listOf(
                AnimateSimpleMoveAction(
                    from = "e8",
                    to = "d7",
                    lastMoveHighlight = LastMoveHighlight(from = "e8", to = "d7"),
                    logicalPlyAfter = 8,
                    durationMs = DefaultBoardMoveAnimationDurationMs,
                ),
                ApplyBoardSceneAction(
                    scene = sceneAfterProgress,
                    logicalPlyAfter = 9,
                    durationMs = DefaultBoardMoveAnimationDurationMs,
                ),
            ),
            actions,
        )
    }

    @Test
    fun buildTrainSingleLineProgressPlaybackActions_returnsCaptureUserMoveAndForcedReply() {
        val uciMoves = listOf("e2e4", "d7d5", "e4d5", "g8f6")
        val uiState = TrainSingleLineUiState(
            phase = TrainSingleLinePhase.Training,
            expectedPly = 2,
        )

        val actions = buildTrainSingleLineProgressPlaybackActions(
            sceneBeforeUserMove = buildSceneAtPly(uciMoves, targetPly = 2),
            sceneAfterUserMove = buildSceneAtPly(uciMoves, targetPly = 3),
            sceneAfterProgress = buildSceneAtPly(uciMoves, targetPly = 4),
            uiState = uiState,
            uciMoves = uciMoves,
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
    fun buildTrainSingleLineProgressPlaybackActions_returnsInstantEnPassantMove() {
        val uciMoves = listOf("e2e4", "a7a6", "e4e5", "d7d5", "e5d6")
        val uiState = TrainSingleLineUiState(
            phase = TrainSingleLinePhase.Training,
            expectedPly = 4,
        )
        val targetScene = buildSceneAtPly(uciMoves, targetPly = 5)

        val actions = buildTrainSingleLineProgressPlaybackActions(
            sceneBeforeUserMove = buildSceneAtPly(uciMoves, targetPly = 4),
            sceneAfterUserMove = targetScene,
            sceneAfterProgress = targetScene,
            uiState = uiState,
            uciMoves = uciMoves,
            currentOrientation = BoardOrientation.WHITE,
            hasMoveCap = false,
        )

        assertEquals(
            listOf(
                ApplyBoardSceneAction(
                    scene = targetScene,
                    logicalPlyAfter = 5,
                    durationMs = DefaultBoardMoveAnimationDurationMs,
                )
            ),
            actions,
        )
    }

    @Test
    fun buildTrainSingleLineProgressPlaybackActions_returnsAnimatedUserCastlingMove() {
        val uciMoves = listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "g8f6", "e1g1")
        val uiState = TrainSingleLineUiState(
            phase = TrainSingleLinePhase.Training,
            expectedPly = 6,
        )
        val targetScene = buildSceneAtPly(uciMoves, targetPly = 7)

        val actions = buildTrainSingleLineProgressPlaybackActions(
            sceneBeforeUserMove = buildSceneAtPly(uciMoves, targetPly = 6),
            sceneAfterUserMove = targetScene,
            sceneAfterProgress = targetScene,
            uiState = uiState,
            uciMoves = uciMoves,
            currentOrientation = BoardOrientation.WHITE,
            hasMoveCap = false,
        )

        assertEquals(
            listOf(
                AnimateCastlingMoveAction(
                    from = "e1",
                    to = "g1",
                    rookFrom = "h1",
                    rookTo = "f1",
                    lastMoveHighlight = LastMoveHighlight(from = "e1", to = "g1"),
                    logicalPlyAfter = 7,
                    durationMs = DefaultBoardMoveAnimationDurationMs,
                )
            ),
            actions,
        )
    }

    private fun buildSceneAtPly(
        uciMoves: List<String>,
        targetPly: Int,
        orientation: BoardOrientation = BoardOrientation.WHITE,
    ): BoardRenderScene {
        val lineController = LineController(orientation)
        lineController.loadFromUciMoves(uciMoves, targetPly = targetPly)
        return buildBoardRenderScene(
            position = lineController.getBoardPosition(),
            orientation = lineController.getSide(),
            lastMoveHighlight = lineController.getLastMoveHighlight(),
        )
    }

    private companion object {
        val PromotionUciMoves = listOf(
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
    }
}
