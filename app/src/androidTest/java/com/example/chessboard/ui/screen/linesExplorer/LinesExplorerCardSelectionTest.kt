package com.example.chessboard.ui.screen.linesExplorer

/**
 * File role: verifies focused LineBlock selection and move-preview behavior.
 * Allowed here:
 * - Compose UI tests for LineBlock card selection and move-tree preview interaction
 * - assertions that LineBlock callbacks drive the supplied board controller correctly
 * Not allowed here:
 * - full lines-explorer database flows or unrelated navigation behavior
 * - service-layer import, save, or Room persistence tests
 * Validation date: 2026-06-16
 */

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.entity.LineEntity
import com.example.chessboard.entity.SideMask
import com.example.chessboard.service.ParsedLine
import com.example.chessboard.service.buildMoveLabels
import com.example.chessboard.service.parsePgnMoves
import com.example.chessboard.testing.normalizeFenForAssertion
import com.example.chessboard.ui.moveChipTestTag
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LinesExplorerCardSelectionTest {
    private val lineBlockTestTag = "line-block-initial-ply-selection"

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun lineBlock_clickingCardSelectsLineAtInitialPly() {
        val line = LineEntity(
            id = 7L,
            event = "Sicilian Defense",
            eco = "B20",
            pgn = "1. e2e4 c7c5 2. g1f3 *",
            initialFen = "",
            sideMask = SideMask.WHITE
        )
        val parsedLine = ParsedLine(
            line = line,
            uciMoves = parsePgnMoves(line.pgn),
            moveLabels = buildMoveLabels(parsePgnMoves(line.pgn))
        )
        var selectedPly = -1

        composeRule.setContent {
            ChessBoardTheme {
                LineBlock(
                    parsedLine = parsedLine,
                    isSelected = false,
                    lineController = LineController(),
                    onSelectClick = { selectedPly = 0 },
                    onMovePlyClick = {},
                    modifier = Modifier.testTag(lineBlockTestTag),
                )
            }
        }

        composeRule.onNodeWithTag(lineBlockTestTag).performTouchInput {
            click(Offset(24f, 24f))
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(0, selectedPly)
        }
    }

    @Test
    fun lineBlock_clickingPromotionMoveReplaysPromotionOnBoard() {
        val line = LineEntity(
            id = 8L,
            event = "Promotion Line",
            eco = "",
            pgn = PromotionStoredPgn,
            initialFen = "",
            sideMask = SideMask.WHITE
        )
        val uciMoves = parsePgnMoves(line.pgn)
        val moveLabels = buildMoveLabels(uciMoves)
        val parsedLine = ParsedLine(
            line = line,
            uciMoves = uciMoves,
            moveLabels = moveLabels
        )
        val lineController = LineController().apply {
            loadFromUciMoves(uciMoves, targetPly = 0)
        }
        val promotionMoveLabel = moveLabels.last()

        composeRule.setContent {
            ChessBoardTheme {
                LineBlock(
                    parsedLine = parsedLine,
                    isSelected = true,
                    lineController = lineController,
                    onSelectClick = {},
                    onMovePlyClick = { ply ->
                        lineController.loadFromUciMoves(uciMoves, targetPly = ply)
                    },
                    modifier = Modifier.testTag(lineBlockTestTag),
                )
            }
        }

        composeRule.onNodeWithTag(moveChipTestTag(promotionMoveLabel)).performClick()
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(
                normalizeFenForAssertion(AfterPromotionFen),
                normalizeFenForAssertion(lineController.getFen())
            )
        }
    }

    private companion object {
        const val PromotionStoredPgn =
            "1. e2e4 c7c5 2. e4e5 d7d6 3. e5e6 b8c6 4. e6f7 e8d7 5. f7g8q *"
        const val AfterPromotionFen =
            "r1bq1bQr/pp1kp1pp/2np4/2p5/8/8/PPPP1PPP/RNBQKBNR b KQ - 0 5"
    }
}
