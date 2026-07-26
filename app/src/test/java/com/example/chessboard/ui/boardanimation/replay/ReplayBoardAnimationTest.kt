package com.example.chessboard.ui.boardanimation.replay

/**
 * Focused JVM coverage for shared replay-board playback helpers.
 * Keep classifier, reset, and forward-navigation checks for the common replay layer here.
 * Do not add Compose UI tests, screen-specific policies, or queue-engine behavior to this file.
 * Validation date: 2026-07-10
 */

import com.example.chessboard.boardmodel.LastMoveHighlight
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.boardanimation.AnimateCaptureMoveAction
import com.example.chessboard.ui.boardanimation.AnimateCastlingMoveAction
import com.example.chessboard.ui.boardanimation.AnimateSimpleMoveAction
import com.example.chessboard.ui.boardanimation.ApplyBoardSceneAction
import com.example.chessboard.ui.boardanimation.BoardAnimationQueueController
import com.example.chessboard.ui.boardanimation.DefaultBoardMoveAnimationDurationMs
import com.example.chessboard.ui.boardanimation.ResetBoardSceneAction
import com.example.chessboard.ui.boardrender.BoardRenderPiece
import com.example.chessboard.ui.boardrender.BoardRenderScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReplayBoardAnimationTest {

    @Test
    fun buildReplayAnimatedMoveActionOrNull_returnsActionForQuietMove() {
        val action = buildReplayAnimatedMoveActionOrNull(
            scene = buildScene(
                pieces = listOf(
                    BoardRenderPiece(letter = 'P', square = "e2"),
                ),
            ),
            moveUci = "e2e4",
            logicalPlyAfter = 1,
            durationMs = DefaultBoardMoveAnimationDurationMs,
        )

        assertEquals(
            AnimateSimpleMoveAction(
                from = "e2",
                to = "e4",
                lastMoveHighlight = LastMoveHighlight(from = "e2", to = "e4"),
                logicalPlyAfter = 1,
                durationMs = DefaultBoardMoveAnimationDurationMs,
            ),
            action,
        )
    }

    @Test
    fun buildReplayAnimatedMoveActionOrNull_returnsCaptureActionForOccupiedTarget() {
        val action = buildReplayAnimatedMoveActionOrNull(
            scene = buildScene(
                pieces = listOf(
                    BoardRenderPiece(letter = 'P', square = "e4"),
                    BoardRenderPiece(letter = 'p', square = "d5"),
                ),
            ),
            moveUci = "e4d5",
            logicalPlyAfter = 2,
            durationMs = DefaultBoardMoveAnimationDurationMs,
        )

        assertEquals(
            AnimateCaptureMoveAction(
                from = "e4",
                to = "d5",
                capturedSquare = "d5",
                lastMoveHighlight = LastMoveHighlight(from = "e4", to = "d5"),
                logicalPlyAfter = 2,
                durationMs = DefaultBoardMoveAnimationDurationMs,
            ),
            action,
        )
    }

    @Test
    fun buildReplayForwardPlaybackActionOrNull_returnsInstantTransitionForPromotion() {
        val targetScene = buildScene(
            pieces = listOf(
                BoardRenderPiece(letter = 'Q', square = "e8"),
            ),
        )
        val action = buildReplayForwardPlaybackActionOrNull(
            sourceScene = buildScene(
                pieces = listOf(
                    BoardRenderPiece(letter = 'P', square = "e7"),
                ),
            ),
            targetScene = targetScene,
            moveUci = "e7e8q",
            logicalPlyAfter = 1,
            durationMs = DefaultBoardMoveAnimationDurationMs,
        )

        assertEquals(
            ApplyBoardSceneAction(
                scene = targetScene,
                logicalPlyAfter = 1,
                durationMs = DefaultBoardMoveAnimationDurationMs,
            ),
            action,
        )
    }

    @Test
    fun buildReplayForwardPlaybackActionOrNull_returnsInstantTransitionForPromotionCapture() {
        val targetScene = buildScene(
            pieces = listOf(
                BoardRenderPiece(letter = 'Q', square = "d8"),
            ),
        )
        val action = buildReplayForwardPlaybackActionOrNull(
            sourceScene = buildScene(
                pieces = listOf(
                    BoardRenderPiece(letter = 'P', square = "e7"),
                    BoardRenderPiece(letter = 'r', square = "d8"),
                ),
            ),
            targetScene = targetScene,
            moveUci = "e7d8q",
            logicalPlyAfter = 1,
            durationMs = DefaultBoardMoveAnimationDurationMs,
        )

        assertEquals(
            ApplyBoardSceneAction(
                scene = targetScene,
                logicalPlyAfter = 1,
                durationMs = DefaultBoardMoveAnimationDurationMs,
            ),
            action,
        )
    }

    @Test
    fun buildReplayAnimatedMoveActionOrNull_returnsActionForEveryStandardCastlingMove() {
        data class CastlingCase(
            val kingLetter: Char,
            val rookLetter: Char,
            val from: String,
            val to: String,
            val rookFrom: String,
            val rookTo: String,
        )

        val cases = listOf(
            CastlingCase(
                kingLetter = 'K',
                rookLetter = 'R',
                from = "e1",
                to = "g1",
                rookFrom = "h1",
                rookTo = "f1",
            ),
            CastlingCase(
                kingLetter = 'K',
                rookLetter = 'R',
                from = "e1",
                to = "c1",
                rookFrom = "a1",
                rookTo = "d1",
            ),
            CastlingCase(
                kingLetter = 'k',
                rookLetter = 'r',
                from = "e8",
                to = "g8",
                rookFrom = "h8",
                rookTo = "f8",
            ),
            CastlingCase(
                kingLetter = 'k',
                rookLetter = 'r',
                from = "e8",
                to = "c8",
                rookFrom = "a8",
                rookTo = "d8",
            ),
        )

        cases.forEach { castlingCase ->
            val action = buildReplayAnimatedMoveActionOrNull(
                scene = buildScene(
                    pieces = listOf(
                        BoardRenderPiece(letter = castlingCase.kingLetter, square = castlingCase.from),
                        BoardRenderPiece(
                            letter = castlingCase.rookLetter,
                            square = castlingCase.rookFrom,
                        ),
                    ),
                ),
                moveUci = castlingCase.from + castlingCase.to,
                logicalPlyAfter = 1,
                durationMs = DefaultBoardMoveAnimationDurationMs,
            )

            assertEquals(
                AnimateCastlingMoveAction(
                    from = castlingCase.from,
                    to = castlingCase.to,
                    rookFrom = castlingCase.rookFrom,
                    rookTo = castlingCase.rookTo,
                    lastMoveHighlight = LastMoveHighlight(
                        from = castlingCase.from,
                        to = castlingCase.to,
                    ),
                    logicalPlyAfter = 1,
                    durationMs = DefaultBoardMoveAnimationDurationMs,
                ),
                action,
            )
        }
    }

    @Test
    fun buildReplayAnimatedMoveActionOrNull_returnsNullWhenStandardCastlingRookIsMissing() {
        val action = buildReplayAnimatedMoveActionOrNull(
            scene = buildScene(
                pieces = listOf(BoardRenderPiece(letter = 'K', square = "e1")),
            ),
            moveUci = "e1g1",
            logicalPlyAfter = 1,
            durationMs = DefaultBoardMoveAnimationDurationMs,
        )

        assertNull(action)
    }

    @Test
    fun buildReplayAnimatedMoveActionOrNull_doesNotClassifyNonStandardKingMoveAsCastling() {
        val action = buildReplayAnimatedMoveActionOrNull(
            scene = buildScene(
                pieces = listOf(BoardRenderPiece(letter = 'K', square = "d1")),
            ),
            moveUci = "d1f1",
            logicalPlyAfter = 1,
            durationMs = DefaultBoardMoveAnimationDurationMs,
        )

        assertEquals(
            AnimateSimpleMoveAction(
                from = "d1",
                to = "f1",
                lastMoveHighlight = LastMoveHighlight(from = "d1", to = "f1"),
                logicalPlyAfter = 1,
                durationMs = DefaultBoardMoveAnimationDurationMs,
            ),
            action,
        )
    }

    @Test
    fun buildReplayForwardPlaybackActionOrNull_returnsInstantTransitionForEnPassant() {
        val targetScene = buildScene(
            pieces = listOf(
                BoardRenderPiece(letter = 'P', square = "d6"),
            ),
        )
        val action = buildReplayForwardPlaybackActionOrNull(
            sourceScene = buildScene(
                pieces = listOf(
                    BoardRenderPiece(letter = 'P', square = "e5"),
                    BoardRenderPiece(letter = 'p', square = "d5"),
                ),
            ),
            targetScene = targetScene,
            moveUci = "e5d6",
            logicalPlyAfter = 1,
            durationMs = DefaultBoardMoveAnimationDurationMs,
        )

        assertEquals(
            ApplyBoardSceneAction(
                scene = targetScene,
                logicalPlyAfter = 1,
                durationMs = DefaultBoardMoveAnimationDurationMs,
            ),
            action,
        )
    }

    @Test
    fun moveReplayBoardForward_queuesInstantPromotionTransition() {
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
        val boardAnimationController = BoardAnimationQueueController()
        boardAnimationController.submit(
            ResetBoardSceneAction(
                scene = buildReplayBoardRenderScene(lineController),
                renderPly = 8,
            )
        )

        val wasMoved = moveReplayBoardForward(
            uciMoves = uciMoves,
            lineController = lineController,
            boardAnimationController = boardAnimationController,
        )

        assertEquals(true, wasMoved)
        assertEquals(9, lineController.currentMoveIndex)
        assertEquals(
            buildReplayBoardRenderScene(lineController),
            (boardAnimationController.state.activeAction as ApplyBoardSceneAction).scene,
        )
    }

    private fun buildScene(
        pieces: List<BoardRenderPiece>,
    ): BoardRenderScene {
        return BoardRenderScene(
            pieces = pieces,
            orientation = BoardOrientation.WHITE,
        )
    }
}
