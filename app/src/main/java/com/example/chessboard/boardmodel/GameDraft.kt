package com.example.chessboard.boardmodel

import com.example.chessboard.entity.GameEntity
import com.example.chessboard.entity.SideMask

data class GameDraft(
    val game: GameEntity = GameEntity(
        // Room treats id = 0 as "not inserted yet" for auto-generated primary keys.
        id = 0,
        white = null,
        black = null,
        result = null,
        event = "",
        site = null,
        date = 0,
        round = null,
        eco = "",
        pgn = "",
        initialFen = "",
        sideMask = SideMask.WHITE,
    ),
)

fun buildGameDraftFromSourceGame(
    sourceGame: GameEntity,
): GameDraft {
    return GameDraft(
        game = sourceGame.copy(id = 0),
    )
}
