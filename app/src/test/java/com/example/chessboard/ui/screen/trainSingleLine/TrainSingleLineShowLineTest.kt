package com.example.chessboard.ui.screen.trainSingleLine

/*
 * File role: verifies TrainSingleLine show-line timing bounds and playback orchestration.
 * Keep delay invariants, callback ordering, playback completion, and failed-forward tests here.
 * Do not add Compose rendering, generic queue-engine coverage, or active-training move validation.
 * Validation date: 2026-07-25
 */

import com.example.chessboard.ui.boardanimation.DefaultBoardMoveAnimationDurationMs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainSingleLineShowLineTest {

    @Test
    fun showLineMoveDelayBoundsStayAboveAnimationDuration() {
        assertTrue(MinShowLineMoveDelayMs >= 100L)
        assertTrue(MinShowLineMoveDelayMs > DefaultBoardMoveAnimationDurationMs.toLong())
        assertTrue(ShowLineMoveDelayMs in MinShowLineMoveDelayMs..MaxShowLineMoveDelayMs)
    }

    @Test
    fun resolveShowLineMoveDelayMsClampsValuesToValidBounds() {
        assertEquals(MinShowLineMoveDelayMs, resolveShowLineMoveDelayMs("0"))
        assertEquals(MaxShowLineMoveDelayMs, resolveShowLineMoveDelayMs(Long.MAX_VALUE.toString()))
        assertEquals(ShowLineMoveDelayMs, resolveShowLineMoveDelayMs("invalid"))
        assertEquals(
            MinShowLineMoveDelayMs.toString(),
            formatShowLineMoveDelayInput(0L),
        )
    }

    @Test
    fun playShowLineMovesSubmitsEveryMoveAndWaitsForPlaybackCompletion() = runBlocking {
        val events = mutableListOf<String>()
        val playbackWaitStarted = CompletableDeferred<Unit>()
        val allowPlaybackCompletion = CompletableDeferred<Unit>()
        val playback =
            TrainSingleLineShowLinePlayback(
                onStartPositionLoaded = {
                    events += "start"
                },
                onMoveForward = {
                    events += "move"
                    true
                },
                awaitCompletion = {
                    events += "await"
                    playbackWaitStarted.complete(Unit)
                    allowPlaybackCompletion.await()
                },
            )

        val result =
            async {
                playShowLineMoves(
                    movesCount = 2,
                    moveDelayMs = 0L,
                    playback = playback,
                )
            }

        playbackWaitStarted.await()

        assertEquals(listOf("start", "move", "move", "await"), events)
        assertFalse(result.isCompleted)

        allowPlaybackCompletion.complete(Unit)

        assertTrue(result.await())
    }

    @Test
    fun playShowLineMovesStopsWithoutCompletionWhenForwardMoveFails() = runBlocking {
        var forwardCalls = 0
        var playbackWaitCalls = 0
        var startPositionLoadedCalls = 0
        val playback =
            TrainSingleLineShowLinePlayback(
                onStartPositionLoaded = {
                    startPositionLoadedCalls += 1
                },
                onMoveForward = {
                    forwardCalls += 1
                    false
                },
                awaitCompletion = {
                    playbackWaitCalls += 1
                },
            )

        val completed =
            playShowLineMoves(
                movesCount = 3,
                moveDelayMs = 0L,
                playback = playback,
            )

        assertFalse(completed)
        assertEquals(1, startPositionLoadedCalls)
        assertEquals(1, forwardCalls)
        assertEquals(1, playbackWaitCalls)
    }
}
