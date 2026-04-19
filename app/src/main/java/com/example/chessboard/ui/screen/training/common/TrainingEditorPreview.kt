package com.example.chessboard.ui.screen.training.common

/*
 * Shared preview-session logic for training-like editors.
 *
 * Keep move parsing, selected-game board state, and preview-board orientation
 * helpers here so training and template editors can reuse them. Do not add
 * screen scaffolds, save flows, or card layout code to this file.
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.entity.SideMask
import com.example.chessboard.service.buildMoveLabels
import com.example.chessboard.service.parsePgnMoves
import com.example.chessboard.ui.BoardOrientation

internal data class ParsedTrainingEditorGame(
    val uciMoves: List<String>,
    val moveLabels: List<String>
)

internal data class TrainingEditorBoardSession(
    val gameController: GameController,
    val parsedGamesById: Map<Long, ParsedTrainingEditorGame>,
    val selectedGameId: Long?,
    val onSelectGame: (Long) -> Unit,
    val onMoveToPly: (Long, Int) -> Unit,
    val onResetSelectedGame: (Long) -> Unit,
)

@Composable
internal fun rememberTrainingEditorBoardSession(
    games: List<TrainingGameEditorItem>
): TrainingEditorBoardSession {
    val gameController = remember { GameController() }
    val gameIds = remember(games) { games.map { it.gameId } }
    val gamesById = remember(gameIds) {
        games.associateBy { game -> game.gameId }
    }
    val parsedGamesById = remember(gameIds) {
        games.associate { game ->
            val uciMoves = parsePgnMoves(game.pgn)
            val moveLabels = buildMoveLabels(uciMoves)
            game.gameId to ParsedTrainingEditorGame(
                uciMoves = uciMoves,
                moveLabels = moveLabels
            )
        }
    }
    var selectedGameId by remember(gameIds) { mutableStateOf(games.firstOrNull()?.gameId) }

    fun loadGameAtPly(gameId: Long, ply: Int) {
        val game = gamesById[gameId] ?: return
        val parsedGame = parsedGamesById[gameId] ?: return
        gameController.setOrientation(resolveTrainingPreviewBoardOrientation(game))
        gameController.loadFromUciMoves(parsedGame.uciMoves, targetPly = ply)
    }

    fun selectGame(gameId: Long) {
        selectedGameId = gameId
        loadGameAtPly(gameId, ply = 0)
    }

    fun moveToPly(gameId: Long, ply: Int) {
        selectedGameId = gameId
        loadGameAtPly(gameId, ply = ply)
    }

    fun resetSelectedGame(gameId: Long) {
        loadGameAtPly(gameId, ply = 0)
    }

    SideEffect {
        gameController.setUserMovesEnabled(false)
    }

    LaunchedEffect(gameIds, parsedGamesById) {
        if (selectedGameId in parsedGamesById.keys) {
            return@LaunchedEffect
        }

        selectedGameId = games.firstOrNull()?.gameId
    }

    LaunchedEffect(selectedGameId, parsedGamesById) {
        val gameId = selectedGameId ?: return@LaunchedEffect
        loadGameAtPly(gameId, ply = 0)
    }

    return TrainingEditorBoardSession(
        gameController = gameController,
        parsedGamesById = parsedGamesById,
        selectedGameId = selectedGameId,
        onSelectGame = ::selectGame,
        onMoveToPly = ::moveToPly,
        onResetSelectedGame = ::resetSelectedGame
    )
}

internal fun resolveTrainingPreviewBoardOrientation(
    game: TrainingGameEditorItem
): BoardOrientation {
    if (game.sideMask == SideMask.BLACK) {
        return BoardOrientation.BLACK
    }

    return BoardOrientation.WHITE
}
