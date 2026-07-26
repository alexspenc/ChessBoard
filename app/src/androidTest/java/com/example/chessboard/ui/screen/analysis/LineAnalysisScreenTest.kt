package com.example.chessboard.ui.screen.analysis

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import com.example.chessboard.boardmodel.InitialBoardFen
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.testing.fenStateDescriptionMatcher
import com.example.chessboard.testing.normalizeFenForAssertion
import com.example.chessboard.ui.LineAnalysisContentTestTag
import com.example.chessboard.ui.LineAnalysisMoveControlsTestTag
import com.example.chessboard.ui.LineAnalysisNextMoveTestTag
import com.example.chessboard.ui.LineAnalysisSearchActionTestTag
import com.example.chessboard.ui.InteractiveChessBoardTestTag
import com.example.chessboard.ui.MoveTreeBoxTestTag
import com.example.chessboard.ui.boardanimation.DefaultBoardMoveAnimationDurationMs
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LineAnalysisScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun lineAnalysisScreen_rendersStandardContent() {
        setAnalysisContent(onSearchByPositionClick = {})

        composeRule.onNodeWithText("Analyze Line").assertIsDisplayed()
        composeRule.onNodeWithTag(LineAnalysisContentTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(LineAnalysisMoveControlsTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("White").assertIsDisplayed()
        composeRule.onNodeWithText("Black").assertIsDisplayed()
        composeRule.onNodeWithTag(InteractiveChessBoardTestTag).assert(
            fenStateDescriptionMatcher(InitialBoardFen)
        )
        scrollToTag(MoveTreeBoxTestTag)
        composeRule.onNodeWithTag(MoveTreeBoxTestTag).assertIsDisplayed()
    }

    @Test
    fun lineAnalysisScreen_loadsFromFenInitialPosition() {
        setAnalysisContent(
            initialPosition = LineAnalysisInitialPosition.FromFen("4k3/8/8/8/8/8/8/4K3 b - -"),
            onSearchByPositionClick = {},
        )

        assertBoardFenEventually("4k3/8/8/8/8/8/8/4K3 b - - 0 1")
    }

    @Test
    fun lineAnalysisScreen_searchActionReturnsCurrentFen() {
        var searchedFen = ""

        setAnalysisContent(
            initialPosition = LineAnalysisInitialPosition.FromFen("4k3/8/8/8/8/8/8/4K3 b - -"),
            onSearchByPositionClick = { searchedFen = it },
        )

        composeRule.onNodeWithTag(LineAnalysisSearchActionTestTag).performClick()

        composeRule.runOnIdle {
            assertEquals("4k3/8/8/8/8/8/8/4K3 b - - 0 1", searchedFen)
        }
    }

    @Test
    fun lineAnalysisScreen_animatedBoardRemainsInteractive() {
        setAnalysisContent(onSearchByPositionClick = {})

        val boardNode = composeRule.onNodeWithTag(InteractiveChessBoardTestTag)
        boardNode.performTouchInput {
            val squareSize = width / 8f
            click(squareCenter(file = 4, row = 6, squareSize = squareSize))
        }
        boardNode.performTouchInput {
            val squareSize = width / 8f
            click(squareCenter(file = 4, row = 4, squareSize = squareSize))
        }

        assertBoardFenEventually(
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
        )
    }

    @Test
    fun lineAnalysisScreen_nextMoveStillChangesBoardPosition() {
        setAnalysisContent(
            initialPosition = LineAnalysisInitialPosition.FromLineLine(
                uciMoves = listOf("e2e4", "e7e5"),
                initialPly = 0,
            ),
            onSearchByPositionClick = {},
        )

        composeRule.onNodeWithTag(LineAnalysisNextMoveTestTag).performClick()

        assertBoardFenEventually(
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
        )
    }

    @Test
    fun lineAnalysisScreen_nextMoveBlocksBoardInputUntilPlaybackCompletes() {
        setAnalysisContent(
            initialPosition = LineAnalysisInitialPosition.FromLineLine(
                uciMoves = listOf("e2e4", "e7e5"),
                initialPly = 0,
            ),
            onSearchByPositionClick = {},
        )

        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.onNodeWithTag(LineAnalysisNextMoveTestTag).performClick()
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.mainClock.advanceTimeByFrame()

            performBoardTapMove(
                fromFile = 4,
                fromRow = 1,
                toFile = 4,
                toRow = 3,
            )
            composeRule.mainClock.advanceTimeByFrame()
            assertBoardFenEventually(
                "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
            )

            composeRule.mainClock.advanceTimeBy(DefaultBoardMoveAnimationDurationMs.toLong())
            composeRule.mainClock.advanceTimeByFrame()

            performBoardTapMove(
                fromFile = 4,
                fromRow = 1,
                toFile = 4,
                toRow = 3,
            )
            composeRule.mainClock.advanceTimeByFrame()
            assertBoardFenEventually(
                "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
            )
        } finally {
            composeRule.mainClock.autoAdvance = true
            composeRule.waitForIdle()
        }
    }

    @Test
    fun lineAnalysisScreen_userMoveBlocksBoardInputUntilPlaybackCompletes() {
        setAnalysisContent(onSearchByPositionClick = {})

        composeRule.mainClock.autoAdvance = false
        try {
            performBoardTapMove(
                fromFile = 4,
                fromRow = 6,
                toFile = 4,
                toRow = 4,
            )
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.mainClock.advanceTimeByFrame()

            performBoardTapMove(
                fromFile = 4,
                fromRow = 1,
                toFile = 4,
                toRow = 3,
            )
            composeRule.mainClock.advanceTimeByFrame()
            assertBoardFenEventually(
                "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
            )

            composeRule.mainClock.advanceTimeBy(DefaultBoardMoveAnimationDurationMs.toLong())
            composeRule.mainClock.advanceTimeByFrame()

            performBoardTapMove(
                fromFile = 4,
                fromRow = 1,
                toFile = 4,
                toRow = 3,
            )
            composeRule.mainClock.advanceTimeByFrame()
            assertBoardFenEventually(
                "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
            )
        } finally {
            composeRule.mainClock.autoAdvance = true
            composeRule.waitForIdle()
        }
    }

    private fun setAnalysisContent(
        initialPosition: LineAnalysisInitialPosition = LineAnalysisInitialPosition.StartPosition,
        onSearchByPositionClick: (String) -> Unit,
    ) {
        val dbProvider = DatabaseProvider.createInstance(composeRule.activity)
        composeRule.setContent {
            ChessBoardTheme {
                LineAnalysisScreenContainer(
                    screenContext = ScreenContainerContext(inDbProvider = dbProvider),
                    initialPosition = initialPosition,
                    onSearchByPositionClick = onSearchByPositionClick,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun scrollToTag(tag: String) {
        composeRule.onNodeWithTag(LineAnalysisContentTestTag)
            .performScrollToNode(hasTestTag(tag))
        composeRule.waitForIdle()
    }

    private fun assertBoardFenEventually(expectedFen: String) {
        val normalizedExpectedFen = normalizeFenForAssertion(expectedFen)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            currentBoardFen()?.let(::normalizeFenForAssertion) == normalizedExpectedFen
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

    private fun performBoardTapMove(
        fromFile: Int,
        fromRow: Int,
        toFile: Int,
        toRow: Int,
    ) {
        val boardNode = composeRule.onNodeWithTag(InteractiveChessBoardTestTag)
        boardNode.performTouchInput {
            val squareSize = width / 8f
            click(squareCenter(file = fromFile, row = fromRow, squareSize = squareSize))
        }
        boardNode.performTouchInput {
            val squareSize = width / 8f
            click(squareCenter(file = toFile, row = toRow, squareSize = squareSize))
        }
    }

    private fun squareCenter(file: Int, row: Int, squareSize: Float): Offset {
        return Offset(
            x = file * squareSize + squareSize / 2f,
            y = row * squareSize + squareSize / 2f,
        )
    }
}
