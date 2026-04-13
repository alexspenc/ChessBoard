package com.example.chessboard.ui.screen

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.chessboard.MainActivity
import com.example.chessboard.boardmodel.InitialBoardFen
import com.example.chessboard.testing.fenStateDescriptionMatcher
import com.example.chessboard.ui.InteractiveChessBoardTestTag
import com.example.chessboard.ui.PositionEditorWhiteShortCastleTestTag
import com.example.chessboard.ui.PositionEditorClearBoardTestTag
import com.example.chessboard.ui.PositionEditorInitialPositionTestTag
import org.junit.Rule
import org.junit.Test

class PositionEditorScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun positionEditorScreen_clearBoardButtonUpdatesVisibleFen() {
        // Home content depends on async startup work, including reading persisted profile settings.
        // On a slow emulator the card may not exist yet when the test starts, so wait for it
        // before clicking instead of assuming the first frame is already stable.
        waitForTextDisplayed("Position Editor")
        composeRule.onNodeWithText("Position Editor").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(PositionEditorInitialPositionTestTag).performScrollTo().performClick()
        composeRule.waitForIdle()
        assertBoardFen(InitialBoardFen)

        composeRule.onNodeWithTag(PositionEditorClearBoardTestTag).performScrollTo().performClick()
        composeRule.waitForIdle()
        assertBoardFen("8/8/8/8/8/8/8/8 w - - 0 1")
    }

    @Test
    fun positionEditorScreen_castlingCheckboxUpdatesVisibleFen() {
        waitForTextDisplayed("Position Editor")
        composeRule.onNodeWithText("Position Editor").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(PositionEditorInitialPositionTestTag).performScrollTo().performClick()
        composeRule.waitForIdle()
        assertBoardFen(InitialBoardFen)

        composeRule.onNodeWithTag(PositionEditorWhiteShortCastleTestTag).performScrollTo().performClick()
        composeRule.waitForIdle()
        assertBoardFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w Qkq - 0 1")
    }


    private fun waitForTextDisplayed(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText(text).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun assertBoardFen(expectedFen: String) {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(InteractiveChessBoardTestTag).assert(
            fenStateDescriptionMatcher(expectedFen)
        )
    }
}
