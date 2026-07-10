package com.example.chessboard.ui.screen.linesExplorer

/**
 * Screen-level tests for Lines Explorer board controls.
 *
 * Keep here:
 * - focused Compose checks for prev/next control availability on the screen shell
 *
 * Do not add here:
 * - queue engine JVM tests
 * - database-backed container flows
 */
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.runtimecontext.linesexplorer.LinesExplorerRuntimeContext
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.LinesExplorerNextMoveTestTag
import com.example.chessboard.ui.LinesExplorerPreviousMoveTestTag
import com.example.chessboard.ui.boardanimation.BoardAnimationQueueController
import com.example.chessboard.ui.boardanimation.ResetBoardSceneAction
import com.example.chessboard.ui.boardrender.BoardRenderPiece
import com.example.chessboard.ui.boardrender.BoardRenderScene
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LinesExplorerBoardControlsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun linesExplorer_duringBoardAnimation_disablesPreviousButKeepsNextEnabled() {
        var previousClicks = 0
        var nextClicks = 0
        val lineController = LineController().apply {
            loadFromUciMoves(
                uciMoves = listOf("e2e4", "e7e5"),
                targetPly = 1,
            )
        }
        val boardAnimationController = BoardAnimationQueueController().apply {
            submit(
                ResetBoardSceneAction(
                    scene = BoardRenderScene(
                        pieces = listOf(BoardRenderPiece(letter = 'P', square = "e4")),
                        orientation = BoardOrientation.WHITE,
                    ),
                    renderPly = 1,
                )
            )
        }

        composeRule.setContent {
            ChessBoardTheme {
                LinesExplorerScreen(
                    state = LinesExplorerScreenState(
                        lineController = lineController,
                        parsedLines = emptyList(),
                        isLoading = false,
                        activeFilterState = LinesExplorerFilterState(),
                        selectedLineIdx = -1,
                        totalLinesCount = 0,
                        lineMistakeTotalsByLineId = emptyMap(),
                        sortMode = LinesExplorerRuntimeContext.LinesSortMode.DEFAULT,
                        currentPage = 1,
                        totalPages = 1,
                        simpleViewEnabled = false,
                        isBoardAnimating = true,
                    ),
                    boardAnimationController = boardAnimationController,
                    copyLinesPgnAction = CallbackWithCfg(canUse = false, onClick = {}),
                    createTrainingAction = CallbackWithCfg(canUse = false, onClick = {}),
                    deleteExplorerLinesAction = CallbackWithCfg(canUse = false, onClick = {}),
                    openPreviousPageAction = CallbackWithCfg(canUse = false, onClick = {}),
                    openNextPageAction = CallbackWithCfg(canUse = false, onClick = {}),
                    onPreviousMoveClick = { previousClicks += 1 },
                    onNextMoveClick = { nextClicks += 1 },
                    onSortModeChange = {},
                )
            }
        }

        composeRule.onNodeWithTag(LinesExplorerPreviousMoveTestTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(LinesExplorerNextMoveTestTag).assertIsEnabled().performClick()
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(0, previousClicks)
            assertEquals(1, nextClicks)
        }
    }
}
