package com.example.chessboard.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.ui.MoveTreeBoxTestTag
import com.example.chessboard.ui.moveChipTestTag
import com.example.chessboard.ui.moveTreeRowTestTag
import com.example.chessboard.ui.components.LineMoveTreeSection
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MoveTreeBoxTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun moveTreeBox_rendersSeparateRowForNestedSubVariation() {
        val importedUciLines = listOf(
            listOf("d2d4", "d7d5", "g1f3", "g8f6", "e2e3", "e7e6", "f1d3", "f8e7"),
            listOf("d2d4", "d7d5", "g1f3", "c8g4", "b1d2", "e7e6", "e2e3", "g8f6", "h2h3"),
            listOf("d2d4", "d7d5", "g1f3", "c8g4", "b1d2", "e7e6", "f3e5", "g8f6", "h2h3")
        )

        composeRule.setContent {
            ChessBoardTheme {
                LineMoveTreeSection(
                    importedUciLines = importedUciLines,
                    lineController = LineController()
                )
            }
        }

        composeRule.onNodeWithTag(MoveTreeBoxTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(moveTreeRowTestTag(0), useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag(moveTreeRowTestTag(1), useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag(moveTreeRowTestTag(2), useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Ne5", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun moveTreeBox_clickingMoveInvokesSelectionWithBackingLineAndTargetPly() {
        val importedUciLines = listOf(
            listOf("e2e4", "c7c6")
        )
        var selectedBackingLine: List<String>? = null
        var selectedTargetPly: Int? = null

        composeRule.setContent {
            ChessBoardTheme {
                LineMoveTreeSection(
                    importedUciLines = importedUciLines,
                    lineController = LineController(),
                    onMoveSelected = { backingLine, targetPly ->
                        selectedBackingLine = backingLine
                        selectedTargetPly = targetPly
                    }
                )
            }
        }

        composeRule.onNodeWithTag(moveChipTestTag("c6")).performClick()

        composeRule.runOnIdle {
            assertEquals(listOf("e2e4", "c7c6"), selectedBackingLine)
            assertEquals(2, selectedTargetPly)
        }
    }
}
