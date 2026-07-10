package com.example.chessboard.ui.screen.training

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.service.buildMoveLabels
import com.example.chessboard.service.parsePgnMoves
import com.example.chessboard.ui.MoveTreeBoxTestTag
import com.example.chessboard.ui.boardanimation.BoardAnimationQueueController
import com.example.chessboard.ui.moveChipTestTag
import com.example.chessboard.ui.screen.training.common.ParsedTrainingEditorLine
import com.example.chessboard.ui.screen.training.common.TrainingEditorLineSection
import com.example.chessboard.ui.screen.training.common.TrainingEditorLineSectionActions
import com.example.chessboard.ui.screen.training.common.TrainingEditorLineSectionState
import com.example.chessboard.ui.screen.training.common.TrainingLineEditorItem
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TrainingEditorLineSectionTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun trainingEditorLineSection_rendersLineMetaAndParsedMoves() {
        val line = createSampleLine()
        val parsedLine = createParsedSampleLine(line)

        setTrainingEditorLineSectionContent(
            state = createSectionState(
                line = line,
                parsedLine = parsedLine,
                isSelected = false
            )
        )

        composeRule.onNodeWithText("Italian Line").assertIsDisplayed()
        composeRule.onNodeWithText("C50").assertIsDisplayed()
        composeRule.onNodeWithText("3").assertIsDisplayed()
        composeRule.onNodeWithTag(MoveTreeBoxTestTag).assertIsDisplayed()
    }

    @Test
    fun trainingEditorLineSection_weightButtonsInvokeCallbacks() {
        var decreaseClicks = 0
        var increaseClicks = 0

        setTrainingEditorLineSectionContent(
            actions = createSectionActions(
                onDecreaseWeightClick = { decreaseClicks += 1 },
                onIncreaseWeightClick = { increaseClicks += 1 }
            )
        )

        composeRule.onNodeWithContentDescription("Decrease").performClick()
        composeRule.onNodeWithContentDescription("Increase").performClick()

        composeRule.runOnIdle {
            assertEquals(1, decreaseClicks)
            assertEquals(1, increaseClicks)
        }
    }

    @Test
    fun trainingEditorLineSection_hidesOptionalSectionsWhenDataIsMissing() {
        setTrainingEditorLineSectionContent(
            state = createSectionState(parsedLine = null, isSelected = false),
        )

        composeRule.onAllNodesWithTag(MoveTreeBoxTestTag).assertCountEquals(0)
    }

    @Test
    fun trainingEditorLineSection_clickingUnselectedTitleInvokesOnSelect() {
        var selectClicks = 0

        setTrainingEditorLineSectionContent(
            state = createSectionState(isSelected = false),
            actions = createSectionActions(
                onSelect = { selectClicks += 1 }
            )
        )

        composeRule.onNodeWithText("Italian Line").performClick()

        composeRule.runOnIdle {
            assertEquals(1, selectClicks)
        }
    }

    @Test
    fun trainingEditorLineSection_clickingMoveInUnselectedTreeInvokesOnMovePlyClick() {
        var movePlyClicks = 0

        setTrainingEditorLineSectionContent(
            state = createSectionState(isSelected = false),
            actions = createSectionActions(
                onMovePlyClick = { movePlyClicks += 1 }
            )
        )

        composeRule.onNodeWithTag(MoveTreeBoxTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(moveChipTestTag("e4")).performClick()

        composeRule.runOnIdle {
            assertEquals(1, movePlyClicks)
        }
    }

    @Test
    fun trainingEditorLineSection_clickingMoveInSelectedTreeInvokesOnMovePlyClick() {
        var movePlyClicks = 0

        setTrainingEditorLineSectionContent(
            state = createSectionState(isSelected = true),
            actions = createSectionActions(
                onMovePlyClick = { movePlyClicks += 1 }
            )
        )

        composeRule.onNodeWithTag(MoveTreeBoxTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(moveChipTestTag("e4")).performClick()

        composeRule.runOnIdle {
            assertEquals(1, movePlyClicks)
        }
    }

    private fun setTrainingEditorLineSectionContent(
        state: TrainingEditorLineSectionState = createSectionState(),
        actions: TrainingEditorLineSectionActions = createSectionActions(),
    ) {
        composeRule.setContent {
            ChessBoardTheme {
                TrainingEditorLineSection(
                    state = state,
                    actions = actions,
                )
            }
        }
    }

    private fun createSectionState(
        line: TrainingLineEditorItem = createSampleLine(),
        parsedLine: ParsedTrainingEditorLine? = createParsedSampleLine(line),
        isSelected: Boolean = false,
    ): TrainingEditorLineSectionState {
        return TrainingEditorLineSectionState(
            line = line,
            parsedLine = parsedLine,
            isSelected = isSelected,
            lineController = LineController(),
            boardAnimationController = BoardAnimationQueueController(),
        )
    }

    private fun createSectionActions(
        onDecreaseWeightClick: () -> Unit = {},
        onIncreaseWeightClick: () -> Unit = {},
        onSelect: () -> Unit = {},
        onMovePlyClick: (Int) -> Unit = {},
    ): TrainingEditorLineSectionActions {
        return TrainingEditorLineSectionActions(
            onDecreaseWeightClick = onDecreaseWeightClick,
            onIncreaseWeightClick = onIncreaseWeightClick,
            onSelect = onSelect,
            onMovePlyClick = onMovePlyClick
        )
    }

    private fun createSampleLine(): TrainingLineEditorItem {
        return TrainingLineEditorItem(
            lineId = 1L,
            title = "Italian Line",
            weight = 3,
            eco = "C50",
            pgn = "1. e2e4 e7e5 2. g1f3 *"
        )
    }

    private fun createParsedSampleLine(
        line: TrainingLineEditorItem
    ): ParsedTrainingEditorLine {
        val uciMoves = parsePgnMoves(line.pgn)
        val moveLabels = buildMoveLabels(uciMoves)

        return ParsedTrainingEditorLine(
            uciMoves = uciMoves,
            moveLabels = moveLabels
        )
    }
}
