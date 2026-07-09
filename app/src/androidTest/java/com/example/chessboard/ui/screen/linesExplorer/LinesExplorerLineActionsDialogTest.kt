package com.example.chessboard.ui.screen.linesExplorer

/**
 * File role: verifies the Lines Explorer line-actions dialog.
 * Allowed here:
 * - Compose tests for action callbacks exposed from the bottom action menu dialog
 * - focused coverage for clone, analyze, sort, and bulk-delete dialog actions
 * Not allowed here:
 * - screen-level navigation, paging, or database-backed line explorer flows
 * - board preview interaction outside the dialog action surface
 * Validation date: 2026-07-09
 */

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.chessboard.boardmodel.buildLineDraftFromSourceLine
import com.example.chessboard.entity.LineEntity
import com.example.chessboard.entity.SideMask
import com.example.chessboard.service.ParsedLine
import com.example.chessboard.service.buildMoveLabels
import com.example.chessboard.service.parsePgnMoves
import com.example.chessboard.ui.LinesExplorerAnalyzeActionTestTag
import com.example.chessboard.ui.LinesExplorerBulkDeleteActionTestTag
import com.example.chessboard.ui.LinesExplorerCloneActionTestTag
import com.example.chessboard.ui.LinesExplorerSortActionTestTag
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class LinesExplorerLineActionsDialogTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun linesExplorer_cloneLineButtonReturnsDraftWithFullPgn() {
        val sourceLine = LineEntity(
            id = 42L,
            event = "CaroKann",
            eco = "B12",
            pgn = "1. e2e4 c7c6 2. d2d4 d7d5 *",
            initialFen = "",
            sideMask = SideMask.WHITE
        )
        val parsedLine = ParsedLine(
            line = sourceLine,
            uciMoves = parsePgnMoves(sourceLine.pgn),
            moveLabels = buildMoveLabels(parsePgnMoves(sourceLine.pgn))
        )
        var clonedLine: LineEntity? = null

        composeRule.setContent {
            ChessBoardTheme {
                RenderLinesExplorerLineActionsDialog(
                    visible = true,
                    onDismiss = {},
                    resetAction = CallbackWithCfg(canUse = true, onClick = {}),
                    sortAction = CallbackWithCfg(canUse = false, onClick = {}),
                    analyzeAction = CallbackWithCfg(canUse = true, onClick = {}),
                    cloneAction = CallbackWithCfg(
                        canUse = true,
                        onClick = { clonedLine = parsedLine.line },
                    ),
                    createTrainingAction = CallbackWithCfg(canUse = false, onClick = {}),
                    copyLinesPgnAction = CallbackWithCfg(canUse = false, onClick = {}),
                    deleteExplorerLinesAction = CallbackWithCfg(canUse = false, onClick = {}),
                )
            }
        }

        composeRule.onNodeWithTag(LinesExplorerCloneActionTestTag).performClick()
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertNotNull(clonedLine)
            assertEquals(sourceLine.id, clonedLine!!.id)
            assertEquals(sourceLine.event, clonedLine!!.event)
            assertEquals(sourceLine.eco, clonedLine!!.eco)
            assertEquals(sourceLine.pgn, clonedLine!!.pgn)
            assertEquals(sourceLine.sideMask, clonedLine!!.sideMask)

            val clonedDraft = buildLineDraftFromSourceLine(clonedLine!!)
            assertEquals(0L, clonedDraft.line.id)
            assertEquals(sourceLine.pgn, clonedDraft.line.pgn)
        }
    }

    @Test
    fun linesExplorer_analyzeLineButtonInvokesCallback() {
        val sourceLine = LineEntity(
            id = 43L,
            event = "Italian Line",
            eco = "C50",
            pgn = "1. e2e4 e7e5 2. g1f3 *",
            initialFen = "",
            sideMask = SideMask.WHITE
        )
        var analyzeClicks = 0

        composeRule.setContent {
            ChessBoardTheme {
                RenderLinesExplorerLineActionsDialog(
                    visible = true,
                    onDismiss = {},
                    resetAction = CallbackWithCfg(canUse = true, onClick = {}),
                    sortAction = CallbackWithCfg(canUse = false, onClick = {}),
                    analyzeAction = CallbackWithCfg(
                        canUse = true,
                        onClick = { analyzeClicks += 1 },
                    ),
                    cloneAction = CallbackWithCfg(canUse = true, onClick = {}),
                    createTrainingAction = CallbackWithCfg(canUse = false, onClick = {}),
                    copyLinesPgnAction = CallbackWithCfg(canUse = false, onClick = {}),
                    deleteExplorerLinesAction = CallbackWithCfg(canUse = false, onClick = {}),
                )
            }
        }

        composeRule.onNodeWithTag(LinesExplorerAnalyzeActionTestTag).performClick()
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(1, analyzeClicks)
        }
    }

    @Test
    fun linesExplorer_deleteLinesButtonInvokesCallback() {
        var deleteClicks = 0

        composeRule.setContent {
            ChessBoardTheme {
                RenderLinesExplorerLineActionsDialog(
                    visible = true,
                    onDismiss = {},
                    resetAction = CallbackWithCfg(canUse = false, onClick = {}),
                    sortAction = CallbackWithCfg(canUse = false, onClick = {}),
                    analyzeAction = CallbackWithCfg(canUse = false, onClick = {}),
                    cloneAction = CallbackWithCfg(canUse = false, onClick = {}),
                    createTrainingAction = CallbackWithCfg(canUse = false, onClick = {}),
                    copyLinesPgnAction = CallbackWithCfg(canUse = false, onClick = {}),
                    deleteExplorerLinesAction = CallbackWithCfg(
                        canUse = true,
                        onClick = { deleteClicks += 1 },
                    ),
                )
            }
        }

        composeRule.onNodeWithTag(LinesExplorerBulkDeleteActionTestTag).performClick()
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(1, deleteClicks)
        }
    }

    @Test
    fun linesExplorer_sortLinesButtonInvokesCallback() {
        var sortClicks = 0

        composeRule.setContent {
            ChessBoardTheme {
                RenderLinesExplorerLineActionsDialog(
                    visible = true,
                    onDismiss = {},
                    resetAction = CallbackWithCfg(canUse = false, onClick = {}),
                    sortAction = CallbackWithCfg(
                        canUse = true,
                        onClick = { sortClicks += 1 },
                    ),
                    analyzeAction = CallbackWithCfg(canUse = false, onClick = {}),
                    cloneAction = CallbackWithCfg(canUse = false, onClick = {}),
                    createTrainingAction = CallbackWithCfg(canUse = false, onClick = {}),
                    copyLinesPgnAction = CallbackWithCfg(canUse = false, onClick = {}),
                    deleteExplorerLinesAction = CallbackWithCfg(canUse = false, onClick = {}),
                )
            }
        }

        composeRule.onNodeWithTag(LinesExplorerSortActionTestTag).performClick()
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(1, sortClicks)
        }
    }
}
