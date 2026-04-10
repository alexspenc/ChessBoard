package com.example.chessboard.ui.screen.gamesExplorer

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.example.chessboard.boardmodel.buildGameDraftFromSourceGame
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.entity.SideMask
import com.example.chessboard.service.ParsedGame
import com.example.chessboard.service.buildMoveLabels
import com.example.chessboard.service.parsePgnMoves
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class GamesExplorerCloneTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun gamesExplorer_cloneGameButtonReturnsDraftWithFullPgn() {
        val sourceGame = GameEntity(
            id = 42L,
            event = "CaroKann",
            eco = "B12",
            pgn = "1. e2e4 c7c6 2. d2d4 d7d5 *",
            initialFen = "",
            sideMask = SideMask.WHITE
        )
        val parsedGame = ParsedGame(
            game = sourceGame,
            uciMoves = parsePgnMoves(sourceGame.pgn),
            moveLabels = buildMoveLabels(parsePgnMoves(sourceGame.pgn))
        )
        var clonedGame: GameEntity? = null

        composeRule.setContent {
            ChessBoardTheme {
                GameBlock(
                    parsedGame = parsedGame,
                    isSelected = true,
                    currentPly = 0,
                    canUndo = false,
                    canRedo = false,
                    onMovePlyClick = {},
                    onPrevClick = {},
                    onNextClick = {},
                    onResetClick = {},
                    onCloneClick = {
                        clonedGame = parsedGame.game
                    },
                    onEditClick = {},
                    onDeleteClick = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("Clone game").performClick()
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertNotNull(clonedGame)
            assertEquals(sourceGame.id, clonedGame!!.id)
            assertEquals(sourceGame.event, clonedGame!!.event)
            assertEquals(sourceGame.eco, clonedGame!!.eco)
            assertEquals(sourceGame.pgn, clonedGame!!.pgn)
            assertEquals(sourceGame.sideMask, clonedGame!!.sideMask)

            val clonedDraft = buildGameDraftFromSourceGame(clonedGame!!)
            assertEquals(0L, clonedDraft.game.id)
            assertEquals(sourceGame.pgn, clonedDraft.game.pgn)
        }
    }
}
