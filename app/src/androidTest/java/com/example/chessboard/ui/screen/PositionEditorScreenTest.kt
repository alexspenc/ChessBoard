package com.example.chessboard.ui.screen

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.chessboard.MainActivity
import com.example.chessboard.ui.InteractiveChessBoardTestTag
import org.junit.Rule
import org.junit.Test

class PositionEditorScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun positionEditorScreen_clearBoardButtonUpdatesVisibleFen() {
        composeRule.onNodeWithText("Position Editor").performClick()

        composeRule.onNodeWithText("Initial position").performClick()
        assertBoardFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")

        composeRule.onNodeWithText("Clear board").performClick()
        assertBoardFen("8/8/8/8/8/8/8/8 w - - 0 1")
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
