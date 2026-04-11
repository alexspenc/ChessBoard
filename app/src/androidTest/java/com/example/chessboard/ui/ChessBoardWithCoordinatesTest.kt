package com.example.chessboard.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.testing.normalizeFenForAssertion
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ChessBoardWithCoordinatesTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun chessBoardWithCoordinates_allowsTapToMovePiece() {
        val gameController = GameController()

        composeRule.setContent {
            ChessBoardTheme {
                ChessBoardWithCoordinates(
                    gameController = gameController,
                    modifier = androidx.compose.ui.Modifier.size(320.dp)
                )
            }
        }

        val boardNode = composeRule.onNodeWithTag(InteractiveChessBoardTestTag)

        boardNode.performTouchInput {
            val squareSize = width / 8f
            click(squareCenter(file = 4, row = 6, squareSize = squareSize)) // e2
        }
        boardNode.performTouchInput {
            val squareSize = width / 8f
            click(squareCenter(file = 4, row = 4, squareSize = squareSize)) // e4
        }

        composeRule.runOnIdle {
            assertEquals(
                "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
                normalizeFenForAssertion(gameController.getFen())
            )
        }
    }

    @Test
    fun chessBoardWithCoordinates_allowsDragToMovePiece() {
        val gameController = GameController()

        composeRule.setContent {
            ChessBoardTheme {
                ChessBoardWithCoordinates(
                    gameController = gameController,
                    modifier = androidx.compose.ui.Modifier.size(320.dp)
                )
            }
        }

        val boardNode = composeRule.onNodeWithTag(InteractiveChessBoardTestTag)

        boardNode.performTouchInput {
            val squareSize = width / 8f
            val from = squareCenter(file = 4, row = 6, squareSize = squareSize) // e2
            val to = squareCenter(file = 4, row = 4, squareSize = squareSize) // e4
            down(from)
            moveTo(to)
            up()
        }

        composeRule.runOnIdle {
            assertEquals(
                "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
                normalizeFenForAssertion(gameController.getFen())
            )
        }
    }

    private fun squareCenter(file: Int, row: Int, squareSize: Float): Offset {
        return Offset(
            x = file * squareSize + squareSize / 2f,
            y = row * squareSize + squareSize / 2f
        )
    }
}
