package com.example.chessboard.ui.screen.training.flow

/**
 * File role: groups smart-training flow orchestration and transition rules.
 * Allowed here:
 * - runtime-context updates and navigation decisions for smart-training queue traversal
 * - smart-training side-destination routing such as analysis, game editor, and create-opening entry points
 * Not allowed here:
 * - composable UI, activity-owned state, or regular-training ordering logic
 * - database access, repository calls, or persistence logic
 * Validation date: 2026-04-26
 */

import com.example.chessboard.boardmodel.GameDraft
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.runtimecontext.RuntimeContext
import com.example.chessboard.service.SmartGamePair
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.screen.trainSingleGame.TrainSingleGameResult

class SmartTrainingFlowCoordinator(
    private val runtimeContext: RuntimeContext,
) {
    fun startTraining(queue: List<SmartGamePair>): TrainingFlowResult? {
        val first = queue.firstOrNull() ?: return null
        runtimeContext.smartTrainingQueue = queue
        return TrainingFlowResult.Navigate(
            ScreenType.SmartTrainGame(first.trainingId, first.gameId)
        )
    }

    fun hasNextGame(gameId: Long): Boolean {
        return runtimeContext.resolveNextSmartGamePair(gameId) != null
    }

    fun sessionCurrent(gameId: Long): Int {
        return runtimeContext.smartTrainingQueue
            .indexOfFirst { it.gameId == gameId }
            .coerceAtLeast(0) + 1
    }

    fun sessionTotal(): Int {
        return runtimeContext.smartTrainingQueue.size
    }

    fun finishGame(): TrainingFlowResult {
        return TrainingFlowResult.Navigate(ScreenType.SmartTraining)
    }

    fun interruptTraining(trainingId: Long): TrainingFlowResult {
        runtimeContext.trainingSession.clearTrainingSession(trainingId)
        runtimeContext.smartTrainingQueue = emptyList()
        return TrainingFlowResult.Navigate(ScreenType.SmartTraining)
    }

    fun openNextGame(result: TrainSingleGameResult): TrainingFlowResult {
        val nextGame = runtimeContext.resolveNextSmartGamePair(result.gameId)
        if (nextGame == null) {
            return TrainingFlowResult.Navigate(ScreenType.SmartTraining)
        }

        return TrainingFlowResult.Navigate(
            ScreenType.SmartTrainGame(nextGame.trainingId, nextGame.gameId)
        )
    }

    fun openGameEditor(
        game: GameEntity,
        trainingId: Long,
        gameId: Long,
    ): TrainingFlowResult {
        return TrainingFlowResult.OpenGameEditor(
            game = game,
            backTarget = ScreenType.SmartTrainGame(trainingId, gameId),
        )
    }

    fun openCreateOpening(
        draft: GameDraft,
        trainingId: Long,
        gameId: Long,
    ): TrainingFlowResult {
        return TrainingFlowResult.OpenCreateOpening(
            draft = draft,
            backTarget = ScreenType.SmartTrainGame(trainingId, gameId),
        )
    }

    fun openPositionEditor(
        fen: String,
        trainingId: Long,
        gameId: Long,
    ): TrainingFlowResult {
        return TrainingFlowResult.OpenPositionEditor(
            initialFen = fen,
            backTarget = ScreenType.SmartTrainGame(trainingId, gameId),
        )
    }

    fun openAnalysis(
        trainingId: Long,
        gameId: Long,
        uciMoves: List<String>,
        initialPly: Int,
    ): TrainingFlowResult {
        return TrainingFlowResult.OpenAnalysis(
            uciMoves = uciMoves,
            initialPly = initialPly,
            backTarget = ScreenType.SmartTrainGame(trainingId, gameId),
        )
    }
}
