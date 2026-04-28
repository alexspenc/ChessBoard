package com.example.chessboard.ui.screen.training.flow

/**
 * File role: groups regular training-flow orchestration and transition rules.
 * Allowed here:
 * - runtime-context updates and navigation decisions for training editor, settings, and single-game training
 * - training-specific side-destination routing such as analysis, game editor, and create-opening entry points
 * Not allowed here:
 * - composable UI, activity state holders, or generic app navigation unrelated to training
 * - database access, repository calls, or persistence logic
 * Validation date: 2026-04-26
 */

import com.example.chessboard.boardmodel.GameDraft
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.runtimecontext.RuntimeContext
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.screen.trainSingleGame.TrainSingleGameResult

class RegularTrainingFlowCoordinator(
    private val runtimeContext: RuntimeContext,
) {
    fun openTraining(trainingId: Long): TrainingFlowResult {
        val startedGameId = runtimeContext.trainingSession.firstStartedGameId(trainingId)
        if (startedGameId != null) {
            runtimeContext.trainingSession.setCurrentGameId(trainingId, startedGameId)
            return TrainingFlowResult.Navigate(
                ScreenType.TrainSingleGame(trainingId, startedGameId)
            )
        }

        runtimeContext.orderGamesInTraining.reset()
        return TrainingFlowResult.Navigate(ScreenType.EditTraining(trainingId))
    }

    fun openSettings(trainingId: Long): TrainingFlowResult {
        return TrainingFlowResult.Navigate(ScreenType.TrainingSettings(trainingId))
    }

    fun closeSettings(trainingId: Long): TrainingFlowResult {
        return TrainingFlowResult.Navigate(ScreenType.EditTraining(trainingId))
    }

    fun startGame(
        trainingId: Long,
        gameId: Long,
        orderedGameIds: List<Long>,
    ): TrainingFlowResult {
        runtimeContext.trainingSession.rememberLaunch(
            trainingId = trainingId,
            gameId = gameId,
            orderedGameIds = orderedGameIds,
        )
        return TrainingFlowResult.Navigate(ScreenType.TrainSingleGame(trainingId, gameId))
    }

    fun hasNextGame(trainingId: Long, gameId: Long): Boolean {
        return runtimeContext.resolveNextTrainingGameId(trainingId, gameId) != null
    }

    fun sessionCurrent(trainingId: Long, gameId: Long): Int {
        return runtimeContext.trainingSession.sessionCurrent(trainingId, gameId)
    }

    fun sessionTotal(trainingId: Long): Int {
        return runtimeContext.trainingSession.sessionTotal(trainingId)
    }

    fun finishGame(result: TrainSingleGameResult): TrainingFlowResult {
        runtimeContext.trainingSession.clearGameProgress(
            trainingId = result.trainingId,
            gameId = result.gameId,
        )
        runtimeContext.trainingSession.setCurrentGameId(
            trainingId = result.trainingId,
            gameId = null,
        )
        runtimeContext.orderGamesInTraining.markGameCompleted(result.gameId)
        return TrainingFlowResult.Navigate(ScreenType.EditTraining(result.trainingId))
    }

    fun interruptTraining(trainingId: Long): TrainingFlowResult {
        runtimeContext.trainingSession.clearTrainingSession(trainingId)
        runtimeContext.orderGamesInTraining.reset()
        return TrainingFlowResult.Navigate(ScreenType.EditTraining(trainingId))
    }

    fun openNextGame(result: TrainSingleGameResult): TrainingFlowResult {
        runtimeContext.trainingSession.clearGameProgress(
            trainingId = result.trainingId,
            gameId = result.gameId,
        )
        runtimeContext.orderGamesInTraining.markGameCompleted(result.gameId)
        val nextGameId = runtimeContext.resolveNextTrainingGameId(
            trainingId = result.trainingId,
            currentGameId = result.gameId,
        )

        if (nextGameId == null) {
            runtimeContext.trainingSession.setCurrentGameId(
                trainingId = result.trainingId,
                gameId = null,
            )
            return TrainingFlowResult.Navigate(ScreenType.EditTraining(result.trainingId))
        }

        runtimeContext.trainingSession.setCurrentGameId(
            trainingId = result.trainingId,
            gameId = nextGameId,
        )
        return TrainingFlowResult.Navigate(
            ScreenType.TrainSingleGame(result.trainingId, nextGameId)
        )
    }

    fun openGameEditorFromEditor(game: GameEntity, trainingId: Long): TrainingFlowResult {
        return TrainingFlowResult.OpenGameEditor(
            game = game,
            backTarget = ScreenType.EditTraining(trainingId),
        )
    }

    fun openGameEditorFromTraining(
        game: GameEntity,
        trainingId: Long,
        gameId: Long,
    ): TrainingFlowResult {
        return TrainingFlowResult.OpenGameEditor(
            game = game,
            backTarget = ScreenType.TrainSingleGame(trainingId, gameId),
        )
    }

    fun openCreateOpeningFromTraining(
        draft: GameDraft,
        trainingId: Long,
        gameId: Long,
    ): TrainingFlowResult {
        return TrainingFlowResult.OpenCreateOpening(
            draft = draft,
            backTarget = ScreenType.TrainSingleGame(trainingId, gameId),
        )
    }

    fun openPositionEditorFromTraining(
        fen: String,
        trainingId: Long,
        gameId: Long,
    ): TrainingFlowResult {
        return TrainingFlowResult.OpenPositionEditor(
            initialFen = fen,
            backTarget = ScreenType.TrainSingleGame(trainingId, gameId),
        )
    }

    fun openAnalysisFromEditor(
        trainingId: Long,
        uciMoves: List<String>,
        initialPly: Int,
    ): TrainingFlowResult {
        return TrainingFlowResult.OpenAnalysis(
            uciMoves = uciMoves,
            initialPly = initialPly,
            backTarget = ScreenType.EditTraining(trainingId),
        )
    }

    fun openAnalysisFromTraining(
        trainingId: Long,
        gameId: Long,
        uciMoves: List<String>,
        initialPly: Int,
    ): TrainingFlowResult {
        return TrainingFlowResult.OpenAnalysis(
            uciMoves = uciMoves,
            initialPly = initialPly,
            backTarget = ScreenType.TrainSingleGame(trainingId, gameId),
        )
    }
}
