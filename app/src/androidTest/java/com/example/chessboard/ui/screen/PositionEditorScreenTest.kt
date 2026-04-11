package com.example.chessboard.ui.screen

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.chessboard.MainActivity
import com.example.chessboard.boardmodel.InitialBoardFen
import com.example.chessboard.testing.fenStateDescriptionMatcher
import com.example.chessboard.ui.InteractiveChessBoardTestTag
import com.example.chessboard.ui.PositionEditorClearBoardTestTag
import com.example.chessboard.ui.PositionEditorInitialPositionTestTag
import org.junit.Rule
import org.junit.Test

class PositionEditorScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun positionEditorScreen_clearBoardButtonUpdatesVisibleFen() {
        composeRule.onNodeWithText("Position Editor").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(PositionEditorInitialPositionTestTag).performScrollTo().performClick()
        composeRule.waitForIdle()
        assertBoardFen(InitialBoardFen)

        composeRule.onNodeWithTag(PositionEditorClearBoardTestTag).performScrollTo().performClick()
        composeRule.waitForIdle()
        assertBoardFen("8/8/8/8/8/8/8/8 w - - 0 1")
    }

    private fun assertBoardFen(expectedFen: String) {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(InteractiveChessBoardTestTag).assert(
            fenStateDescriptionMatcher(expectedFen)
        )
    }
}
