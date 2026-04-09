package com.example.chessboard.boardmodel

import com.example.chessboard.entity.GameEntity
import com.example.chessboard.entity.SideMask
import org.junit.Assert.assertEquals
import org.junit.Test

class GameDraftTest {

    @Test
    fun buildGameDraftFromSourceGame_clonesGameAndResetsId() {
        val sourceGame = GameEntity(
            id = 42L,
            white = "Carlsen",
            black = "Nepo",
            result = "1-0",
            event = "World Championship",
            site = "Dubai",
            date = 1700000000000L,
            round = "6",
            eco = "C88",
            pgn = "1. e4 e5 2. Nf3 Nc6 *",
            initialFen = "",
            sideMask = SideMask.BLACK
        )

        val draft = buildGameDraftFromSourceGame(sourceGame)

        assertEquals(0L, draft.game.id)
        assertEquals(sourceGame.white, draft.game.white)
        assertEquals(sourceGame.black, draft.game.black)
        assertEquals(sourceGame.result, draft.game.result)
        assertEquals(sourceGame.event, draft.game.event)
        assertEquals(sourceGame.site, draft.game.site)
        assertEquals(sourceGame.date, draft.game.date)
        assertEquals(sourceGame.round, draft.game.round)
        assertEquals(sourceGame.eco, draft.game.eco)
        assertEquals(sourceGame.pgn, draft.game.pgn)
        assertEquals(sourceGame.initialFen, draft.game.initialFen)
        assertEquals(sourceGame.sideMask, draft.game.sideMask)
    }
}
