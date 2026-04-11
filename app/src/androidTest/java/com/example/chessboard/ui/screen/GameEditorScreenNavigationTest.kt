package com.example.chessboard.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.boardmodel.InitialBoardFen
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.entity.SideMask
import com.example.chessboard.service.buildMoveLabels
import com.example.chessboard.testing.normalizeFenForAssertion
import com.example.chessboard.ui.GameEditorNextTestTag
import com.example.chessboard.ui.InteractiveChessBoardTestTag
import com.example.chessboard.ui.moveChipTestTag
import com.example.chessboard.ui.theme.ChessBoardTheme
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

        // Keep these waits. On slower emulators the screen can still be settling after
        // initial composition and the button may exist before it is stably displayed.
        composeRule.onNodeWithTag(GameEditorNextTestTag).performScrollTo()
        waitForNodeDisplayed(GameEditorNextTestTag)
        // Wait for the board to be at the known initial position before we assert the
        // one-step transition caused by the next-arrow click.
        assertBoardFenEventually(InitialBoardFen)
        composeRule.onNodeWithTag(GameEditorNextTestTag).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            gameController.currentMoveIndex == 1
        }
        assertBoardFenEventually(AfterE4Fen)
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

        // This wait is intentionally defensive. The move chips live inside a scrollable
        // container, and on slower emulators performScrollTo() can finish before the node is
        // actually stable and displayed for interaction.
        composeRule.onNodeWithTag(moveChipTestTag("1.e4")).performScrollTo()
        waitForNodeDisplayed(moveChipTestTag("1.e4"))
        composeRule.onNodeWithTag(moveChipTestTag("1.e4"))
            .performSemanticsAction(SemanticsActions.OnClick)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            gameController.currentMoveIndex == 1
        }
        assertBoardFenEventually(AfterE4Fen)
    }


    private fun assertBoardFenEventually(expectedFen: String) {
        // Do not replace this with an immediate assert unless the screen stops updating the
        // visible board through Compose effects. On a slow emulator the click can complete a bit
        // earlier than the board semantics update.
        val normalizedExpectedFen = normalizeFenForAssertion(expectedFen)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            currentBoardFen()?.let(::normalizeFenForAssertion) == normalizedExpectedFen
        }
    }

    private fun waitForNodeDisplayed(tag: String) {
        // The node can already exist in the semantics tree while layout/scroll is still settling.
        // Waiting for display removes a common source of flaky failures on slower emulators.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag(tag).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun currentBoardFen(): String? {
        return runCatching {
            composeRule.onNodeWithTag(InteractiveChessBoardTestTag)
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.StateDescription)
        }.getOrNull()
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
