package com.example.chessboard.ui.screen.trainSingleLine

import androidx.activity.ComponentActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.testing.fenStateDescriptionMatcher
import com.example.chessboard.ui.InteractiveChessBoardTestTag
import com.example.chessboard.ui.boardanimation.BoardAnimationQueueController
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Rule
import org.junit.Test

class TrainingBoardSectionTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun trainingBoardSection_updatesVisibleBoardWhenControllerPositionChanges() {
        composeRule.setContent {
            ChessBoardTheme {
                val lineController = remember { LineController() }
                val boardAnimationController = remember { BoardAnimationQueueController() }
                val targetPly = remember { mutableStateOf(0) }

                LaunchedEffect(targetPly.value) {
                    lineController.loadFromUciMoves(listOf("e2e4", "e7e5"), targetPly = targetPly.value)
                }

                TrainingBoardSection(
                    lineController = lineController,
                    boardAnimationController = boardAnimationController,
                    interactionEnabled = false,
                )

                LaunchedEffect(Unit) {
                    targetPly.value = 1
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag(InteractiveChessBoardTestTag).assert(
            fenStateDescriptionMatcher(
                "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
            )
        )
    }
}
