package com.example.chessboard.runtimecontext

/**
 * File role: groups unit tests for TrainingRuntimeContext session and progress behavior.
 * Allowed here:
 * - pure runtime-state tests for launch tracking, next-game resolution, and snapshot restore
 * - assertions about progress sanitization and training-session isolation
 * Not allowed here:
 * - Compose UI tests, navigation rendering checks, or persistence-backed behavior
 * - unrelated RuntimeContext wrapper tests that belong in separate test files
 * Validation date: 2026-04-26
 */

import com.example.chessboard.ui.screen.trainSingleGame.TrainSingleGamePhase
import com.example.chessboard.ui.screen.trainSingleGame.TrainSingleGameUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrainingRuntimeContextTest {

    @Test
    fun `rememberLaunch creates new session with active game and ordered ids`() {
        val runtimeContext = TrainingRuntimeContext()

        runtimeContext.rememberLaunch(
            trainingId = 1L,
            gameId = 20L,
            orderedGameIds = listOf(10L, 20L, 30L),
        )

        assertEquals(20L, runtimeContext.activeGameId(1L))
        assertEquals(listOf(10L, 20L, 30L), runtimeContext.orderedGameIds(1L))
    }

    @Test
    fun `rememberLaunch replaces current game for existing session`() {
        val runtimeContext = TrainingRuntimeContext()
        runtimeContext.rememberLaunch(
            trainingId = 1L,
            gameId = 10L,
            orderedGameIds = listOf(10L, 20L, 30L),
        )

        runtimeContext.rememberLaunch(
            trainingId = 1L,
            gameId = 30L,
            orderedGameIds = listOf(10L, 20L, 30L),
        )

        assertEquals(30L, runtimeContext.activeGameId(1L))
    }

    @Test
    fun `activeGameId returns null for unknown training`() {
        val runtimeContext = TrainingRuntimeContext()

        assertNull(runtimeContext.activeGameId(99L))
    }

    @Test
    fun `resolveNextGameId returns next item in ordered list`() {
        val runtimeContext = TrainingRuntimeContext()
        runtimeContext.rememberLaunch(
            trainingId = 1L,
            gameId = 10L,
            orderedGameIds = listOf(10L, 20L, 30L),
        )

        val nextGameId = runtimeContext.resolveNextGameId(
            trainingId = 1L,
            currentGameId = 20L,
        )

        assertEquals(30L, nextGameId)
    }

    @Test
    fun `resolveNextGameId returns null for last item`() {
        val runtimeContext = TrainingRuntimeContext()
        runtimeContext.rememberLaunch(
            trainingId = 1L,
            gameId = 10L,
            orderedGameIds = listOf(10L, 20L, 30L),
        )

        val nextGameId = runtimeContext.resolveNextGameId(
            trainingId = 1L,
            currentGameId = 30L,
        )

        assertNull(nextGameId)
    }

    @Test
    fun `resolveNextGameId returns null for missing current game`() {
        val runtimeContext = TrainingRuntimeContext()
        runtimeContext.rememberLaunch(
            trainingId = 1L,
            gameId = 10L,
            orderedGameIds = listOf(10L, 20L, 30L),
        )

        val nextGameId = runtimeContext.resolveNextGameId(
            trainingId = 1L,
            currentGameId = 99L,
        )

        assertNull(nextGameId)
    }

    @Test
    fun `sessionCurrent returns one based index`() {
        val runtimeContext = TrainingRuntimeContext()
        runtimeContext.rememberLaunch(
            trainingId = 1L,
            gameId = 10L,
            orderedGameIds = listOf(10L, 20L, 30L),
        )

        assertEquals(1, runtimeContext.sessionCurrent(1L, 10L))
        assertEquals(2, runtimeContext.sessionCurrent(1L, 20L))
        assertEquals(3, runtimeContext.sessionCurrent(1L, 30L))
    }

    @Test
    fun `sessionCurrent falls back to one for unknown game`() {
        val runtimeContext = TrainingRuntimeContext()
        runtimeContext.rememberLaunch(
            trainingId = 1L,
            gameId = 10L,
            orderedGameIds = listOf(10L, 20L, 30L),
        )

        assertEquals(1, runtimeContext.sessionCurrent(1L, 99L))
    }

    @Test
    fun `sessionTotal returns ordered list size`() {
        val runtimeContext = TrainingRuntimeContext()
        runtimeContext.rememberLaunch(
            trainingId = 1L,
            gameId = 10L,
            orderedGameIds = listOf(10L, 20L, 30L),
        )

        assertEquals(3, runtimeContext.sessionTotal(1L))
    }

    @Test
    fun `sessionTotal returns zero for unknown training`() {
        val runtimeContext = TrainingRuntimeContext()

        assertEquals(0, runtimeContext.sessionTotal(99L))
    }

    @Test
    fun `saveGameProgress stores snapshot for game`() {
        val runtimeContext = TrainingRuntimeContext()
        val uiState = trainUiState(
            phase = TrainSingleGamePhase.Training,
            expectedPly = 4,
            mistakesCount = 2,
        )

        runtimeContext.saveGameProgress(
            trainingId = 1L,
            gameId = 10L,
            currentPly = 4,
            lineFingerprint = "line-a",
            uiState = uiState,
        )

        val restored = runtimeContext.restoreGameProgress(1L, 10L)

        assertEquals(4, restored?.currentPly)
        assertEquals("line-a", restored?.lineFingerprint)
        assertEquals(uiState, restored?.uiState)
    }

    @Test
    fun `saveGameProgress overwrites previous snapshot for same game`() {
        val runtimeContext = TrainingRuntimeContext()
        runtimeContext.saveGameProgress(
            trainingId = 1L,
            gameId = 10L,
            currentPly = 2,
            lineFingerprint = "line-a",
            uiState = trainUiState(expectedPly = 2, mistakesCount = 1),
        )

        runtimeContext.saveGameProgress(
            trainingId = 1L,
            gameId = 10L,
            currentPly = 5,
            lineFingerprint = "line-b",
            uiState = trainUiState(expectedPly = 5, mistakesCount = 3),
        )

        val restored = runtimeContext.restoreGameProgress(1L, 10L)

        assertEquals(5, restored?.currentPly)
        assertEquals("line-b", restored?.lineFingerprint)
        assertEquals(5, restored?.uiState?.expectedPly)
        assertEquals(3, restored?.uiState?.mistakesCount)
    }

    @Test
    fun `saveGameProgress for one game does not affect another game`() {
        val runtimeContext = TrainingRuntimeContext()
        runtimeContext.saveGameProgress(
            trainingId = 1L,
            gameId = 10L,
            currentPly = 2,
            lineFingerprint = "line-a",
            uiState = trainUiState(expectedPly = 2),
        )
        runtimeContext.saveGameProgress(
            trainingId = 1L,
            gameId = 20L,
            currentPly = 6,
            lineFingerprint = "line-b",
            uiState = trainUiState(expectedPly = 6, mistakesCount = 4),
        )

        val first = runtimeContext.restoreGameProgress(1L, 10L)
        val second = runtimeContext.restoreGameProgress(1L, 20L)

        assertEquals(2, first?.currentPly)
        assertEquals(6, second?.currentPly)
        assertEquals(4, second?.uiState?.mistakesCount)
    }

    @Test
    fun `restoreGameProgress returns null for unsaved game`() {
        val runtimeContext = TrainingRuntimeContext()

        assertNull(runtimeContext.restoreGameProgress(1L, 10L))
    }

    @Test
    fun `clearGameProgress removes only target game snapshot`() {
        val runtimeContext = TrainingRuntimeContext()
        runtimeContext.saveGameProgress(
            trainingId = 1L,
            gameId = 10L,
            currentPly = 2,
            lineFingerprint = "line-a",
            uiState = trainUiState(expectedPly = 2),
        )
        runtimeContext.saveGameProgress(
            trainingId = 1L,
            gameId = 20L,
            currentPly = 4,
            lineFingerprint = "line-b",
            uiState = trainUiState(expectedPly = 4),
        )

        runtimeContext.clearGameProgress(trainingId = 1L, gameId = 10L)

        assertNull(runtimeContext.restoreGameProgress(1L, 10L))
        assertEquals(4, runtimeContext.restoreGameProgress(1L, 20L)?.currentPly)
    }

    @Test
    fun `clearGameProgress keeps session metadata`() {
        val runtimeContext = TrainingRuntimeContext()
        runtimeContext.rememberLaunch(
            trainingId = 1L,
            gameId = 20L,
            orderedGameIds = listOf(10L, 20L, 30L),
        )
        runtimeContext.saveGameProgress(
            trainingId = 1L,
            gameId = 20L,
            currentPly = 4,
            lineFingerprint = "line-a",
            uiState = trainUiState(expectedPly = 4),
        )

        runtimeContext.clearGameProgress(trainingId = 1L, gameId = 20L)

        assertEquals(20L, runtimeContext.activeGameId(1L))
        assertEquals(listOf(10L, 20L, 30L), runtimeContext.orderedGameIds(1L))
    }

    @Test
    fun `saveGameProgress clears wrongMoveSquare before storing`() {
        val runtimeContext = TrainingRuntimeContext()

        runtimeContext.saveGameProgress(
            trainingId = 1L,
            gameId = 10L,
            currentPly = 3,
            lineFingerprint = "line-a",
            uiState = trainUiState(
                phase = TrainSingleGamePhase.Mistake,
                wrongMoveSquare = "e4",
            ),
        )

        val restored = runtimeContext.restoreGameProgress(1L, 10L)

        assertNull(restored?.uiState?.wrongMoveSquare)
    }

    @Test
    fun `saveGameProgress converts ShowingLine phase to Idle`() {
        val runtimeContext = TrainingRuntimeContext()

        runtimeContext.saveGameProgress(
            trainingId = 1L,
            gameId = 10L,
            currentPly = 3,
            lineFingerprint = "line-a",
            uiState = trainUiState(phase = TrainSingleGamePhase.ShowingLine),
        )

        val restored = runtimeContext.restoreGameProgress(1L, 10L)

        assertEquals(TrainSingleGamePhase.Idle, restored?.uiState?.phase)
    }

    @Test
    fun `saveGameProgress preserves non ShowingLine phases`() {
        val runtimeContext = TrainingRuntimeContext()

        runtimeContext.saveGameProgress(
            trainingId = 1L,
            gameId = 10L,
            currentPly = 3,
            lineFingerprint = "line-a",
            uiState = trainUiState(phase = TrainSingleGamePhase.Training),
        )
        runtimeContext.saveGameProgress(
            trainingId = 1L,
            gameId = 20L,
            currentPly = 6,
            lineFingerprint = "line-b",
            uiState = trainUiState(phase = TrainSingleGamePhase.Mistake),
        )

        assertEquals(
            TrainSingleGamePhase.Training,
            runtimeContext.restoreGameProgress(1L, 10L)?.uiState?.phase,
        )
        assertEquals(
            TrainSingleGamePhase.Mistake,
            runtimeContext.restoreGameProgress(1L, 20L)?.uiState?.phase,
        )
    }

    @Test
    fun `sessions are isolated by trainingId`() {
        val runtimeContext = TrainingRuntimeContext()
        runtimeContext.saveGameProgress(
            trainingId = 1L,
            gameId = 10L,
            currentPly = 2,
            lineFingerprint = "line-a",
            uiState = trainUiState(expectedPly = 2),
        )
        runtimeContext.saveGameProgress(
            trainingId = 2L,
            gameId = 10L,
            currentPly = 7,
            lineFingerprint = "line-b",
            uiState = trainUiState(expectedPly = 7),
        )

        assertEquals(2, runtimeContext.restoreGameProgress(1L, 10L)?.currentPly)
        assertEquals(7, runtimeContext.restoreGameProgress(2L, 10L)?.currentPly)
    }

    @Test
    fun `saveGameProgress updates active game`() {
        val runtimeContext = TrainingRuntimeContext()

        runtimeContext.saveGameProgress(
            trainingId = 1L,
            gameId = 30L,
            currentPly = 3,
            lineFingerprint = "line-a",
            uiState = trainUiState(expectedPly = 3),
        )

        assertEquals(30L, runtimeContext.activeGameId(1L))
    }

    @Test
    fun `setCurrentGameId updates active game without touching snapshots`() {
        val runtimeContext = TrainingRuntimeContext()
        runtimeContext.rememberLaunch(
            trainingId = 1L,
            gameId = 10L,
            orderedGameIds = listOf(10L, 20L),
        )
        runtimeContext.saveGameProgress(
            trainingId = 1L,
            gameId = 10L,
            currentPly = 2,
            lineFingerprint = "line-a",
            uiState = trainUiState(expectedPly = 2),
        )

        runtimeContext.setCurrentGameId(trainingId = 1L, gameId = 20L)

        assertEquals(20L, runtimeContext.activeGameId(1L))
        assertEquals(2, runtimeContext.restoreGameProgress(1L, 10L)?.currentPly)
    }

    @Test
    fun `setCurrentGameId does nothing for unknown training`() {
        val runtimeContext = TrainingRuntimeContext()

        runtimeContext.setCurrentGameId(trainingId = 99L, gameId = 20L)

        assertNull(runtimeContext.activeGameId(99L))
    }

    private fun trainUiState(
        phase: TrainSingleGamePhase = TrainSingleGamePhase.Idle,
        expectedPly: Int = 0,
        mistakesCount: Int = 0,
        wrongMoveSquare: String? = null,
    ): TrainSingleGameUiState {
        return TrainSingleGameUiState(
            phase = phase,
            expectedPly = expectedPly,
            mistakesCount = mistakesCount,
            wrongMoveSquare = wrongMoveSquare,
        )
    }
}
