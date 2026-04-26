package com.example.chessboard.ui.screen.training.flow

/**
 * File role: groups unit tests for regular training-flow coordination.
 * Allowed here:
 * - runtime-only tests for regular training navigation decisions and runtime-context updates
 * - assertions about back targets and next-game transitions returned by the regular coordinator
 * Not allowed here:
 * - Compose UI tests, activity wiring checks, or smart-training queue behavior
 * - database-backed integration tests or persistence behavior
 * Validation date: 2026-04-26
 */

import com.example.chessboard.boardmodel.GameDraft
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.runtimecontext.RuntimeContext
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.screen.trainSingleGame.TrainSingleGameResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegularTrainingFlowCoordinatorTest {

    @Test
    fun `openTraining resets ordering and navigates to editor`() {
        val runtimeContext = RuntimeContext()
        val coordinator = RegularTrainingFlowCoordinator(runtimeContext)
        val games = listOf(
            weightedGame(gameId = 10L, weight = 5),
            weightedGame(gameId = 20L, weight = 3),
        )
        runtimeContext.orderGamesInTraining.markGameCompleted(10L)

        coordinator.openTraining(trainingId = 5L)

        val orderedIds = runtimeContext.orderGamesInTraining
            .orderGames(
                games = games,
                getGameId = { it.gameId },
                getWeight = { it.weight },
            )
            .map { it.gameId }

        val result = coordinator.openTraining(trainingId = 5L)

        assertEquals(listOf(10L, 20L), orderedIds)
        assertEquals(
            TrainingFlowResult.Navigate(ScreenType.EditTraining(5L)),
            result,
        )
    }

    @Test
    fun `startGame remembers launch and navigates to single game`() {
        val runtimeContext = RuntimeContext()
        val coordinator = RegularTrainingFlowCoordinator(runtimeContext)

        val result = coordinator.startGame(
            trainingId = 7L,
            gameId = 20L,
            orderedGameIds = listOf(10L, 20L, 30L),
        )

        assertEquals(20L, runtimeContext.trainingSession.activeGameId(7L))
        assertEquals(listOf(10L, 20L, 30L), runtimeContext.trainingSession.orderedGameIds(7L))
        assertEquals(
            TrainingFlowResult.Navigate(ScreenType.TrainSingleGame(7L, 20L)),
            result,
        )
    }

    @Test
    fun `finishGame clears progress resets active game and navigates to editor`() {
        val runtimeContext = RuntimeContext()
        val coordinator = RegularTrainingFlowCoordinator(runtimeContext)
        runtimeContext.trainingSession.rememberLaunch(
            trainingId = 1L,
            gameId = 10L,
            orderedGameIds = listOf(10L, 20L),
        )
        runtimeContext.trainingSession.saveGameProgress(
            trainingId = 1L,
            gameId = 10L,
            currentPly = 4,
            lineFingerprint = "line-a",
            uiState = trainUiState(expectedPly = 4),
        )

        val result = coordinator.finishGame(
            TrainSingleGameResult(
                gameId = 10L,
                trainingId = 1L,
                mistakesCount = 2,
            )
        )

        assertNull(runtimeContext.trainingSession.restoreGameProgress(1L, 10L))
        assertNull(runtimeContext.trainingSession.activeGameId(1L))
        assertEquals(
            TrainingFlowResult.Navigate(ScreenType.EditTraining(1L)),
            result,
        )
    }

    @Test
    fun `openNextGame moves to next stored game`() {
        val runtimeContext = RuntimeContext()
        val coordinator = RegularTrainingFlowCoordinator(runtimeContext)
        runtimeContext.trainingSession.rememberLaunch(
            trainingId = 1L,
            gameId = 10L,
            orderedGameIds = listOf(10L, 20L, 30L),
        )
        runtimeContext.trainingSession.saveGameProgress(
            trainingId = 1L,
            gameId = 10L,
            currentPly = 2,
            lineFingerprint = "line-a",
            uiState = trainUiState(expectedPly = 2),
        )

        val result = coordinator.openNextGame(
            TrainSingleGameResult(
                gameId = 10L,
                trainingId = 1L,
                mistakesCount = 0,
            )
        )

        assertNull(runtimeContext.trainingSession.restoreGameProgress(1L, 10L))
        assertEquals(20L, runtimeContext.trainingSession.activeGameId(1L))
        assertEquals(
            TrainingFlowResult.Navigate(ScreenType.TrainSingleGame(1L, 20L)),
            result,
        )
    }

    @Test
    fun `openNextGame falls back to editor when next game is missing`() {
        val runtimeContext = RuntimeContext()
        val coordinator = RegularTrainingFlowCoordinator(runtimeContext)
        runtimeContext.trainingSession.rememberLaunch(
            trainingId = 1L,
            gameId = 30L,
            orderedGameIds = listOf(10L, 20L, 30L),
        )

        val result = coordinator.openNextGame(
            TrainSingleGameResult(
                gameId = 30L,
                trainingId = 1L,
                mistakesCount = 1,
            )
        )

        assertNull(runtimeContext.trainingSession.activeGameId(1L))
        assertEquals(
            TrainingFlowResult.Navigate(ScreenType.EditTraining(1L)),
            result,
        )
    }

    @Test
    fun `openSettings and closeSettings navigate around editor`() {
        val runtimeContext = RuntimeContext()
        val coordinator = RegularTrainingFlowCoordinator(runtimeContext)

        assertEquals(
            TrainingFlowResult.Navigate(ScreenType.TrainingSettings(9L)),
            coordinator.openSettings(9L),
        )
        assertEquals(
            TrainingFlowResult.Navigate(ScreenType.EditTraining(9L)),
            coordinator.closeSettings(9L),
        )
    }

    @Test
    fun `hasNextGame and session counters delegate to training session order`() {
        val runtimeContext = RuntimeContext()
        val coordinator = RegularTrainingFlowCoordinator(runtimeContext)
        runtimeContext.trainingSession.rememberLaunch(
            trainingId = 3L,
            gameId = 10L,
            orderedGameIds = listOf(10L, 20L, 30L),
        )

        assertTrue(coordinator.hasNextGame(trainingId = 3L, gameId = 10L))
        assertEquals(2, coordinator.sessionCurrent(trainingId = 3L, gameId = 20L))
        assertEquals(3, coordinator.sessionTotal(trainingId = 3L))
    }

    @Test
    fun `side destination results from editor use edit training back target`() {
        val runtimeContext = RuntimeContext()
        val coordinator = RegularTrainingFlowCoordinator(runtimeContext)
        val game = gameEntity(gameId = 50L)

        assertEquals(
            TrainingFlowResult.OpenGameEditor(
                game = game,
                backTarget = ScreenType.EditTraining(4L),
            ),
            coordinator.openGameEditorFromEditor(game = game, trainingId = 4L),
        )
        assertEquals(
            TrainingFlowResult.OpenAnalysis(
                uciMoves = listOf("e2e4"),
                initialPly = 1,
                backTarget = ScreenType.EditTraining(4L),
            ),
            coordinator.openAnalysisFromEditor(
                trainingId = 4L,
                uciMoves = listOf("e2e4"),
                initialPly = 1,
            ),
        )
    }

    @Test
    fun `side destination results from training use single game back target`() {
        val runtimeContext = RuntimeContext()
        val coordinator = RegularTrainingFlowCoordinator(runtimeContext)
        val game = gameEntity(gameId = 60L)
        val draft = GameDraft(game = game)

        assertEquals(
            TrainingFlowResult.OpenGameEditor(
                game = game,
                backTarget = ScreenType.TrainSingleGame(8L, 60L),
            ),
            coordinator.openGameEditorFromTraining(
                game = game,
                trainingId = 8L,
                gameId = 60L,
            ),
        )
        assertEquals(
            TrainingFlowResult.OpenCreateOpening(
                draft = draft,
                backTarget = ScreenType.TrainSingleGame(8L, 60L),
            ),
            coordinator.openCreateOpeningFromTraining(
                draft = draft,
                trainingId = 8L,
                gameId = 60L,
            ),
        )
        assertEquals(
            TrainingFlowResult.OpenPositionEditor(
                initialFen = "fen",
                backTarget = ScreenType.TrainSingleGame(8L, 60L),
            ),
            coordinator.openPositionEditorFromTraining(
                fen = "fen",
                trainingId = 8L,
                gameId = 60L,
            ),
        )
        assertEquals(
            TrainingFlowResult.OpenAnalysis(
                uciMoves = listOf("e2e4", "e7e5"),
                initialPly = 2,
                backTarget = ScreenType.TrainSingleGame(8L, 60L),
            ),
            coordinator.openAnalysisFromTraining(
                trainingId = 8L,
                gameId = 60L,
                uciMoves = listOf("e2e4", "e7e5"),
                initialPly = 2,
            ),
        )
    }

    private data class WeightedGame(
        val gameId: Long,
        val weight: Int,
    )

    private fun weightedGame(
        gameId: Long,
        weight: Int,
    ): WeightedGame {
        return WeightedGame(
            gameId = gameId,
            weight = weight,
        )
    }

    private fun gameEntity(gameId: Long): GameEntity {
        return GameEntity(
            id = gameId,
            pgn = "",
            initialFen = "",
        )
    }

    private fun trainUiState(expectedPly: Int) =
        com.example.chessboard.ui.screen.trainSingleGame.TrainSingleGameUiState(
            expectedPly = expectedPly,
        )
}
