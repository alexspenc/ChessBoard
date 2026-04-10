package com.example.chessboard.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.entity.SideMask
import com.example.chessboard.service.buildMoveLabels
import com.example.chessboard.testing.normalizeFenForAssertion
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GameEditorScreenNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun gameEditorScreen_rightArrowAdvancesBoardPosition() {
        val gameController = createGameControllerAtStart()
        val moveLabels = buildMoveLabels(TestUciMoves)

        composeRule.setContent {
            ChessBoardTheme {
                GameEditorScreen(
                    game = TestGame,
                    gameController = gameController,
                    moveLabels = moveLabels,
                    isLoading = false
                )
            }
        }

        composeRule.onNodeWithTag("game-editor-next").performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(1, gameController.currentMoveIndex)
            assertEquals(AfterE4Fen, normalizeFenForAssertion(gameController.getFen()))
        }
    }

    @Test
    fun gameEditorScreen_moveChipAdvancesBoardPosition() {
        val gameController = createGameControllerAtStart()
        val moveLabels = buildMoveLabels(TestUciMoves)

        composeRule.setContent {
            ChessBoardTheme {
                GameEditorScreen(
                    game = TestGame,
                    gameController = gameController,
                    moveLabels = moveLabels,
                    isLoading = false
                )
            }
        }

        composeRule.onNodeWithTag("move-chip-1.e4").performScrollTo()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("move-chip-1.e4")
            .performSemanticsAction(SemanticsActions.OnClick)

        composeRule.runOnIdle {
            assertEquals(1, gameController.currentMoveIndex)
            assertEquals(AfterE4Fen, normalizeFenForAssertion(gameController.getFen()))
        }
    }

    private fun createGameControllerAtStart(): GameController {
        return GameController().apply {
            loadFromUciMoves(TestUciMoves, targetPly = 0)
        }
    }

    private companion object {
        val TestUciMoves = listOf("e2e4", "e7e5")
        const val AfterE4Fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
        val TestGame = GameEntity(
            id = 1L,
            event = "Test Opening",
            pgn = "1. e4 e5 *",
            initialFen = "",
            sideMask = SideMask.BOTH
        )
    }
}
