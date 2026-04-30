package com.example.chessboard.ui.screen.analysis

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.chessboard.boardmodel.InitialBoardFen
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.testing.fenStateDescriptionMatcher
import com.example.chessboard.testing.normalizeFenForAssertion
import com.example.chessboard.ui.GameAnalysisContentTestTag
import com.example.chessboard.ui.GameAnalysisMoveControlsTestTag
import com.example.chessboard.ui.GameAnalysisSearchActionTestTag
import com.example.chessboard.ui.InteractiveChessBoardTestTag
import com.example.chessboard.ui.MoveTreeBoxTestTag
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GameAnalysisScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun gameAnalysisScreen_rendersStandardContent() {
        setAnalysisContent(onSearchByPositionClick = {})

        composeRule.onNodeWithText("Analyze Game").assertIsDisplayed()
        composeRule.onNodeWithTag(GameAnalysisContentTestTag).assertIsDisplayed()
        scrollToTag(GameAnalysisMoveControlsTestTag)
        composeRule.onNodeWithTag(GameAnalysisMoveControlsTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("♔").assertIsDisplayed()
        composeRule.onNodeWithText("♚").assertIsDisplayed()
        composeRule.onNodeWithTag(InteractiveChessBoardTestTag).assert(
            fenStateDescriptionMatcher(InitialBoardFen)
        )
        scrollToTag(MoveTreeBoxTestTag)
        composeRule.onNodeWithTag(MoveTreeBoxTestTag).assertIsDisplayed()
    }

    @Test
    fun gameAnalysisScreen_loadsFromFenInitialPosition() {
        setAnalysisContent(
            initialPosition = GameAnalysisInitialPosition.FromFen("4k3/8/8/8/8/8/8/4K3 b - -"),
            onSearchByPositionClick = {},
        )

        assertBoardFenEventually("4k3/8/8/8/8/8/8/4K3 b - - 0 1")
    }

    @Test
    fun gameAnalysisScreen_searchActionReturnsCurrentFen() {
        var searchedFen = ""

        setAnalysisContent(
            initialPosition = GameAnalysisInitialPosition.FromFen("4k3/8/8/8/8/8/8/4K3 b - -"),
            onSearchByPositionClick = { searchedFen = it },
        )

        composeRule.onNodeWithTag(GameAnalysisSearchActionTestTag).performClick()

        composeRule.runOnIdle {
            assertEquals("4k3/8/8/8/8/8/8/4K3 b - - 0 1", searchedFen)
        }
    }

    private fun setAnalysisContent(
        initialPosition: GameAnalysisInitialPosition = GameAnalysisInitialPosition.StartPosition,
        onSearchByPositionClick: (String) -> Unit,
    ) {
        val dbProvider = DatabaseProvider.createInstance(composeRule.activity)
        composeRule.setContent {
            ChessBoardTheme {
                GameAnalysisScreenContainer(
                    screenContext = ScreenContainerContext(inDbProvider = dbProvider),
                    initialPosition = initialPosition,
                    onSearchByPositionClick = onSearchByPositionClick,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun scrollToTag(tag: String) {
        composeRule.onNodeWithTag(GameAnalysisContentTestTag)
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
}
