package com.example.chessboard.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.ui.InteractiveChessBoardTestTag
import com.example.chessboard.ui.PositionEditorBoardWithCoordinates
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Rule
import org.junit.Test

class PositionEditorBoardTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun positionEditorBoard_updatesVisibleFenWhenPreviewPositionChanges() {
        val gameController = GameController()

        composeRule.setContent {
            ChessBoardTheme {
                PositionEditorBoardHost(gameController = gameController)
            }
        }

        composeRule.runOnIdle {
            gameController.loadPreviewFen("8/8/8/8/8/8/8/4K3 w - - 0 1")
        }

        assertBoardFen("8/8/8/8/8/8/8/4K3 w - - 0 1")

        composeRule.runOnIdle {
            gameController.loadPreviewFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1")
        }

        assertBoardFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1")
    }

    private fun assertBoardFen(expectedFen: String) {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(InteractiveChessBoardTestTag).assert(
            SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.StateDescription,
                expectedFen
            )
        )
    }
}

@Composable
private fun PositionEditorBoardHost(gameController: GameController) {
    val boardState = gameController.boardState

    key(boardState) {
        PositionEditorBoardWithCoordinates(
            gameController = gameController,
            onSquareClick = {},
            onPieceMove = { _, _ -> },
            modifier = Modifier
        )
    }
}
